package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.commons.utils.TestUtils;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WithProductTypeReferencesTest {
  private ProductTypeService productTypeService;
  private static final String PRODUCT_TYPE_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    productTypeService = ProductSyncMockUtils.getMockProductTypeService(PRODUCT_TYPE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            Mockito.mock(CustomerGroupService.class),
            Mockito.mock(ProductService.class),
            productTypeService,
            Mockito.mock(CategoryService.class),
            Mockito.mock(CustomObjectService.class),
            Mockito.mock(StateService.class),
            Mockito.mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingProductTypeReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId("nonExistingProductTypeKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject(
            "nonExistingProductTypeKey", ProductTypeReference.PRODUCT_TYPE);
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
      resolveReferences_WithNullIdFieldInProductTypeReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("attributeName").value(new ReferenceImpl()).build();
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
    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject("foo", ProductTypeReference.PRODUCT_TYPE);
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
    assertThat(reference.getId()).isEqualTo(PRODUCT_TYPE_ID);
    assertThat(reference.getTypeId().getJsonName()).isEqualTo(ProductTypeReference.PRODUCT_TYPE);
  }

  @Test
  void resolveReferences_WithProductTypeReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute productTypeReferenceSetAttributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo",
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), ProductTypeReference.PRODUCT_TYPE),
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), ProductTypeReference.PRODUCT_TYPE));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<ProductTypeReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductTypeReference.typeReference());
    assertThat(referenceList).isNotEmpty();
    final ProductTypeReference resolvedReference =
        ProductTypeReferenceBuilder.of().id(PRODUCT_TYPE_ID).build();
    assertThat(referenceList).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void
      resolveReferences_WithNonExistingProductTypeReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference productTypeReference =
        ProductSyncMockUtils.createReferenceObject(
            UUID.randomUUID().toString(), ProductTypeReference.PRODUCT_TYPE);
    final Attribute productTypeReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft("foo", productTypeReference);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<ProductTypeReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductTypeReference.typeReference());
    final Set<ProductTypeReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).containsExactly((ProductTypeReference) productTypeReference);
  }

  @Test
  void
      resolveReferences_WithSomeExistingProductTypeReferenceSetAttribute_ShouldResolveExistingReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId("existingKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
    when(productTypeService.fetchCachedProductTypeId("randomKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference productTypeReference1 =
        ProductSyncMockUtils.createReferenceObject(
            "existingKey", ProductTypeReference.PRODUCT_TYPE);
    final Reference productTypeReference2 =
        ProductSyncMockUtils.createReferenceObject("randomKey", ProductTypeReference.PRODUCT_TYPE);

    final Attribute productTypeReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo", productTypeReference1, productTypeReference2);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productTypeReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<ProductTypeReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductTypeReference.typeReference());
    final Set<ProductTypeReference> resolvedSet = new HashSet<>(referenceList);

    final ProductTypeReference resolvedReference1 =
        (ProductTypeReference)
            ProductSyncMockUtils.createReferenceObject(
                "existingId", ProductTypeReference.PRODUCT_TYPE);
    final ProductTypeReference resolvedReference2 =
        (ProductTypeReference)
            ProductSyncMockUtils.createReferenceObject(
                "randomKey", ProductTypeReference.PRODUCT_TYPE);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
