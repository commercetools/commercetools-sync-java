package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithCategoryReferencesTest {
  private CategoryService categoryService;
  private static final String CATEGORY_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    categoryService = getMockCategoryService(CATEGORY_ID);
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
            categoryService,
            mock(CustomObjectService.class),
            mock(StateService.class),
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingCategoryReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(categoryService.fetchCachedCategoryId("nonExistingCatKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference attributeValue =
        createReferenceObject("nonExistingCatKey", CategoryReference.CATEGORY);
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
  void resolveReferences_WithNullIdFieldInCategoryReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    final Reference attributeValue = new ReferenceImpl();
    final Attribute categoryRef =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(categoryRef).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft).isEqualTo(productVariantDraft);
  }

  @Test
  void resolveReferences_WithExistingCategoryReferenceAttribute_ShouldResolveReferences() {
    // preparation
    final Reference attributeValue = createReferenceObject("foo", CategoryReference.CATEGORY);
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of().name("attributeName").value(attributeValue).build();
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(categoryReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();
    final Attribute resolvedAttribute = resolvedAttributeDraft.getAttributes().get(0);
    assertThat(resolvedAttribute).isNotNull();
    assertThat(resolvedAttribute.getValue()).isInstanceOf(Reference.class);
    assertThat(((CategoryReference) resolvedAttribute.getValue()).getId()).isEqualTo(CATEGORY_ID);
  }

  @Test
  void resolveReferences_WithCategoryReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute categoryReferenceSetAttributeDraft =
        getReferenceSetAttributeDraft(
            "foo",
            createReferenceObject(UUID.randomUUID().toString(), CategoryReference.CATEGORY),
            createReferenceObject(UUID.randomUUID().toString(), CategoryReference.CATEGORY));

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(categoryReferenceSetAttributeDraft).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);
    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<CategoryReference> referenceList = (List) resolvedAttributeDraft.getValue();
    assertThat(referenceList).isNotEmpty();
    final CategoryReference resolvedReference =
        CategoryReferenceBuilder.of().id(CATEGORY_ID).build();
    assertThat(referenceList).containsExactlyInAnyOrder(resolvedReference, resolvedReference);
  }

  @Test
  void resolveReferences_WithNonExistingCategoryReferenceSetAttribute_ShouldNotResolveReferences() {
    // preparation
    when(categoryService.fetchCachedCategoryId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference categoryReference =
        createReferenceObject(UUID.randomUUID().toString(), CategoryReference.CATEGORY);
    final Attribute categoryReferenceAttribute =
        getReferenceSetAttributeDraft("foo", categoryReference);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(categoryReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<CategoryReference> referenceList = (List) resolvedAttributeDraft.getValue();
    final Set<CategoryReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).isNotNull();
    assertThat(resolvedSet).containsExactly((CategoryReference) categoryReference);
  }

  @Test
  void
      resolveReferences_WithSomeExistingCategoryReferenceSetAttribute_ShouldResolveExistingReferences() {
    // preparation
    when(categoryService.fetchCachedCategoryId("existingKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("existingId")));
    when(categoryService.fetchCachedCategoryId("randomKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference categoryReference1 =
        createReferenceObject("existingKey", CategoryReference.CATEGORY);
    final Reference categoryReference2 =
        createReferenceObject("randomKey", CategoryReference.CATEGORY);

    final Attribute categoryReferenceAttribute =
        getReferenceSetAttributeDraft("foo", categoryReference1, categoryReference2);

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().attributes(categoryReferenceAttribute).build();

    // test
    final ProductVariantDraft resolvedProductVariantDraft =
        referenceResolver.resolveReferences(productVariantDraft).toCompletableFuture().join();

    // assertions
    assertThat(resolvedProductVariantDraft).isNotNull();
    assertThat(resolvedProductVariantDraft.getAttributes()).isNotNull();

    final Attribute resolvedAttributeDraft = resolvedProductVariantDraft.getAttributes().get(0);

    assertThat(resolvedAttributeDraft).isNotNull();
    assertThat(resolvedAttributeDraft.getValue()).isNotNull();
    final List<CategoryReference> referenceList = (List) resolvedAttributeDraft.getValue();
    final Set<CategoryReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).isNotNull();

    final CategoryReference resolvedReference1 =
        (CategoryReference) createReferenceObject("existingId", CategoryReference.CATEGORY);
    final CategoryReference resolvedReference2 =
        (CategoryReference) createReferenceObject("randomKey", CategoryReference.CATEGORY);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
