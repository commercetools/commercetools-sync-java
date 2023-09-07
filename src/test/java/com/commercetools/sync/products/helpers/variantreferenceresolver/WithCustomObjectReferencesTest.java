package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.custom_object.CustomObjectReference;
import com.commercetools.api.models.custom_object.CustomObjectReferenceBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WithCustomObjectReferencesTest {
  private CustomObjectService customObjectService;
  private static final String CUSTOM_OBJECT_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    customObjectService = ProductSyncMockUtils.getMockCustomObjectService(CUSTOM_OBJECT_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            Mockito.mock(CustomerGroupService.class),
            Mockito.mock(ProductService.class),
            Mockito.mock(ProductTypeService.class),
            Mockito.mock(CategoryService.class),
            customObjectService,
            Mockito.mock(StateService.class),
            Mockito.mock(CustomerService.class));
  }

  @Test
  void
      resolveReferences_WithNonExistingCustomObjectReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final CustomObjectCompositeIdentifier nonExistingCustomObjectId =
        CustomObjectCompositeIdentifier.of("non-existing-key", "non-existing-container");

    when(customObjectService.fetchCachedCustomObjectId(nonExistingCustomObjectId))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject(
            "non-existing-container|non-existing-key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Attribute attributeDraft =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void
      resolveReferences_WithNullIdFieldInCustomObjectReferenceAttribute_ShouldNotResolveReferences() {
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
  void resolveReferences_WithInvalidCustomObjectIdentifier_ShouldNotResolveReferences() {
    // preparation
    final String invalidCustomObjectIdentifier = "container-key";
    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject(
            invalidCustomObjectIdentifier, CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Attribute attributeDraft =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    assertThat(referenceResolver.resolveReferences(productVariantDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(IllegalArgumentException.class)
        .withMessageContaining(
            "The custom object identifier value: \"container-key\" does not have the correct format. "
                + "The correct format must have a vertical bar \"|\" character between the container and key.");
  }

  @Test
  void resolveReferences_WithUuidCustomObjectIdentifier_ShouldNotResolveReferences() {
    // preparation
    final String uuid = UUID.randomUUID().toString();
    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject(uuid, CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Attribute attributeDraft =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithExistingCustomObjectReferenceAttribute_ShouldResolveReferences() {
    // preparation
    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject(
            "container|key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Attribute attributeDraft =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
    final Attribute resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
    assertThat(resolvedAttribute).isNotNull();
    final Reference reference =
        JsonUtils.fromJsonNode((JsonNode) resolvedAttribute.getValue(), Reference.typeReference());
    assertThat(reference.getId()).isEqualTo(CUSTOM_OBJECT_ID);
    assertThat(reference.getTypeId().getJsonName())
        .isEqualTo(CustomObjectReference.KEY_VALUE_DOCUMENT);
  }

  @Test
  void resolveReferences_WithCustomObjectReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute attributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "attributeName",
            ProductSyncMockUtils.createReferenceObject(
                "container|key1", CustomObjectReference.KEY_VALUE_DOCUMENT),
            ProductSyncMockUtils.createReferenceObject(
                "container|key2", CustomObjectReference.KEY_VALUE_DOCUMENT));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<CustomObjectReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CustomObjectReference.typeReference());
    assertThat(referenceList).isNotEmpty();
    final CustomObjectReference resolvedReference =
        CustomObjectReferenceBuilder.of().id(CUSTOM_OBJECT_ID).build();
    assertThat(referenceList).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void
      resolveReferences_WithNonExistingCustomObjectReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(customObjectService.fetchCachedCustomObjectId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference customObjectReference =
        ProductSyncMockUtils.createReferenceObject(
            "container|key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Attribute attributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft("attributeName", customObjectReference);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<CustomObjectReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CustomObjectReference.typeReference());
    final Set<CustomObjectReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).containsExactly((CustomObjectReference) customObjectReference);
  }

  @Test
  void
      resolveReferences_WithSomeExistingCustomObjectReferenceSetAttribute_ShouldResolveExistingReferences() {
    // preparation
    final CustomObjectCompositeIdentifier existingCustomObjectId =
        CustomObjectCompositeIdentifier.of("existing-key", "existing-container");

    final CustomObjectCompositeIdentifier randomCustomObjectId =
        CustomObjectCompositeIdentifier.of("random-key", "random-container");

    when(customObjectService.fetchCachedCustomObjectId(existingCustomObjectId))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));

    when(customObjectService.fetchCachedCustomObjectId(randomCustomObjectId))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference customObjectReference1 =
        ProductSyncMockUtils.createReferenceObject(
            "existing-container|existing-key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final Reference customObjectReference2 =
        ProductSyncMockUtils.createReferenceObject(
            "random-container|random-key", CustomObjectReference.KEY_VALUE_DOCUMENT);

    final Attribute attributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "attributeName", customObjectReference1, customObjectReference2);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(attributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<CustomObjectReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CustomObjectReference.typeReference());
    final Set<CustomObjectReference> resolvedSet = new HashSet<>(referenceList);

    final CustomObjectReference resolvedReference1 =
        (CustomObjectReference)
            ProductSyncMockUtils.createReferenceObject(
                "existingId", CustomObjectReference.KEY_VALUE_DOCUMENT);
    final CustomObjectReference resolvedReference2 =
        (CustomObjectReference)
            ProductSyncMockUtils.createReferenceObject(
                "random-container|random-key", CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
