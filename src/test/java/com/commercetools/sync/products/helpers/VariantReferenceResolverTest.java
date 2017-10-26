package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceSetAttributeDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithRandomId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariantReferenceResolverTest { ;
    private ProductService productService;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_ID = "productId";
    private VariantReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        final TypeService typeService = getMockTypeService();
        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        productService = getMockProductService(PRODUCT_ID);
        ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions, typeService, channelService, productService);
    }

    @Test
    public void resolvePricesReferences_WithNullPrices_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isNull();
    }

    @Test
    public void resolvePricesReferences_WithANullPrice_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().prices((PriceDraft) null);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isEmpty();
    }

    @Test
    public void resolvePricesReferences_WithPriceReferences_ShouldResolvePrices() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson("customTypeId", new HashMap<>());

        final PriceDraft priceDraft = PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
                                                       .custom(customFieldsDraft).build();

        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().prices(priceDraft);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isNotEmpty();
        final PriceDraft resolvedPriceDraft = resolvedBuilderPrices.get(0);
        assertThat(resolvedPriceDraft).isNotNull();
        assertThat(resolvedPriceDraft.getCustom()).isNotNull();
        assertThat(resolvedPriceDraft.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveAttributesReferences_WithNullAttributes_ShouldNotResolveAttributes() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAttributesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
        assertThat(resolvedBuilderAttributes).isNull();
    }

    @Test
    public void resolveAttributesReferences_WithANullAttribute_ShouldNotResolveAttributes() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().attributes((AttributeDraft) null);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAttributesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
        assertThat(resolvedBuilderAttributes).isEmpty();
    }

    @Test
    public void resolveAttributesReferences_WithMixedReference_ShouldResolveProductReferenceAttributes() {
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final ObjectNode categoryReference1 = JsonNodeFactory.instance.objectNode();
        categoryReference1.put("typeId", "category");
        categoryReference1.put("id", UUID.randomUUID().toString());

        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", categoryReference1);
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "textValue");

        final List<AttributeDraft> attributeDrafts =
            Arrays.asList(productReferenceSetAttribute, categoryReferenceAttribute, textAttribute);

        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of()
                                      .attributes(attributeDrafts);


        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAttributesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();

        assertThat(resolvedBuilderAttributes).hasSize(3);
        assertThat(resolvedBuilderAttributes).contains(categoryReferenceAttribute);
        assertThat(resolvedBuilderAttributes).contains(textAttribute);
        final AttributeDraft resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
        assertThat(resolvedProductReferenceSetAttribute).isNotNull();
        final JsonNode resolvedProductReferenceSetValue = resolvedProductReferenceSetAttribute.getValue();
        assertThat(resolvedProductReferenceSetValue).isNotNull();
        final JsonNode resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
        assertThat(resolvedProductReferenceValue).isNotNull();
        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get("id");
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    public void resolveAttributeReference_WithEmptyValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithEmptyValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithEmptyValue);
    }

    @Test
    public void resolveAttributeReference_WithNullValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithNullValue =
            AttributeDraft.of("attributeName", null);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithNullValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithNullValue);
    }

    @Test
    public void resolveAttributeReference_WithTextValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithTextValue =
            AttributeDraft.of("attributeName", "textValue");

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithTextValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithTextValue);
    }

    @Test
    public void resolveAttributeReference_WithProductReferenceAttribute_ShouldResolveAttribute() {
        when(productService.fetchCachedProductId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("typeId", "product");
        attributeValue.put("id", "nonExistingProductKey");
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft).isSameAs(productReferenceAttribute);
    }

    @Test
    public void resolveAttributeReference_WithNonExistingProductReferenceAttribute_ShouldNotResolveAttribute() {
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft).isNotSameAs(productReferenceAttribute);
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();
        assertThat(resolvedAttributeDraft.getValue().get("id")).isNotNull();
        assertThat(resolvedAttributeDraft.getValue().get("id").asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    public void resolveAttributeReference_WithEmptyReferenceSetAttribute_ShouldNotResolveReferences() {
        final ArrayNode referenceSet = JsonNodeFactory.instance.arrayNode();

        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", referenceSet);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).isEmpty();
    }

    @Test
    public void resolveAttributeReference_WithCategoryReferenceSetAttribute_ShouldNotResolveReferences() {
        final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
        categoryReference.put("typeId", "category");
        categoryReference.put("id", UUID.randomUUID().toString());

        final ObjectNode categoryReference1 = JsonNodeFactory.instance.objectNode();
        categoryReference1.put("typeId", "category");
        categoryReference1.put("id", UUID.randomUUID().toString());

        final ArrayNode referenceSet = JsonNodeFactory.instance.arrayNode();
        referenceSet.add(categoryReference);
        referenceSet.add(categoryReference1);

        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", referenceSet);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).isNotEmpty();
        assertThat(resolvedSet).contains(categoryReference, categoryReference1);
    }

    @Test
    public void resolveAttributeReference_WithProductReferenceSetAttribute_ShouldResolveReferences() {
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttributeDraft =
            getProductReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceSetAttributeDraft)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).isNotEmpty();
        final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
        resolvedReference.put("typeId", "product");
        resolvedReference.put("id", PRODUCT_ID);
        assertThat(resolvedSet).containsExactly(resolvedReference);
    }

    @Test
    public void resolveAttributeReference_WithNullReferenceInSetAttribute_ShouldResolveReferences() {
        final ObjectNode productReference = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute =
            getProductReferenceSetAttributeDraft("foo", productReference, null);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).isNotEmpty();
        final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
        resolvedReference.put("typeId", "product");
        resolvedReference.put("id", PRODUCT_ID);
        assertThat(resolvedSet).containsExactly(resolvedReference);
    }

    @Test
    public void resolveAttributeReference_WithNonExistingProductReferenceSetAttribute_ShouldNotResolveReferences() {
        when(productService.fetchCachedProductId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode productReference = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute =
            getProductReferenceSetAttributeDraft("foo", productReference);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).containsExactly(productReference);
    }

    @Test
    public void
        resolveAttributeReference_WithSomeExistingProductReferenceSetAttribute_ShouldResolveExistingReferences() {
        when(productService.fetchCachedProductId("existingKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
        when(productService.fetchCachedProductId("randomKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode productReference1 = getProductReferenceWithId("existingKey");
        final ObjectNode productReference2 = getProductReferenceWithId("randomKey");

        final AttributeDraft productReferenceAttribute =
            getProductReferenceSetAttributeDraft("foo", productReference1, productReference2);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());

        final ObjectNode resolvedReference1 = getProductReferenceWithId("existingId");
        final ObjectNode resolvedReference2 = getProductReferenceWithId("randomKey");
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
    }

    @Test
    public void isProductReference_WithEmptyValue_ShouldReturnFalse() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());
        assertThat(VariantReferenceResolver.isProductReference(attributeWithEmptyValue.getValue())).isFalse();
    }

    @Test
    public void isProductReference_WithTextAttribute_ShouldReturnFalse() {
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        assertThat(VariantReferenceResolver.isProductReference(textAttribute.getValue())).isFalse();
    }

    @Test
    public void isProductReference_WithNonReferenceAttribute_ShouldReturnFalse() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("anyString", "anyValue");
        final AttributeDraft attribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(attribute.getValue())).isFalse();
    }

    @Test
    public void isProductReference_WithCategoryReferenceAttribute_ShouldReturnFalse() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("typeId", "category");
        attributeValue.put("id", UUID.randomUUID().toString());
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(categoryReferenceAttribute.getValue())).isFalse();
    }

    @Test
    public void isProductReference_WithProductReferenceAttribute_ShouldReturnTrue() {
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(categoryReferenceAttribute.getValue())).isTrue();
    }

    @Test
    public void getResolvedIdFromKeyInReference_WithEmptyValue_ShouldGetEmptyId() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());
        final Optional<String> optionalId =
            referenceResolver.getResolvedIdFromKeyInReference(attributeWithEmptyValue.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();

    }

    @Test
    public void getResolvedIdFromKeyInReference_WithTextAttribute_ShouldGetEmptyId() {
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        final Optional<String> optionalId = referenceResolver.getResolvedIdFromKeyInReference(textAttribute.getValue())
                                                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    public void getResolvedIdFromKeyInReference_WithNonReferenceAttribute_ShouldGetEmptyId() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("anyString", "anyValue");
        final AttributeDraft attribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId = referenceResolver.getResolvedIdFromKeyInReference(attribute.getValue())
                                                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    public void getResolvedIdFromKeyInReference_WithNonExistingProductReferenceAttribute_ShouldGetEmptyId() {
        when(productService.fetchCachedProductId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId = referenceResolver.getResolvedIdFromKeyInReference(attributeDraft.getValue())
                                                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    public void getResolvedIdFromKeyInReference_WithProductReferenceAttributeWithNullIdField_ShouldGetEmptyId() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("typeId", "product");
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId =
            referenceResolver.getResolvedIdFromKeyInReference(productReferenceAttribute.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    public void getResolvedIdFromKeyInReference_WithProductReferenceAttributeWithIdField_ShouldGetId() {
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId =
            referenceResolver.getResolvedIdFromKeyInReference(productReferenceAttribute.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).contains(PRODUCT_ID);
    }

    @Test
    public void resolveReferences_WithNoPriceReferences_ShouldResolveAttributeReferences() {
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "textValue");

        final List<AttributeDraft> attributeDrafts =
            Arrays.asList(productReferenceSetAttribute, textAttribute);


        final ProductVariantDraft variantDraft = ProductVariantDraftBuilder.of()
                                                                           .attributes(attributeDrafts)
                                                                           .build();


        final ProductVariantDraft resolvedDraft = referenceResolver.resolveReferences(variantDraft)
                                                                   .toCompletableFuture().join();

        final List<PriceDraft> resolvedDraftPrices = resolvedDraft.getPrices();
        assertThat(resolvedDraftPrices).isNull();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedDraft.getAttributes();
        assertThat(resolvedBuilderAttributes).hasSize(2);
        assertThat(resolvedBuilderAttributes).contains(textAttribute);
        final AttributeDraft resolvedProductReferenceSetAttribute = resolvedBuilderAttributes.get(0);
        assertThat(resolvedProductReferenceSetAttribute).isNotNull();
        final JsonNode resolvedProductReferenceSetValue = resolvedProductReferenceSetAttribute.getValue();
        assertThat(resolvedProductReferenceSetValue).isNotNull();
        final JsonNode resolvedProductReferenceValue = resolvedProductReferenceSetValue.get(0);
        assertThat(resolvedProductReferenceValue).isNotNull();
        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get("id");
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);
    }
}
