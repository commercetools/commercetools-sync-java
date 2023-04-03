package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.helpers.DefaultCurrencyUnits.EUR;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.SIMPLE_PRODUCT_WITH_MASTER_VARIANT_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.SIMPLE_PRODUCT_WITH_MULTIPLE_VARIANTS_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductVariantDraftBuilder;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.ImageBuilder;
import com.commercetools.api.models.common.ImageDimensionsBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductAddAssetActionBuilder;
import com.commercetools.api.models.product.ProductAddExternalImageActionBuilder;
import com.commercetools.api.models.product.ProductAddPriceActionBuilder;
import com.commercetools.api.models.product.ProductAddToCategoryActionBuilder;
import com.commercetools.api.models.product.ProductAddVariantActionBuilder;
import com.commercetools.api.models.product.ProductChangeMasterVariantActionBuilder;
import com.commercetools.api.models.product.ProductChangeNameActionBuilder;
import com.commercetools.api.models.product.ProductChangeSlugActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product.ProductPublishActionBuilder;
import com.commercetools.api.models.product.ProductRemoveFromCategoryActionBuilder;
import com.commercetools.api.models.product.ProductRemoveImageActionBuilder;
import com.commercetools.api.models.product.ProductRemovePriceActionBuilder;
import com.commercetools.api.models.product.ProductRemoveVariantActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductSetCategoryOrderHintActionBuilder;
import com.commercetools.api.models.product.ProductSetDescriptionActionBuilder;
import com.commercetools.api.models.product.ProductSetMetaDescriptionActionBuilder;
import com.commercetools.api.models.product.ProductSetMetaKeywordsActionBuilder;
import com.commercetools.api.models.product.ProductSetMetaTitleActionBuilder;
import com.commercetools.api.models.product.ProductSetSearchKeywordsActionBuilder;
import com.commercetools.api.models.product.ProductSetSkuActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product.SearchKeywordBuilder;
import com.commercetools.api.models.product.SearchKeywordsBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.sdk2.products.AttributeMetaData;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncUtilsTest {
  private ProductProjection oldProduct;
  private ProductSyncOptions productSyncOptions;

  /** Initializes an instance of {@link ProductSyncOptions} and {@link Product}. */
  @BeforeEach
  void setup() {
    productSyncOptions = ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final InputStream resourceAsStream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH);
    final Product productFromJson = fromInputStream(resourceAsStream, Product.class);
    oldProduct = ProductMixin.toProjection(productFromJson, ProductProjectionType.STAGED);
  }

  @Test
  void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
    final LocalizedString newName = ofEnglish("newName");
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build())
            .name(newName)
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, new HashMap<>());

    assertThat(updateActions)
        .containsExactly(
            ProductChangeNameActionBuilder.of().name(newName).staged(true).build(),
            ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
    // preparation
    final ProductDraftBuilder draftBuilder =
        createProductDraftBuilder(
            PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build());

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .plusSources(AssetSourceBuilder.of().uri("uri").build())
            .name(ofEnglish("assetName"))
            .key("anyKey")
            .build();

    final ProductVariantDraft masterVariantDraftWithAssets =
        ProductVariantDraftBuilder.of(draftBuilder.getMasterVariant())
            .assets(singletonList(assetDraft))
            .build();

    final ProductDraft newProductDraft =
        ProductDraftBuilder.of(draftBuilder.build())
            .masterVariant(masterVariantDraftWithAssets)
            .build();

    final Category expectedCategoryToAdd = mock(Category.class);
    when(expectedCategoryToAdd.getId()).thenReturn("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
    when(expectedCategoryToAdd.toReference())
        .thenReturn(
            CategoryReferenceBuilder.of().id("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f").build());
    when(expectedCategoryToAdd.toResourceIdentifier())
        .thenReturn(
            CategoryResourceIdentifierBuilder.of()
                .id("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .build());

    final Category expectedCategoryToRemove = mock(Category.class);
    when(expectedCategoryToRemove.getId()).thenReturn("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f");
    when(expectedCategoryToRemove.toReference())
        .thenReturn(
            CategoryReferenceBuilder.of().id("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f").build());
    when(expectedCategoryToRemove.toResourceIdentifier())
        .thenReturn(
            CategoryResourceIdentifierBuilder.of()
                .id("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .build());

    // test
    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, new HashMap<>());

    // asserts
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductChangeNameActionBuilder.of().name(ofEnglish("new name")).staged(true).build(),
            ProductSetDescriptionActionBuilder.of()
                .description(ofEnglish("new description"))
                .staged(true)
                .build(),
            ProductChangeSlugActionBuilder.of()
                .slug(ofEnglish("rehruecken-o-k1"))
                .staged(true)
                .build(),
            ProductSetSearchKeywordsActionBuilder.of()
                .searchKeywords(
                    SearchKeywordsBuilder.of()
                        .addValue(
                            Locale.ENGLISH.getLanguage(),
                            asList(
                                SearchKeywordBuilder.of().text("key1").build(),
                                SearchKeywordBuilder.of().text("key2").build()))
                        .build())
                .staged(true)
                .build(),
            ProductSetMetaTitleActionBuilder.of().metaTitle(ofEnglish("new title")).build(),
            ProductSetMetaDescriptionActionBuilder.of()
                .metaDescription(ofEnglish("new Meta description"))
                .build(),
            ProductSetMetaKeywordsActionBuilder.of().metaKeywords(ofEnglish("key1,key2")).build(),
            ProductAddToCategoryActionBuilder.of()
                .category(expectedCategoryToAdd.toResourceIdentifier())
                .staged(true)
                .build(),
            ProductSetCategoryOrderHintActionBuilder.of()
                .categoryId("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .staged(true)
                .build(),
            ProductSetCategoryOrderHintActionBuilder.of()
                .categoryId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .orderHint("0.83")
                .staged(true)
                .build(),
            ProductSetCategoryOrderHintActionBuilder.of()
                .categoryId("5dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .orderHint("0.93")
                .staged(true)
                .build(),
            ProductRemoveFromCategoryActionBuilder.of()
                .category(expectedCategoryToRemove.toResourceIdentifier())
                .staged(true)
                .build(),
            ProductRemoveImageActionBuilder.of()
                .variantId(1L)
                .imageUrl(
                    "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                        + "old-image.png")
                .staged(true)
                .build(),
            ProductAddExternalImageActionBuilder.of()
                .variantId(1L)
                .image(
                    ImageBuilder.of()
                        .url(
                            "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                                + "new-image.png")
                        .dimensions(ImageDimensionsBuilder.of().h(0).w(0).build())
                        .build())
                .staged(true)
                .build(),
            ProductAddExternalImageActionBuilder.of()
                .variantId(1L)
                .image(
                    ImageBuilder.of()
                        .url(
                            "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                                + "new-image-2.png")
                        .dimensions(ImageDimensionsBuilder.of().w(0).h(0).build())
                        .build())
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .staged(true)
                .build(),
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-r9e7wv99vfb")
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(118L)
                                .currencyCode(EUR.getCurrencyCode())
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(100L)
                                .currencyCode("EGP")
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(1L)
                .asset(assetDraft)
                .position(0)
                .staged(true)
                .build(),
            ProductSetSkuActionBuilder.of().variantId(1L).sku("3065831").staged(true).build(),
            ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActionsInCorrectOrder() {
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, new HashMap<>());

    // Assert that "removeImage" actions are always before "addExternalImage"
    assertThat(updateActions)
        .containsSubsequence(
            ProductRemoveImageActionBuilder.of()
                .variantId(1L)
                .imageUrl(
                    "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                        + "old-image.png")
                .staged(true)
                .build(),
            ProductAddExternalImageActionBuilder.of()
                .variantId(1L)
                .image(
                    ImageBuilder.of()
                        .url(
                            "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                                + "new-image.png")
                        .dimensions(ImageDimensionsBuilder.of().w(0).h(0).build())
                        .build())
                .staged(true)
                .build(),
            ProductAddExternalImageActionBuilder.of()
                .variantId(1L)
                .image(
                    ImageBuilder.of()
                        .url(
                            "https://53346cfbf3c7e017ed3d-6de74c3efa80f1c837c6a988b57abe66.ssl.cf3.rackcdn.com/"
                                + "new-image-2.png")
                        .dimensions(ImageDimensionsBuilder.of().w(0).h(0).build())
                        .build())
                .staged(true)
                .build());

    // Assert that "removePrice" actions are always before "addPrice"
    assertThat(updateActions)
        .containsSubsequence(
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(100L)
                                .currencyCode("EGP")
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build());

    assertThat(updateActions)
        .containsSubsequence(
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f")
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(118L)
                                .currencyCode(EUR.getCurrencyCode())
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build());

    assertThat(updateActions)
        .containsSubsequence(
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-r9e7wv99vfb")
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(100L)
                                .currencyCode("EGP")
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build());

    assertThat(updateActions)
        .containsSubsequence(
            ProductRemovePriceActionBuilder.of()
                .priceId("4dfc8bea-84f2-45bc-b3c2-r9e7wv99vfb")
                .staged(true)
                .build(),
            ProductAddPriceActionBuilder.of()
                .variantId(1L)
                .price(
                    PriceDraftBuilder.of()
                        .value(
                            CentPrecisionMoneyBuilder.of()
                                .centAmount(118L)
                                .currencyCode(EUR.getCurrencyCode())
                                .fractionDigits(2)
                                .build())
                        .channel(ChannelResourceIdentifierBuilder.of().id("channel-key_1").build())
                        .build())
                .staged(true)
                .build());
  }

  @Test
  void buildActions_FromDraftsWithSameNameValues_ShouldNotBuildUpdateActions() {
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, new HashMap<>());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_FromDraftsWithSameForAllAttribute_ShouldBuildUpdateActions() {
    final ProductVariant masterVariant = oldProduct.getMasterVariant();
    final Attribute brandNameAttribute =
        AttributeBuilder.of().name("brandName").value("sameForAllBrand").build();
    final ProductVariantDraft newMasterVariant =
        createProductVariantDraftBuilder(masterVariant).plusAttributes(brandNameAttribute).build();
    final ProductVariantDraft variant =
        ProductVariantDraftBuilder.of()
            .key("v2")
            .sku("3065834")
            .attributes(brandNameAttribute)
            .build();

    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build())
            .masterVariant(newMasterVariant)
            .plusVariants(variant)
            .build();

    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeMetaData brandName =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("brandName")
                .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
                .type(AttributeTypeBuilder::textBuilder)
                .label(ofEnglish("brandName"))
                .isRequired(false)
                .inputHint(TextInputHint.SINGLE_LINE)
                .isSearchable(true)
                .build());
    attributesMetaData.put("brandName", brandName);

    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, attributesMetaData);

    // check that we only have one generated action for all the variants and no duplicates
    // and is ordered correctly before addVariant action
    assertThat(updateActions.size()).isEqualTo(3);
    assertThat(updateActions)
        .containsOnlyOnce(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("brandName")
                .value(brandNameAttribute.getValue())
                .staged(true)
                .build());
    assertThat(updateActions.get(0))
        .isEqualTo(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("brandName")
                .value(brandNameAttribute.getValue())
                .staged(true)
                .build());
    assertThat(updateActions.get(1))
        .isEqualTo(
            ProductAddVariantActionBuilder.of()
                .attributes(brandNameAttribute)
                .sku("3065834")
                .key("v2")
                .staged(true)
                .build());

    assertThat(updateActions.get(2)).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildActions_FromDraftsWithDifferentAttributes_ShouldBuildUpdateActions() {
    // Reloading the oldProduct object with a specific file for this test
    final InputStream resourceAsStream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(SIMPLE_PRODUCT_WITH_MULTIPLE_VARIANTS_RESOURCE_PATH);
    final Product productFromJson = fromInputStream(resourceAsStream, Product.class);
    oldProduct = ProductMixin.toProjection(productFromJson, ProductProjectionType.STAGED);

    final Attribute brandNameAttribute =
        AttributeBuilder.of().name("brandName").value("myBrand").build();
    final Attribute orderLimitAttribute =
        AttributeBuilder.of().name("orderLimit").value("5").build();
    final Attribute priceInfoAttribute =
        AttributeBuilder.of().name("priceInfo").value("80,20/kg").build();
    final ProductVariantDraft variant =
        ProductVariantDraftBuilder.of()
            .key("v3")
            .sku("1065834")
            .plusAttributes(orderLimitAttribute, priceInfoAttribute, brandNameAttribute)
            .build();

    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                SIMPLE_PRODUCT_WITH_MASTER_VARIANT_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id("anyProductType").build())
            .plusVariants(variant)
            .build();

    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeMetaData brandName =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("brandName")
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .label(ofEnglish("brandName"))
                .isRequired(false)
                .inputHint(TextInputHint.SINGLE_LINE)
                .isSearchable(true)
                .build());
    final AttributeMetaData orderLimit =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("orderLimit")
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .label(ofEnglish("orderLimit"))
                .isRequired(false)
                .inputHint(TextInputHint.SINGLE_LINE)
                .isSearchable(true)
                .build());
    final AttributeMetaData priceInfo =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("priceInfo")
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .label(ofEnglish("priceInfo"))
                .isRequired(false)
                .inputHint(TextInputHint.SINGLE_LINE)
                .isSearchable(true)
                .build());
    final AttributeMetaData size =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("size")
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .label(ofEnglish("size"))
                .isRequired(false)
                .inputHint(TextInputHint.SINGLE_LINE)
                .isSearchable(true)
                .build());
    attributesMetaData.put("brandName", brandName);
    attributesMetaData.put("orderLimit", orderLimit);
    attributesMetaData.put("priceInfo", priceInfo);
    attributesMetaData.put("size", size);

    final List<ProductUpdateAction> updateActions =
        ProductSyncUtils.buildActions(
            oldProduct, newProductDraft, productSyncOptions, attributesMetaData);

    // check the generated attribute update actions
    assertThat(updateActions.size()).isEqualTo(9);
    assertThat(updateActions)
        .containsSequence(
            ProductRemoveVariantActionBuilder.of().id(5L).staged(true).build(),
            ProductAddVariantActionBuilder.of()
                .prices(emptyList())
                .assets(emptyList())
                .images(emptyList())
                .attributes(
                    AttributeBuilder.of().name("priceInfo").value("64,90/kg").build(),
                    AttributeBuilder.of().name("size").value("ca. 1 x 1000 g").build())
                .sku("1065833")
                .key("v2")
                .staged(true)
                .build(),
            ProductChangeMasterVariantActionBuilder.of().sku("1065833").staged(true).build(),
            ProductRemoveVariantActionBuilder.of().id(1L).build(),
            ProductSetAttributeActionBuilder.of().variantId(2L).name("size").staged(true).build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(2L)
                .name("orderLimit")
                .value(orderLimitAttribute.getValue())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(2L)
                .name("priceInfo")
                .value(priceInfoAttribute.getValue())
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(2L)
                .name("brandName")
                .value(brandNameAttribute.getValue())
                .staged(true)
                .build(),
            ProductPublishActionBuilder.of().build());
  }
}
