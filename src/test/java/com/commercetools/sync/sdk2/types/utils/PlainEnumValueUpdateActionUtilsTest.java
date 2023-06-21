package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.PlainEnumValueFixtures.CustomFieldEnumValueFixtures.*;
import static com.commercetools.sync.sdk2.types.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.type.TypeAddEnumValueActionBuilder;
import com.commercetools.api.models.type.TypeChangeEnumValueOrderActionBuilder;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static final String FIELD_NAME_1 = "field1";

  @Test
  void
      buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, emptyList(), emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_A).build(),
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_B).build(),
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_C).build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildAddEnumValueActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithDifferentOrder_ShouldBuildChangeEnumValueOrderAction() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_A.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAction() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CB);

    assertThat(updateActions)
        .containsExactly(
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey())
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

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
  void
      buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

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
  void
      buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAddAndChangeOrderActions() {
    final List<TypeUpdateAction> updateActions =
        buildEnumValuesUpdateActions(FIELD_NAME_1, ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            TypeAddEnumValueActionBuilder.of().fieldName(FIELD_NAME_1).value(ENUM_VALUE_D).build(),
            TypeChangeEnumValueOrderActionBuilder.of()
                .fieldName(FIELD_NAME_1)
                .keys(ENUM_VALUE_C.getKey(), ENUM_VALUE_B.getKey(), ENUM_VALUE_D.getKey())
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDuplicateEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                buildEnumValuesUpdateActions(
                    "field_definition_name", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            "Enum Values have duplicated keys. Definition name: "
                + "'field_definition_name', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
  }
}
