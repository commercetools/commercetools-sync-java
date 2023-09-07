package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.state.StateReference;
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
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WithStateReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private StateService stateService;
  private static final String RES_SUB_ROOT = "withstatereferences/";
  private static final String NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";
  private static final String STATE_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    stateService = ProductSyncMockUtils.getMockStateService(STATE_ID);
    referenceResolver =
        new VariantReferenceResolver(
            syncOptions,
            Mockito.mock(TypeService.class),
            Mockito.mock(ChannelService.class),
            Mockito.mock(CustomerGroupService.class),
            Mockito.mock(ProductService.class),
            Mockito.mock(ProductTypeService.class),
            Mockito.mock(CategoryService.class),
            Mockito.mock(CustomObjectService.class),
            stateService,
            Mockito.mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNestedStateReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedStateReferenceAttributes =
        TestUtils.readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedStateReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final List<Attribute> resolvedNestedAttributes =
        TestUtils.convertArrayNodeToList((ArrayNode) value, Attribute.typeReference());

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-1-name", STATE_ID, StateReference.STATE);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, StateReference.STATE);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, StateReference.STATE);
  }

  @Test
  void resolveReferences_WithNestedSetOfStateReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedSetOfStateReferenceAttributes =
        TestUtils.readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedSetOfStateReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    assertThat(resolvedAttributeDraft.getAttributes()).isNotNull();

    final Object value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final List<Attribute> resolvedNestedAttributes =
        TestUtils.convertArrayNodeToList((ArrayNode) value, Attribute.typeReference());

    final Map<String, Object> resolvedNestedAttributesMap =
        resolvedNestedAttributes.stream()
            .collect(
                Collectors.toMap(
                    attribute -> attribute.getName(), attribute -> attribute.getValue()));

    AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-1-name", 2, STATE_ID, StateReference.STATE);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, StateReference.STATE);
    AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, StateReference.STATE);
  }
}
