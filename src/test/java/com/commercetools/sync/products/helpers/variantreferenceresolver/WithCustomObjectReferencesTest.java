package com.commercetools.sync.products.helpers.variantreferenceresolver;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
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
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
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
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomObjectService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WithCustomObjectReferencesTest {
    private CustomObjectService customObjectService;
    private static final String CUSTOM_OBJECT_ID = UUID.randomUUID().toString();
    private VariantReferenceResolver referenceResolver;

    @BeforeEach
    void setup() {
        customObjectService = getMockCustomObjectService(CUSTOM_OBJECT_ID);
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new VariantReferenceResolver(syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            customObjectService);
    }

    @Test
    void resolveReferences_WithNonExistingCustomObjectReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final CustomObjectCompositeIdentifier nonExistingCustomObjectId =
            CustomObjectCompositeIdentifier.of("non-existing-key", "non-existing-container");

        when(customObjectService.fetchCachedCustomObjectId(nonExistingCustomObjectId))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode attributeValue =
            createReferenceObject("non-existing-container|non-existing-key", CustomObject.referenceTypeId());
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
    void resolveReferences_WithNullIdFieldInCustomObjectReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, CustomObject.referenceTypeId());

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
    void resolveReferences_WithNullNodeIdFieldInCustomObjectReferenceAttribute_ShouldNotResolveReferences() {
        // preparation
        final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
        attributeValue.put(REFERENCE_TYPE_ID_FIELD, CustomObject.referenceTypeId());
        attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());

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
    void resolveReferences_WithInvalidCustomObjectIdentifier_ShouldNotResolveReferences() {
        // preparation
        final String invalidCustomObjectIdentifier = "container-key";
        final ObjectNode attributeValue =
            createReferenceObject(invalidCustomObjectIdentifier, CustomObject.referenceTypeId());
        final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
            .build();

        // test
        assertThat(referenceResolver.resolveReferences(productVariantDraft))
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("The custom object identifier value: \"container-key\" does not have the correct format. "
                + "The correct format must have a vertical bar \"|\" character between the container and key.");
    }

    @Test
    void resolveReferences_WithUuidCustomObjectIdentifier_ShouldNotResolveReferences() {
        // preparation
        final String uuid = UUID.randomUUID().toString();
        final ObjectNode attributeValue =
            createReferenceObject(uuid, CustomObject.referenceTypeId());
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
    void resolveReferences_WithExistingCustomObjectReferenceAttribute_ShouldResolveReferences() {
        // preparation
        final ObjectNode attributeValue = createReferenceObject("container|key", CustomObject.referenceTypeId());
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
        assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
        final AttributeDraft resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
        assertThat(resolvedAttribute).isNotNull();
        assertThat(resolvedAttribute.getValue().get(REFERENCE_ID_FIELD).asText()).isEqualTo(CUSTOM_OBJECT_ID);
        assertThat(resolvedAttribute.getValue().get(REFERENCE_TYPE_ID_FIELD).asText())
            .isEqualTo(CustomObject.referenceTypeId());
    }

    @Test
    void resolveReferences_WithCustomObjectReferenceSetAttribute_ShouldResolveReferences() {
        final AttributeDraft attributeDraft = getReferenceSetAttributeDraft("attributeName",
            createReferenceObject("container|key1", CustomObject.referenceTypeId()),
            createReferenceObject("container|key2", CustomObject.referenceTypeId()));

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
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
        resolvedReference.put(REFERENCE_TYPE_ID_FIELD, CustomObject.referenceTypeId());
        resolvedReference.put(REFERENCE_ID_FIELD, CUSTOM_OBJECT_ID);
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
    }

    @Test
    void resolveReferences_WithNonExistingCustomObjectReferenceSetAttribute_ShouldNotResolveReferences() {
        // preparation
        when(customObjectService.fetchCachedCustomObjectId(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode customObjectReference =
            createReferenceObject("container|key", CustomObject.referenceTypeId());
        final AttributeDraft attributeDraft =
            getReferenceSetAttributeDraft("attributeName", customObjectReference);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
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
        assertThat(resolvedSet).containsExactly(customObjectReference);
    }

    @Test
    void resolveReferences_WithSomeExistingCustomObjectReferenceSetAttribute_ShouldResolveExistingReferences() {
        // preparation
        final CustomObjectCompositeIdentifier existingCustomObjectId =
            CustomObjectCompositeIdentifier.of("existing-key", "existing-container");

        final CustomObjectCompositeIdentifier randomCustomObjectId =
            CustomObjectCompositeIdentifier.of("random-key", "random-container");

        when(customObjectService.fetchCachedCustomObjectId(existingCustomObjectId))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));

        when(customObjectService.fetchCachedCustomObjectId(randomCustomObjectId))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ObjectNode customObjectReference1 =
            createReferenceObject("existing-container|existing-key", CustomObject.referenceTypeId());
        final ObjectNode customObjectReference2 =
            createReferenceObject("random-container|random-key", CustomObject.referenceTypeId());

        final AttributeDraft attributeDraft =
            getReferenceSetAttributeDraft("attributeName", customObjectReference1, customObjectReference2);

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .attributes(attributeDraft)
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

        final ObjectNode resolvedReference1 = createReferenceObject("existingId", CustomObject.referenceTypeId());
        final ObjectNode resolvedReference2 =
            createReferenceObject("random-container|random-key", CustomObject.referenceTypeId());
        assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
    }
}
