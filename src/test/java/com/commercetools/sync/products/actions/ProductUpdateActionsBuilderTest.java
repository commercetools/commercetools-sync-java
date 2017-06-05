package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.products.ProductTestUtils.product;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static com.commercetools.sync.products.actions.ProductUpdateActionsBuilder.masterData;
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
        ProductDraft productDraft = productDraft("product.json", productType(), null, syncOptions);

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildActions_nonEmptyForDifferent_comparingStaged() {
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(true, true);
        ProductDraft productDraft = productDraft("product-changed.json", productType(), null, syncOptions);

        List<UpdateAction<Product>> updateActions = ProductUpdateActionsBuilder.of()
                .buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEqualTo(expectedUpdateActions(product, syncOptions, productDraft));
    }

    @Test
    public void buildActions_nonEmptyForDifferent_comparingCurrent() {
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(true, false);
        ProductDraft productDraft = productDraft("product-changed.json", productType(), null, syncOptions);

        List<UpdateAction<Product>> updateActions = ProductUpdateActionsBuilder.of()
                .buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEqualTo(expectedUpdateActions(product, syncOptions, productDraft));
    }

    @SuppressWarnings("ConstantConditions")
    private List<UpdateAction<Product>> expectedUpdateActions(final Product product, final ProductSyncOptions syncOptions, final ProductDraft productDraft) {
        return asList(
                ChangeName.of(productDraft.getName(), syncOptions.isUpdateStaged()),
                ChangeSlug.of(productDraft.getSlug(), syncOptions.isUpdateStaged()),
                SetMetaDescription.of(productDraft.getMetaDescription()),
                SetMetaKeywords.of(productDraft.getMetaKeywords()),
                SetMetaTitle.of(productDraft.getMetaTitle()),
                SetSku.of(masterData(product, syncOptions).getMasterVariant().getId(),
                        productDraft.getMasterVariant().getSku(), syncOptions.isUpdateStaged()),
                SetSearchKeywords.of(productDraft.getSearchKeywords(), syncOptions.isUpdateStaged()));
    }

}
