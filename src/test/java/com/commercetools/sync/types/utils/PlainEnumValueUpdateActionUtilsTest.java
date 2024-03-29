package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.PlainEnumValueFixtures.CustomFieldEnumValueFixtures.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.type.TypeAddEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeChangeEnumValueLabelActionBuilder;
import com.commercetools.api.models.type.TypeChangeEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static final String FIELD_NAME_1 = "field1";

  @Test
  void buildEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, emptyList(), emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_A).build(),
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_B).build(),
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_C).build());
  }

  @Test
  void buildEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build());
  }

  @Test
  void buildEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build());
  }

  @Test
  void buildEnumUpdateActions_WithDifferentOrder_ShouldBuildChangeEnumValueOrderAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_A.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CB);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_BAC, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_A_DIFFERENT_LABEL.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_C.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_D.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_D.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_C.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndChangeOrderActions() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey(), ENUM_VALUE_D.getKey())
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithDuplicateEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
                    "field_definition_name", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            "Enum Values have duplicated keys. Definition name: "
                + "'field_definition_name', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
  }

  @Test
  void buildEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_AB, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsAnyOf(
            TypeChangeEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void buildEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
    final List<TypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_AB, ENUM_VALUES_AB);

    assertThat(updateActions)
        .doesNotContain(
            TypeChangeEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A)
                .build());
  }
}
