package com.commercetools.sync.producttypes.utils.producttypeactionutils;

import static com.commercetools.sync.commons.utils.PlainEnumValueFixtures.*;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions;
import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddEnumValue;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeEnumValueOrder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveEnumValues;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildPlainEnumUpdateActionsTest {

  @Test
  void
      buildPlainEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions("attribute_definition_name_1", ENUM_VALUES_ABC, null);

    assertThat(updateActions)
        .containsExactly(RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c")));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, Collections.emptyList());

    assertThat(updateActions)
        .containsExactly(RemoveEnumValues.of("attribute_definition_name_1", asList("a", "b", "c")));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", Collections.emptyList(), Collections.emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", Collections.emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_A),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_B),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_C));
  }

  @Test
  void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildPlainEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                buildEnumValuesUpdateActions(
                    "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            "Enum Values have duplicated keys. Definition name: "
                + "'attribute_definition_name_1', Duplicated enum value: 'b'. "
                + "Enum Values are expected to be unique inside their definition.");
  }

  @Test
  void buildPlainEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_AB);

    assertThat(updateActions)
        .containsExactly(RemoveEnumValues.of("attribute_definition_name_1", singletonList("c")));
  }

  @Test
  void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    assertThat(updateActions)
        .containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", Collections.singletonList("c")),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D));
  }

  @Test
  void buildPlainEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            ChangeEnumValueOrder.of(
                "attribute_definition_name_1", asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B)));
  }

  @Test
  void buildPlainEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
    final String attributeDefinitionName = "attribute_definition_name_1";
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            attributeDefinitionName, ENUM_VALUES_BAC, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsExactly(
            RemoveEnumValues.of(attributeDefinitionName, ENUM_VALUE_C.getKey()),
            ChangePlainEnumValueLabel.of(attributeDefinitionName, ENUM_VALUE_A_DIFFERENT_LABEL),
            ChangeEnumValueOrder.of(
                attributeDefinitionName, asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B)));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_CB);

    assertThat(updateActions)
        .containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", singletonList("a")),
            ChangeEnumValueOrder.of(
                "attribute_definition_name_1", asList(ENUM_VALUE_C, ENUM_VALUE_B)));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of(
                "attribute_definition_name_1",
                asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D)));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of(
                "attribute_definition_name_1",
                asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C)));
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            RemoveEnumValues.of("attribute_definition_name_1", Collections.singletonList("a")),
            AddEnumValue.of("attribute_definition_name_1", ENUM_VALUE_D),
            ChangeEnumValueOrder.of(
                "attribute_definition_name_1", asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D)));
  }

  @Test
  void buildPlainEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValueUpdateActions(
            "attribute_definition_name_1", ENUM_VALUE_A, ENUM_VALUE_A_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsAnyOf(
            ChangePlainEnumValueLabel.of(
                "attribute_definition_name_1", ENUM_VALUE_A_DIFFERENT_LABEL));
  }

  @Test
  void buildPlainEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
    final List<UpdateAction<ProductType>> updateActions =
        buildEnumValueUpdateActions("attribute_definition_name_1", ENUM_VALUE_A, ENUM_VALUE_A);

    assertThat(updateActions)
        .doesNotContain(ChangePlainEnumValueLabel.of("attribute_definition_name_1", ENUM_VALUE_A));
  }
}
