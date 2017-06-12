package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.products.ProductTestUtils.product;
import static com.commercetools.sync.products.ProductTestUtils.productDraftBuilder;
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductUpdateActionsBuilderTest {

    @Test
    public void of_expectSingleton() {
        ProductUpdateActionsBuilder productUpdateActionsBuilder = ProductUpdateActionsBuilder.of();

        assertThat(productUpdateActionsBuilder).isNotNull();

        ProductUpdateActionsBuilder productUpdateActionsBuilder1 = ProductUpdateActionsBuilder.of();

        assertThat(productUpdateActionsBuilder1).isSameAs(productUpdateActionsBuilder);
    }

    @Test
    public void buildActions_emptyForIdentical() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of();
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(true, true);
        ProductDraft productDraft = productDraft("product.json", productType(), syncOptions);

        List<UpdateAction<Product>> updateActions =
            updateActionsBuilder.buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildActions_nonEmptyForDifferent_comparingStaged() {
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(true, true);
        ProductDraft productDraft = productDraft("product-changed.json", productType(), syncOptions);

        List<UpdateAction<Product>> updateActions = ProductUpdateActionsBuilder.of()
            .buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEqualTo(expectedUpdateActions(product, syncOptions, productDraft));
    }

    @Test
    public void buildActions_nonEmptyForDifferent_comparingCurrent() {
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(true, false);
        ProductDraft productDraft = productDraft("product-changed.json", productType(), syncOptions);

        List<UpdateAction<Product>> updateActions = ProductUpdateActionsBuilder.of()
            .buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEqualTo(expectedUpdateActions(product, syncOptions, productDraft));
    }

    @SuppressWarnings("ConstantConditions")
    private List<UpdateAction<Product>> expectedUpdateActions(final Product product,
                                                              final ProductSyncOptions syncOptions,
                                                              final ProductDraft productDraft) {
        ProductData productData = masterData(product, syncOptions);
        boolean updateStaged = syncOptions.shouldUpdateStaged();
        List<UpdateAction<Product>> result = new ArrayList<>();
        result.addAll(asList(
            ChangeName.of(productDraft.getName(), updateStaged),
            ChangeSlug.of(productDraft.getSlug(), updateStaged),
            SetDescription.of(productDraft.getDescription(), updateStaged),
            SetSearchKeywords.of(productDraft.getSearchKeywords(), updateStaged),
            SetMetaDescription.of(productDraft.getMetaDescription()),
            SetMetaKeywords.of(productDraft.getMetaKeywords()),
            SetMetaTitle.of(productDraft.getMetaTitle()),
            SetSku.of(productData.getMasterVariant().getId(),
                productDraft.getMasterVariant().getSku(), updateStaged)
        ));
        result.addAll(expectedCategoryUpdateActions(productDraft, productData, updateStaged));
        return result;
    }

    private static ProductDraft productDraft(final String resourcePath, final ProductType productType,
                                             final ProductSyncOptions syncOptions) {
        return productDraftBuilder(resourcePath, productType, syncOptions).build();
    }

    @SuppressWarnings("ConstantConditions")
    private List<UpdateAction<Product>> expectedCategoryUpdateActions(final ProductDraft productDraft,
                                                                      final ProductData productData,
                                                                      final boolean updateStaged) {
        Reference<Category> removed = productData.getCategories().stream()
            .filter(c -> c.getId().startsWith("1")).findFirst().get();
        Reference<Category> unsetHint = productData.getCategories().stream()
            .filter(c -> c.getId().startsWith("3")).findFirst().get();
        Reference<Category> updatedHint = productDraft.getCategories().stream()
            .filter(c -> c.getId().startsWith("4")).findFirst().get();
        Reference<Category> added = productDraft.getCategories().stream()
            .filter(c -> c.getId().startsWith("5")).findFirst().get();
        AddToCategory addToCategory = AddToCategory.of(added, updateStaged);
        RemoveFromCategory removeFromCategory = RemoveFromCategory.of(removed, updateStaged);

        List<UpdateAction<Product>> result = new ArrayList<>();
        result.add(addToCategory);
        result.add(removeFromCategory);
        result.addAll(expectedSetCategoryOrderHints(productDraft,
            unsetHint.getId(), updatedHint.getId(), added.getId(), updateStaged));
        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private List<SetCategoryOrderHint> expectedSetCategoryOrderHints(final ProductDraft productDraft,
                                                                     final String unsetHintId,
                                                                     final String updatedHintId,
                                                                     final String addedHintId,
                                                                     final boolean updateStaged) {
        CategoryOrderHints draftHints = productDraft.getCategoryOrderHints();
        return asList(
            SetCategoryOrderHint.of(unsetHintId, null, updateStaged),
            SetCategoryOrderHint.of(updatedHintId, draftHints.get(updatedHintId), updateStaged),
            SetCategoryOrderHint.of(addedHintId, draftHints.get(addedHintId), updateStaged)
        );
    }
}
