package com.commercetools.sync.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.products.ProductVariantDraft;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithCustomerReferencesTest {

  private VariantReferenceResolver referenceResolver;
  private CustomerService customerService;
  private static final String CUSTOMER_ID = UUID.randomUUID().toString();
  private static final String RES_SUB_ROOT = "withcustomerreferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    customerService = getMockCustomerService(CUSTOMER_ID);
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(ProductService.class),
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(CustomObjectService.class),
            mock(StateService.class),
            customerService);
  }

  @Test
  void resolveReferences_WithSetOfNestedCustomerReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedCustomerReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedCustomerReferenceAttributes)
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
        CUSTOMER_ID,
        Customer.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOMER_ID,
        Customer.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOMER_ID,
        Customer.referenceTypeId());
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfCustomerReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfCustomerReferenceAttributes =
        readObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfCustomerReferenceAttributes)
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
        CUSTOMER_ID,
        Customer.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOMER_ID,
        Customer.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOMER_ID,
        Customer.referenceTypeId());
  }
}
