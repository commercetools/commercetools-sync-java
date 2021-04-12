package com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes;

import static com.commercetools.sync.products.ProductSyncMockUtils.getMockStateService;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.AssertionUtilsForVariantReferenceResolver.assertReferenceSetAttributeValue;
import static com.commercetools.sync.products.helpers.variantreferenceresolver.withnestedattributes.WithNoReferencesTest.RES_ROOT;
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
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.states.State;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithStateReferencesTest {
  private VariantReferenceResolver referenceResolver;
  private StateService stateService;
  private static final String RES_SUB_ROOT = "withstatereferences/";
  private static final String NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-reference.json";
  private static final String NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES =
      RES_ROOT + RES_SUB_ROOT + "with-set-of-references.json";
  private static final String STATE_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
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
  void resolveReferences_WithNestedStateReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedStateReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_STATE_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedStateReferenceAttributes)
            .toCompletableFuture()
            .join();
    // assertions
    final JsonNode value = resolvedAttributeDraft.getAttributes().get(0).getValue();
    assertThat(value).isInstanceOf(ArrayNode.class);
    final ArrayNode resolvedNestedAttributes = (ArrayNode) value;

    final Map<String, JsonNode> resolvedNestedAttributesMap =
        StreamSupport.stream(resolvedNestedAttributes.spliterator(), false)
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("name").asText(), jsonNode -> jsonNode));

    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-1-name", STATE_ID, State.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, State.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, State.referenceTypeId());
  }

  @Test
  void resolveReferences_WithNestedSetOfStateReferenceAttributes_ShouldResolveReferences() {
    // preparation
    final ProductVariantDraft withNestedSetOfStateReferenceAttributes =
        readObjectFromResource(
            NESTED_ATTRIBUTE_WITH_SET_OF_STATE_REFERENCE_ATTRIBUTES, ProductVariantDraft.class);

    // test
    final ProductVariantDraft resolvedAttributeDraft =
        referenceResolver
            .resolveReferences(withNestedSetOfStateReferenceAttributes)
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
        STATE_ID,
        State.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-2-name", STATE_ID, State.referenceTypeId());
    assertReferenceAttributeValue(
        resolvedNestedAttributesMap, "nested-attribute-3-name", STATE_ID, State.referenceTypeId());
  }
}
