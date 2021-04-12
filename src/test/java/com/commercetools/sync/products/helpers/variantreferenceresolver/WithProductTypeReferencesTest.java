package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductTypeService;
import static com.commercetools.sync.products.ProductSyncMockUtils.getReferenceSetAttributeDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithProductTypeReferencesTest {
  private ProductTypeService productTypeService;
  private static final String PRODUCT_TYPE_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    productTypeService = getMockProductTypeService(PRODUCT_TYPE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            productTypeService,
            mock(CategoryService.class),
            mock(CustomObjectService.class),
            mock(StateService.class),
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingProductTypeReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId("nonExistingProductTypeKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ObjectNode attributeValue =
        createReferenceObject("nonExistingProductTypeKey", ProductType.referenceTypeId());
    final AttributeDraft attributeDraft = AttributeDraft.of("attributeName", attributeValue);
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
      resolveReferences_WithNullIdFieldInProductTypeReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
    attributeValue.put(REFERENCE_TYPE_ID_FIELD, ProductType.referenceTypeId());
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("attributeName", attributeValue);
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void
      resolveReferences_WithNullNodeIdFieldInProductTypeReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
    attributeValue.put(REFERENCE_TYPE_ID_FIELD, ProductType.referenceTypeId());
    attributeValue.set(REFERENCE_ID_FIELD, JsonNodeFactory.instance.nullNode());

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("attributeName", attributeValue);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithExistingProductTypeReferenceAttribute_ShouldResolveReferences() {
    // preparation
    final ObjectNode attributeValue = createReferenceObject("foo", ProductType.referenceTypeId());
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("attributeName", attributeValue);
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
    final AttributeDraft resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
    assertThat(resolvedAttribute).isNotNull();
    assertThat(resolvedAttribute.getValue().get(REFERENCE_ID_FIELD).asText())
        .isEqualTo(PRODUCT_TYPE_ID);
    assertThat(resolvedAttribute.getValue().get(REFERENCE_TYPE_ID_FIELD).asText())
        .isEqualTo(ProductType.referenceTypeId());
  }

  @Test
  void resolveReferences_WithProductTypeReferenceSetAttribute_ShouldResolveReferences() {
    final AttributeDraft productTypeReferenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(UUID.randomUUID().toString(), ProductType.referenceTypeId()),
            createReferenceObject(UUID.randomUUID().toString(), ProductType.referenceTypeId()));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceSetAttributeDraft).build();

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
    resolvedReference.put(REFERENCE_TYPE_ID_FIELD, ProductType.referenceTypeId());
    resolvedReference.put(REFERENCE_ID_FIELD, PRODUCT_TYPE_ID);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void
      resolveReferences_WithNonExistingProductTypeReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ObjectNode productTypeReference =
        createReferenceObject(UUID.randomUUID().toString(), ProductType.referenceTypeId());
    final AttributeDraft productTypeReferenceAttribute =
        getReferenceSetAttributeDraft("foo", productTypeReference);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

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
    final Set<JsonNode> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toSet());
    assertThat(resolvedSet).containsExactly(productTypeReference);
  }

  @Test
  void
      resolveReferences_WithSomeExistingProductTypeReferenceSetAttribute_ShouldResolveExistingReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId("existingKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
    when(productTypeService.fetchCachedProductTypeId("randomKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ObjectNode productTypeReference1 =
        createReferenceObject("existingKey", ProductType.referenceTypeId());
    final ObjectNode productTypeReference2 =
        createReferenceObject("randomKey", ProductType.referenceTypeId());

    final AttributeDraft productTypeReferenceAttribute =
        getReferenceSetAttributeDraft("foo", productTypeReference1, productTypeReference2);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

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
    final Set<JsonNode> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toSet());

    final ObjectNode resolvedReference1 =
        createReferenceObject("existingId", ProductType.referenceTypeId());
    final ObjectNode resolvedReference2 =
        createReferenceObject("randomKey", ProductType.referenceTypeId());
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
