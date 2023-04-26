package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
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

class WithProductTypeReferencesTest {
  private ProductTypeService productTypeService;

  private static final String PRODUCT_TYPE_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  private static final String RES_SUB_ROOT = "withproducttypereferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_PRODUCT_TYPE_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String
      SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_TYPE_REFERENCE_ATTRIBUTES =
          WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
  private static final String
      SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_TYPE_REFERENCE_ATTRIBUTES =
          WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

  @BeforeEach
  void setup() {
    productTypeService = getMockProductTypeService(PRODUCT_TYPE_ID);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
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
  void resolveReferences_WithSetOfNestedProductTypeReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedProductTypeReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_PRODUCT_TYPE_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedProductTypeReferenceAttributes)
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

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfProductTypeReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfProductTypeReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_TYPE_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfProductTypeReferenceAttributes)
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
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedProdTypeReferenceSetOfSomeNonExisting_ShouldOnlyResolveExistingReferences() {
    // preparation
    when(productTypeService.fetchCachedProductTypeId("nonExistingProductTypeKey1"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(productTypeService.fetchCachedProductTypeId("nonExistingProductTypeKey3"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductVariantDraft withSetOfNestedProductTypeReferenceSetWithSomeNonExisting =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_TYPE_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedProductTypeReferenceSetWithSomeNonExisting)
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
        createReferenceObject("nonExistingProductTypeKey1", ProductTypeReference.PRODUCT_TYPE),
        createReferenceObject(PRODUCT_TYPE_ID, ProductTypeReference.PRODUCT_TYPE));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_TYPE_ID,
        ProductTypeReference.PRODUCT_TYPE);

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "nonExistingProductTypeKey3",
        ProductTypeReference.PRODUCT_TYPE);
  }
}
