package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.PlainEnumValueFixtures.AttributePlainEnumValueFixtures.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class EnumValuesUpdateActionUtilsTest {

  private static final String attributeDefinitionName = "attribute_definition_name_1";

  @Test
  void buildActions_WithoutCallbacks_ShouldNotBuildActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_ABCD,
            null,
            null,
            null,
            null,
            null);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_WithoutRemoveCallback_ShouldNotBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName, ENUM_VALUES_ABC, null, null, null, null, null, null);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void buildActions_WithNullNewEnumValues_ShouldJustBuildRemoveActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            (defintionName, keysToRemove) ->
                ProductTypeRemoveEnumValuesActionBuilder.of()
                    .attributeName(defintionName)
                    .keys(keysToRemove)
                    .build(),
            null,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build(),
            null,
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .keys(asList("a", "b", "c"))
                .build());

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .value(ENUM_VALUE_A)
                .build());
  }

  @Test
  void buildActions_WithEmptyNewEnumValues_ShouldJustBuildRemoveActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            Collections.emptyList(),
            (defintionName, keysToRemove) ->
                ProductTypeRemoveEnumValuesActionBuilder.of()
                    .attributeName(defintionName)
                    .keys(keysToRemove)
                    .build(),
            null,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build(),
            null,
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .keys(asList("a", "b", "c"))
                .build());

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .value(ENUM_VALUE_A)
                .build());
  }

  @Test
  void buildActions_WithRemoveCallback_ShouldBuildRemoveAction() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            (defintionName, keysToRemove) ->
                ProductTypeRemoveEnumValuesActionBuilder.of()
                    .attributeName(defintionName)
                    .keys(keysToRemove)
                    .build(),
            null,
            null,
            null,
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .keys(asList("a", "b", "c"))
                .build());
  }

  @Test
  void buildActions_WithoutMatchingEnumCallback_ShouldNotBuildMatchingActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName, ENUM_VALUES_AB, null, null, null, null, null, null);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(ENUM_VALUE_A)
                .build(),
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(ENUM_VALUE_B)
                .build());
  }

  @Test
  void buildActions_WithMatchingEnumCallback_ShouldBuildMatchingActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB_WITH_DIFFERENT_LABEL,
            null,
            getMatchingValueFunction(),
            null,
            null,
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void buildActions_WithoutAddEnumCallback_ShouldNotBuildAddEnumActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName, ENUM_VALUES_AB, ENUM_VALUES_ABC, null, null, null, null, null);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .value(ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildActions_WithAddEnumCallback_ShouldBuildAddEnumActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_ABC,
            null,
            null,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build(),
            null,
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .value(ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildActions_WithoutChangeOrderEnumCallback_ShouldNotBuildChangeOrderEnumActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB,
            null,
            null,
            null,
            null,
            null);

    assertThat(updateActions)
        .doesNotContain(
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .values(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B))
                .build());
  }

  @Test
  void buildActions_WithChangeOrderEnumCallback_ShouldBuildChangeOrderEnumActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildActions(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB,
            null,
            null,
            null,
            (definitionName, newEnumValues) ->
                ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                    .attributeName(definitionName)
                    .values(newEnumValues)
                    .build(),
            null);

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .values(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B))
                .build());
  }

  @Test
  void buildRemoveEnumValuesUpdateAction_WithNullNewEnumValues_ShouldBuildRemoveActions() {
    final Optional<ProductTypeUpdateAction> updateAction =
        EnumValuesUpdateActionUtils.buildRemoveEnumValuesUpdateAction(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            null,
            (defintionName, keysToRemove) ->
                ProductTypeRemoveEnumValuesActionBuilder.of()
                    .attributeName(defintionName)
                    .keys(keysToRemove)
                    .build());

    assertThat(updateAction).isNotEmpty();
    assertThat(updateAction)
        .isEqualTo(
            Optional.of(
                ProductTypeRemoveEnumValuesActionBuilder.of()
                    .attributeName(attributeDefinitionName)
                    .keys(asList("a", "b", "c"))
                    .build()));
  }

  @Test
  void
      buildMatchingEnumValuesUpdateActions_WithDifferentEnumValues_ShouldBuildChangeLabelActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildMatchingEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB_WITH_DIFFERENT_LABEL,
            getMatchingValueFunction());

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .newValue(ENUM_VALUE_A_DIFFERENT_LABEL)
                .build());
  }

  @Test
  void
      buildMatchingEnumValuesUpdateActions_WithSameNewEnumValues_ShouldNotBuildChangeLabelActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildMatchingEnumValuesUpdateActions(
            attributeDefinitionName, ENUM_VALUES_AB, ENUM_VALUES_AB, getMatchingValueFunction());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddEnumValuesUpdateActions_WithNewEnumValues_ShouldBuildAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildAddEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_ABC,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build());

    assertThat(updateActions)
        .containsAnyOf(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinitionName)
                .value(ENUM_VALUE_C)
                .build());
  }

  @Test
  void buildAddEnumValuesUpdateActions_WithSameEnumValues_ShouldNotBuildAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildAddEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build());

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildChangeEnumValuesOrderUpdateAction_WithNewEnumValues_ShouldBuildAddActions() {
    final Optional<ProductTypeUpdateAction> updateAction =
        EnumValuesUpdateActionUtils.buildChangeEnumValuesOrderUpdateAction(
            attributeDefinitionName,
            ENUM_VALUES_ABC,
            ENUM_VALUES_CAB,
            (definitionName, newEnumValues) ->
                ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                    .attributeName(definitionName)
                    .values(newEnumValues)
                    .build());

    assertThat(updateAction).isNotEmpty();
    assertThat(updateAction)
        .isEqualTo(
            Optional.of(
                ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                    .attributeName(attributeDefinitionName)
                    .values(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B))
                    .build()));
  }

  @Test
  void buildChangeEnumValuesOrderUpdateAction_WithSameEnumValues_ShouldNotBuildAddActions() {
    final List<ProductTypeUpdateAction> updateActions =
        EnumValuesUpdateActionUtils.buildAddEnumValuesUpdateActions(
            attributeDefinitionName,
            ENUM_VALUES_AB,
            ENUM_VALUES_AB,
            (definitionName, newEnumValue) ->
                ProductTypeAddPlainEnumValueActionBuilder.of()
                    .attributeName(definitionName)
                    .value(newEnumValue)
                    .build());

    assertThat(updateActions).isEmpty();
  }

  @Nonnull
  private TriFunction<
          String, AttributePlainEnumValue, AttributePlainEnumValue, List<ProductTypeUpdateAction>>
      getMatchingValueFunction() {
    return (definitionName, oldEnumValue, newEnumValue) -> {
      if (oldEnumValue == null || newEnumValue == null) {
        return Collections.emptyList();
      }

      return PlainEnumValueUpdateActionUtils.buildEnumValueUpdateActions(
          attributeDefinitionName, oldEnumValue, newEnumValue);
    };
  }
}
