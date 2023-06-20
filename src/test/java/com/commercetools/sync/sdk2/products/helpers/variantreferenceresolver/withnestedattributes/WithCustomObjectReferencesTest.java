package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withnestedattributes;

import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObjectReference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
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

class WithCustomObjectReferencesTest {
  private CustomObjectService customObjectService;

  private static final String CUSTOM_OBJECT_ID = UUID.randomUUID().toString();
  private VariantReferenceResolver referenceResolver;

  private static final String RES_SUB_ROOT = "withcustomobjectreferences/";
  private static final String NESTED_ATTRIBUTE_WITH_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String
      NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES =
          RES_ROOT + RES_SUB_ROOT + "with-non-existing-references.json";
  private static final String NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";

  @BeforeEach
  void setup() {
    customObjectService = getMockCustomObjectService(CUSTOM_OBJECT_ID);
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
            mock(CategoryService.class),
            customObjectService,
            mock(StateService.class),
            mock(CustomerService.class));
  }

  @Test
  void resolveReferences_WithNestedCustomObjectReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedCustomObjReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedCustomObjReferenceAttributes)
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

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
  }

  @Test
  void
      resolveReferences_WithNestedSetOfCustomObjectReferenceAttributes_ShouldOnlyResolveExistingReferences() {
    // preparation
    final ProductVariantDraft withNestedSetOfCustomObjReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SET_OF_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedSetOfCustomObjReferenceAttributes)
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
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
  }

  @Test
  void
      resolveReferences_WithSomeNonExistingNestedCustomObjReferenceAttrs_ShouldOnlyResolveExistingReferences() {
    // preparation
    final CustomObjectCompositeIdentifier nonExistingCustomObject1Id =
        CustomObjectCompositeIdentifier.of("non-existing-key-1", "non-existing-container");

    when(customObjectService.fetchCachedCustomObjectId(nonExistingCustomObject1Id))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final CustomObjectCompositeIdentifier nonExistingCustomObject3Id =
        CustomObjectCompositeIdentifier.of("non-existing-key-3", "non-existing-container");

    when(customObjectService.fetchCachedCustomObjectId(nonExistingCustomObject3Id))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final ProductVariantDraft withSomeNonExistingNestedCustomObjReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SOME_NOT_EXISTING_CUSTOM_OBJECT_REFERENCE_ATTRIBUTES,
            ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withSomeNonExistingNestedCustomObjReferenceAttributes)
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
        createReferenceObject(
            "non-existing-container|non-existing-key-1", CustomObjectReference.KEY_VALUE_DOCUMENT),
        createReferenceObject(CUSTOM_OBJECT_ID, CustomObjectReference.KEY_VALUE_DOCUMENT));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObjectReference.KEY_VALUE_DOCUMENT);
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "non-existing-container|non-existing-key-3",
        CustomObjectReference.KEY_VALUE_DOCUMENT);
  }
}
