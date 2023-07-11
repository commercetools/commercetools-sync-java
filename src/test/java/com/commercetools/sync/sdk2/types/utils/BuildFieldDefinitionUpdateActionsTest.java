package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.types.utils.FieldDefinitionFixtures.*;
import static com.commercetools.sync.sdk2.types.utils.TypeUpdateActionUtils.buildFieldDefinitionsUpdateActions;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import com.commercetools.sync.sdk2.types.TypeSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildFieldDefinitionUpdateActionsTest {
  private static final String TYPE_KEY = "key";
  private static final LocalizedString TYPE_NAME = ofEnglish("name");
  private static final LocalizedString TYPE_DESCRIPTION = ofEnglish("description");

  private static final TypeDraft TYPE_DRAFT =
      TypeDraftBuilder.of()
          .key(TYPE_KEY)
          .name(TYPE_NAME)
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .description(TYPE_DESCRIPTION)
          .build();

  private static final TypeSyncOptions SYNC_OPTIONS =
      TypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithNullNewFieldDefAndExistingFieldDefs_ShouldBuild3RemoveActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, TYPE_DRAFT, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_A).build(),
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_B).build(),
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_C).build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithNullNewFieldDefsAndNoOldFieldDefs_ShouldNotBuildActions() {
    final Type oldType = mock(Type.class);
    when(oldType.getFieldDefinitions()).thenReturn(emptyList());
    when(oldType.getKey()).thenReturn("type_key_1");

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, TYPE_DRAFT, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithNewFieldDefsAndNoOldFieldDefinitions_ShouldBuild3AddActions() {
    final Type oldType = mock(Type.class);
    when(oldType.getFieldDefinitions()).thenReturn(emptyList());
    when(oldType.getKey()).thenReturn("type_key_1");

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ABC, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_A).build(),
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_B).build(),
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_C).build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithIdenticalFieldDefinitions_ShouldNotBuildUpdateActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ABC, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithDuplicateFieldDefNames_ShouldNotBuildActionsAndTriggerErrorCb() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ABB, TypeDraft.class);

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final TypeSyncOptions syncOptions =
        TypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .matches(
            "Failed to build update actions for the field definitions of the "
                + "type with the key 'key'. Reason: .*DuplicateNameException: Field definitions "
                + "have duplicated names. Duplicated field definition name: 'b'. Field definitions names are "
                + "expected to be unique inside their type.");
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            "Field definitions have duplicated names. "
                + "Duplicated field definition name: 'b'. Field definitions names are expected to be unique "
                + "inside their type.");
    assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateNameException.class);
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithOneMissingFieldDefinition_ShouldBuildRemoveFieldDefAction() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_AB, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(TypeRemoveFieldDefinitionActionBuilder.of().fieldName("c").build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithOneExtraFieldDef_ShouldBuildTypeAddFieldDefinitionAction() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ABCD, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_D).build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithOneFieldDefSwitch_ShouldBuildRemoveAndAddFieldDefActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ABD, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_C).build(),
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_D).build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithDifferent_ShouldBuildChangeFieldDefinitionOrderAction() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_CAB, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeFieldDefinitionOrderActionBuilder.of()
                .fieldNames(
                    FIELD_DEFINITION_C.getName(),
                    FIELD_DEFINITION_A.getName(),
                    FIELD_DEFINITION_B.getName())
                .build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithRemovedAndDiffOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_CB, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_A).build(),
            TypeChangeFieldDefinitionOrderActionBuilder.of()
                .fieldNames(FIELD_DEFINITION_C.getName(), FIELD_DEFINITION_B.getName())
                .build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ACBD, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_D).build(),
            TypeChangeFieldDefinitionOrderActionBuilder.of()
                .fieldNames(
                    FIELD_DEFINITION_A.getName(),
                    FIELD_DEFINITION_C.getName(),
                    FIELD_DEFINITION_B.getName(),
                    FIELD_DEFINITION_D.getName())
                .build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithAddedFieldDefInBetween_ShouldBuildChangeOrderAndAddActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_ADBC, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_D).build(),
            TypeChangeFieldDefinitionOrderActionBuilder.of()
                .fieldNames(
                    FIELD_DEFINITION_A.getName(),
                    FIELD_DEFINITION_D.getName(),
                    FIELD_DEFINITION_B.getName(),
                    FIELD_DEFINITION_C.getName())
                .build());
  }

  @Test
  void buildFieldDefinitionsUpdateActions_WithMixedFields_ShouldBuildAllThreeMoveFieldDefActions() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft = readObjectFromResource(TYPE_WITH_FIELDS_CBD, TypeDraft.class);

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);
    assertThat(updateActions)
        .containsExactly(
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_A).build(),
            TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(FIELD_DEFINITION_D).build(),
            TypeChangeFieldDefinitionOrderActionBuilder.of()
                .fieldNames(
                    FIELD_DEFINITION_C.getName(),
                    FIELD_DEFINITION_B.getName(),
                    FIELD_DEFINITION_D.getName())
                .build());
  }

  @Test
  void
      buildFieldDefinitionsUpdateActions_WithDifferentType_ShouldRemoveOldFieldDefAndAddNewFieldDef() {
    final Type oldType = readObjectFromResource(TYPE_WITH_FIELDS_ABC, Type.class);

    final TypeDraft newTypeDraft =
        readObjectFromResource(TYPE_WITH_FIELDS_ABC_WITH_DIFFERENT_TYPE, TypeDraft.class);
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, newTypeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            TypeRemoveFieldDefinitionActionBuilder.of().fieldName(FIELD_A).build(),
            TypeAddFieldDefinitionActionBuilder.of()
                .fieldDefinition(FIELD_DEFINITION_A_LOCALIZED_TYPE)
                .build());
  }

  @Test
  void buildFieldDefinitionsUpdateActions_WithNullNewFieldDef_ShouldSkipNullFieldDefs() {
    // preparation
    final Type oldType = mock(Type.class);
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldLocalizedEnumTypeBuilder.of().values(emptyList()).build())
            .name("field_1")
            .label(ofEnglish("label1"))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    when(oldType.getFieldDefinitions()).thenReturn(singletonList(oldFieldDefinition));

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldLocalizedEnumTypeBuilder.of().values(emptyList()).build())
            .name("field_1")
            .label(ofEnglish("label2"))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("key")
            .name(ofEnglish("label"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(null, newFieldDefinition)
            .build();

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(oldType, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName(newFieldDefinition.getName())
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void buildFieldsUpdateActions_WithSetOfText_ShouldBuildActions() {
    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of().elementType(FieldTypeBuilder::stringBuilder).build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.MULTI_LINE)
            .build();
    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of().elementType(FieldTypeBuilder::stringBuilder).build())
            .name("a")
            .label(ofEnglish("old_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            TypeChangeInputHintActionBuilder.of()
                .fieldName("a")
                .inputHint(TypeTextInputHint.MULTI_LINE)
                .build(),
            TypeChangeLabelActionBuilder.of()
                .fieldName(newDefinition.getName())
                .label(newDefinition.getLabel())
                .build());
  }

  @Test
  void buildFieldsUpdateActions_WithSetOfEnumsChanges_ShouldBuildCorrectActions() {
    // preparation
    final CustomFieldEnumValue value_A =
        CustomFieldEnumValueBuilder.of().key("a").label("a").build();
    final CustomFieldEnumValue value_B =
        CustomFieldEnumValueBuilder.of().key("b").label("b").build();
    final CustomFieldEnumValue value_C =
        CustomFieldEnumValueBuilder.of().key("c").label("c").build();
    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldEnumTypeBuilder.of().values(value_A, value_B, value_C).build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(CustomFieldEnumTypeBuilder.of().values(value_B, value_A).build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName("a").value(value_C).build(),
            TypeChangeEnumValueOrderActionBuilder.of().fieldName("a").keys("a", "b", "c").build());
  }

  @Test
  void buildFieldsUpdateActions_WithSetOfIdenticalEnums_ShouldNotBuildActions() {
    // preparation
    final CustomFieldEnumValue value_A =
        CustomFieldEnumValueBuilder.of().key("a").label("a").build();
    final CustomFieldEnumValue value_B =
        CustomFieldEnumValueBuilder.of().key("b").label("b").build();
    final CustomFieldEnumValue value_C =
        CustomFieldEnumValueBuilder.of().key("c").label("c").build();
    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldEnumTypeBuilder.of().values(value_A, value_B, value_C).build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldEnumTypeBuilder.of().values(value_A, value_B, value_C).build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildFieldsUpdateActions_WithSetOfLEnumsChanges_ShouldBuildCorrectActions() {
    // preparation
    final CustomFieldLocalizedEnumValue value_A =
        CustomFieldLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("a")).build();
    final CustomFieldLocalizedEnumValue value_B =
        CustomFieldLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("b")).build();
    final CustomFieldLocalizedEnumValue value_C =
        CustomFieldLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("c")).build();

    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(value_A, value_B, value_C)
                            .build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of().values(value_B, value_A).build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of().fieldName("a").value(value_C).build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName("a")
                .keys("a", "b", "c")
                .build());
  }

  @Test
  void buildFieldsUpdateActions_WithSetOfIdenticalLEnums_ShouldNotBuildActions() {
    // preparation
    final CustomFieldLocalizedEnumValue value_A =
        CustomFieldLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("a")).build();
    final CustomFieldLocalizedEnumValue value_B =
        CustomFieldLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("b")).build();
    final CustomFieldLocalizedEnumValue value_C =
        CustomFieldLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("c")).build();

    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(value_A, value_B, value_C)
                            .build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(value_A, value_B, value_C)
                            .build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildFieldsUpdateActions_WithSetOfLEnumsChangesAndDefinitionLabelChange_ShouldBuildCorrectActions() {
    // preparation
    final CustomFieldLocalizedEnumValue value_A =
        CustomFieldLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("a")).build();
    final CustomFieldLocalizedEnumValue value_B =
        CustomFieldLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("newB")).build();
    final CustomFieldLocalizedEnumValue value_old_B =
        CustomFieldLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("b")).build();
    final CustomFieldLocalizedEnumValue value_C =
        CustomFieldLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("c")).build();

    final FieldDefinition newDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(value_A, value_B, value_C)
                            .build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .fieldDefinitions(newDefinition)
            .build();

    final FieldDefinition oldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(value_old_B, value_A)
                            .build())
                    .build())
            .name("a")
            .label(ofEnglish("new_label"))
            .required(true)
            .inputHint(TypeTextInputHint.MULTI_LINE)
            .build();

    final Type type = mock(Type.class);
    when(type.getFieldDefinitions()).thenReturn(singletonList(oldDefinition));

    // test
    final List<TypeUpdateAction> updateActions =
        buildFieldDefinitionsUpdateActions(type, typeDraft, SYNC_OPTIONS);

    // assertion
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            TypeChangeInputHintActionBuilder.of()
                .fieldName("a")
                .inputHint(TypeTextInputHint.SINGLE_LINE)
                .build(),
            TypeAddLocalizedEnumValueActionBuilder.of().fieldName("a").value(value_C).build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName("a")
                .keys("a", "b", "c")
                .build(),
            TypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .fieldName("a")
                .value(value_B)
                .build());
  }
}
