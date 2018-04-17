package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.ImageDimensions;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
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
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        //TODO: Should contain only changeName but no setPrices. See: GITHUB ISSUE: #101
        assertThat(updateActions).containsExactly(
            ChangeName.of(newName, true),
            SetPrices.of(1, emptyList())
        );
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final ProductDraftBuilder draftBuilder = createProductDraftBuilder(
            PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH, ProductType.referenceOfId("anyProductType"));

        final AssetDraft assetDraft = AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()),
            ofEnglish("assetName")).build();

        final ProductVariantDraft masterVariantDraftWithAssets =
            ProductVariantDraftBuilder.of(draftBuilder.getMasterVariant())
                                      .assets(singletonList(assetDraft))
                                      .build();

        final ProductDraft newProductDraft = ProductDraftBuilder.of(draftBuilder.build())
                                                                .masterVariant(masterVariantDraftWithAssets)
                                                                .build();

        final Category expectedCategoryToAdd = mock(Category.class);
        when(expectedCategoryToAdd.getId()).thenReturn("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
        when(expectedCategoryToAdd.toReference())
            .thenReturn(Category.referenceOfId("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final Category expectedCategoryToRemove = mock(Category.class);
        when(expectedCategoryToRemove.getId()).thenReturn("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
        when(expectedCategoryToRemove.toReference())
            .thenReturn(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).containsExactly(
            ChangeName.of(ofEnglish("new name"), true),
            SetDescription.of(ofEnglish("new description"), true),
            ChangeSlug.of(ofEnglish("rehruecken-o-k1"), true),
            SetSearchKeywords.of(SearchKeywords.of(Locale.ENGLISH, asList(SearchKeyword.of("key1"),
                SearchKeyword.of("key2"))), true),
            SetMetaTitle.of(ofEnglish("new title")),
            SetMetaDescription.of(ofEnglish("new Meta description")),
            SetMetaKeywords.of(ofEnglish("key1,key2")),
            AddToCategory.of(expectedCategoryToAdd, true),
            SetCategoryOrderHint.of("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", null, true),
            SetCategoryOrderHint.of("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.83", true),
            SetCategoryOrderHint.of("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.93", true),
            RemoveFromCategory.of(expectedCategoryToRemove, true),
            RemoveImage.ofVariantId(1,
                Image.of("https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                    + "old-image.png", ImageDimensions.of(0, 0), null), true),
            AddExternalImage.ofVariantId(1,
                Image.of("https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                    + "new-image.png", ImageDimensions.of(0, 0), null), true),
            AddExternalImage.ofVariantId(1,
                Image.of("https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                    + "new-image-2.png", ImageDimensions.of(0, 0), null), true),
            SetPrices.of(1,
                asList(PriceDraft.of(BigDecimal.valueOf(1.18), DefaultCurrencyUnits.EUR)
                                 .withChannel(Channel.referenceOfId("channel-key_1")),
                    PriceDraft.of(BigDecimal.valueOf(1), MoneyImpl.createCurrencyByCode("EGP"))
                              .withChannel(Channel.referenceOfId("channel-key_1")))),
            AddAsset.ofVariantId(1, assetDraft).withPosition(0).withStaged(true),
            SetSku.of(1, "3065831", true)
        );
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValuesWithFilterFunction_ShouldBuildFilteredActions() {
        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
                ProductType.referenceOfId("anyProductType"))
                .build();

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            filterCategoryActions = (unfilteredList, newDraft, oldProduct) ->
            unfilteredList != null ? unfilteredList.stream()
                                                   .filter(productUpdateAction ->
                                                       productUpdateAction instanceof RemoveFromCategory
                                                           || productUpdateAction instanceof AddToCategory
                                                           || productUpdateAction instanceof SetCategoryOrderHint)
                                                   .collect(Collectors.toList()) : null;

        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                      .beforeUpdateCallback(filterCategoryActions)
                                                      .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();

        final Category categoryToAdd = mock(Category.class);
        when(categoryToAdd.getId()).thenReturn("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
        when(categoryToAdd.toReference()).thenReturn(Category.referenceOfId("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final Category categoryToRemove = mock(Category.class);
        when(categoryToRemove.getId()).thenReturn("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
        when(categoryToRemove.toReference()).thenReturn(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        assertThat(updateActions).containsExactly(
            AddToCategory.of(categoryToAdd, true),
            SetCategoryOrderHint.of("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", null, true),
            SetCategoryOrderHint.of("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.83", true),
            SetCategoryOrderHint.of("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.93", true),
            RemoveFromCategory.of(categoryToRemove, true)
        );
    }

    @Test
    public void buildActions_FromDraftsWithSameNameValues_ShouldNotBuildUpdateActions() {
        final ProductDraft newProductDraft =
            createProductDraftBuilder(PRODUCT_KEY_1_RESOURCE_PATH, ProductType.referenceOfId("anyProductType"))
                .build();

        final List<UpdateAction<Product>> updateActions =
            ProductSyncUtils.buildActions(oldProduct, newProductDraft, productSyncOptions, new HashMap<>());
        assertThat(updateActions).isNotNull();

        //TODO: Should be empty but because of SetPrices GITHUB ISSUE: #101
        assertThat(updateActions).containsExactly(
            SetPrices.of(1, emptyList())
        );
    }
}
