package com.commercetools.sync.integration.externalsource.products.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetsCustomType;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createVariantDraft;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPricesCustomType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceSetAttributeDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductReferenceReplacementUtilsIT {
    /**
     * Delete all product related test data from the target project.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    /**
     * Delete all product related test data from the target project.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void buildProductQuery_Always_ShouldFetchProductWithAllExpandedReferences() {
        final ProductType productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        final TaxCategory taxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        final State state = createState(CTP_TARGET_CLIENT, StateType.PRODUCT_STATE);

        final Type assetsCustomType = createAssetsCustomType("assetsCustomTypeKey", ENGLISH,
            "assetsCustomTypeName", CTP_TARGET_CLIENT);

        final Type pricesCustomType = createPricesCustomType("pricesCustomTypeKey", ENGLISH, "pricesCustomTypeName",
            CTP_TARGET_CLIENT);

        final List<AssetDraft> assetDrafts = singletonList(
            createAssetDraft("1", ofEnglish("1"), assetsCustomType.getId()));

        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, ENGLISH, OLD_CATEGORY_CUSTOM_TYPE_NAME,
            CTP_TARGET_CLIENT);
        final List<Category> targetCategories =
            createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 1));
        final List<Reference<Category>> categoryReferences = getReferencesWithIds(targetCategories);


        final Channel priceChannel =
            executeBlocking(CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(ChannelDraft.of(SUPPLY_CHANNEL_KEY_1))));
        final PriceDraft priceDraft = PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
                                                       .channel(priceChannel)
                                                       .build();



        final ProductDraft parentProductDraft = ProductDraftBuilder
            .of(productType.toReference(), ofEnglish("parentProduct"), ofEnglish("parentProduct"),
                createVariantDraft("parent", emptyList()))
            .key("parentProduct")
            .build();

        // Create Parent Product.
        final Product parentProduct =
            executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(parentProductDraft)));

        final String attribute1Name = "product-reference";
        final AttributeDraft productRefAttr = AttributeDraft.of(attribute1Name,
            getProductReferenceWithId(parentProduct.getId()));
        final String attribute2Name = "product-reference-set";
        final AttributeDraft productSetRefAttr = getProductReferenceSetAttributeDraft(attribute2Name,
            getProductReferenceWithId(parentProduct.getId()));
        final List<AttributeDraft> attributeDrafts = Arrays.asList(productRefAttr, productSetRefAttr);

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of()
                                                                            .key("v1")
                                                                            .sku("v1")
                                                                            .assets(assetDrafts)
                                                                            .prices(priceDraft)
                                                                            .attributes(attributeDrafts)
                                                                            .build();

        final ProductVariantDraft variant2 = ProductVariantDraftBuilder.of().sku("v2")
                                                                       .assets(assetDrafts)
                                                                       .prices(priceDraft)
                                                                       .attributes(attributeDrafts)
                                                                       .build();

        // Create Product Draft with all kind of references that are needed to be checked for expansion.
        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType.toReference(), ofEnglish("draftName"), ofEnglish("existingSlug"),
                masterVariant)
            .plusVariants(variant2)
            .key("existingProduct")
            .taxCategory(taxCategory)
            .state(state)
            .categories(categoryReferences)
            .build();

        final ProductDraft draftWithPriceChannelReferences = getDraftWithPriceChannelReferences(productDraft,
            priceChannel.toReference());


        // Create Product.
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draftWithPriceChannelReferences)));


        // Fetch Products with reference expansions.
        final List<Product> products = CTP_TARGET_CLIENT.execute(buildProductQuery().bySku("v1", STAGED))
                                                        .toCompletableFuture().join().getResults();


        // Assert Product is fetched with all the references expanded.
        assertThat(products).hasSize(1);
        final Product createdProduct = products.get(0);
        // Assert product type references are expanded
        assertThat(createdProduct.getProductType().getObj()).isNotNull();
        // Assert tax category references are expanded
        assertThat(createdProduct.getTaxCategory()).isNotNull();
        assertThat(createdProduct.getTaxCategory().getObj()).isNotNull();
        // Assert state references are expanded
        assertThat(createdProduct.getState()).isNotNull();
        assertThat(createdProduct.getState().getObj()).isNotNull();

        final ProductData stagedProjection = createdProduct.getMasterData().getStaged();

        // Assert category references are expanded
        assertThat(stagedProjection.getCategories()).hasSize(1);
        final Reference<Category> category = stagedProjection.getCategories().iterator().next();
        assertThat(category).isNotNull();
        assertThat(category.getObj()).isNotNull();

        // Assert variants' assets custom types references are expanded
        final ProductVariant variant = stagedProjection.getVariant(2);
        assertThat(variant).isNotNull();
        assertThat(variant.getAssets()).hasSize(1);
        final Asset asset = variant.getAssets().get(0);
        assertThat(asset.getCustom()).isNotNull();
        assertThat(asset.getCustom().getType().getObj()).isNotNull();

        final ProductVariant stagedProjectionMasterVariant = stagedProjection.getMasterVariant();

        // Assert variants' price channel references are expanded.
        assertThat(stagedProjectionMasterVariant.getPrices()).hasSize(1);
        final Price price = stagedProjectionMasterVariant.getPrices().get(0);
        assertThat(price.getChannel()).isNotNull();
        assertThat(price.getChannel().getObj()).isNotNull();

        // Assert master variant assets custom type references are expanded.
        assertThat(stagedProjectionMasterVariant.getAssets()).hasSize(1);
        final Asset masterVariantAsset = stagedProjectionMasterVariant.getAssets().get(0);
        assertThat(masterVariantAsset.getCustom()).isNotNull();
        assertThat(masterVariantAsset.getCustom().getType().getObj()).isNotNull();


        // Assert variants attribute references are expanded.
        final Attribute attribute1 = stagedProjectionMasterVariant.getAttribute(attribute1Name);
        assertThat(attribute1).isNotNull();
        final JsonNode attribute1ValueAsJsonNode = attribute1.getValueAsJsonNode();
        assertThat(attribute1ValueAsJsonNode).isNotNull();
        assertThat(attribute1ValueAsJsonNode.get("obj")).isNotNull();

        final Attribute attribute2 = stagedProjectionMasterVariant.getAttribute(attribute2Name);
        assertThat(attribute2).isNotNull();
        final JsonNode attribute2ValueAsJsonNode = attribute2.getValueAsJsonNode();
        assertThat(attribute2ValueAsJsonNode).isNotNull();
        assertThat(attribute2ValueAsJsonNode.isArray()).isTrue();
        assertThat(attribute2ValueAsJsonNode).hasSize(1);

        final JsonNode firstReferenceInSet = attribute2ValueAsJsonNode.get(0);
        assertThat(firstReferenceInSet).isNotNull();
        assertThat(firstReferenceInSet.get("obj")).isNotNull();
    }
}
