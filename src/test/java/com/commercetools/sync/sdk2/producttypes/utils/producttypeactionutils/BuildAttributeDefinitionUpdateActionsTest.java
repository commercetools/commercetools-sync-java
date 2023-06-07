package com.commercetools.sync.sdk2.producttypes.utils.producttypeactionutils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.producttypes.MockBuilderUtils.*;
import static com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters.toAttributeDefinitionDraftBuilder;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeUpdateActionUtils.buildAttributesUpdateActions;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeAddAttributeDefinitionAction;
import com.commercetools.api.models.product_type.ProductTypeAddAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeAttributeOrderByNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeInputHintActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeIsSearchableActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildAttributeDefinitionUpdateActionsTest {
  private static final String RES_ROOT =
      "com/commercetools/sync/producttypes/utils/updateattributedefinitions/attributes/";

  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_AB =
      RES_ROOT + "product-type-with-attribute-definitions-ab.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABB =
      RES_ROOT + "product-type-with-attribute-definitions-abb.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABC =
      RES_ROOT + "product-type-with-attribute-definitions-abc.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABCD =
      RES_ROOT + "product-type-with-attribute-definitions-abcd.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ABD =
      RES_ROOT + "product-type-with-attribute-definitions-abd.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CAB =
      RES_ROOT + "product-type-with-attribute-definitions-cab.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CB =
      RES_ROOT + "product-type-with-attribute-definitions-cb.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ACBD =
      RES_ROOT + "product-type-with-attribute-definitions-acbd.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_ADBC =
      RES_ROOT + "product-type-with-attribute-definitions-adbc.json";
  private static final String PRODUCT_TYPE_WITH_ATTRIBUTES_CBD =
      RES_ROOT + "product-type-with-attribute-definitions-cbd.json";

  private static final ProductTypeSyncOptions SYNC_OPTIONS =
      ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  private static final AttributeDefinition ATTRIBUTE_DEFINITION_A =
      createMockAttributeDefinitionBuilder()
          .name("a")
          .label(ofEnglish("label_en"))
          .type(AttributeTypeBuilder::textBuilder)
          .build();

  private static final AttributeDefinition ATTRIBUTE_DEFINITION_B =
      createMockAttributeDefinitionBuilder()
          .name("b")
          .label(ofEnglish("label_en"))
          .type(AttributeTypeBuilder::textBuilder)
          .build();

  private static final AttributeDefinition ATTRIBUTE_DEFINITION_C =
      createMockAttributeDefinitionBuilder()
          .name("c")
          .label(ofEnglish("label_en"))
          .type(AttributeTypeBuilder::textBuilder)
          .build();

  private static final AttributeDefinition ATTRIBUTE_DEFINITION_D =
      createMockAttributeDefinitionBuilder()
          .name("d")
          .label(ofEnglish("label_en"))
          .type(AttributeTypeBuilder::textBuilder)
          .isRequired(false)
          .build();

  @Test
  void
      buildAttributesUpdateActions_WithNullNewAttributesAndExistingAttributes_ShouldBuild3RemoveActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(
            oldProductType,
            ProductTypeDraftBuilder.of()
                .key("key")
                .name("name")
                .description("description")
                .attributes((List<AttributeDefinitionDraft>) null)
                .build(),
            SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("a").build(),
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("b").build(),
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("c").build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithNullNewAttributesAndNoOldAttributes_ShouldNotBuildActions() {
    final ProductType oldProductType = mock(ProductType.class);
    when(oldProductType.getAttributes()).thenReturn(emptyList());
    when(oldProductType.getKey()).thenReturn("product_type_key_1");

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(
            oldProductType,
            ProductTypeDraftBuilder.of()
                .key("key")
                .name("name")
                .description("description")
                .attributes((List<AttributeDefinitionDraft>) null)
                .build(),
            SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAttributesUpdateActions_WithNewAttributesAndNoOldAttributes_ShouldBuild3AddActions() {
    final ProductType oldProductType = mock(ProductType.class);
    when(oldProductType.getAttributes()).thenReturn(emptyList());
    when(oldProductType.getKey()).thenReturn("product_type_key_1");

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(3);
    assertThat(updateActions)
        .allSatisfy(
            action -> {
              assertThat(action).isInstanceOf(ProductTypeAddAttributeDefinitionAction.class);
              final ProductTypeAddAttributeDefinitionAction addAttributeDefinition =
                  (ProductTypeAddAttributeDefinitionAction) action;
              final AttributeDefinitionDraft attribute = addAttributeDefinition.getAttribute();
              assertThat(newProductTypeDraft.getAttributes()).contains(attribute);
            });
  }

  @Test
  void buildAttributesUpdateActions_WithIdenticalAttributes_ShouldNotBuildUpdateActions() {
    final ProductType oldProductType =
        createMockProductTypeBuilder()
            .attributes(
                asList(ATTRIBUTE_DEFINITION_A, ATTRIBUTE_DEFINITION_B, ATTRIBUTE_DEFINITION_C))
            .build();

    final AttributeDefinitionDraft attributeA =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_A.getType())
            .name(ATTRIBUTE_DEFINITION_A.getName())
            .label(ATTRIBUTE_DEFINITION_A.getLabel())
            .isRequired(ATTRIBUTE_DEFINITION_A.getIsRequired())
            .build();

    final AttributeDefinitionDraft attributeB =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_B.getType())
            .name(ATTRIBUTE_DEFINITION_B.getName())
            .label(ATTRIBUTE_DEFINITION_B.getLabel())
            .isRequired(ATTRIBUTE_DEFINITION_B.getIsRequired())
            .build();

    final AttributeDefinitionDraft attributeC =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_C.getType())
            .name(ATTRIBUTE_DEFINITION_C.getName())
            .label(ATTRIBUTE_DEFINITION_C.getLabel())
            .isRequired(ATTRIBUTE_DEFINITION_C.getIsRequired())
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder()
            .attributes(asList(attributeA, attributeB, attributeC))
            .build();

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, productTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAttributesUpdateActions_WithChangedAttributes_ShouldBuildUpdateActions() {
    final ProductType oldProductType =
        createMockProductTypeBuilder()
            .attributes(
                asList(ATTRIBUTE_DEFINITION_A, ATTRIBUTE_DEFINITION_B, ATTRIBUTE_DEFINITION_C))
            .build();

    final AttributeDefinitionDraft attributeA =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_A.getType())
            .name(ATTRIBUTE_DEFINITION_A.getName())
            .label(ATTRIBUTE_DEFINITION_A.getLabel())
            .isRequired(ATTRIBUTE_DEFINITION_A.getIsRequired())
            .isSearchable(true)
            .build();

    final AttributeDefinitionDraft attributeB =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_B.getType())
            .name(ATTRIBUTE_DEFINITION_B.getName())
            .label(ofEnglish("newLabel"))
            .isRequired(ATTRIBUTE_DEFINITION_B.getIsRequired())
            .inputHint(TextInputHint.TextInputHintEnum.SINGLE_LINE)
            .build();

    final AttributeDefinitionDraft attributeC =
        createMockAttributeDefinitionDraftBuilder()
            .type(ATTRIBUTE_DEFINITION_C.getType())
            .name(ATTRIBUTE_DEFINITION_C.getName())
            .label(ATTRIBUTE_DEFINITION_C.getLabel())
            .isRequired(ATTRIBUTE_DEFINITION_C.getIsRequired())
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder()
            .attributes(asList(attributeA, attributeB, attributeC))
            .build();

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, productTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductTypeChangeIsSearchableActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_A.getName())
                .isSearchable(true)
                .build(),
            ProductTypeChangeInputHintActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_B.getName())
                .newValue(TextInputHint.TextInputHintEnum.SINGLE_LINE)
                .build(),
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_B.getName())
                .label(ofEnglish("newLabel"))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithDuplicateAttributeNames_ShouldNotBuildActionsAndTriggerErrorCb() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABB, ProductTypeDraft.class);

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .matches(
            "Failed to build update actions for the attributes definitions of the "
                + "product type with the key 'key'. Reason: .*DuplicateNameException: Attribute definitions drafts "
                + "have duplicated names. Duplicated attribute definition name: 'b'. Attribute definitions names are "
                + "expected to be unique inside their product type.");
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            "Attribute definitions drafts have duplicated names. "
                + "Duplicated attribute definition name: 'b'. Attribute definitions names are expected to be unique "
                + "inside their product type.");
    assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateNameException.class);
  }

  @Test
  void buildAttributesUpdateActions_WithOneMissingAttribute_ShouldBuildRemoveAttributeAction() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_AB, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("c").build());
  }

  @Test
  void buildAttributesUpdateActions_WithOneExtraAttribute_ShouldBuildAddAttributesAction() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABCD, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .type(ATTRIBUTE_DEFINITION_D.getType())
                        .name(ATTRIBUTE_DEFINITION_D.getName())
                        .label(ATTRIBUTE_DEFINITION_D.getLabel())
                        .isRequired(ATTRIBUTE_DEFINITION_D.getIsRequired())
                        .isSearchable(true)
                        .build())
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithOneAttributeSwitch_ShouldBuildRemoveAndAddAttributesActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABD, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("c").build(),
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .type(ATTRIBUTE_DEFINITION_D.getType())
                        .name(ATTRIBUTE_DEFINITION_D.getName())
                        .label(ATTRIBUTE_DEFINITION_D.getLabel())
                        .isRequired(ATTRIBUTE_DEFINITION_D.getIsRequired())
                        .isSearchable(true)
                        .build())
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithDifferentOrder_ShouldBuildChangeAttributeOrderAction() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_CAB, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    asList(
                        ATTRIBUTE_DEFINITION_C.getName(),
                        ATTRIBUTE_DEFINITION_A.getName(),
                        ATTRIBUTE_DEFINITION_B.getName()))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_CB, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("a").build(),
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    asList(ATTRIBUTE_DEFINITION_C.getName(), ATTRIBUTE_DEFINITION_B.getName()))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ACBD, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .type(ATTRIBUTE_DEFINITION_D.getType())
                        .name(ATTRIBUTE_DEFINITION_D.getName())
                        .label(ATTRIBUTE_DEFINITION_D.getLabel())
                        .isRequired(ATTRIBUTE_DEFINITION_D.getIsRequired())
                        .isSearchable(true)
                        .inputHint(TextInputHint.SINGLE_LINE)
                        .attributeConstraint(AttributeConstraintEnum.NONE)
                        .build())
                .build(),
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    asList(
                        ATTRIBUTE_DEFINITION_A.getName(),
                        ATTRIBUTE_DEFINITION_C.getName(),
                        ATTRIBUTE_DEFINITION_B.getName(),
                        ATTRIBUTE_DEFINITION_D.getName()))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithAddedAttributeInBetween_ShouldBuildChangeOrderAndAddActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ADBC, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .type(ATTRIBUTE_DEFINITION_D.getType())
                        .name(ATTRIBUTE_DEFINITION_D.getName())
                        .label(ATTRIBUTE_DEFINITION_D.getLabel())
                        .isRequired(ATTRIBUTE_DEFINITION_D.getIsRequired())
                        .isSearchable(true)
                        .build())
                .build(),
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    asList(
                        ATTRIBUTE_DEFINITION_A.getName(),
                        ATTRIBUTE_DEFINITION_D.getName(),
                        ATTRIBUTE_DEFINITION_B.getName(),
                        ATTRIBUTE_DEFINITION_C.getName()))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAttributeActions() {
    final ProductType oldProductType =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_ABC, ProductType.class);

    final ProductTypeDraft newProductTypeDraft =
        readObjectFromResource(PRODUCT_TYPE_WITH_ATTRIBUTES_CBD, ProductTypeDraft.class);

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("a").build(),
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .type(ATTRIBUTE_DEFINITION_D.getType())
                        .name(ATTRIBUTE_DEFINITION_D.getName())
                        .label(ATTRIBUTE_DEFINITION_D.getLabel())
                        .isRequired(ATTRIBUTE_DEFINITION_D.getIsRequired())
                        .isSearchable(true)
                        .build())
                .build(),
            ProductTypeChangeAttributeOrderByNameActionBuilder.of()
                .attributeNames(
                    asList(
                        ATTRIBUTE_DEFINITION_C.getName(),
                        ATTRIBUTE_DEFINITION_B.getName(),
                        ATTRIBUTE_DEFINITION_D.getName()))
                .build());
  }

  @Test
  void
      buildAttributesUpdateActions_WithDifferentAttributeType_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.textBuilder())
            .label(ofEnglish("new_label"))
            .build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.ltextBuilder())
            .label(ofEnglish("old_label"))
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(singletonList(newDefinition)).build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(singletonList(oldDefinition)).build();
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                newDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithDifferentSetAttributeType_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(AttributeTypeBuilder::textBuilder))
            .label(ofEnglish("new_label"))
            .build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(AttributeTypeBuilder::ltextBuilder))
            .label(ofEnglish("old_label"))
            .isRequired(true)
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(singletonList(newDefinition)).build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(singletonList(oldDefinition)).build();
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                newDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithDifferentNestedAttributeRefs_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinition ofNestedType =
        createMockAttributeDefinitionBuilder()
            .name("nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(ProductTypeReferenceBuilder.of().id("foo").build()))
            .build();

    final AttributeDefinition ofSetOfNestedType =
        createMockAttributeDefinitionBuilder()
            .name("set-of-nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        ProductTypeReferenceBuilder.of().id("foo").build())))
            .build();

    final AttributeDefinition ofSetOfSetOfNestedType =
        createMockAttributeDefinitionBuilder()
            .name("set-of-set-of-nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        ProductTypeReferenceBuilder.of().id("foo").build())))
            .build();

    final AttributeDefinitionDraft ofNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofNestedType)
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(ProductTypeReferenceBuilder.of().id("bar").build()))
            .build();

    final AttributeDefinitionDraft ofSetOfNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofSetOfNestedType)
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        ProductTypeReferenceBuilder.of().id("bar").build())))
            .build();

    final AttributeDefinitionDraft ofSetOfSetOfNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofSetOfSetOfNestedType)
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder2 ->
                                            attributeTypeBuilder2
                                                .nestedBuilder()
                                                .typeReference(
                                                    ProductTypeReferenceBuilder.of()
                                                        .id("bar")
                                                        .build()))))
            .build();

    final ProductType oldProductType = mock(ProductType.class);
    when(oldProductType.getAttributes())
        .thenReturn(asList(ofNestedType, ofSetOfNestedType, ofSetOfSetOfNestedType));

    final ProductTypeDraft newProductTypeDraft = mock(ProductTypeDraft.class);
    when(newProductTypeDraft.getAttributes())
        .thenReturn(asList(ofNestedTypeDraft, ofSetOfNestedTypeDraft, ofSetOfSetOfNestedTypeDraft));

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            "changing the attribute definition type (attribute name='nested') is not supported programmatically");
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void buildAttributesUpdateActions_WithIdenticalNestedAttributeRefs_ShouldNotBuildActions() {
    // preparation
    final AttributeDefinition ofNestedType =
        createMockAttributeDefinitionBuilder()
            .name("nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(ProductTypeReferenceBuilder.of().id("foo").build()))
            .build();

    final AttributeDefinition ofSetOfNestedType =
        createMockAttributeDefinitionBuilder()
            .name("set-of-nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(ProductTypeReferenceBuilder.of().id("foo").build()))
            .build();

    final AttributeDefinition ofSetOfSetOfNestedType =
        createMockAttributeDefinitionBuilder()
            .name("set-of-set-of-nested")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder2 ->
                                            attributeTypeBuilder2
                                                .nestedBuilder()
                                                .typeReference(
                                                    ProductTypeReferenceBuilder.of()
                                                        .id("foo")
                                                        .build()))))
            .build();

    final AttributeDefinitionDraft ofNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofNestedType).build();

    final AttributeDefinitionDraft ofSetOfNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofSetOfNestedType).build();

    final AttributeDefinitionDraft ofSetOfSetOfNestedTypeDraft =
        toAttributeDefinitionDraftBuilder(ofSetOfSetOfNestedType).build();

    final ProductType oldProductType =
        createMockProductTypeBuilder()
            .attributes(asList(ofNestedType, ofSetOfNestedType, ofSetOfSetOfNestedType))
            .build();

    final ProductTypeDraft newProductTypeDraft =
        createMockProductTypeDraftBuilder()
            .attributes(
                asList(ofNestedTypeDraft, ofSetOfNestedTypeDraft, ofSetOfSetOfNestedTypeDraft))
            .build();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, newProductTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAttributesUpdateActions_WithANullAttributeDefinitionDraft_ShouldSkipNullAttributes() {
    // preparation
    final ProductType oldProductType = mock(ProductType.class);
    final AttributeDefinition attributeDefinition =
        createMockAttributeDefinitionBuilder()
            .label(ofEnglish("label1"))
            .type(attributeTypeBuilder -> attributeTypeBuilder.lenumBuilder().values(emptyList()))
            .build();

    when(oldProductType.getAttributes()).thenReturn(singletonList(attributeDefinition));

    final AttributeDefinitionDraft attributeDefinitionDraftWithDifferentLabel =
        createMockAttributeDefinitionDraftBuilder()
            .label(ofEnglish("label2"))
            .type(attributeTypeBuilder -> attributeTypeBuilder.lenumBuilder().values(emptyList()))
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder()
            .attributes(asList(null, attributeDefinitionDraftWithDifferentLabel))
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(oldProductType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(attributeDefinitionDraftWithDifferentLabel.getName())
                .label(attributeDefinitionDraftWithDifferentLabel.getLabel())
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfText_ShouldBuildActions() {
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(AttributeTypeBuilder::textBuilder))
            .label(ofEnglish("new_label"))
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(singletonList(newDefinition)).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(AttributeTypeBuilder::textBuilder))
            .label(ofEnglish("old_label"))
            .build();
    final ProductType productType = mock(ProductType.class);
    when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(newDefinition.getName())
                .label(newDefinition.getLabel())
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfSetOfText_ShouldBuildActions() {

    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(AttributeTypeBuilder::textBuilder)))
            .label(ofEnglish("new_label"))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(singletonList(newDefinition)).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(AttributeTypeBuilder::textBuilder)))
            .label(ofEnglish("old_label"))
            .build();
    final ProductType productType = mock(ProductType.class);
    when(productType.getAttributes()).thenReturn(singletonList(oldDefinition));

    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(newDefinition.getName())
                .label(newDefinition.getLabel())
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfEnumsChanges_ShouldBuildCorrectActions() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        asList(
                                            AttributePlainEnumValueBuilder.of()
                                                .key("a")
                                                .label("a")
                                                .build(),
                                            AttributePlainEnumValueBuilder.of()
                                                .key("b")
                                                .label("newB")
                                                .build(),
                                            AttributePlainEnumValueBuilder.of()
                                                .key("c")
                                                .label("c")
                                                .build()))))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(singletonList(newDefinition)).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        asList(
                                            AttributePlainEnumValueBuilder.of()
                                                .key("d")
                                                .label("d")
                                                .build(),
                                            AttributePlainEnumValueBuilder.of()
                                                .key("b")
                                                .label("b")
                                                .build(),
                                            AttributePlainEnumValueBuilder.of()
                                                .key("a")
                                                .label("a")
                                                .build()))))
            .build();
    final ProductType productType =
        createMockProductTypeBuilder().attributes(singletonList(oldDefinition)).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .keys("d")
                .build(),
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .newValue(AttributePlainEnumValueBuilder.of().key("b").label("newB").build())
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .value(AttributePlainEnumValueBuilder.of().key("c").label("c").build())
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .values(
                    asList(
                        AttributePlainEnumValueBuilder.of().key("a").label("a").build(),
                        AttributePlainEnumValueBuilder.of().key("b").label("newB").build(),
                        AttributePlainEnumValueBuilder.of().key("c").label("c").build()))
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfIdenticalEnums_ShouldNotBuildActions() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        singletonList(
                                            AttributePlainEnumValueBuilder.of()
                                                .key("foo")
                                                .label("bar")
                                                .build()))))
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        AttributePlainEnumValueBuilder.of()
                                            .key("foo")
                                            .label("bar")
                                            .build())))
            .build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfLEnumsChanges_ShouldBuildCorrectActions() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("foo")
                                            .label(ofEnglish("bar"))
                                            .build())))
            .build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1.lenumBuilder().values(emptyList())))
            .build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .value(
                    AttributeLocalizedEnumValueBuilder.of()
                        .key("foo")
                        .label(ofEnglish("bar"))
                        .build())
                .build());
  }

  @Test
  void buildAttributesUpdateActions_WithSetOfIdenticalLEnums_ShouldBuildNoActions() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("foo")
                                            .label(ofEnglish("bar"))
                                            .build())))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        AttributeLocalizedEnumValueBuilder.of()
                                            .key("foo")
                                            .label(ofEnglish("bar"))
                                            .build())))
            .build();
    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildAttributesUpdateActions_WithOldEnumAndNewAsNonEnum_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder().type(AttributeTypeBuilder::textBuilder).build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .lenumBuilder()
                        .values(
                            AttributeLocalizedEnumValueBuilder.of()
                                .key("foo")
                                .label(ofEnglish("bar"))
                                .build()))
            .build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();
    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                oldDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithNewEnumAndOldAsNonEnum_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .enumBuilder()
                        .values(
                            AttributePlainEnumValueBuilder.of().key("foo").label("bar").build()))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder().type(AttributeTypeBuilder::textBuilder).build();
    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                oldDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithOldLenumAndNewAsNonLenum_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder().type(AttributeTypeBuilder::textBuilder).build();

    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .lenumBuilder()
                        .values(
                            AttributeLocalizedEnumValueBuilder.of()
                                .key("foo")
                                .label(ofEnglish("bar"))
                                .build()))
            .build();
    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                oldDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithNewLenumAndOldAsNonLenum_ShouldNotBuildActionsAndTriggerErrorCb() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .lenumBuilder()
                        .values(
                            AttributeLocalizedEnumValueBuilder.of()
                                .key("foo")
                                .label(ofEnglish("bar"))
                                .build()))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.textBuilder())
            .build();
    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();
    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, syncOptions);

    // assertions
    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            format(
                "changing the attribute definition type (attribute name='%s') is not supported programmatically",
                oldDefinition.getName()));
    assertThat(exceptions.get(0).getCause())
        .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void
      buildAttributesUpdateActions_WithSetOfLEnumsChangesAndDefLabelChange_ShouldBuildCorrectActions() {
    // preparation
    final AttributeDefinitionDraft newDefinition =
        createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        asList(
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("a")
                                                .label(ofEnglish("a"))
                                                .build(),
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("b")
                                                .label(ofEnglish("newB"))
                                                .build(),
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("c")
                                                .label(ofEnglish("c"))
                                                .build()))))
            .label(ofEnglish("new_label"))
            .build();
    final ProductTypeDraft productTypeDraft =
        createMockProductTypeDraftBuilder().attributes(newDefinition).build();

    final AttributeDefinition oldDefinition =
        createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        asList(
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("d")
                                                .label(ofEnglish("d"))
                                                .build(),
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("b")
                                                .label(ofEnglish("b"))
                                                .build(),
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("a")
                                                .label(ofEnglish("a"))
                                                .build()))))
            .build();

    final ProductType productType =
        createMockProductTypeBuilder().attributes(oldDefinition).build();

    // test
    final List<ProductTypeUpdateAction> updateActions =
        buildAttributesUpdateActions(productType, productTypeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .keys("d")
                .build(),
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .newValue(
                    AttributeLocalizedEnumValueBuilder.of()
                        .key("b")
                        .label(ofEnglish("newB"))
                        .build())
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .value(
                    AttributeLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("c")).build())
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .values(
                    asList(
                        AttributeLocalizedEnumValueBuilder.of()
                            .key("a")
                            .label(ofEnglish("a"))
                            .build(),
                        AttributeLocalizedEnumValueBuilder.of()
                            .key("b")
                            .label(ofEnglish("newB"))
                            .build(),
                        AttributeLocalizedEnumValueBuilder.of()
                            .key("c")
                            .label(ofEnglish("c"))
                            .build()))
                .build(),
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .label(ofEnglish("new_label"))
                .build());
  }
}
