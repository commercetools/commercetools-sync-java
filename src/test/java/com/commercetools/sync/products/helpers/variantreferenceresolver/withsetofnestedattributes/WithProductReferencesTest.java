package com.commercetools.sync.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockProductService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
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
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
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
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_PRODUCT_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedProductReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final JsonNode resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributeAsArray = (ArrayNode) resolvedNestedAttribute;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributeAsArray.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        PRODUCT_ID,
        Product.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        Product.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_ID,
        Product.referenceTypeId());
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfProductReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfProductReferenceAttributes =
        readObjectFromResource(
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

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final JsonNode resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributeAsArray = (ArrayNode) resolvedNestedAttribute;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributeAsArray.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        PRODUCT_ID,
        Product.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        Product.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        PRODUCT_ID,
        Product.referenceTypeId());
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
        readObjectFromResource(
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

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode setOfResolvedNestedAttributes = (ArrayNode) value;

    final JsonNode resolvedNestedAttribute = setOfResolvedNestedAttributes.get(0);
    assertThat(resolvedNestedAttribute).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributeAsArray = (ArrayNode) resolvedNestedAttribute;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributeAsArray.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        createReferenceObject("nonExistingProductKey1", Product.referenceTypeId()),
        createReferenceObject(PRODUCT_ID, Product.referenceTypeId()));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        PRODUCT_ID,
        Product.referenceTypeId());

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "nonExistingProductKey3",
        Product.referenceTypeId());
  }
}
