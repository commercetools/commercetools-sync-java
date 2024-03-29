package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.LocalizedEnumValueFixtures.CustomFieldLocalizedEnumValueFixtures.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.type.TypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.type.TypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalizedEnumValueUpdateActionUtilsTest {
  private static final String FIELD_NAME_1 = "field1";

  @Test
  void
      buildLocalizedEnumUpdateActions_WithEmptyLocalizedEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, emptyList(), emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithNewLocalizedEnumValuesAndNoOldLocalizedEnumValues_ShouldBuild3AddActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A)
                .build(),
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_B)
                .build(),
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithIdenticalLocalizedEnum_ShouldNotBuildUpdateActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithOneLocalizedEnumValue_ShouldBuildAddEnumValueAction() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_D)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    // remove enum value actions not exists for type resources
    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_D)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_A.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_BAC, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_A_DIFFERENT_LABEL.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CB);

    // remove enum value actions not exists for type resources
    assertThat(updateActions)
        .containsExactly(
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_D)
                .build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_C.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_D.getKey())
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_D)
                .build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_D.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_C.getKey())
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndOrderEnumValueActions() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddLocalizedEnumValueActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_D)
                .build(),
            TypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey(), ENUM_VALUE_D.getKey())
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDuplicateEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
                    "field_definition_name", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            "Enum Values have duplicated keys. Definition name: "
                + "'field_definition_name', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_AB, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsAnyOf(
            TypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
    final List<TypeUpdateAction> updateActions =
        LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions(
            FIELD_NAME_1, ENUM_VALUES_AB, ENUM_VALUES_AB);

    assertThat(updateActions)
        .doesNotContain(
            TypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .value(ENUM_VALUE_A)
                .build());
  }
}
