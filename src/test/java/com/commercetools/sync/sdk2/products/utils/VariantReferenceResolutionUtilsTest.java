package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.utils.VariantReferenceResolutionUtils.*;
import static com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelReferenceBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupReferenceBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductReferenceBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VariantReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void
      mapToProductVariantDrafts_WithReferenceIdToKeyValuesCached_ShouldReturnVariantDraftsWithReplacedKeys() {

    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    referenceIdToKeyCache.add(customTypeId, customTypeKey);

    final String channelKey = "channelKey";
    final String channelId = UUID.randomUUID().toString();
    referenceIdToKeyCache.add(channelId, channelKey);

    final ChannelReference channelReference = ChannelReferenceBuilder.of().id(channelId).build();

    final TypeReference priceCustomTypeReference =
        TypeReferenceBuilder.of().id(customTypeId).build();

    final String customerGroupId = "customer-group-id";
    final String customerGroupKey = "customer-group-key";
    referenceIdToKeyCache.add(customerGroupId, customerGroupKey);

    final CustomerGroupReference customerGroupReference =
        CustomerGroupReferenceBuilder.of().id(customerGroupId).build();

    final Price price =
        getPriceMockWithReferences(
            channelReference, priceCustomTypeReference, customerGroupReference);

    final Asset asset =
        getAssetMockWithCustomFields(TypeReferenceBuilder.of().id(customTypeId).build());

    final ProductVariant productVariant =
        getProductVariantMock(singletonList(price), singletonList(asset));

    final List<ProductVariantDraft> variantDrafts =
        mapToProductVariantDrafts(singletonList(productVariant), referenceIdToKeyCache);

    assertThat(variantDrafts).hasSize(1);
    assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
    final ChannelResourceIdentifier channelReferenceAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    assertThat(channelReferenceAfterReplacement.getKey()).isEqualTo(channelKey);

    final CustomFieldsDraft priceCustomAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getCustom();
    assertThat(priceCustomAfterReplacement).isNotNull();
    final TypeResourceIdentifier priceCustomTypeAfterReplacement =
        priceCustomAfterReplacement.getType();
    assertThat(priceCustomTypeAfterReplacement).isNotNull();
    assertThat(priceCustomTypeAfterReplacement.getKey()).isEqualTo(customTypeKey);

    assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
    final TypeResourceIdentifier referenceReplacedType =
        variantDrafts.get(0).getAssets().get(0).getCustom().getType();
    assertThat(referenceReplacedType).isNotNull();
    assertThat(referenceReplacedType.getKey()).isEqualTo(customTypeKey);
  }

  @Test
  void mapToProductVariantDrafts_WithReferenceIdToKeyValuesNoneCached_ShouldNotReplaceIds() {
    final ChannelReference channelReference =
        ChannelReferenceBuilder.of().id(UUID.randomUUID().toString()).build();
    final TypeReference customTypeReference =
        TypeReferenceBuilder.of().id(UUID.randomUUID().toString()).build();

    final Asset asset2 =
        getAssetMockWithCustomFields(
            TypeReferenceBuilder.of().id(UUID.randomUUID().toString()).build());

    final Price price = getPriceMockWithReferences(channelReference, customTypeReference, null);
    final ProductVariant productVariant =
        getProductVariantMock(singletonList(price), singletonList(asset2));

    final List<ProductVariantDraft> variantDrafts =
        mapToProductVariantDrafts(singletonList(productVariant), referenceIdToKeyCache);

    assertThat(variantDrafts).hasSize(1);
    assertThat(variantDrafts.get(0).getPrices()).hasSize(1);

    final ChannelResourceIdentifier channelReferenceAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    // Assert price channel reference id is not replaced.
    assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

    final CustomFieldsDraft priceCustomFields = variantDrafts.get(0).getPrices().get(0).getCustom();
    assertThat(priceCustomFields).isNotNull();
    final TypeResourceIdentifier priceCustomTypeAfterReplacement = priceCustomFields.getType();
    // Assert price custom type reference id is not replaced.
    assertThat(priceCustomTypeAfterReplacement.getId()).isEqualTo(customTypeReference.getId());

    assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
    final TypeResourceIdentifier assetCustomTypeReference =
        variantDrafts.get(0).getAssets().get(0).getCustom().getType();
    assertThat(assetCustomTypeReference).isNotNull();
    // Assert asset custom type reference id is not replaced.
    assertThat(assetCustomTypeReference.getId())
        .isEqualTo(variantDrafts.get(0).getAssets().get(0).getCustom().getType().getId());
  }

  @Test
  void mapToPriceDraft_WithReferenceIdToKeyValuesNoneCached_ShouldNotReplaceIds() {
    final ChannelReference channelReference =
        ChannelReferenceBuilder.of().id(UUID.randomUUID().toString()).build();
    final TypeReference typeReference =
        TypeReferenceBuilder.of().id(UUID.randomUUID().toString()).build();

    final Price price = getPriceMockWithReferences(channelReference, typeReference, null);
    final ProductVariant productVariant = getProductVariantMock(singletonList(price));

    final List<PriceDraft> priceDrafts = mapToPriceDrafts(productVariant, referenceIdToKeyCache);

    assertThat(priceDrafts).hasSize(1);
    final PriceDraft priceDraftAfterReplacement = priceDrafts.get(0);

    final ChannelResourceIdentifier channelReferenceAfterReplacement =
        priceDraftAfterReplacement.getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    // Assert Id is not replaced with key.
    assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

    final CustomFieldsDraft customAfterReplacement = priceDraftAfterReplacement.getCustom();
    assertThat(customAfterReplacement).isNotNull();
    final TypeResourceIdentifier customTypeAfterReplacement = customAfterReplacement.getType();

    assertThat(customTypeAfterReplacement).isNotNull();
    // Assert Id is not replaced with key.
    assertThat(customTypeAfterReplacement.getId()).isEqualTo(typeReference.getId());
  }

  @Test
  void mapToPriceDraft_WithReferenceIdToKeyValuesCached_ShouldReplaceIds() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    referenceIdToKeyCache.add(customTypeId, customTypeKey);

    final String channel1Key = "channel1Key";
    final String channel1Id = UUID.randomUUID().toString();
    referenceIdToKeyCache.add(channel1Id, channel1Key);

    final String channel2Key = "channel2Key";
    final String channel2Id = UUID.randomUUID().toString();
    referenceIdToKeyCache.add(channel2Id, channel2Key);

    final ChannelReference channelReference1 = ChannelReferenceBuilder.of().id(channel1Id).build();
    final ChannelReference channelReference2 = ChannelReferenceBuilder.of().id(channel2Id).build();
    final TypeReference customTypeReference = TypeReferenceBuilder.of().id(customTypeId).build();

    final Price price1 = getPriceMockWithReferences(channelReference1, customTypeReference, null);
    final Price price2 = getPriceMockWithReferences(channelReference2, customTypeReference, null);

    final ProductVariant productVariant = getProductVariantMock(asList(price1, price2));

    final List<PriceDraft> priceDrafts = mapToPriceDrafts(productVariant, referenceIdToKeyCache);

    assertThat(priceDrafts).hasSize(2);

    final PriceDraft priceDraft1AfterReplacement = priceDrafts.get(0);
    final ChannelResourceIdentifier channelReference1AfterReplacement =
        priceDraft1AfterReplacement.getChannel();
    assertThat(channelReference1AfterReplacement).isNotNull();
    assertThat(channelReference1AfterReplacement.getKey()).isEqualTo(channel1Key);

    final CustomFieldsDraft custom1AfterReplacement = priceDraft1AfterReplacement.getCustom();
    assertThat(custom1AfterReplacement).isNotNull();
    final TypeResourceIdentifier customType1AfterReplacement = custom1AfterReplacement.getType();
    assertThat(customType1AfterReplacement).isNotNull();
    assertThat(customType1AfterReplacement.getKey()).isEqualTo(customTypeKey);

    final PriceDraft priceDraft2AfterReplacement = priceDrafts.get(1);
    final ChannelResourceIdentifier channelReference2AfterReplacement =
        priceDraft2AfterReplacement.getChannel();
    assertThat(channelReference2AfterReplacement).isNotNull();
    assertThat(channelReference2AfterReplacement.getKey()).isEqualTo(channel2Key);

    final CustomFieldsDraft custom2AfterReplacement = priceDraft2AfterReplacement.getCustom();
    assertThat(custom2AfterReplacement).isNotNull();
    final TypeResourceIdentifier customType2AfterReplacement = custom2AfterReplacement.getType();
    assertThat(customType2AfterReplacement).isNotNull();
    assertThat(customType2AfterReplacement.getKey()).isEqualTo(customTypeKey);
  }

  @Test
  void replaceAttributeReferenceIdWithKey_WithTextAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute = AttributeBuilder.of().name("attrName").value("value").build();
    final Optional<ProductReference> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).isEmpty();
  }

  @Test
  void
      replaceAttributeReferenceIdWithKey_WithProductReferenceSetAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute =
        AttributeBuilder.of().name("attrName").value(new HashSet<>()).build();
    final Optional<ProductReference> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).isEmpty();
  }

  @Test
  void replaceAttributeReferenceIdWithKey_WithProductReferenceAttribute_ShouldNotReplaceId() {
    final ProductReference unexpandedReference =
        ProductReferenceBuilder.of().id(UUID.randomUUID().toString()).build();
    final Attribute attribute =
        AttributeBuilder.of().name("attrName").value(unexpandedReference).build();
    final Optional<ProductReference> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).contains(unexpandedReference);
  }

  @Test
  void
      replaceAttributeReferenceIdWithKey_WithUnexpandedProductReferenceAttribute_ShouldReplaceId() {
    final String productAttributeKey = "productKey1";
    final String productAttributeId = UUID.randomUUID().toString();
    final ProductReference unexpandedReference =
        ProductReferenceBuilder.of().id(productAttributeId).build();

    referenceIdToKeyCache.add(productAttributeId, productAttributeKey);

    final Attribute attribute =
        AttributeBuilder.of().name("attrName").value(unexpandedReference).build();
    final Optional<ProductReference> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);
    assertThat(attributeReferenceIdWithKey)
        .contains(ProductReferenceBuilder.of().id(productAttributeKey).build());
  }

  @Test
  void replaceAttributeReferenceSetIdsWithKeys_WithTextAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute = AttributeBuilder.of().name("attrName").value("value").build();
    final Optional<Set<ProductReference>> attributeReferenceSetIdsWithKeys =
        replaceAttributeReferenceSetIdsWithKeys(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceSetIdsWithKeys).isEmpty();
  }

  @Test
  void replaceAttributesReferencesIdsWithKeys_WithNoAttributes_ShouldNotReplaceIds() {
    final ProductVariant variant = mock(ProductVariant.class);
    when(variant.getAttributes()).thenReturn(new ArrayList<>());
    final List<Attribute> replacedDrafts =
        replaceAttributesReferencesIdsWithKeys(variant, referenceIdToKeyCache);
    assertThat(replacedDrafts).isEmpty();
  }

  @Test
  void
      replaceAttributesReferencesIdsWithKeys_WithAttributesWithNoReferences_ShouldNotChangeAttributes() {
    final ProductProjection product =
        ProductMixin.toProjection(
            createObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class),
            ProductProjectionType.STAGED);
    final ProductVariant masterVariant = product.getMasterVariant();
    final List<Attribute> replacedDrafts =
        replaceAttributesReferencesIdsWithKeys(masterVariant, referenceIdToKeyCache);
    replacedDrafts.forEach(
        attributeDraft -> {
          final String name = attributeDraft.getName();
          final Attribute originalAttribute = masterVariant.getAttribute(name);
          assertThat(originalAttribute).isNotNull();
          assertThat(originalAttribute.getValue()).isEqualTo(attributeDraft.getValue());
        });
  }

  @Test
  void
      isProductReference_WithDifferentAttributeTypes_ShouldBeTrueForProductReferenceAttributeOnly() {
    assertThat(isProductReference(BOOLEAN_ATTRIBUTE_TRUE)).isFalse();
    assertThat(isProductReference(TEXT_ATTRIBUTE_BAR)).isFalse();
    assertThat(isProductReference(LTEXT_ATTRIBUTE_EN_BAR)).isFalse();
    assertThat(isProductReference(ENUM_ATTRIBUTE_BARLABEL_BARKEY)).isFalse();
    assertThat(isProductReference(LENUM_ATTRIBUTE_EN_BAR)).isFalse();
    assertThat(isProductReference(NUMBER_ATTRIBUTE_10)).isFalse();
    assertThat(isProductReference(MONEY_ATTRIBUTE_EUR_2300)).isFalse();
    assertThat(isProductReference(DATE_ATTRIBUTE_2017_11_09)).isFalse();
    assertThat(isProductReference(TIME_ATTRIBUTE_10_08_46)).isFalse();
    assertThat(isProductReference(DATE_TIME_ATTRIBUTE_2016_05_20T01_02_46)).isFalse();
    assertThat(isProductReference(PRODUCT_REFERENCE_SET_ATTRIBUTE)).isFalse();
    assertThat(isProductReference(CATEGORY_REFERENCE_ATTRIBUTE)).isFalse();
    assertThat(isProductReference(LTEXT_SET_ATTRIBUTE)).isFalse();
    assertThat(isProductReference(EMPTY_SET_ATTRIBUTE)).isFalse();
    assertThat(isProductReference(PRODUCT_REFERENCE_ATTRIBUTE)).isTrue();
  }

  @Test
  void
      isProductReferenceSet_WithDifferentAttributeTypes_ShouldBeTrueForProductReferenceSetAttributeOnly() {
    assertThat(isProductReferenceSet(BOOLEAN_ATTRIBUTE_TRUE)).isFalse();
    assertThat(isProductReferenceSet(TEXT_ATTRIBUTE_BAR)).isFalse();
    assertThat(isProductReferenceSet(LTEXT_ATTRIBUTE_EN_BAR)).isFalse();
    assertThat(isProductReferenceSet(ENUM_ATTRIBUTE_BARLABEL_BARKEY)).isFalse();
    assertThat(isProductReferenceSet(LENUM_ATTRIBUTE_EN_BAR)).isFalse();
    assertThat(isProductReferenceSet(NUMBER_ATTRIBUTE_10)).isFalse();
    assertThat(isProductReferenceSet(MONEY_ATTRIBUTE_EUR_2300)).isFalse();
    assertThat(isProductReferenceSet(DATE_ATTRIBUTE_2017_11_09)).isFalse();
    assertThat(isProductReferenceSet(TIME_ATTRIBUTE_10_08_46)).isFalse();
    assertThat(isProductReferenceSet(DATE_TIME_ATTRIBUTE_2016_05_20T01_02_46)).isFalse();
    assertThat(isProductReferenceSet(PRODUCT_REFERENCE_ATTRIBUTE)).isFalse();
    assertThat(isProductReferenceSet(CATEGORY_REFERENCE_ATTRIBUTE)).isFalse();
    assertThat(isProductReferenceSet(LTEXT_SET_ATTRIBUTE)).isFalse();
    assertThat(isProductReferenceSet(EMPTY_SET_ATTRIBUTE)).isFalse();
    assertThat(isProductReferenceSet(PRODUCT_REFERENCE_SET_ATTRIBUTE)).isTrue();
  }
}
