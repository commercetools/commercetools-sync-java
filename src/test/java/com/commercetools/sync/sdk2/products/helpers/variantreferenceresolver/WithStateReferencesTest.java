package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.sdk2.commons.utils.TestUtils.convertArrayNodeToList;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateReferenceBuilder;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;
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
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
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
    final Attribute attributeDraft =
        AttributeBuilder.of().name("attributeName").value(new ReferenceImpl()).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithStateReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute stateReferenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(UUID.randomUUID().toString(), StateReference.STATE),
            createReferenceObject(UUID.randomUUID().toString(), StateReference.STATE));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(stateReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<StateReference> referenceList =
        convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), StateReference.typeReference());
    assertThat(referenceList).isNotEmpty();
    final StateReference resolvedReference = StateReferenceBuilder.of().id(STATE_ID).build();
    assertThat(referenceList).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }
}
