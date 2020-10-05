package com.commercetools.sync.products.helpers.variantreferenceresolver;

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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCategoryService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WithCategoryReferencesTest {
    private CategoryService categoryService;
    private static final String CATEGORY_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

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
            categoryService,
            mock(CustomObjectService.class));
    }

    @Test
    void resolveReferences_WithNonExistingCategoryReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        when(categoryService.fetchCachedCategoryId("nonExistingCatKey"))
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
    void resolveReferences_WithNullIdFieldInCategoryReferenceAttribute_ShouldNotResolveReferences() {
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
    void resolveReferences_WithNullNodeIdFieldInCategoryReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());

        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("attributeName", attributeValue);

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
    void resolveReferences_WithExistingCategoryReferenceAttribute_ShouldResolveReferences() {
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
        assertThat(resolvedAttribute.getValue().get(REFERENCE_ID_FIELD).asText()).isEqualTo(CATEGORY_ID);
        assertThat(resolvedAttribute.getValue().get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(Category.referenceTypeId());
    }

    @Test
    void resolveReferences_WithCategoryReferenceSetAttribute_ShouldResolveReferences() {
        final AttributeDraft categoryReferenceSetAttributeDraft = getReferenceSetAttributeDraft("foo",
            createReferenceObject(UUID.randomUUID().toString(), Category.referenceTypeId()),
            createReferenceObject(UUID.randomUUID().toString(), Category.referenceTypeId()));

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(categoryReferenceSetAttributeDraft)
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
        final List<JsonNode> resolvedSet = StreamSupport.stream(attributeReferencesIterator, false)
                                                        .collect(Collectors.toList());
        assertThat(resolvedSet).isNotEmpty();
        final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        resolvedReference.put(REFERENCE_ID_FIELD, CATEGORY_ID);
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
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
