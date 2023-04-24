package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.*;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithProductReferencesTest {
  private ProductService productService;
  private static final String PRODUCT_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    productService = getMockProductService(PRODUCT_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            productService,
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class),
            mock(StateService.class),
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingProductReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductReference attributeValue = getProductReferenceWithRandomId();
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
    final ProductReference attributeValue = getProductReferenceWithRandomId();
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
    final Reference productRef = (Reference) resolvedAttribute.getValue();
    assertThat(productRef.getId()).isEqualTo(PRODUCT_ID);
    assertThat(productRef.getTypeId().getJsonName()).isEqualTo(ProductReference.PRODUCT);
  }

  @Test
  void resolveReferences_WithProductReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute productReferenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo", getProductReferenceWithRandomId(), getProductReferenceWithRandomId());

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
    final List<ProductReference> referenceList = (List) resolvedAttributeDraft.getValue();

    final Spliterator<ProductReference> attributeReferencesIterator = referenceList.spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final List<ProductReference> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toList());
    assertThat(resolvedSet).isNotEmpty();
    final ProductReference resolvedReference = ProductReferenceBuilder.of().id(PRODUCT_ID).build();
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void resolveReferences_WithNonExistingProductReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductReference productReference = getProductReferenceWithRandomId();
    final Attribute productReferenceAttribute =
        getReferenceSetAttributeDraft("foo", productReference);

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

    final List<ProductReference> referenceList = (List) resolvedAttributeDraft.getValue();
    final Spliterator<ProductReference> attributeReferencesIterator = referenceList.spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final Set<ProductReference> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toSet());
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

    final ProductReference productReference1 = getProductReferenceWithId("existingKey");
    final ProductReference productReference2 = getProductReferenceWithId("randomKey");

    final Attribute productReferenceAttribute =
        getReferenceSetAttributeDraft("foo", productReference1, productReference2);

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

    final List<ProductReference> referenceList = (List) resolvedAttributeDraft.getValue();
    final Spliterator<ProductReference> attributeReferencesIterator = referenceList.spliterator();
    assertThat(attributeReferencesIterator).isNotNull();
    final Set<ProductReference> resolvedSet =
        StreamSupport.stream(attributeReferencesIterator, false).collect(Collectors.toSet());

    final ProductReference resolvedReference1 = getProductReferenceWithId("existingId");
    final ProductReference resolvedReference2 = getProductReferenceWithId("randomKey");
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
