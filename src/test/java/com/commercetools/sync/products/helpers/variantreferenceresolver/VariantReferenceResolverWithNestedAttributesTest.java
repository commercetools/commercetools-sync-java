package com.commercetools.sync.products.helpers.variantreferenceresolver;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.ProductVariantDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCategoryService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariantReferenceResolverWithNestedAttributesTest {
    private ProductService productService;
    private CategoryService categoryService;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = UUID.randomUUID().toString();
    private static final String PRODUCT_ID = UUID.randomUUID().toString();
    private static final String CATEGORY1_ID = UUID.randomUUID().toString();
    private static final String CATEGORY2_ID = UUID.randomUUID().toString();
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
        categoryService = getMockCategoryService(CATEGORY1_ID, CATEGORY2_ID);
        ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions, typeService, channelService,
            mock(CustomerGroupService.class), productService, categoryService);
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
