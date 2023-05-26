package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.utils.PlainEnumValueUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static AttributePlainEnumValue old;
  private static AttributePlainEnumValue newSame;
  private static AttributePlainEnumValue newDifferent;
  private static final String ATTRIBUTE_DEFINITION_NAME = "attribute_definition_name_1";
  private static final String ATTRIBUTE_KEY_1 = "key_1";
  private static final String ATTRIBUTE_KEY_2 = "key_2";

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newSame = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newDifferent = AttributePlainEnumValueBuilder.of().key("key1").label("label2").build();
  }

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final List<ProductTypeUpdateAction> result =
        buildEnumValueUpdateActions(ATTRIBUTE_DEFINITION_NAME, old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .newValue(newDifferent)
                .build());
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final List<ProductTypeUpdateAction> result =
        buildEnumValueUpdateActions("attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildEnumValuesUpdateActions_withDifferentEnum_shouldRemoveOldEnumAndAddNewEnum() {
    final AttributePlainEnumValue enumValue1 =
        AttributePlainEnumValueBuilder.of().key(ATTRIBUTE_KEY_1).label("test").build();
    final AttributePlainEnumValue enumValue2 =
        AttributePlainEnumValueBuilder.of().key(ATTRIBUTE_KEY_2).label("test").build();
    final List<ProductTypeUpdateAction> result =
        buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME, List.of(enumValue1), List.of(enumValue2));

    assertThat(result).hasSize(2);
    assertThat(result)
        .contains(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .keys(ATTRIBUTE_KEY_1)
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(ATTRIBUTE_DEFINITION_NAME)
                .value(enumValue2)
                .build());
  }

  @Test
  void buildEnumValuesUpdateActions_withDifferentEnumOrder_shouldChangeEnumOrder() {
    final AttributePlainEnumValue enumValue1 =
        AttributePlainEnumValueBuilder.of().key(ATTRIBUTE_KEY_1).label("test").build();
    final AttributePlainEnumValue enumValue2 =
        AttributePlainEnumValueBuilder.of().key(ATTRIBUTE_KEY_2).label("test").build();
    final List<ProductTypeUpdateAction> result =
        buildEnumValuesUpdateActions(
            ATTRIBUTE_DEFINITION_NAME,
            List.of(enumValue1, enumValue2),
            List.of(enumValue2, enumValue1));

    assertThat(result).hasSize(1);
    assertThat(result)
        .isEqualTo(
            Collections.singletonList(
                ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                    .attributeName(ATTRIBUTE_DEFINITION_NAME)
                    .values(enumValue2, enumValue1)
                    .build()));
  }
}
