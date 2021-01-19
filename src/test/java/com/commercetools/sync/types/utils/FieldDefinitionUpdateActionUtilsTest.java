package com.commercetools.sync.types.utils;

import static com.commercetools.sync.types.utils.FieldDefinitionFixtures.stringFieldDefinition;
import static com.commercetools.sync.types.utils.FieldDefinitionUpdateActionUtils.buildActions;
import static com.commercetools.sync.types.utils.FieldDefinitionUpdateActionUtils.buildChangeLabelUpdateAction;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.SetFieldType;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddEnumValue;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionLabel;
import io.sphere.sdk.types.commands.updateactions.ChangeLocalizedEnumValueOrder;
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

  private static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
  private static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");

  private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_A =
      LocalizedEnumValue.of("a", ofEnglish("label_a"));
  private static final LocalizedEnumValue LOCALIZED_ENUM_VALUE_B =
      LocalizedEnumValue.of("b", ofEnglish("label_b"));

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TextInputHint.SINGLE_LINE);
    newSame = stringFieldDefinition(FIELD_NAME_1, LABEL_1, false, TextInputHint.SINGLE_LINE);
    newDifferent = stringFieldDefinition(FIELD_NAME_1, LABEL_2, true, TextInputHint.MULTI_LINE);
  }

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<Type>> result = buildChangeLabelUpdateAction(old, newDifferent);

    assertThat(result)
        .contains(ChangeFieldDefinitionLabel.of(old.getName(), newDifferent.getLabel()));
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<Type>> result = buildChangeLabelUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewDifferentValues_ShouldReturnActions() {
    final List<UpdateAction<Type>> result = buildActions(old, newDifferent);

    assertThat(result)
        .containsExactlyInAnyOrder(
            ChangeFieldDefinitionLabel.of(old.getName(), newDifferent.getLabel()));
  }

  @Test
  void buildActions_WithSameValues_ShouldReturnEmpty() {
    final List<UpdateAction<Type>> result = buildActions(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            EnumFieldType.of(singletonList(ENUM_VALUE_A)),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            EnumFieldType.of(asList(ENUM_VALUE_A, ENUM_VALUE_B)),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result).containsExactly(AddEnumValue.of(FIELD_NAME_1, ENUM_VALUE_B));
  }

  @Test
  void buildActions_WithoutOldPlainEnum_ShouldNotReturnAnyValueAction() {

    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            EnumFieldType.of(singletonList(ENUM_VALUE_A)),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            EnumFieldType.of(emptyList()),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithNewLocalizedEnum_ShouldReturnAddLocalizedEnumValueAction() {

    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            LocalizedEnumFieldType.of(singletonList(LOCALIZED_ENUM_VALUE_A)),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            LocalizedEnumFieldType.of(asList(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B)),
            FIELD_NAME_1,
            LocalizedString.ofEnglish(LABEL_1),
            false,
            TextInputHint.SINGLE_LINE);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(AddLocalizedEnumValue.of(FIELD_NAME_1, LOCALIZED_ENUM_VALUE_B));
  }

  @Test
  void buildActions_WithStringFieldTypesWithLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(StringFieldType.of(), "fieldName1", ofEnglish("label1"), false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(StringFieldType.of(), "fieldName1", ofEnglish("label2"), false);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            ChangeFieldDefinitionLabel.of("fieldName1", newFieldDefinition.getLabel()));
  }

  @Test
  void
      buildActions_WithSetOfStringFieldTypesWithDefinitionLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()), "fieldName1", ofEnglish("label1"), false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()), "fieldName1", ofEnglish("label2"), false);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            ChangeFieldDefinitionLabel.of("fieldName1", newFieldDefinition.getLabel()));
  }

  @Test
  void
      buildActions_WithSetOfSetOfStringFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(SetFieldType.of(StringFieldType.of())),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(SetFieldType.of(StringFieldType.of())),
            "fieldName1",
            ofEnglish("label2"),
            false);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            ChangeFieldDefinitionLabel.of("fieldName1", newFieldDefinition.getLabel()));
  }

  @Test
  void buildActions_WithSameSetOfEnumsFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(EnumFieldType.of(emptyList())),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(EnumFieldType.of(emptyList())),
            "fieldName1",
            ofEnglish("label2"),
            false);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            ChangeFieldDefinitionLabel.of("fieldName1", newFieldDefinition.getLabel()));
  }

  @Test
  void buildActions_WithChangedSetOfEnumFieldTypes_ShouldBuildEnumActions() {
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(EnumFieldType.of(asList(ENUM_VALUE_A, ENUM_VALUE_B))),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(
                EnumFieldType.of(asList(ENUM_VALUE_B, ENUM_VALUE_A, EnumValue.of("c", "c")))),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    assertThat(result)
        .containsExactly(
            AddEnumValue.of("fieldName1", EnumValue.of("c", "c")),
            ChangeEnumValueOrder.of("fieldName1", asList("b", "a", "c")));
  }

  @Test
  void buildActions_WithSameSetOfLEnumFieldTypesWithDefLabelChanges_ShouldBuildChangeLabelAction() {
    // preparation
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(SetFieldType.of(LocalizedEnumFieldType.of(emptyList()))),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(SetFieldType.of(LocalizedEnumFieldType.of(emptyList()))),
            "fieldName1",
            ofEnglish("label2"),
            false);

    // test
    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    // assertion
    assertThat(result)
        .containsExactly(
            ChangeFieldDefinitionLabel.of("fieldName1", newFieldDefinition.getLabel()));
  }

  @Test
  void buildActions_WithChangedSetOfLocalizedEnumFieldTypes_ShouldBuildEnumActions() {
    // preparation
    final FieldDefinition oldFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(
                LocalizedEnumFieldType.of(asList(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B))),
            "fieldName1",
            ofEnglish("label1"),
            false);

    final FieldDefinition newFieldDefinition =
        FieldDefinition.of(
            SetFieldType.of(
                LocalizedEnumFieldType.of(
                    asList(
                        LOCALIZED_ENUM_VALUE_B,
                        LOCALIZED_ENUM_VALUE_A,
                        LocalizedEnumValue.of("c", ofEnglish("c"))))),
            "fieldName1",
            ofEnglish("label1"),
            false);

    // test
    final List<UpdateAction<Type>> result = buildActions(oldFieldDefinition, newFieldDefinition);

    // assertion
    assertThat(result)
        .containsExactly(
            AddLocalizedEnumValue.of("fieldName1", LocalizedEnumValue.of("c", ofEnglish("c"))),
            ChangeLocalizedEnumValueOrder.of("fieldName1", asList("b", "a", "c")));
  }
}
