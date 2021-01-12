package com.commercetools.sync.types.utils;

import static com.commercetools.sync.commons.utils.LocalizedEnumValueFixtures.*;
import static com.commercetools.sync.types.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddLocalizedEnumValue;
import io.sphere.sdk.types.commands.updateactions.ChangeLocalizedEnumValueOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildLocalizedEnumUpdateActionsTest {
  private static final String FIELD_NAME_1 = "field1";

  @Test
  void
      buildLocalizedEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, emptyList(), emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_A),
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_B),
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_C));
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions).containsExactly(AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D));
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    // remove enum value actions not exists for type resources
    assertThat(updateActions).containsExactly(AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D));
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            ChangeLocalizedEnumValueOrder.of(
                FIELD_NAME_1,
                asList(ENUM_VALUE_C.getKey(), ENUM_VALUE_A.getKey(), ENUM_VALUE_B.getKey())));
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CB);

    // remove enum value actions not exists for type resources
    assertThat(updateActions)
        .containsExactly(
            ChangeLocalizedEnumValueOrder.of(
                FIELD_NAME_1, asList(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey())));
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(
                FIELD_NAME_1,
                asList(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_C.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_D.getKey())));
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(
                FIELD_NAME_1,
                asList(
                    ENUM_VALUE_A.getKey(),
                    ENUM_VALUE_D.getKey(),
                    ENUM_VALUE_B.getKey(),
                    ENUM_VALUE_C.getKey())));
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndOrderEnumValueActions() {
    final List<UpdateAction<Type>> updateActions =
        buildLocalizedEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            AddLocalizedEnumValue.of(FIELD_NAME_1, ENUM_VALUE_D),
            ChangeLocalizedEnumValueOrder.of(
                FIELD_NAME_1,
                asList(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey(), ENUM_VALUE_D.getKey())));
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDuplicateEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                buildLocalizedEnumValuesUpdateActions(
                    "field_definition_name", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            "Enum Values have duplicated keys. Definition name: "
                + "'field_definition_name', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
  }
}
