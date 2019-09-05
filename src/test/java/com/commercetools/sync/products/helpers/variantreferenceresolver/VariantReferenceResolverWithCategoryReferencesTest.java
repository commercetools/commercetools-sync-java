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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCategoryService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariantReferenceResolverWithCategoryReferencesTest {
    private CategoryService categoryService;
    private static final String CATEGORY1_ID = UUID.randomUUID().toString();
    private static final String CATEGORY2_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        categoryService = getMockCategoryService(CATEGORY1_ID, CATEGORY2_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            categoryService);
    }

    @Test
    void resolveReferences_WithNonExistingCategoryReferenceAttribute_ShouldReturnEqualDraft() {
        // preparation
        when(categoryService.fetchCachedCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue = createReferenceObject("nonExistingCatKey", Category.referenceTypeId());
        final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNullIdFieldInCategoryReferenceAttribute_ShouldReturnEqualDraft() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithNullNodeIdFieldInCategoryReferenceAttribute_ShouldReturnEqualDraft() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("attributeName", JsonNodeFactory.instance.nullNode());
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
    }

    @Test
    void resolveReferences_WithExistingCategoryReferenceAttribute_ShouldReturnResolvedDraft() {
        // preparation
        final ObjectNode attributeValue = createReferenceObject("foo", Category.referenceTypeId());
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedAttributeDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture()
                             .join();
        // assertions
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
        final AttributeDraft resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
        assertThat(resolvedAttribute).isNotNull();
        assertThat(resolvedAttribute.getValue().get(REFERENCE_ID_FIELD).asText()).isEqualTo(CATEGORY1_ID);
        assertThat(resolvedAttribute.getValue().get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(Category.referenceTypeId());
    }

    @Test
    void resolveReferences_WithCategoryReferenceSetAttribute_ShouldResolveReferences() {
        // preparation
        final ArrayNode referenceSet = JsonNodeFactory.instance.arrayNode();
        referenceSet.add(createReferenceObject("foo", Category.referenceTypeId()));
        referenceSet.add(createReferenceObject("bar", Category.referenceTypeId()));
        final AttributeDraft categoryReferenceAttribute = AttributeDraft.of("attributeName", referenceSet);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();


        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).isNotEmpty();
        assertThat(resolvedSet).containsExactlyInAnyOrder(
            createReferenceObject(CATEGORY1_ID, Category.referenceTypeId()),
            createReferenceObject(CATEGORY2_ID, Category.referenceTypeId()));
    }

    @Test
    void resolveReferences_WithNonExistingCategoryReferenceSetAttribute_ShouldNotResolveReferences() {
        // preparation
        when(categoryService.fetchCachedCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode categoryReference =
            createReferenceObject(UUID.randomUUID().toString(), Category.referenceTypeId());
        final AttributeDraft categoryReferenceAttribute =
            getReferenceSetAttributeDraft("foo", categoryReference);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());
        assertThat(resolvedSet).containsExactly(categoryReference);
    }

    @Test
    void resolveReferences_WithSomeExistingCategoryReferenceSetAttribute_ShouldResolveExistingReferences() {
        // preparation
        when(categoryService.fetchCachedCategoryId("existingKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
        when(categoryService.fetchCachedCategoryId("randomKey"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode categoryReference1 = createReferenceObject("existingKey", Category.referenceTypeId());
        final ObjectNode categoryReference2 = createReferenceObject("randomKey", Category.referenceTypeId());

        final AttributeDraft categoryReferenceAttribute =
            getReferenceSetAttributeDraft("foo", categoryReference1, categoryReference2);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceAttribute)
            .build();

        // test
        final ProductVariantDraft resolvedProductVariantDraft =
            referenceResolver.resolveReferences(productVariantDraft)
                             .toCompletableFuture().join();

        // assertions
        assertThat(resolvedProductVariantDraft).isNotNull();
        assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

        final AttributeDraft resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

        assertThat(resolvedAttributeDraft).isNotNull();
        assertThat(resolvedAttributeDraft.getValue()).isNotNull();

        final Spliterator<JsonNode> attributeReferencesIterator = resolvedAttributeDraft.getValue().spliterator();
        assertThat(attributeReferencesIterator).isNotNull();
        final Set<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                       .collect(Collectors.toSet());

        final ObjectNode resolvedReference1 = createReferenceObject("existingId", Category.referenceTypeId());
        final ObjectNode resolvedReference2 = createReferenceObject("randomKey", Category.referenceTypeId());
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
    }
}
