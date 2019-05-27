package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncMockUtils;
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
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
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

public class VariantReferenceReplacementUtilsTest {

    @Test
    public void replaceVariantsReferenceIdsWithKeys_WithExpandedReferences_ShouldReturnVariantDraftsWithReplacedKeys() {
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        final String channelKey = "channelKey";
        final Channel channel = getChannelMock(channelKey);

        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);

        final Reference<Type> priceCustomTypeReference =
            Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType);

        final Price price = ProductSyncMockUtils.getPriceMockWithReferences(channelReference, priceCustomTypeReference);

        final Asset asset =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));

        final ProductVariant productVariant = getProductVariantMock(singletonList(price), singletonList(asset));

        final List<ProductVariantDraft> variantDrafts = replaceVariantsReferenceIdsWithKeys(
            singletonList(productVariant));

        assertThat(variantDrafts).hasSize(1);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final ResourceIdentifier<Channel> channelReferenceAfterReplacement =
            variantDrafts.get(0).getPrices().get(0).getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        // Assert that price channel reference id is replaced with key.
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelKey);

        final CustomFieldsDraft priceCustomAfterReplacement = variantDrafts.get(0).getPrices().get(0).getCustom();
        assertThat(priceCustomAfterReplacement).isNotNull();
        final ResourceIdentifier<Type> priceCustomTypeAfterReplacement = priceCustomAfterReplacement.getType();
        assertThat(priceCustomTypeAfterReplacement).isNotNull();
        // Assert that price custom type reference id is replaced with key.
        assertThat(priceCustomTypeAfterReplacement.getId()).isEqualTo(customType.getKey());

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> referenceReplacedType =
            variantDrafts.get(0).getAssets().get(0).getCustom().getType();
        assertThat(referenceReplacedType).isNotNull();
        // Assert that price asset custom type reference id is replaced with key.
        assertThat(referenceReplacedType.getId()).isEqualTo(customType.getKey());
    }

    @Test
    public void replaceVariantsReferenceIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceSomeKeys() {
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        final Reference<Type> priceCustomTypeReference1 =
            Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType);
        final Reference<Type> priceCustomTypeReference2 = Type.referenceOfId(UUID.randomUUID().toString());

        final String channelKey1 = "channelKey1";
        final Channel channel1 = getChannelMock(channelKey1);

        final Reference<Channel> channelReference1 = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel1.getId(), channel1);
        final Reference<Channel> channelReference2 = Channel.referenceOfId(UUID.randomUUID().toString());

        final Price price1 = getPriceMockWithReferences(channelReference1, priceCustomTypeReference1);
        final Price price2 = getPriceMockWithReferences(channelReference2, priceCustomTypeReference2);

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
        final ResourceIdentifier<Channel> channel1ReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                           .getChannel();
        assertThat(channel1ReferenceAfterReplacement).isNotNull();
        assertThat(channel1ReferenceAfterReplacement.getId()).isEqualTo(channelKey1);

        final CustomFieldsDraft price1CustomFieldsAfterReplacement =
            variantDrafts.get(0).getPrices().get(0).getCustom();
        assertThat(price1CustomFieldsAfterReplacement).isNotNull();
        final ResourceIdentifier<Type> priceCustomType1ReferenceAfterReplacement =
            price1CustomFieldsAfterReplacement.getType();
        assertThat(priceCustomType1ReferenceAfterReplacement).isNotNull();
        // Asset price custom type reference id is replaced with key.
        assertThat(priceCustomType1ReferenceAfterReplacement.getId()).isEqualTo(customType.getKey());

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> asset1CustomType = variantDrafts.get(0).getAssets().get(0).getCustom().getType();
        assertThat(asset1CustomType).isNotNull();
        assertThat(asset1CustomType.getId()).isEqualTo(customType.getKey());

        assertThat(variantDrafts.get(1).getPrices()).hasSize(1);
        final ResourceIdentifier<Channel> channel2ReferenceAfterReplacement = variantDrafts.get(1).getPrices().get(0)
                                                                                           .getChannel();
        assertThat(channel2ReferenceAfterReplacement).isNotNull();
        // Asset price channel reference id is not replaced.
        assertThat(channel2ReferenceAfterReplacement.getId()).isEqualTo(channelReference2.getId());

        final CustomFieldsDraft price2CustomFieldsAfterReplacement =
            variantDrafts.get(1).getPrices().get(0).getCustom();
        assertThat(price2CustomFieldsAfterReplacement).isNotNull();
        final ResourceIdentifier<Type> priceCustomType2ReferenceAfterReplacement =
            price2CustomFieldsAfterReplacement.getType();
        assertThat(priceCustomType2ReferenceAfterReplacement).isNotNull();
        // Asset price custom type reference id is not replaced.
        assertThat(priceCustomType2ReferenceAfterReplacement.getId()).isEqualTo(priceCustomTypeReference2.getId());

        assertThat(variantDrafts.get(1).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> asset2CustomType = variantDrafts.get(1).getAssets().get(0).getCustom().getType();
        assertThat(asset2CustomType).isNotNull();
        // Asset price asset custom type reference id is not replaced.
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
        final ResourceIdentifier<Channel> channelReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                          .getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        // Assert price channel reference id is not replaced.
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());


        final CustomFieldsDraft priceCustomFields = variantDrafts.get(0).getPrices().get(0).getCustom();
        assertThat(priceCustomFields).isNotNull();
        final ResourceIdentifier<Type> priceCustomTypeAfterReplacement = priceCustomFields.getType();
        // Assert price custom type reference id is not replaced.
        assertThat(priceCustomTypeAfterReplacement.getId()).isEqualTo(customTypeReference.getId());

        assertThat(variantDrafts.get(0).getAssets()).hasSize(1);
        final ResourceIdentifier<Type> assetCustomTypeReference = variantDrafts.get(0).getAssets().get(0)
                                                                               .getCustom().getType();
        assertThat(assetCustomTypeReference).isNotNull();
        // Assert asset custom type reference id is not replaced.
        assertThat(assetCustomTypeReference.getId()).isEqualTo(variantDrafts.get(0).getAssets().get(0)
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
        final PriceDraft priceDraftAfterReplacement = priceDrafts.get(0);

        final ResourceIdentifier<Channel> channelReferenceAfterReplacement = priceDraftAfterReplacement.getChannel();
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
        final Reference<Type> customTypeReference =
            Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType);

        final Price price1 = ProductSyncMockUtils.getPriceMockWithReferences(channelReference1, customTypeReference);
        final Price price2 = ProductSyncMockUtils.getPriceMockWithReferences(channelReference2, customTypeReference);

        final ProductVariant productVariant = getProductVariantMock(asList(price1, price2));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(2);

        final PriceDraft priceDraft1AfterReplacement = priceDrafts.get(0);
        final ResourceIdentifier<Channel> channelReference1AfterReplacement = priceDraft1AfterReplacement.getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        // Assert id is replaced with key.
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);

        final CustomFieldsDraft custom1AfterReplacement = priceDraft1AfterReplacement.getCustom();
        assertThat(custom1AfterReplacement).isNotNull();
        final ResourceIdentifier<Type> customType1AfterReplacement = custom1AfterReplacement.getType();
        assertThat(customType1AfterReplacement).isNotNull();
        // Assert id is replaced with key.
        assertThat(customType1AfterReplacement.getId()).isEqualTo(customType.getKey());


        final PriceDraft priceDraft2AfterReplacement = priceDrafts.get(1);
        final ResourceIdentifier<Channel> channelReference2AfterReplacement = priceDraft2AfterReplacement.getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        // Assert id is replaced with key.
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelKey2);

        final CustomFieldsDraft custom2AfterReplacement = priceDraft2AfterReplacement.getCustom();
        assertThat(custom2AfterReplacement).isNotNull();
        final ResourceIdentifier<Type> customType2AfterReplacement = custom2AfterReplacement.getType();
        assertThat(customType2AfterReplacement).isNotNull();
        // Assert id is replaced with key.
        assertThat(customType2AfterReplacement.getId()).isEqualTo(customType.getKey());
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

        final ResourceIdentifier<Channel> channelReference1AfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        // Assert expanded reference has id replaced with key.
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);

        final ResourceIdentifier<Channel> channelReference2AfterReplacement = priceDrafts.get(1).getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        // Assert non expanded reference has id not replaced.
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelReference2.getId());

        final CustomFieldsDraft customType1AfterReplacement = priceDrafts.get(0).getCustom();
        assertThat(customType1AfterReplacement).isNotNull();
        assertThat(customType1AfterReplacement.getType()).isNotNull();
        // Assert expanded reference has id replaced with key.
        assertThat(customType1AfterReplacement.getType().getId()).isEqualTo(customType.getKey());

        final CustomFieldsDraft customType2AfterReplacement = priceDrafts.get(1).getCustom();
        assertThat(customType2AfterReplacement).isNotNull();
        assertThat(customType2AfterReplacement.getType()).isNotNull();
        // Assert expanded reference has id not replaced.
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
    public void isProductReferenceSet_WithDifferentAttributeTypes_ShouldBeTrueForProductReferenceSetAttributeOnly() {
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
