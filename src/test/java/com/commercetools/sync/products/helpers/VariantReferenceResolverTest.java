package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariantReferenceResolverTest {
    private ProductService productService;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";
    private static final String PRODUCT_ID = "productId";
    private VariantReferenceResolver referenceResolver;

    private static final String RES_ROOT =
        "com/commercetools/sync/products/helpers/variantReferenceResolver/attributes/";
    private static final String NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES =
        RES_ROOT + "nested-attribute-with-text-attributes.json";
    private static final String NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES =
        RES_ROOT + "nested-attribute-with-set-of-text-attributes.json";
    private static final String NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + "nested-attribute-with-product-reference-attributes.json";
    private static final String NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + "nested-attribute-with-non-existing-product-reference-attributes.json";
    private static final String NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + "nested-attribute-with-set-of-product-reference-attributes.json";

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        final TypeService typeService = getMockTypeService();
        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        productService = getMockProductService(PRODUCT_ID);
        ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions, typeService, channelService,
            mock(CustomerGroupService.class), productService);
    }

    @Test
    void resolveAssetsReferences_WithEmptyAssets_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of()
                                                                                                .assets(emptyList());

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    void resolveAssetsReferences_WithNullAssets_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNull();
    }

    @Test
    void resolveAssetsReferences_WithANullAsset_ShouldNotResolveAssets() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().assets(singletonList(null));

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    void resolveAssetsReferences_WithAssetReferences_ShouldResolveAssets() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson("customTypeId", new HashMap<>());

        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .custom(customFieldsDraft)
                                                       .build();

        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().assets(singletonList(assetDraft));

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAssetsReferences(productVariantDraftBuilder).toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNotEmpty();
        final AssetDraft resolvedAssetDraft = resolvedBuilderAssets.get(0);
        assertThat(resolvedAssetDraft).isNotNull();
        assertThat(resolvedAssetDraft.getCustom()).isNotNull();
        assertThat(resolvedAssetDraft.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    void resolvePricesReferences_WithNullPrices_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isNull();
    }

    @Test
    void resolvePricesReferences_WithANullPrice_ShouldNotResolvePrices() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().prices((PriceDraft) null);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolvePricesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<PriceDraft> resolvedBuilderPrices = resolvedBuilder.getPrices();
        assertThat(resolvedBuilderPrices).isEmpty();
    }

    @Test
    void resolvePricesReferences_WithPriceReferences_ShouldResolvePrices() {
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
    void resolveAttributesReferences_WithNullAttributes_ShouldNotResolveAttributes() {
        final ProductVariantDraftBuilder productVariantDraftBuilder = ProductVariantDraftBuilder.of();

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAttributesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
        assertThat(resolvedBuilderAttributes).isNull();
    }

    @Test
    void resolveAttributesReferences_WithANullAttribute_ShouldNotResolveAttributes() {
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of().attributes((AttributeDraft) null);

        final ProductVariantDraftBuilder resolvedBuilder = referenceResolver
            .resolveAttributesReferences(productVariantDraftBuilder)
            .toCompletableFuture().join();

        final List<AttributeDraft> resolvedBuilderAttributes = resolvedBuilder.getAttributes();
        assertThat(resolvedBuilderAttributes).isEmpty();
    }

    @Test
    void resolveAttributesReferences_WithMixedReference_ShouldResolveProductReferenceAttributes() {
        final ObjectNode productReferenceWithRandomId = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceSetAttribute =
            getProductReferenceSetAttributeDraft("foo", productReferenceWithRandomId);

        final ObjectNode categoryReference1 = JsonNodeFactory.instance.objectNode();
        categoryReference1.put(REFERENCE_TYPE_ID_FIELD, "category");
        categoryReference1.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());

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
        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get(REFERENCE_ID_FIELD);
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void resolveAttributeReference_WithEmptyValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithEmptyValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithEmptyValue);
    }

    @Test
    void resolveAttributeReference_WithNullValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithNullValue =
            AttributeDraft.of("attributeName", null);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithNullValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithNullValue);
    }

    @Test
    void resolveAttributeReference_WithTextValue_ShouldNotResolveAttribute() {
        final AttributeDraft attributeWithTextValue =
            AttributeDraft.of("attributeName", "textValue");

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(attributeWithTextValue)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isSameAs(attributeWithTextValue);
    }

    @Test
    void resolveAttributeReference_WithNonExistingProductReferenceAttribute_ShouldNotResolveAttribute() {
        when(productService.getIdFromCacheOrFetch(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, "product");
        attributeValue.put(REFERENCE_ID_FIELD, "nonExistingProductKey");
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft).isEqualTo(productReferenceAttribute);
    }

    @Test
    void resolveAttributeReference_WithProductReferenceAttribute_ShouldResolveAttribute() {
        // preparation
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        // test
        final AttributeDraft resolvedAttributeDraft =
            referenceResolver.resolveAttributeReference(productReferenceAttribute)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft).isNotEqualTo(productReferenceAttribute);
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();
        assertThat(resolvedAttributeDraft.getValue().get(REFERENCE_ID_FIELD)).isNotNull();
        assertThat(resolvedAttributeDraft.getValue().get(REFERENCE_ID_FIELD).asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void resolveAttributeReference_WithEmptyReferenceSetAttribute_ShouldNotResolveReferences() {
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
    void resolveAttributeReference_WithCategoryReferenceSetAttribute_ShouldNotResolveReferences() {
        final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
        categoryReference.put(REFERENCE_TYPE_ID_FIELD, "category");
        categoryReference.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());

        final ObjectNode categoryReference1 = JsonNodeFactory.instance.objectNode();
        categoryReference1.put(REFERENCE_TYPE_ID_FIELD, "category");
        categoryReference1.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());

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
    void resolveAttributeReference_WithProductReferenceSetAttribute_ShouldResolveReferences() {
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
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, "product");
        resolvedReference.put(REFERENCE_ID_FIELD, PRODUCT_ID);
        assertThat(resolvedSet).containsExactly(resolvedReference);
    }

    @Disabled("Fails due to possible bug on https://github.com/FasterXML/jackson-databind/issues/2442")
    @Test
    void resolveAttributeReference_WithNullReferenceInSetAttribute_ShouldResolveReferences() {
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
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, "product");
        resolvedReference.put(REFERENCE_ID_FIELD, PRODUCT_ID);
        assertThat(resolvedSet).containsExactly(resolvedReference);
    }

    @Test
    void resolveAttributeReference_WithNonExistingProductReferenceSetAttribute_ShouldNotResolveReferences() {
        when(productService.getIdFromCacheOrFetch(anyString()))
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
    void
        resolveAttributeReference_WithSomeExistingProductReferenceSetAttribute_ShouldResolveExistingReferences() {
        when(productService.getIdFromCacheOrFetch("existingKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
        when(productService.getIdFromCacheOrFetch("randomKey"))
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
    void isProductReference_WithEmptyValue_ShouldReturnFalse() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());
        assertThat(VariantReferenceResolver.isProductReference(attributeWithEmptyValue.getValue())).isFalse();
    }

    @Test
    void isProductReference_WithTextAttribute_ShouldReturnFalse() {
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        assertThat(VariantReferenceResolver.isProductReference(textAttribute.getValue())).isFalse();
    }

    @Test
    void isProductReference_WithNonReferenceAttribute_ShouldReturnFalse() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("anyString", "anyValue");
        final AttributeDraft attribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(attribute.getValue())).isFalse();
    }

    @Test
    void isProductReference_WithCategoryReferenceAttribute_ShouldReturnFalse() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, "category");
        attributeValue.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(categoryReferenceAttribute.getValue())).isFalse();
    }

    @Test
    void isProductReference_WithProductReferenceAttribute_ShouldReturnTrue() {
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        assertThat(VariantReferenceResolver.isProductReference(categoryReferenceAttribute.getValue())).isTrue();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithEmptyValue_ShouldResultInEmptyOptional() {
        final AttributeDraft attributeWithEmptyValue =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.objectNode());
        final Optional<String> optionalId =
            referenceResolver.getProductResolvedIdFromKeyInReference(attributeWithEmptyValue.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithTextAttribute_ShouldResultInEmptyOptional() {
        final AttributeDraft textAttribute = AttributeDraft.of("attributeName", "attributeValue");
        final Optional<String> optionalId = referenceResolver
            .getProductResolvedIdFromKeyInReference(textAttribute.getValue())
            .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithNonReferenceAttribute_ShouldResultInEmptyOptional() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put("anyString", "anyValue");
        final AttributeDraft attribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId = referenceResolver
            .getProductResolvedIdFromKeyInReference(attribute.getValue())
            .toCompletableFuture().join();

        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithNonExistingProductReferenceAttribute_ShouldResultInEmptyOptional() {
        when(productService.getIdFromCacheOrFetch(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId = referenceResolver
            .getProductResolvedIdFromKeyInReference(attributeDraft.getValue())
            .toCompletableFuture().join();

        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithNullIdField_ShouldResultInEmptyOptional() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, "product");
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId =
            referenceResolver.getProductResolvedIdFromKeyInReference(productReferenceAttribute.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithNullNodeIdField_ShouldResultInEmptyOptional() {
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, "product");
        attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId =
            referenceResolver.getProductResolvedIdFromKeyInReference(productReferenceAttribute.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).isEmpty();
    }

    @Test
    void getProductResolvedIdFromKeyInReference_WithIdField_ShouldResultInResolvedProductId() {
        final ObjectNode attributeValue = getProductReferenceWithRandomId();
        final AttributeDraft productReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);

        final Optional<String> optionalId =
            referenceResolver.getProductResolvedIdFromKeyInReference(productReferenceAttribute.getValue())
                             .toCompletableFuture().join();
        assertThat(optionalId).contains(PRODUCT_ID);
    }

    @Test
    void resolveReferences_WithNoPriceReferences_ShouldResolveAttributeReferences() {
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
        final JsonNode resolvedProductReferenceIdTextNode = resolvedProductReferenceValue.get(REFERENCE_ID_FIELD);
        assertThat(resolvedProductReferenceIdTextNode).isNotNull();
        assertThat(resolvedProductReferenceIdTextNode.asText()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void resolveReferences_WithNestedTextAttributesOnly_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft withNestedTextAttributesOnly = SphereJsonUtils
            .readObjectFromResource(NESTED_ATTRIBUTE_WITH_TEXT_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedTextAttributesOnly)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(withNestedTextAttributesOnly);
    }

    @Test
    void resolveReferences_WithNestedSetOfTextAttributesOnly_ShouldReturnEqualDraft() {
        // preparation
        final ProductVariantDraft withNestedTextAttributesOnly = SphereJsonUtils
            .readObjectFromResource(NESTED_ATTRIBUTE_WITH_SET_OF_TEXT_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedTextAttributesOnly)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(withNestedTextAttributesOnly);
    }

    @Test
    void resolveReferences_WithNestedProductReferenceAttributesOnly_ShouldResolveReferences() {
        // preparation
        final ProductVariantDraft withNestedTextAttributesOnly = SphereJsonUtils
            .readObjectFromResource(NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedTextAttributesOnly)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

        final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
        assertThat(value).isInstanceOf(ArrayNode.class);
        final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

        final Map<String, JsonNode> resolvedNestedAttributesMap = StreamSupport
            .stream(resolvedNestedAttributes.spliterator(), false)
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-1-name", PRODUCT_ID, "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-2-name", PRODUCT_ID, "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-3-name", PRODUCT_ID, "product");
    }

    @Test
    void resolveReferences_WithSomeNonExistingNestedProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        when(productService.getIdFromCacheOrFetch("nonExistingProductKey1"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(productService.getIdFromCacheOrFetch("nonExistingProductKey3"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductVariantDraft withNestedTextAttributesOnly = SphereJsonUtils
            .readObjectFromResource(NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedTextAttributesOnly)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

        final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
        assertThat(value).isInstanceOf(ArrayNode.class);
        final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

        final Map<String, JsonNode> resolvedNestedAttributesMap = StreamSupport
            .stream(resolvedNestedAttributes.spliterator(), false)
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-1-name", "nonExistingProductKey1", "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", PRODUCT_ID, "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", "nonExistingProductKey3", "product");
    }

    @Test
    void resolveReferences_WithNestedSetOfProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        final ProductVariantDraft withNestedTextAttributesOnly = SphereJsonUtils
            .readObjectFromResource(NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedTextAttributesOnly)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

        final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
        assertThat(value).isInstanceOf(ArrayNode.class);
        final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

        final Map<String, JsonNode> resolvedNestedAttributesMap = StreamSupport
            .stream(resolvedNestedAttributes.spliterator(), false)
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

        assertReferenceSetAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-1-name", 2, PRODUCT_ID, "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", PRODUCT_ID, "product");
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", PRODUCT_ID, "product");
    }

    private void assertReferenceAttributeValue(
        @Nonnull final Map<String, JsonNode> attributeDraftMap,
        @Nonnull final String attributeName,
        @Nonnull final String referenceId,
        @Nonnull final String referenceTypeId) {

        assertThat(attributeDraftMap.get(attributeName)).isNotNull();
        assertThat(attributeDraftMap.get(attributeName).get("value")).isNotNull();
        assertThat(attributeDraftMap.get(attributeName)
                                    .get("value")
                                    .get(REFERENCE_ID_FIELD).asText()).isEqualTo(referenceId);
        assertThat(attributeDraftMap.get(attributeName)
                                    .get("value")
                                    .get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(referenceTypeId);
    }

    private void assertReferenceSetAttributeValue(
        @Nonnull final Map<String, JsonNode> attributeDraftMap,
        @Nonnull final String attributeName,
        final int numberOfReferences,
        @Nonnull final String referenceId,
        @Nonnull final String referenceTypeId) {

        assertThat(attributeDraftMap.get(attributeName)).isNotNull();
        final JsonNode value = attributeDraftMap.get(attributeName).get("value");
        assertThat(value).isInstanceOf(ArrayNode.class);

        final ArrayNode valueAsArrayNode = (ArrayNode) value;
        assertThat(valueAsArrayNode).hasSize(numberOfReferences);
        assertThat(valueAsArrayNode).allSatisfy(jsonNode -> {
            assertThat(jsonNode.get(REFERENCE_ID_FIELD).asText()).isEqualTo(referenceId);
            assertThat(jsonNode.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(referenceTypeId);
        });
    }
}
