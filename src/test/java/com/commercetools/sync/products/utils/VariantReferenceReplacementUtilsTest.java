package com.commercetools.sync.products.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeAccess;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.getPriceMockWithChannelReference;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductVariantMockWithPrices;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributeReferenceIdWithKey;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributeReferenceSetIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceAttributesReferencesIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceChannelReferenceIdWithKey;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replacePricesReferencesIdsWithKeys;
import static com.commercetools.sync.products.utils.VariantReferenceReplacementUtils.replaceVariantsReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
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
        final ProductVariant productVariant = getProductVariantMockWithPrices(Collections.singletonList(price));

        final List<ProductVariantDraft> variantDrafts = replaceVariantsReferenceIdsWithKeys(
            Collections.singletonList(productVariant));

        assertThat(variantDrafts).hasSize(1);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement =
            variantDrafts.get(0).getPrices().get(0).getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelKey);
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

        final ProductVariant productVariant1 = getProductVariantMockWithPrices(Collections.singletonList(price1));
        final ProductVariant productVariant2 = getProductVariantMockWithPrices(Collections.singletonList(price2));

        final Product product = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
        final String uuid = UUID.randomUUID().toString();
        final Reference<Product> expandedReference =
            Reference.ofResourceTypeIdAndIdAndObj(Product.referenceTypeId(), uuid, product);
        final Attribute expandedProductRefAttribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), expandedReference);

        when(productVariant2.getAttributes()).thenReturn(Collections.singletonList(expandedProductRefAttribute));

        final Reference<Product> nonExpandedReference = Product.referenceOfId(uuid);
        final Attribute nonExpandedProductRefAttribute =
            Attribute.of("attrName", AttributeAccess.ofProductReference(), nonExpandedReference);
        when(productVariant1.getAttributes()).thenReturn(Collections.singletonList(nonExpandedProductRefAttribute));


        final List<ProductVariantDraft> variantDrafts = replaceVariantsReferenceIdsWithKeys(
            Arrays.asList(productVariant1, productVariant2));

        assertThat(variantDrafts).hasSize(2);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channel1ReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                  .getChannel();
        assertThat(channel1ReferenceAfterReplacement).isNotNull();
        assertThat(channel1ReferenceAfterReplacement.getId()).isEqualTo(channelKey1);

        assertThat(variantDrafts.get(1).getPrices()).hasSize(1);
        final Reference<Channel> channel2ReferenceAfterReplacement = variantDrafts.get(1).getPrices().get(0)
                                                                                  .getChannel();
        assertThat(channel2ReferenceAfterReplacement).isNotNull();
        assertThat(channel2ReferenceAfterReplacement.getId()).isEqualTo(channelReference2.getId());

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

        final Price price = getPriceMockWithChannelReference(channelReference);
        final ProductVariant productVariant = getProductVariantMockWithPrices(Collections.singletonList(price));

        final List<ProductVariantDraft> variantDrafts =
            replaceVariantsReferenceIdsWithKeys(Collections.singletonList(productVariant));

        assertThat(variantDrafts).hasSize(1);
        assertThat(variantDrafts.get(0).getPrices()).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement = variantDrafts.get(0).getPrices().get(0)
                                                                                 .getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());
    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithNoExpandedReferences_ShouldNotReplaceIds() {
        final Reference<Channel> channelReference = Channel.referenceOfId(UUID.randomUUID().toString());

        final Price price = getPriceMockWithChannelReference(channelReference);
        final ProductVariant productVariant = getProductVariantMockWithPrices(Collections.singletonList(price));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(1);
        final Reference<Channel> channelReferenceAfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReferenceAfterReplacement).isNotNull();
        assertThat(channelReferenceAfterReplacement.getId()).isEqualTo(channelReference.getId());
    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithAllExpandedReferences_ShouldReplaceIds() {
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
        final ProductVariant productVariant = getProductVariantMockWithPrices(Arrays.asList(price1, price2));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(2);
        final Reference<Channel> channelReference1AfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);
        final Reference<Channel> channelReference2AfterReplacement = priceDrafts.get(1).getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelKey2);
    }

    @Test
    public void replacePricesReferencesIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceOnlyExpandedIds() {
        final String channelKey1 = "channelKey1";

        final Channel channel1 = getChannelMock(channelKey1);

        final Reference<Channel> channelReference1 =
            Reference.ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel1.getId(), channel1);
        final Reference<Channel> channelReference2 = Channel.referenceOfId(UUID.randomUUID().toString());

        final Price price1 = getPriceMockWithChannelReference(channelReference1);
        final Price price2 = getPriceMockWithChannelReference(channelReference2);
        final ProductVariant productVariant = getProductVariantMockWithPrices(Arrays.asList(price1, price2));

        final List<PriceDraft> priceDrafts = replacePricesReferencesIdsWithKeys(productVariant);

        assertThat(priceDrafts).hasSize(2);
        final Reference<Channel> channelReference1AfterReplacement = priceDrafts.get(0).getChannel();
        assertThat(channelReference1AfterReplacement).isNotNull();
        assertThat(channelReference1AfterReplacement.getId()).isEqualTo(channelKey1);
        final Reference<Channel> channelReference2AfterReplacement = priceDrafts.get(1).getChannel();
        assertThat(channelReference2AfterReplacement).isNotNull();
        assertThat(channelReference2AfterReplacement.getId()).isEqualTo(channelReference2.getId());
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferenceWithoutReplacedKeys() {
        final String channelId = UUID.randomUUID().toString();
        final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
        final Price price = getPriceMockWithChannelReference(channelReference);


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
        final Price price = getPriceMockWithChannelReference(channelReference);


        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(price);

        assertThat(channelReferenceWithKey).isNotNull();
        assertThat(channelReferenceWithKey.getId()).isEqualTo(channelKey);
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithNullReference_ShouldReturnNull() {
        final Price price = getPriceMockWithChannelReference(null);

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
}
