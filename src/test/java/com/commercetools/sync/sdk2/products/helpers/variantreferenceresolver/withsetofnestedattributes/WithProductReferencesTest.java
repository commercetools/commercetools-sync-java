package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductReference;
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
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithProductReferencesTest {
  private ProductService productService;

  private static final String PRODUCT_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  private static final String RES_SUB_ROOT = "withproductreferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String
      SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES =
          WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

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
  void resolveReferences_WithSetOfNestedProductReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedProductReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedProductReferenceAttributes)
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
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfProductReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_PRODUCT_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfProductReferenceAttributes)
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
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        PRODUCT_ID,
        ProductReference.PRODUCT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedProductReferenceSetWithSomeNonExisting_ShouldOnlyResolveExistingReferences() {
    // preparation
    when(productService.getIdFromCacheOrFetch("nonExistingProductKey1"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(productService.getIdFromCacheOrFetch("nonExistingProductKey3"))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductVariantDraft withSetOfNestedProductReferenceSetWithSomeNonExisting =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_PRODUCT_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedProductReferenceSetWithSomeNonExisting)
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
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        createReferenceObject("nonExistingProductKey1", ProductReference.PRODUCT),
        createReferenceObject(PRODUCT_ID, ProductReference.PRODUCT));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "nonExistingProductKey3",
        ProductReference.PRODUCT);
  }
}
