package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
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
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCategoryService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WithCategoryReferencesTest {
    private CategoryService categoryService;

    private static final String CATEGORY_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    private static final String RES_SUB_ROOT = "withcategoryreferences/";
    private static final String NESTED_ATTRIBUTE_WITH_CATEGORY_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-reference.json";
    private static final String NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CATEGORY_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
    private static final String NESTED_ATTRIBUTE_WITH_SET_OF_CATEGORY_REFERENCE_ATTRIBUTES =
        RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

    @BeforeEach
    void setup() {
        categoryService = getMockCategoryService(CATEGORY_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            categoryService);
    }

    @Test
    void resolveReferences_WithNestedCategoryReferenceAttributes_ShouldResolveReferences() {
        // preparation
        final ProductVariantDraft withNestedCategoryReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_CATEGORY_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedCategoryReferenceAttributes)
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

        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-1-name", CATEGORY_ID,
            Category.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-2-name", CATEGORY_ID,
            Category.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap, "nested-attribute-3-name", CATEGORY_ID,
            Category.referenceTypeId());
    }

    @Test
    void resolveReferences_WithNestedSetOfCategoryReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        final ProductVariantDraft withNestedSetOfCategoryReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_SET_OF_CATEGORY_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withNestedSetOfCategoryReferenceAttributes)
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
            "nested-attribute-1-name", 2, CATEGORY_ID, Category.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", CATEGORY_ID, Category.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", CATEGORY_ID, Category.referenceTypeId());
    }

    @Test
    void resolveReferences_WithSomeNonExistingNestedCategoryReferenceAttributes_ShouldOnlyResolveExistingReferences() {
        // preparation
        when(categoryService.fetchCachedCategoryId("nonExistingCategoryKey1"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(categoryService.fetchCachedCategoryId("nonExistingCategoryKey3"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ProductVariantDraft withSomeNonExistingNestedCategoryReferenceAttributes =
            readObjectFromResource(NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CATEGORY_REFERENCE_ATTRIBUTES,
                ProductVariantDraft.class);

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(withSomeNonExistingNestedCategoryReferenceAttributes)
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
            createReferenceObject("nonExistingCategoryKey1", Category.referenceTypeId()),
            createReferenceObject(CATEGORY_ID, Category.referenceTypeId()));

        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-2-name", CATEGORY_ID, Category.referenceTypeId());
        assertReferenceAttributeValue(resolvedNestedAttributesMap,
            "nested-attribute-3-name", "nonExistingCategoryKey3", Category.referenceTypeId());
    }
}
