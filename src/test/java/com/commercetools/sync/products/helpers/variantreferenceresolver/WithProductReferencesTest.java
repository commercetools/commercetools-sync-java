package com.commercetools.sync.products.helpers.variantreferenceresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.*;
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

class WithProductReferencesTest {
  private ProductService productService;
  private static final String PRODUCT_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    productService = ProductSyncMockUtils.getMockProductService(PRODUCT_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            Mockito.mock(CustomerGroupService.class),
            productService,
            Mockito.mock(ProductTypeService.class),
            Mockito.mock(CategoryService.class),
            Mockito.mock(CustomObjectService.class),
            Mockito.mock(StateService.class),
            Mockito.mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingProductReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductReference attributeValue = ProductSyncMockUtils.getProductReferenceWithRandomId();
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
  void resolveReferences_WithNullIdFieldInProductReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("attributeName").value(new ReferenceImpl()).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithExistingProductReferenceAttribute_ShouldResolveReferences() {
    // preparation
    final ProductReference attributeValue = ProductSyncMockUtils.getProductReferenceWithRandomId();
    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
    final Attribute resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
    assertThat(resolvedAttribute).isNotNull();
    final Reference productRef =
        JsonUtils.fromJsonNode((JsonNode) resolvedAttribute.getValue(), Reference.typeReference());
    assertThat(productRef.getId()).isEqualTo(PRODUCT_ID);
    assertThat(productRef.getTypeId().getJsonName()).isEqualTo(ProductReference.PRODUCT);
  }

  @Test
  void resolveReferences_WithProductReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute productReferenceSetAttributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo",
            ProductSyncMockUtils.getProductReferenceWithRandomId(),
            ProductSyncMockUtils.getProductReferenceWithRandomId());

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<ProductReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductReference.typeReference());

    assertThat(referenceList).isNotEmpty();
    final ProductReference resolvedReference = ProductReferenceBuilder.of().id(PRODUCT_ID).build();
    assertThat(referenceList).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void resolveReferences_WithNonExistingProductReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductReference productReference =
        ProductSyncMockUtils.getProductReferenceWithRandomId();
    final Attribute productReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft("foo", productReference);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<ProductReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductReference.typeReference());
    final Set<ProductReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).containsExactly(productReference);
  }

  @Test
  void
      resolveReferences_WithSomeExistingProductReferenceSetAttribute_ShouldResolveExistingReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch("existingKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
    when(productService.getIdFromCacheOrFetch("randomKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductReference productReference1 =
        ProductSyncMockUtils.getProductReferenceWithId("existingKey");
    final ProductReference productReference2 =
        ProductSyncMockUtils.getProductReferenceWithId("randomKey");

    final Attribute productReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo", productReference1, productReference2);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(productReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();

    final List<ProductReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), ProductReference.typeReference());
    final Set<ProductReference> resolvedSet = new HashSet<>(referenceList);

    final ProductReference resolvedReference1 =
        ProductSyncMockUtils.getProductReferenceWithId("existingId");
    final ProductReference resolvedReference2 =
        ProductSyncMockUtils.getProductReferenceWithId("randomKey");
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
