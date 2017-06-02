package com.commercetools.sync.products.actions;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.client.SphereClient;
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
import static com.commercetools.sync.products.ProductTestUtils.productType;
import static com.commercetools.sync.products.ProductTestUtils.productDraft;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        ProductSyncOptions syncOptions = syncOptions(mock(SphereClient.class), true, true);
        ProductDraft productDraft = productDraft("product.json", productType(), null, syncOptions);

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft, syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildActions_nonEmptyForDifferent() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of();
        Product product = product("product.json");
        ProductSyncOptions syncOptions = syncOptions(mock(SphereClient.class), true, true);
        ProductDraft productDraft = productDraft("product-changed.json", productType(), null, syncOptions);

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft, syncOptions);

        List<UpdateAction<Product>> expectedUpdateActions = asList(
                ChangeName.of(productDraft.getName()),
                ChangeSlug.of(productDraft.getSlug()),
                SetMetaDescription.of(productDraft.getMetaDescription()),
                SetMetaKeywords.of(productDraft.getMetaKeywords()),
                SetMetaTitle.of(productDraft.getMetaTitle()),
                SetSku.of(product.getMasterData().getStaged().getMasterVariant().getId(), productDraft.getMasterVariant().getSku()),
                SetSearchKeywords.of(productDraft.getSearchKeywords()));
        assertThat(updateActions).hasSize(expectedUpdateActions.size());
        expectedUpdateActions
                .forEach(action -> assertThat(updateActions.contains(action)));
    }

}
