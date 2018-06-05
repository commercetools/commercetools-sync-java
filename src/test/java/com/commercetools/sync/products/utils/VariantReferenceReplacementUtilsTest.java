package com.commercetools.sync.products.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeAccess;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.BOOLEAN_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_REFERENCE_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.DATE_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.DATE_TIME_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.ENUM_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.LENUM_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.LTEXT_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.LTEXT_SET_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.MONEY_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.NUMBER_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_REFERENCE_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_REFERENCE_SET_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.TEXT_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.TIME_ATTRIBUTE;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.getPriceMockWithChannelReference;
import static com.commercetools.sync.products.ProductSyncMockUtils.getPriceMockWithReferences;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductVariantMock;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.isProductReference;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.isProductReferenceSet;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributeReferenceIdWithKey;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributeReferenceSetIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributesReferencesIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceChannelReferenceIdWithKey;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replacePricesReferencesIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceVariantsReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariantReferenceReplacementUtilsTest {

    @Test
    public void replaceVariantsReferenceIdsWithKeys_WithExpandedReferences_ShouldReturnVariantDraftsWithReplacedKeys() {
        final String channelKey = "channelKey";
        final Channel channel = getChannelMock(channelKey);

        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);

        final Price price = getPriceMockWithChannelReference(channelReference);

        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final Asset asset =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));

        final ProductVariant productVariant = getProductVariantMock(singletonList(price), singletonList(asset));

        final List<ProductVariantDraft> variantDrafts = replaceVariantsReferenceIdsWithKeys(
            singletonList(productVariant));

        assertThat(variantDrafts).hasSize(1);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement =
            variantDrafts.get(0).getPrices().get(0).getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelKey);

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> referenceReplacedType =
            variantDrafts.get(0).getAssets().get(0).getCustom().getType();
        assertThat(referenceReplacedType).isNotNull();
        assertThat(referenceReplacedType.getId()).isEqualTo(customType.getKey());
    }

    @Test
    public void replaceVariantsReferenceIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceSomeKeys() {
        final String channelKey1 = "channelKey1";
        final Channel channel1 = getChannelMock(channelKey1);

        final Reference<Channel> channelReference1 = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel1.getId(), channel1);
        final Reference<Channel> channelReference2 = Channel.referenceOfId(UUID.randomUUID().toString());

        final Price price1 = getPriceMockWithChannelReference(channelReference1);
        final Price price2 = getPriceMockWithChannelReference(channelReference2);

        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final Asset asset1 = getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(),
            customType));
        final Asset asset2 = getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
                UUID.randomUUID().toString()));

        final ProductVariant productVariant1 = getProductVariantMock(singletonList(price1), singletonList(asset1));
        final ProductVariant productVariant2 = getProductVariantMock(singletonList(price2), singletonList(asset2));

        final Product product = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
        final String uuid = UUID.randomUUID().toString();
        final Reference<Product> expandedReference =
            Reference.ofResourceTypeIdAndIdAndObj(Product.referenceTypeId(), uuid, product);
        final Attribute expandedProductRefAttribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), expandedReference);

        when(productVariant2.getAttributes()).thenReturn(singletonList(expandedProductRefAttribute));

        final Reference<Product> nonExpandedReference = Product.referenceOfId(uuid);
        final Attribute nonExpandedProductRefAttribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), nonExpandedReference);
        when(productVariant1.getAttributes()).thenReturn(singletonList(nonExpandedProductRefAttribute));


        final List<ProductVariantDraft> variantDrafts = replaceVariantsReferenceIdsWithKeys(
            asList(productVariant1, productVariant2));

        assertThat(variantDrafts).hasSize(2);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channel1ReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                  .getChannel();
        assertThat(channel1ReferenceAfterReplacement).isNotNull();
        assertThat(channel1ReferenceAfterReplacement.getId()).isEqualTo(channelKey1);

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> asset1CustomType = variantDrafts.get(0).getAssets().get(0).getCustom().getType();
        assertThat(asset1CustomType).isNotNull();
        assertThat(asset1CustomType.getId()).isEqualTo(customType.getKey());

        assertThat(variantDrafts.get(1).getPrices()).hasSize(1);
        final Reference<Channel> channel2ReferenceAfterReplacement = variantDrafts.get(1).getPrices().get(0)
                                                                                  .getChannel();
        assertThat(channel2ReferenceAfterReplacement).isNotNull();
        assertThat(channel2ReferenceAfterReplacement.getId()).isEqualTo(channelReference2.getId());

        assertThat(variantDrafts.get(1).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> asset2CustomType = variantDrafts.get(1).getAssets().get(0).getCustom().getType();
        assertThat(asset2CustomType).isNotNull();
        assertThat(asset2CustomType.getId()).isEqualTo(asset2.getCustom().getType().getId());

        final JsonNode productReference1Value = variantDrafts.get(0).getAttributes().get(0).getValue();
        assertThat(productReference1Value).isNotNull();
        assertThat(productReference1Value.get("id")).isNotNull();
        assertThat(productReference1Value.get("id").asText()).isEqualTo(uuid);


        final JsonNode productReference2Value = variantDrafts.get(1).getAttributes().get(0).getValue();
        assertThat(productReference2Value).isNotNull();
        assertThat(productReference2Value.get("id")).isNotNull();
        assertThat(productReference2Value.get("id").asText()).isEqualTo("productKey1");
    }

    @Test
    public void replaceVariantsReferenceIdsWithKeys_WithNoExpandedReferences_ShouldNotReplaceIds() {
        final Reference<Channel> channelReference = Channel.referenceOfId(UUID.randomUUID().toString());
        final Reference<Type> customTypeReference = Type.referenceOfId(UUID.randomUUID().toString());

        final Asset asset2 = getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
            UUID.randomUUID().toString()));

        final Price price = getPriceMockWithReferences(channelReference, customTypeReference);
        final ProductVariant productVariant = getProductVariantMock(singletonList(price), singletonList(asset2));

        final List<ProductVariantDraft> variantDrafts =
            replaceVariantsReferenceIdsWithKeys(singletonList(productVariant));

        assertThat(variantDrafts).hasSize(1);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                 .getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> customTypeReference = variantDrafts.get(0).getAssets().get(0)
                                                                          .getCustom().getType();
        assertThat(customTypeReference).isNotNull();
        assertThat(customTypeReference.getId()).isEqualTo(variantDrafts.get(0).getAssets().get(0)
                                                                       .getCustom().getType().getId());
    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithNoExpandedReferences_ShouldNotReplaceIds() {
        final Reference<Channel> channelReference = Channel.referenceOfId(UUID.randomUUID().toString());
        final Reference<Type> typeReference = Type.referenceOfId(UUID.randomUUID().toString());

        final Price price = getPriceMockWithReferences(channelReference, typeReference);
        final ProductVariant productVariant = getProductVariantMock(singletonList(price));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement = priceDrafts.get(0).getChannel();;
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());

        CustomFieldsDraft custom = priceDrafts.get(0).getCustom();
        assertThat(custom).isNotNull();
        assertThat(custom.getType()).isNotNull();
        assertThat(custom.getType().getId()).isEqualTo(typeReference.getId());
    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithAllExpandedReferences_ShouldReplaceIds() {
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        final String channelKey1 = "channelKey1";
        final String channelKey2 = "channelKey2";

        final Channel channel1 = getChannelMock(channelKey1);
        final Channel channel2 = getChannelMock(channelKey2);

        final Reference<Channel> channelReference1 =
            Reference.ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel1.getId(), channel1);
        final Reference<Channel> channelReference2 =
            Reference.ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel2.getId(), channel2);

        final Price price1 = getPriceMockWithChannelReference(channelReference1);
        final Price price2 = getPriceMockWithChannelReference(channelReference2);
        final Price price3 = getPriceMockWithReferences(channelReference1,
                Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));

        final ProductVariant productVariant = getProductVariantMock(asList(price1, price2, price3));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(3);
        final Reference<Channel> channelReference1AfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);
        final Reference<Channel> channelReference2AfterReplacement = priceDrafts.get(1).getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelKey2);

        CustomFieldsDraft customTypeAfterReplacement = priceDrafts.get(2).getCustom();
        assertThat(customTypeAfterReplacement).isNotNull();
        assertThat(customTypeAfterReplacement.getType()).isNotNull();
        assertThat(customTypeAfterReplacement.getType().getId()).isEqualTo(customType.getKey());

    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceOnlyExpandedIds() {
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final String channelKey1 = "channelKey1";

        final Channel channel1 = getChannelMock(channelKey1);

        final Reference<Channel> channelReference1 =
            Reference.ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel1.getId(), channel1);
        final Reference<Channel> channelReference2 = Channel.referenceOfId(UUID.randomUUID().toString());

        final Reference<Type> typeReference1 = Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType);
        final Reference<Type> typeReference2 = Type.referenceOfId(UUID.randomUUID().toString());

        final Price price1 = getPriceMockWithReferences(channelReference1, typeReference1);
        final Price price2 = getPriceMockWithReferences(channelReference2, typeReference2);

        final ProductVariant productVariant = getProductVariantMock(asList(price1, price2));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(2);
        final Reference<Channel> channelReference1AfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);
        final Reference<Channel> channelReference2AfterReplacement = priceDrafts.get(1).getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelReference2.getId());

        CustomFieldsDraft customType1AfterReplacement = priceDrafts.get(0).getCustom();
        assertThat(customType1AfterReplacement).isNotNull();
        assertThat(customType1AfterReplacement.getType()).isNotNull();
        assertThat(customType1AfterReplacement.getType().getId()).isEqualTo(customType.getKey());

        CustomFieldsDraft customType2AfterReplacement = priceDrafts.get(1).getCustom();
        assertThat(customType2AfterReplacement).isNotNull();
        assertThat(customType2AfterReplacement.getType()).isNotNull();
        assertThat(customType2AfterReplacement.getType().getId()).isEqualTo(typeReference2.getId());
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferenceWithoutReplacedKeys() {
        final String channelId = UUID.randomUUID().toString();
        final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
        final Price price = getPriceMockWithReferences(channelReference, null);

        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);

        assertThat(channelReferenceWithKey).isNotNull();
        assertThat(channelReferenceWithKey.getId()).isEqualTo(channelId);
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithExpandedReferences_ShouldReturnReplaceReferenceIdsWithKey() {
        final String channelKey = "channelKey";
        final Channel channel = getChannelMock(channelKey);

        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);
        final Price price = getPriceMockWithReferences(channelReference, null);


        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);

        assertThat(channelReferenceWithKey).isNotNull();
        assertThat(channelReferenceWithKey.getId()).isEqualTo(channelKey);
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithNullChannelReference_ShouldReturnNull() {
        final Price price = getPriceMockWithReferences(null, null);

        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);

        assertThat(channelReferenceWithKey).isNull();
    }

    @Test
    public void replaceAttributeReferenceIdWithKey_WithTextAttribute_ShouldReturnEmptyOptional() {
        final Attribute attribute = Attribute.of("attrName", AttributeAccess.ofText(), "value");
        final Optional<Reference<Product>> attributeReferenceIdWithKey = replaceAttributeReferenceIdWithKey(attribute);

        assertThat(attributeReferenceIdWithKey).isEmpty();
    }

    @Test
    public void replaceAttributeReferenceIdWithKey_WithProductReferenceSetAttribute_ShouldReturnEmptyOptional() {
        final Attribute attribute =
            Attribute.of("attrName", AttributeAccess.ofProductReferenceSet(), new HashSet<>());
        final Optional<Reference<Product>> attributeReferenceIdWithKey = replaceAttributeReferenceIdWithKey(attribute);

        assertThat(attributeReferenceIdWithKey).isEmpty();
    }

    @Test
    public void replaceAttributeReferenceIdWithKey_WithNonExpandedProductReferenceAttribute_ShouldNotReplaceId() {
        final Reference<Product> nonExpandedReference = Product.referenceOfId(UUID.randomUUID().toString());
        final Attribute attribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), nonExpandedReference);
        final Optional<Reference<Product>> attributeReferenceIdWithKey = replaceAttributeReferenceIdWithKey(attribute);

        assertThat(attributeReferenceIdWithKey).contains(nonExpandedReference);
    }

    @Test
    public void replaceAttributeReferenceIdWithKey_WithExpandedProductReferenceAttribute_ShouldReplaceId() {
        final Product product = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
        final Reference<Product> expandedReference =
            Reference.ofResourceTypeIdAndIdAndObj(Product.referenceTypeId(), UUID.randomUUID().toString(), product);
        final Attribute attribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), expandedReference);
        final Optional<Reference<Product>> attributeReferenceIdWithKey = replaceAttributeReferenceIdWithKey(attribute);
        assertThat(attributeReferenceIdWithKey).contains(Product.referenceOfId("productKey1"));
    }

    @Test
    public void replaceAttributeReferenceSetIdsWithKeys_WithTextAttribute_ShouldReturnEmptyOptional() {
        final Attribute attribute = Attribute.of("attrName", AttributeAccess.ofText(), "value");
        final Optional<Set<Reference<Product>>> attributeReferenceSetIdsWithKeys =
            replaceAttributeReferenceSetIdsWithKeys(attribute);

        assertThat(attributeReferenceSetIdsWithKeys).isEmpty();
    }

    @Test
    public void replaceAttributesReferencesIdsWithKeys_WithNoAttributes_ShouldNotReplaceIds() {
        final ProductVariant variant = mock(ProductVariant.class);
        when(variant.getAttributes()).thenReturn(new ArrayList<>());
        final List<AttributeDraft> replacedDrafts = replaceAttributesReferencesIdsWithKeys(variant);
        assertThat(replacedDrafts).isEmpty();
    }

    @Test
    public void replaceAttributesReferencesIdsWithKeys_WithAttributesWithNoReferences_ShouldNotChangeAttributes() {
        final Product product = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
        final ProductVariant masterVariant = product.getMasterData().getStaged().getMasterVariant();
        final List<AttributeDraft> replacedDrafts = replaceAttributesReferencesIdsWithKeys(masterVariant);
        replacedDrafts.forEach(attributeDraft -> {
            final String name = attributeDraft.getName();
            final Attribute originalAttribute = masterVariant.getAttribute(name);
            assertThat(originalAttribute).isNotNull();
            assertThat(originalAttribute.getValueAsJsonNode()).isEqualTo(attributeDraft.getValue());
        });
    }

    @Test
    public void isProductReference_WithDifferentAttributeTypes_ShouldBeTrueForProductReferenceAttributeOnly() {
        final Attribute booleanAttribute = readObjectFromResource(BOOLEAN_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(booleanAttribute)).isFalse();

        final Attribute textAttribute = readObjectFromResource(TEXT_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(textAttribute)).isFalse();

        final Attribute ltextAttribute = readObjectFromResource(LTEXT_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(ltextAttribute)).isFalse();

        final Attribute enumAttribute = readObjectFromResource(ENUM_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(enumAttribute)).isFalse();

        final Attribute lenumAttribute = readObjectFromResource(LENUM_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(lenumAttribute)).isFalse();

        final Attribute numberAttribute = readObjectFromResource(NUMBER_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(numberAttribute)).isFalse();

        final Attribute moneyAttribute = readObjectFromResource(MONEY_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(moneyAttribute)).isFalse();

        final Attribute dateAttribute = readObjectFromResource(DATE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(dateAttribute)).isFalse();

        final Attribute timeAttribute = readObjectFromResource(TIME_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(timeAttribute)).isFalse();

        final Attribute dateTimeAttribute = readObjectFromResource(DATE_TIME_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(dateTimeAttribute)).isFalse();

        final Attribute productReferenceSetAttribute =
            readObjectFromResource(PRODUCT_REFERENCE_SET_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(productReferenceSetAttribute)).isFalse();

        final Attribute categoryReferenceAttribute =
            readObjectFromResource(CATEGORY_REFERENCE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(categoryReferenceAttribute)).isFalse();

        final Attribute ltextSetAttribute = readObjectFromResource(LTEXT_SET_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(ltextSetAttribute)).isFalse();

        final Attribute productReferenceAttribute =
            readObjectFromResource(PRODUCT_REFERENCE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReference(productReferenceAttribute)).isTrue();
    }

    @Test
    public void isProductReferenceSet_WithDifferentAttributeTypes_ShouldBeTrueForProductReferenceSetAttributeOnly() {
        final Attribute booleanAttribute = readObjectFromResource(BOOLEAN_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(booleanAttribute)).isFalse();

        final Attribute textAttribute = readObjectFromResource(TEXT_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(textAttribute)).isFalse();

        final Attribute ltextAttribute = readObjectFromResource(LTEXT_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(ltextAttribute)).isFalse();

        final Attribute enumAttribute = readObjectFromResource(ENUM_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(enumAttribute)).isFalse();

        final Attribute lenumAttribute = readObjectFromResource(LENUM_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(lenumAttribute)).isFalse();

        final Attribute numberAttribute = readObjectFromResource(NUMBER_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(numberAttribute)).isFalse();

        final Attribute moneyAttribute = readObjectFromResource(MONEY_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(moneyAttribute)).isFalse();

        final Attribute dateAttribute = readObjectFromResource(DATE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(dateAttribute)).isFalse();

        final Attribute timeAttribute = readObjectFromResource(TIME_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(timeAttribute)).isFalse();

        final Attribute dateTimeAttribute = readObjectFromResource(DATE_TIME_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(dateTimeAttribute)).isFalse();

        final Attribute productReferenceAttribute =
            readObjectFromResource(PRODUCT_REFERENCE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(productReferenceAttribute)).isFalse();

        final Attribute categoryReferenceAttribute =
            readObjectFromResource(CATEGORY_REFERENCE_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(categoryReferenceAttribute)).isFalse();

        final Attribute ltextSetAttribute = readObjectFromResource(LTEXT_SET_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(ltextSetAttribute)).isFalse();

        final Attribute productReferenceSetAttribute =
            readObjectFromResource(PRODUCT_REFERENCE_SET_ATTRIBUTE, Attribute.class);
        assertThat(isProductReferenceSet(productReferenceSetAttribute)).isTrue();
    }
}
