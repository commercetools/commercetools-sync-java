package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.getPriceMockWithReferences;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductVariantMock;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.isProductReference;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.isProductReferenceSet;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.mapToPriceDrafts;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.mapToProductVariantDrafts;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.replaceAttributeReferenceIdWithKey;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.replaceAttributeReferenceSetIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceResolutionUtils.replaceAttributesReferencesIdsWithKeys;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.BOOLEAN_ATTRIBUTE_TRUE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.CATEGORY_REFERENCE_ATTRIBUTE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.DATE_ATTRIBUTE_2017_11_09;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.DATE_TIME_ATTRIBUTE_2016_05_20T01_02_46;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.EMPTY_SET_ATTRIBUTE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.ENUM_ATTRIBUTE_BARLABEL_BARKEY;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.LENUM_ATTRIBUTE_EN_BAR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.LTEXT_ATTRIBUTE_EN_BAR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.LTEXT_SET_ATTRIBUTE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.MONEY_ATTRIBUTE_EUR_2300;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.NUMBER_ATTRIBUTE_10;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.PRODUCT_REFERENCE_ATTRIBUTE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.PRODUCT_REFERENCE_SET_ATTRIBUTE;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TEXT_ATTRIBUTE_BAR;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes.AttributeFixtures.TIME_ATTRIBUTE_10_08_46;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeAccess;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
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

    final Reference<Channel> channelReference = Reference.of(Channel.referenceTypeId(), channelId);

    final Reference<Type> priceCustomTypeReference =
        Reference.of(Type.referenceTypeId(), customTypeId);

    final String customerGroupId = "customer-group-id";
    final String customerGroupKey = "customer-group-key";
    referenceIdToKeyCache.add(customerGroupId, customerGroupKey);

    final Reference<CustomerGroup> customerGroupReference =
        Reference.of(CustomerGroup.referenceTypeId(), customerGroupId);

    final Price price =
        getPriceMockWithReferences(
            channelReference, priceCustomTypeReference, customerGroupReference);

    final Asset asset =
        getAssetMockWithCustomFields(Reference.of(Type.referenceTypeId(), customTypeId));

    final ProductVariant productVariant =
        getProductVariantMock(singletonList(price), singletonList(asset));

    final List<ProductVariantDraft> variantDrafts =
        mapToProductVariantDrafts(singletonList(productVariant), referenceIdToKeyCache);

    assertThat(variantDrafts).hasSize(1);
    assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
    final ResourceIdentifier<Channel> channelReferenceAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    assertThat(channelReferenceAfterReplacement.getKey()).isEqualTo(channelKey);

    final CustomFieldsDraft priceCustomAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getCustom();
    assertThat(priceCustomAfterReplacement).isNotNull();
    final ResourceIdentifier<Type> priceCustomTypeAfterReplacement =
        priceCustomAfterReplacement.getType();
    assertThat(priceCustomTypeAfterReplacement).isNotNull();
    assertThat(priceCustomTypeAfterReplacement.getKey()).isEqualTo(customTypeKey);

    assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
    final ResourceIdentifier<Type> referenceReplacedType =
        variantDrafts.get(0).getAssets().get(0).getCustom().getType();
    assertThat(referenceReplacedType).isNotNull();
    assertThat(referenceReplacedType.getKey()).isEqualTo(customTypeKey);
  }

  @Test
  void mapToProductVariantDrafts_WithReferenceIdToKeyValuesNoneCached_ShouldNotReplaceIds() {
    final Reference<Channel> channelReference = Channel.referenceOfId(UUID.randomUUID().toString());
    final Reference<Type> customTypeReference = Type.referenceOfId(UUID.randomUUID().toString());

    final Asset asset2 =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), UUID.randomUUID().toString()));

    final Price price = getPriceMockWithReferences(channelReference, customTypeReference, null);
    final ProductVariant productVariant =
        getProductVariantMock(singletonList(price), singletonList(asset2));

    final List<ProductVariantDraft> variantDrafts =
        mapToProductVariantDrafts(singletonList(productVariant), referenceIdToKeyCache);

    assertThat(variantDrafts).hasSize(1);
    assertThat(variantDrafts.get(0).getPrices()).hasSize(1);

    final ResourceIdentifier<Channel> channelReferenceAfterReplacement =
        variantDrafts.get(0).getPrices().get(0).getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    // Assert price channel reference id is not replaced.
    assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

    final CustomFieldsDraft priceCustomFields = variantDrafts.get(0).getPrices().get(0).getCustom();
    assertThat(priceCustomFields).isNotNull();
    final ResourceIdentifier<Type> priceCustomTypeAfterReplacement = priceCustomFields.getType();
    // Assert price custom type reference id is not replaced.
    assertThat(priceCustomTypeAfterReplacement.getId()).isEqualTo(customTypeReference.getId());

    assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
    final ResourceIdentifier<Type> assetCustomTypeReference =
        variantDrafts.get(0).getAssets().get(0).getCustom().getType();
    assertThat(assetCustomTypeReference).isNotNull();
    // Assert asset custom type reference id is not replaced.
    assertThat(assetCustomTypeReference.getId())
        .isEqualTo(variantDrafts.get(0).getAssets().get(0).getCustom().getType().getId());
  }

  @Test
  void mapToPriceDraft_WithReferenceIdToKeyValuesNoneCached_ShouldNotReplaceIds() {
    final Reference<Channel> channelReference = Channel.referenceOfId(UUID.randomUUID().toString());
    final Reference<Type> typeReference = Type.referenceOfId(UUID.randomUUID().toString());

    final Price price = getPriceMockWithReferences(channelReference, typeReference, null);
    final ProductVariant productVariant = getProductVariantMock(singletonList(price));

    final List<PriceDraft> priceDrafts = mapToPriceDrafts(productVariant, referenceIdToKeyCache);

    assertThat(priceDrafts).hasSize(1);
    final PriceDraft priceDraftAfterReplacement = priceDrafts.get(0);

    final ResourceIdentifier<Channel> channelReferenceAfterReplacement =
        priceDraftAfterReplacement.getChannel();
    assertThat(channelReferenceAfterReplacement).isNotNull();
    // Assert Id is not replaced with key.
    assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

    final CustomFieldsDraft customAfterReplacement = priceDraftAfterReplacement.getCustom();
    assertThat(customAfterReplacement).isNotNull();
    final ResourceIdentifier<Type> customTypeAfterReplacement = customAfterReplacement.getType();

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

    final Reference<Channel> channelReference1 =
        Reference.of(Channel.referenceTypeId(), channel1Id);
    final Reference<Channel> channelReference2 =
        Reference.of(Channel.referenceTypeId(), channel2Id);
    final Reference<Type> customTypeReference = Reference.of(Type.referenceTypeId(), customTypeId);

    final Price price1 = getPriceMockWithReferences(channelReference1, customTypeReference, null);
    final Price price2 = getPriceMockWithReferences(channelReference2, customTypeReference, null);

    final ProductVariant productVariant = getProductVariantMock(asList(price1, price2));

    final List<PriceDraft> priceDrafts = mapToPriceDrafts(productVariant, referenceIdToKeyCache);

    assertThat(priceDrafts).hasSize(2);

    final PriceDraft priceDraft1AfterReplacement = priceDrafts.get(0);
    final ResourceIdentifier<Channel> channelReference1AfterReplacement =
        priceDraft1AfterReplacement.getChannel();
    assertThat(channelReference1AfterReplacement).isNotNull();
    assertThat(channelReference1AfterReplacement.getKey()).isEqualTo(channel1Key);

    final CustomFieldsDraft custom1AfterReplacement = priceDraft1AfterReplacement.getCustom();
    assertThat(custom1AfterReplacement).isNotNull();
    final ResourceIdentifier<Type> customType1AfterReplacement = custom1AfterReplacement.getType();
    assertThat(customType1AfterReplacement).isNotNull();
    assertThat(customType1AfterReplacement.getKey()).isEqualTo(customTypeKey);

    final PriceDraft priceDraft2AfterReplacement = priceDrafts.get(1);
    final ResourceIdentifier<Channel> channelReference2AfterReplacement =
        priceDraft2AfterReplacement.getChannel();
    assertThat(channelReference2AfterReplacement).isNotNull();
    assertThat(channelReference2AfterReplacement.getKey()).isEqualTo(channel2Key);

    final CustomFieldsDraft custom2AfterReplacement = priceDraft2AfterReplacement.getCustom();
    assertThat(custom2AfterReplacement).isNotNull();
    final ResourceIdentifier<Type> customType2AfterReplacement = custom2AfterReplacement.getType();
    assertThat(customType2AfterReplacement).isNotNull();
    assertThat(customType2AfterReplacement.getKey()).isEqualTo(customTypeKey);
  }

  @Test
  void replaceAttributeReferenceIdWithKey_WithTextAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute = Attribute.of("attrName", AttributeAccess.ofText(), "value");
    final Optional<Reference<Product>> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).isEmpty();
  }

  @Test
  void
      replaceAttributeReferenceIdWithKey_WithProductReferenceSetAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute =
        Attribute.of("attrName", AttributeAccess.ofProductReferenceSet(), new HashSet<>());
    final Optional<Reference<Product>> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).isEmpty();
  }

  @Test
  void replaceAttributeReferenceIdWithKey_WithProductReferenceAttribute_ShouldNotReplaceId() {
    final Reference<Product> unexpandedReference =
        Product.referenceOfId(UUID.randomUUID().toString());
    final Attribute attribute =
        Attribute.of("attrName", AttributeAccess.ofProductReference(), unexpandedReference);
    final Optional<Reference<Product>> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceIdWithKey).contains(unexpandedReference);
  }

  @Test
  void
      replaceAttributeReferenceIdWithKey_WithUnexpandedProductReferenceAttribute_ShouldReplaceId() {
    final String productAttributeKey = "productKey1";
    final String productAttributeId = UUID.randomUUID().toString();
    final Reference<Product> unexpandedReference =
        Reference.ofResourceTypeIdAndId(Product.referenceTypeId(), productAttributeId);

    referenceIdToKeyCache.add(productAttributeId, productAttributeKey);

    final Attribute attribute =
        Attribute.of("attrName", AttributeAccess.ofProductReference(), unexpandedReference);
    final Optional<Reference<Product>> attributeReferenceIdWithKey =
        replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache);
    assertThat(attributeReferenceIdWithKey).contains(Product.referenceOfId(productAttributeKey));
  }

  @Test
  void replaceAttributeReferenceSetIdsWithKeys_WithTextAttribute_ShouldReturnEmptyOptional() {
    final Attribute attribute = Attribute.of("attrName", AttributeAccess.ofText(), "value");
    final Optional<Set<Reference<Product>>> attributeReferenceSetIdsWithKeys =
        replaceAttributeReferenceSetIdsWithKeys(attribute, referenceIdToKeyCache);

    assertThat(attributeReferenceSetIdsWithKeys).isEmpty();
  }

  @Test
  void replaceAttributesReferencesIdsWithKeys_WithNoAttributes_ShouldNotReplaceIds() {
    final ProductVariant variant = mock(ProductVariant.class);
    when(variant.getAttributes()).thenReturn(new ArrayList<>());
    final List<AttributeDraft> replacedDrafts =
        replaceAttributesReferencesIdsWithKeys(variant, referenceIdToKeyCache);
    assertThat(replacedDrafts).isEmpty();
  }

  @Test
  void
      replaceAttributesReferencesIdsWithKeys_WithAttributesWithNoReferences_ShouldNotChangeAttributes() {
    final ProductProjection product =
        readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
            .toProjection(ProductProjectionType.STAGED);
    final ProductVariant masterVariant = product.getMasterVariant();
    final List<AttributeDraft> replacedDrafts =
        replaceAttributesReferencesIdsWithKeys(masterVariant, referenceIdToKeyCache);
    replacedDrafts.forEach(
        attributeDraft -> {
          final String name = attributeDraft.getName();
          final Attribute originalAttribute = masterVariant.getAttribute(name);
          assertThat(originalAttribute).isNotNull();
          assertThat(originalAttribute.getValueAsJsonNode()).isEqualTo(attributeDraft.getValue());
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
