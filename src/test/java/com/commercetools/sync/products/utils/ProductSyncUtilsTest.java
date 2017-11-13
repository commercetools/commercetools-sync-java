package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetPrices;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductSyncUtilsTest {
    private Product oldProduct;
    private ProductSyncOptions productSyncOptions;

    /**
     * Initializes an instance of {@link ProductSyncOptions} and {@link Product}.
     */
    @Before
    public void setup() {
        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                      .build();

        oldProduct = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
    }

    @Test
    public void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "newName");

        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH, ProductType.referenceOfId("anyProductType"))
                .name(newName)
                .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(2); //TODO: Should be 1 but because of SetPrices GITHUB ISSUE: #101

        final UpdateAction<Product> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(newName);
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
                ProductType.referenceOfId("anyProductType"))
                .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(17);


        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof ChangeName))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetDescription))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof ChangeSlug))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetSearchKeywords))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaTitle))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaDescription))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaKeywords))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof AddToCategory))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetCategoryOrderHint))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof RemoveFromCategory))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof AddExternalImage))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof RemoveImage))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetPrices))
            .isTrue();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetSku))
            .isTrue();
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValuesWithFilterFunction_ShouldBuildFilteredActions() {
        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
                ProductType.referenceOfId("anyProductType"))
                .build();

        final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>>
            filterCategoryActions = (unfilteredList) ->
            unfilteredList.stream()
                          .filter(productUpdateAction ->
                              productUpdateAction instanceof RemoveFromCategory
                                  || productUpdateAction instanceof AddToCategory
                                  || productUpdateAction instanceof SetCategoryOrderHint)
                          .collect(Collectors.toList());

        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                      .beforeUpdateCallback(filterCategoryActions)
                                                      .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(5);

        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof ChangeName))
            .isFalse();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetDescription))
            .isFalse();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof ChangeSlug))
            .isFalse();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetSearchKeywords))
            .isFalse();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaTitle))
            .isFalse();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaDescription))
            .isFalse();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetMetaKeywords))
            .isFalse();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof AddExternalImage))
            .isFalse();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof RemoveImage))
            .isFalse();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetPrices))
            .isFalse();
        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetSku))
            .isFalse();

        assertThat(updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof AddToCategory))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof SetCategoryOrderHint))
            .isTrue();
        assertThat(
            updateActions.stream().anyMatch(productUpdateAction -> productUpdateAction instanceof RemoveFromCategory))
            .isTrue();
    }

    @Test
    public void buildActions_FromDraftsWithSameNameValues_ShouldNotBuildUpdateActions() {
        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH, ProductType.referenceOfId("anyProductType"))
                .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1); //TODO: Should be empty but because of SetPrices GITHUB ISSUE: #101

        final UpdateAction<Product> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("setPrices");
    }
}
