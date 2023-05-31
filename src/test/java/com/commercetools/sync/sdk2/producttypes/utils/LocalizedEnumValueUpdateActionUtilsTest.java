package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.commons.utils.LocalizedEnumValueFixtures.*;
import static com.commercetools.sync.sdk2.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions;
import static com.commercetools.sync.sdk2.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalizedEnumValueUpdateActionUtilsTest {

  private static final String ATTRIBUTE_DEFINITION_NAME = "attribute_definition_name_1";
  private static AttributeLocalizedEnumValue old =
      AttributeLocalizedEnumValueBuilder.of()
          .key("key1")
          .label(LocalizedString.ofEnglish("label1"))
          .build();
  private static AttributeLocalizedEnumValue newSame =
      AttributeLocalizedEnumValueBuilder.of()
          .key("key1")
          .label(LocalizedString.ofEnglish("label1"))
          .build();
  private static AttributeLocalizedEnumValue newDifferent =
      AttributeLocalizedEnumValueBuilder.of()
          .key("key1")
          .label(LocalizedString.ofEnglish("label2"))
          .build();

  @Test
  void buildLocalizedEnumValueUpdateActions_WithDifferentValues_ShouldReturnAction() {
    final List<ProductTypeUpdateAction> result =
        buildLocalizedEnumValueUpdateActions(ATTRIBUTE_DEFINITION_NAME, old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(newDifferent)
                .build());
  }

  @Test
  void buildLocalizedEnumValueUpdateActions_WithSameValues_ShouldReturnEmptyOptional() {
    final List<ProductTypeUpdateAction> result =
        buildLocalizedEnumValueUpdateActions(ATTRIBUTE_DEFINITION_NAME, old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithNullNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, null);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithEmptyNewEnumValuesAndExistingEnumValues_ShouldBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, Collections.emptyList());

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithEmptyPlainEnumValuesAndNoOldEnumValues_ShouldNotBuildActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, Collections.emptyList(), Collections.emptyList());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithNewPlainEnumValuesAndNoOldPlainEnumValues_ShouldBuild3AddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, Collections.emptyList(), ENUM_VALUES_ABC);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_A)
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_B)
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithIdenticalPlainEnum_ShouldNotBuildUpdateActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_ABC);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithDuplicatePlainEnumValues_ShouldTriggerDuplicateKeyError() {
    assertThatThrownBy(
            () ->
                buildLocalizedEnumValuesUpdateActions(
                    "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ABB))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessage(
            format(
                "Enum Values have duplicated keys. Definition name: "
                    + "'%s', Duplicated enum value: 'b'. "
                    + "Enum Values are expected to be unique inside their definition.",
                ATTRIBUTE_DEFINITION_NAME));
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithOneMissingPlainEnumValue_ShouldBuildRemoveEnumValueAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_AB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("c"))
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithOnePlainEnumValue_ShouldBuildAddEnumValueAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_ABCD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName("attribute_definition_name_1")
                .value(ENUM_VALUE_D)
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithOneEnumValueSwitch_ShouldBuildRemoveAndAddEnumValueActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_ABD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("c"))
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_D)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDifferent_ShouldBuildChangeEnumValueOrderAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_CAB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B))
                .build());
  }

  @Test
  void buildPlainEnumUpdateActions_WithMixedCase_ShouldBuildChangeEnumValueOrderAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_BAC, ENUM_VALUES_AB_WITH_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(ENUM_VALUE_C.getKey())
                .build(),
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_CB);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("a"))
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_C, ENUM_VALUE_B))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, ENUM_VALUES_ABC, ENUM_VALUES_ACBD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_D)
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedEnumValueInBetween_ShouldBuildChangeOrderAndAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_ADBC);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_D)
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C))
                .build());
  }

  @Test
  void
      buildLocalizedEnumUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveEnumValueActions() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValuesUpdateActions(
            "attribute_definition_name_1", ENUM_VALUES_ABC, ENUM_VALUES_CBD);

    assertThat(updateActions)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(singletonList("a"))
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(ENUM_VALUE_D)
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .values(asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D))
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithDifferentLabels_ShouldReturnChangeLabelAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValueUpdateActions(
            "attribute_definition_name_1", ENUM_VALUE_A, ENUM_VALUE_A_DIFFERENT_LABEL);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void buildLocalizedEnumUpdateActions_WithSameLabels_ShouldNotReturnChangeLabelAction() {
    final List<ProductTypeUpdateAction> updateActions =
        buildLocalizedEnumValueUpdateActions(
            "attribute_definition_name_1", ENUM_VALUE_A, ENUM_VALUE_A);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(ENUM_VALUE_A)
                .build());
  }
}
