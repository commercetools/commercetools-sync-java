package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.ProductSyncMockUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.states.State;
import java.util.List;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithStateReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private static final String STATE_ID = UUID.randomUUID().toString();
  private StateService stateService;

  @BeforeEach
  void setup() {
    stateService = getMockStateService(STATE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class),
            stateService,
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNullIdFieldInStateReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
    attributeValue.put(REFERENCE_TYPE_ID_FIELD, State.referenceTypeId());
    final AttributeDraft stateReferenceAttribute =
        AttributeDraft.of("attributeName", attributeValue);
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(stateReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithNullNodeIdFieldInStateReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
    attributeValue.put(REFERENCE_TYPE_ID_FIELD, State.referenceTypeId());
    attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());
    final AttributeDraft stateReferenceAttribute =
        AttributeDraft.of("attributeName", attributeValue);
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(stateReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithStateReferenceSetAttribute_ShouldResolveReferences() {
    final AttributeDraft stateReferenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(UUID.randomUUID().toString(), State.referenceTypeId()),
            createReferenceObject(UUID.randomUUID().toString(), State.referenceTypeId()));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(stateReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final AttributeDraft resolvedAttributeDraft =
        resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final Spliterator<JsonNode> attributeReferencesIterator =
        resolvedAttributeDraft.getValue().spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final List<JsonNode> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toList());
    assertThat(resolvedSet).isNotEmpty();
    final ObjectNode resolvedReference = JsonNodeFactory.instance.objectNode();
    resolvedReference.put(REFERENCE_TYPE_ID_FIELD, State.referenceTypeId());
    resolvedReference.put(REFERENCE_ID_FIELD, STATE_ID);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }
}
