package com.commercetools.sync.products.actions;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.products.ProductTestUtils.getProduct;
import static com.commercetools.sync.products.ProductTestUtils.getProductSyncOptions;
import static com.commercetools.sync.products.ProductTestUtils.getProductType;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductUpdateActionsBuilderTest {

    @Test
    public void of_expectSingleton() {
        ProductUpdateActionsBuilder productUpdateActionsBuilder = ProductUpdateActionsBuilder.of(getProductSyncOptions());

        assertThat(productUpdateActionsBuilder).isNotNull();

        ProductUpdateActionsBuilder productUpdateActionsBuilder1 = ProductUpdateActionsBuilder.of(getProductSyncOptions());

        assertThat(productUpdateActionsBuilder1).isSameAs(productUpdateActionsBuilder);
    }

    @Test
    public void buildActions_emptyForIdentical() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of(getProductSyncOptions());
        Product product = getProduct("product.json");
        ProductDraft productDraft = productDraft("product.json", getProductType());

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildActions_nonEmptyForDifferent() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of(getProductSyncOptions());
        Product product = getProduct("product.json");
        ProductDraft productDraft = productDraft("product-changed.json", getProductType());

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft);

        List<UpdateAction<Product>> expectedUpdateActions = asList(
                ChangeName.of(productDraft.getName()),
                ChangeSlug.of(productDraft.getSlug()),
                SetMetaDescription.of(productDraft.getMetaDescription()),
                SetMetaKeywords.of(productDraft.getMetaKeywords()),
                SetMetaTitle.of(productDraft.getMetaTitle()),
                SetSku.of(product.getMasterData().getStaged().getMasterVariant().getId(), productDraft.getMasterVariant().getSku()),
                SetSearchKeywords.of(productDraft.getSearchKeywords()),
                Publish.of());
        assertThat(updateActions).hasSize(expectedUpdateActions.size());
        expectedUpdateActions
                .forEach(action -> assertThat(updateActions.contains(action)));
    }

}
