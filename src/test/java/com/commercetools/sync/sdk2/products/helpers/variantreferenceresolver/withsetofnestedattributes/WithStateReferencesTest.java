package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withsetofnestedattributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.VariantReferenceResolver;
import com.commercetools.sync.sdk2.services.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithStateReferencesTest {

  private VariantReferenceResolver referenceResolver;
  private StateService stateService;
  private static final String STATE_ID = UUID.randomUUID().toString();
  private static final String RES_SUB_ROOT = "withstatereferences/";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES =
      WithNoReferencesTest.RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    stateService = getMockStateService(STATE_ID);
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
            stateService,
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithSetOfNestedStateReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedStateReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedStateReferenceAttributes)
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
        resolvedNestedAttributesMap, "nested-attribute-1-name", STATE_ID, StateReference.STATE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, StateReference.STATE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, StateReference.STATE);
  }

  @Test
  void
      resolveReferences_WithSetOfNestedSetOfStateReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withSetOfNestedSetOfStateReferenceAttributes =
        createObjectFromResource(
            SET_OF_NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSetOfNestedSetOfStateReferenceAttributes)
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
        resolvedNestedAttributesMap, "nested-attribute-1-name", 2, STATE_ID, StateReference.STATE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, StateReference.STATE);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, StateReference.STATE);
  }
}
