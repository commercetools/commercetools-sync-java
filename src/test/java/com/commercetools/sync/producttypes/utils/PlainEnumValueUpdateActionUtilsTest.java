package com.commercetools.sync.producttypes.utils;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.utils.PlainEnumValueFixtures;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static AttributePlainEnumValue old;
  private static AttributePlainEnumValue newSame;
  private static AttributePlainEnumValue newDifferent;
  private static final String ATTRIBUTE_DEFINITION_NAME = "attribute_definition_name_1";

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newSame = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newDifferent = AttributePlainEnumValueBuilder.of().key("key1").label("label2").build();
  }

  @Test
  void buildEnumValueUpdateActions_WithDifferentValues_ShouldReturnAction() {
    final List<ProductTypeUpdateAction> result =
        PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(newDifferent)
                .build());
  }

  @Test
  void buildEnumValueUpdateActions_WithSameValues_ShouldReturnEmptyOptional() {
    final List<ProductTypeUpdateAction> result =
        PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions(
            "attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            null);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            Collections.emptyList());

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, Collections.emptyList(), Collections.emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            Collections.emptyList(),
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A)
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B)
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildPlainEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
                    ATTRIBUTE_DEFINITION_NAME,
                    PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
                    PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            format(
                "Enum Values have duplicated keys. Definition name: "
                    + "'%s', Duplicated enum value: 'b'. "
                    + "Enum Values are expected to be unique inside their definition.",
                ATTRIBUTE_DEFINITION_NAME));
  }

  @Test
  void buildPlainEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_AB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("c"))
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(Collections.singletonList("c"))
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B))
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_BAC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures
                .ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C.getKey())
                .build(),
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(
                    PlainEnumValueFixtures.AttributePlainEnumValueFixtures
                        .ENUM_VALUE_A_DIFFERENT_LABEL)
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures
                            .ENUM_VALUE_A_DIFFERENT_LABEL,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_CB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("a"))
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C))
                .build());
  }

  @Test
  void
      buildPlainEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_ABC,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(Collections.singletonList("a"))
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(
                    asList(
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_C,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_B,
                        PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D))
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(
                    PlainEnumValueFixtures.AttributePlainEnumValueFixtures
                        .ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
    final List<ProductTypeUpdateAction> updateActions =
        PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A,
            PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_A);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(PlainEnumValueFixtures.AttributePlainEnumValueFixtures.ENUM_VALUE_D)
                .build());
  }
}
