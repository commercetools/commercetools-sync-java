package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithCategoryReferencesTest {
  private CategoryService categoryService;

  private static final String CATEGORY_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  private static final String RES_SUB_ROOT = "withcategoryreferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_CATEGORY_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String
      SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CATEGORY_REFERENCE_ATTRIBUTES =
          WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CATEGORY_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

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
  void resolveReferences_WithSetOfNestedCategoryReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedCategoryReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_CATEGORY_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedCategoryReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(List.class);
    final List setOfResolvedNestedAttributes = (List) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(List.class);
    final List<Attribute> resolvedNestedAttributeAsArray = (List) resolvedNestedAttribute;

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributeAsArray.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfCategoryReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfCategoryReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CATEGORY_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfCategoryReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(List.class);
    final List setOfResolvedNestedAttributes = (List) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(List.class);
    final List<Attribute> resolvedNestedAttributes = (List) resolvedNestedAttribute;

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        CATEGORY_ID,
        CategoryReference.CATEGORY);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedCategoryReferenceSetOfSomeNonExisting_ShouldOnlyResolveExistingReferences() {
    // preparation
    when(categoryService.fetchCachedCategoryId("nonExistingCategoryKey1"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(categoryService.fetchCachedCategoryId("nonExistingCategoryKey3"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductVariantDraft withSetOfNestedCategoryReferenceSetWithSomeNonExisting =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CATEGORY_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedCategoryReferenceSetWithSomeNonExisting)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(List.class);
    final List setOfResolvedNestedAttributes = (List) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(List.class);
    final List<Attribute> resolvedNestedAttributes = (List) resolvedNestedAttribute;

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        createReferenceObject("nonExistingCategoryKey1", CategoryReference.CATEGORY),
        createReferenceObject(CATEGORY_ID, CategoryReference.CATEGORY));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CATEGORY_ID,
        CategoryReference.CATEGORY);

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "nonExistingCategoryKey3",
        CategoryReference.CATEGORY);
  }
}
