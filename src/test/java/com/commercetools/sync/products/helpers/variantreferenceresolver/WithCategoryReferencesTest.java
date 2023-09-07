package com.commercetools.sync.products.helpers.variantreferenceresolver;

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

class WithCategoryReferencesTest {
  private CategoryService categoryService;
  private static final String CATEGORY_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  @BeforeEach
  void setup() {
    categoryService = ProductSyncMockUtils.getMockCategoryService(CATEGORY_ID);
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
            categoryService,
            Mockito.mock(CustomObjectService.class),
            Mockito.mock(StateService.class),
            Mockito.mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNonExistingCategoryReferenceAttribute_ShouldNotResolveReferences() {
    // preparation
    when(categoryService.fetchCachedCategoryId("nonExistingCatKey"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject("nonExistingCatKey", CategoryReference.CATEGORY);
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
    final Reference attributeValue =
        ProductSyncMockUtils.createReferenceObject("foo", CategoryReference.CATEGORY);
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
    assertThat(resolvedAttribute.getValue()).isInstanceOf(JsonNode.class);
    final CategoryReference resolvedReference =
        JsonUtils.fromJsonNode(
            (JsonNode) resolvedAttribute.getValue(), CategoryReference.typeReference());
    assertThat(resolvedReference.getId()).isEqualTo(CATEGORY_ID);
  }

  @Test
  void resolveReferences_WithCategoryReferenceSetAttribute_ShouldResolveReferences() {
    final Attribute categoryReferenceSetAttributeDraft =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo",
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), CategoryReference.CATEGORY),
            ProductSyncMockUtils.createReferenceObject(
                UUID.randomUUID().toString(), CategoryReference.CATEGORY));

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
    final List<CategoryReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CategoryReference.typeReference());
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
        ProductSyncMockUtils.createReferenceObject(
            UUID.randomUUID().toString(), CategoryReference.CATEGORY);
    final Attribute categoryReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft("foo", categoryReference);

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
    final List<CategoryReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CategoryReference.typeReference());
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
        ProductSyncMockUtils.createReferenceObject("existingKey", CategoryReference.CATEGORY);
    final Reference categoryReference2 =
        ProductSyncMockUtils.createReferenceObject("randomKey", CategoryReference.CATEGORY);

    final Attribute categoryReferenceAttribute =
        ProductSyncMockUtils.getReferenceSetAttributeDraft(
            "foo", categoryReference1, categoryReference2);

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
    final List<CategoryReference> referenceList =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedAttributeDraft.getValue(), CategoryReference.typeReference());
    final Set<CategoryReference> resolvedSet = new HashSet<>(referenceList);
    assertThat(resolvedSet).isNotNull();

    final CategoryReference resolvedReference1 =
        (CategoryReference)
            ProductSyncMockUtils.createReferenceObject("existingId", CategoryReference.CATEGORY);
    final CategoryReference resolvedReference2 =
        (CategoryReference)
            ProductSyncMockUtils.createReferenceObject("randomKey", CategoryReference.CATEGORY);
    assertThat(resolvedSet).containsExactlyInAnyOrder(resolvedReference1, resolvedReference2);
  }
}
