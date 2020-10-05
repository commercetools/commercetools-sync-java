package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WithProductReferencesTest {
    private ProductService productService;

    private static final String PRODUCT_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    private static final String RES_SUB_ROOT = "withproductreferences/";
    private static final String NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-reference.json";
    private static final String NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
    private static final String NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

    @BeforeEach
    void setup() {
        productService = getMockProductService(PRODUCT_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            productService,
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class));
    }

    @Test
    void resolveReferences_WithNestedProductReferenceAttributes_ShouldResolveReferences() {
        // preparation
        final ProductVariantDraft withNestedProductReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedProductReferenceAttributes)
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

        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-1-name", PRODUCT_ID,
            Product.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-2-name", PRODUCT_ID,
            Product.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-3-name", PRODUCT_ID,
            Product.referenceTypeId());
    }

    @Test
    void resolveReferences_WithNestedSetOfProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        final ProductVariantDraft withNestedSetOfProductReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedSetOfProductReferenceAttributes)
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
            "nested-attribute-1-name", 2, PRODUCT_ID, Product.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", PRODUCT_ID, Product.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", PRODUCT_ID, Product.referenceTypeId());
    }

    @Test
    void resolveReferences_WithSomeNonExistingNestedProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        when(productService.getIdFromCacheOrFetch("nonExistingProductKey1"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(productService.getIdFromCacheOrFetch("nonExistingProductKey3"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductVariantDraft withSomeNonExistingNestedProductReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withSomeNonExistingNestedProductReferenceAttributes)
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
            "nested-attribute-1-name",
            createReferenceObject("nonExistingProductKey1", Product.referenceTypeId()),
            createReferenceObject(PRODUCT_ID, Product.referenceTypeId()));

        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", PRODUCT_ID, Product.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", "nonExistingProductKey3", Product.referenceTypeId());
    }
}
