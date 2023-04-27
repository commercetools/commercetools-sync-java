package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.getMockCustomerService;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithCustomerReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private CustomerService customerService;
  private static final String RES_SUB_ROOT = "withcustomerreferences/";
  private static final String NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";
  private static final String CUSTOMER_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
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
  void resolveReferences_WithNestedCustomerReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedCustomerReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_CUSTOMER_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedCustomerReferenceAttributes)
            .toCompletableFuture()
            .join();

    // assertions
    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(List.class);
    final List<Attribute> resolvedNestedAttributes = (List) value;

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
  }

  @Test
  void resolveReferences_WithNestedSetOfCustomerReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedSetOfCustomerReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOMER_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedSetOfCustomerReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(List.class);
    final List<Attribute> resolvedNestedAttributes = (List) value;

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOMER_ID,
        CustomerReference.CUSTOMER);
  }
}
