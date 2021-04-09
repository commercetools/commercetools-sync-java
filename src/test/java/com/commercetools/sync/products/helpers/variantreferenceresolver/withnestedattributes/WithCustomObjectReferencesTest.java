package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomObjectService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
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
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.products.ProductVariantDraft;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
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

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
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

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        2,
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
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

    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceSetAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-1-name",
        createReferenceObject(
            "non-existing-container|non-existing-key-1", CustomObject.referenceTypeId()),
        createReferenceObject(CUSTOM_OBJECT_ID, CustomObject.referenceTypeId()));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-2-name",
        CUSTOM_OBJECT_ID,
        CustomObject.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap,
        "nested-attribute-3-name",
        "non-existing-container|non-existing-key-3",
        CustomObject.referenceTypeId());
  }
}
