package com.commercetools.sync.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
  void resolveReferences_WithSetOfNestedProductReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedProductReferenceAttributes =
        TestUtils.readObjectFromResource(
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
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final List<Attribute> resolvedNestedAttributes =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedNestedAttribute, Attribute.typeReference());

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
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
        TestUtils.readObjectFromResource(
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
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final List<Attribute> resolvedNestedAttributes =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedNestedAttribute, Attribute.typeReference());

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        PRODUCT_ID,
        ProductReference.PRODUCT);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
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
        TestUtils.readObjectFromResource(
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
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final Object resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final List<Attribute> resolvedNestedAttributes =
        TestUtils.convertArrayNodeToList(
            (ArrayNode) resolvedNestedAttribute, Attribute.typeReference());

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        ProductSyncMockUtils.createReferenceObject(
            "nonExistingProductKey1", ProductReference.PRODUCT),
        ProductSyncMockUtils.createReferenceObject(PRODUCT_ID, ProductReference.PRODUCT));

    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        ProductReference.PRODUCT);

    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "nonExistingProductKey3",
        ProductReference.PRODUCT);
  }
}
