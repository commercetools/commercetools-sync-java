package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.utils.PlainEnumValueFixtures.CustomFieldEnumValueFixtures.ENUM_VALUE_A;
import static com.commercetools.sync.sdk2.commons.utils.PlainEnumValueFixtures.CustomFieldEnumValueFixtures.ENUM_VALUE_B;
import static com.commercetools.sync.sdk2.types.utils.FieldDefinitionFixtures.stringFieldDefinition;
import static com.commercetools.sync.sdk2.types.utils.FieldDefinitionUpdateActionUtils.buildActions;
import static com.commercetools.sync.sdk2.types.utils.FieldDefinitionUpdateActionUtils.buildChangeLabelUpdateAction;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.type.CustomFieldEnumTypeBuilder;
import com.commercetools.api.models.type.CustomFieldEnumValueBuilder;
import com.commercetools.api.models.type.CustomFieldLocalizedEnumTypeBuilder;
import com.commercetools.api.models.type.CustomFieldLocalizedEnumValue;
import com.commercetools.api.models.type.CustomFieldLocalizedEnumValueBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.TypeAddEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeChangeEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeChangeLabelActionBuilder;
import com.commercetools.api.models.type.TypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeTextInputHint;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.commons.utils.LocalizedEnumValueFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FieldDefinitionUpdateActionUtilsTest {
  private static final String FIELD_NAME_1 = "fieldName1";
  private static final String LABEL_1 = "label1";
  private static final String LABEL_2 = "label2";

  private static FieldDefinition old;
  private static FieldDefinition newSame;
  private static FieldDefinition newDifferent;

  private static final CustomFieldLocalizedEnumValue LOCALIZED_ENUM_VALUE_A =
      LocalizedEnumValueFixtures.CustomFieldLocalizedEnumValueFixtures.ENUM_VALUE_A;
  private static final CustomFieldLocalizedEnumValue LOCALIZED_ENUM_VALUE_B =
      LocalizedEnumValueFixtures.CustomFieldLocalizedEnumValueFixtures.ENUM_VALUE_B;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TypeTextInputHint.SINGLE_LINE);
    newSame = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TypeTextInputHint.SINGLE_LINE);
    newDifferent = stringFieldDefinition(FIELD_NAME_1, LABEL_2, true, TypeTextInputHint.MULTI_LINE);
  }

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<TypeUpdateAction> result = buildChangeLabelUpdateAction(old, newDifferent);

    assertThat(result)
        .contains(
            TypeChangeLabelActionBuilder.of()
                .fieldName(old.getName())
                .label(newDifferent.getLabel())
                .build());
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<TypeUpdateAction> result = buildChangeLabelUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewDifferentValues_ShouldReturnActions() {
    final List<TypeUpdateAction> result = buildActions(old, newDifferent);

    assertThat(result)
        .containsExactlyInAnyOrder(
            TypeChangeLabelActionBuilder.of()
                .fieldName(old.getName())
                .label(newDifferent.getLabel())
                .build());
  }

  @Test
  void buildActions_WithSameValues_ShouldReturnEmpty() {
    final List<TypeUpdateAction> result = buildActions(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldEnumTypeBuilder.of().plusValues(ENUM_VALUE_A).build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldEnumTypeBuilder.of().plusValues(ENUM_VALUE_A, ENUM_VALUE_B).build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_B).build());
  }

  @Test
  void buildActions_WithoutOldPlainEnum_ShouldNotReturnAnyValueAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldEnumTypeBuilder.of().plusValues(ENUM_VALUE_A).build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldEnumTypeBuilder.of().values(emptyList()).build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewLocalizedEnum_ShouldReturnAddLocalizedEnumValueAction() {

    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(CustomFieldLocalizedEnumTypeBuilder.of().values(LOCALIZED_ENUM_VALUE_A).build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldLocalizedEnumTypeBuilder.of()
                    .values(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B)
                    .build())
            .name(FIELD_NAME_1)
            .label(ofEnglish(LABEL_1))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(LOCALIZED_ENUM_VALUE_B)
                .build());
  }

  @Test
  void buildActions_WithStringFieldTypesWithLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(fieldTypeBuilder -> fieldTypeBuilder.stringBuilder())
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(fieldTypeBuilder -> fieldTypeBuilder.stringBuilder())
            .name("fieldName1")
            .label(ofEnglish("label2"))
            .required(false)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName("fieldName1")
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void
      buildActions_WithSetOfStringFieldTypesWithDefinitionLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(fieldTypeBuilder1 -> fieldTypeBuilder1.stringBuilder()))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(fieldTypeBuilder1 -> fieldTypeBuilder1.stringBuilder()))
            .name("fieldName1")
            .label(ofEnglish("label2"))
            .required(false)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName("fieldName1")
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void
      buildActions_WithSetOfSetOfStringFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        fieldTypeBuilder2 -> fieldTypeBuilder2.stringBuilder())))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        fieldTypeBuilder2 -> fieldTypeBuilder2.stringBuilder())))
            .name("fieldName1")
            .label(ofEnglish("label2"))
            .required(false)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName("fieldName1")
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void buildActions_WithSameSetOfEnumsFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1.enumBuilder().values(emptyList())))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1.enumBuilder().values(emptyList())))
            .name("fieldName1")
            .label(ofEnglish("label2"))
            .required(false)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName("fieldName1")
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void buildActions_WithChangedSetOfEnumFieldTypes_ShouldBuildEnumActions() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1.enumBuilder().values(ENUM_VALUE_A, ENUM_VALUE_B)))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        ENUM_VALUE_B,
                                        ENUM_VALUE_A,
                                        CustomFieldEnumValueBuilder.of()
                                            .key("c")
                                            .label("c")
                                            .build())))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of()
                .fieldName("fieldName1")
                .value(CustomFieldEnumValueBuilder.of().key("c").label("c").build())
                .build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName("fieldName1")
                .keys("b", "a", "c")
                .build());
  }

  @Test
  void buildActions_WithSameSetOfLEnumFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    // preparation
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        fieldTypeBuilder2 ->
                                            fieldTypeBuilder2
                                                .localizedEnumBuilder()
                                                .values(emptyList()))))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        fieldTypeBuilder2 ->
                                            fieldTypeBuilder2
                                                .localizedEnumBuilder()
                                                .values(emptyList()))))
            .name("fieldName1")
            .label(ofEnglish("label2"))
            .required(false)
            .build();

    // test
    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    // assertion
    assertThat(result)
        .containsExactly(
            TypeChangeLabelActionBuilder.of()
                .fieldName("fieldName1")
                .label(newFieldDefinition.getLabel())
                .build());
  }

  @Test
  void buildActions_WithChangedSetOfLocalizedEnumFieldTypes_ShouldBuildEnumActions() {
    // preparation
    final FieldDefinition oldFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .localizedEnumBuilder()
                                    .values(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B)))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    final FieldDefinition newFieldDefinition =
        FieldDefinitionBuilder.of()
            .type(
                fieldTypeBuilder ->
                    fieldTypeBuilder
                        .setBuilder()
                        .elementType(
                            fieldTypeBuilder1 ->
                                fieldTypeBuilder1
                                    .localizedEnumBuilder()
                                    .values(
                                        LOCALIZED_ENUM_VALUE_B,
                                        LOCALIZED_ENUM_VALUE_A,
                                        CustomFieldLocalizedEnumValueBuilder.of()
                                            .key("c")
                                            .label(ofEnglish("c"))
                                            .build())))
            .name("fieldName1")
            .label(ofEnglish("label1"))
            .required(false)
            .build();

    // test
    final List<TypeUpdateAction> result = buildActions(oldFieldDefinition, newFieldDefinition);

    // assertion
    assertThat(result)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName("fieldName1")
                .value(
                    CustomFieldLocalizedEnumValueBuilder.of()
                        .key("c")
                        .label(ofEnglish("c"))
                        .build())
                .build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName("fieldName1")
                .keys("b", "a", "c")
                .build());
  }
}
