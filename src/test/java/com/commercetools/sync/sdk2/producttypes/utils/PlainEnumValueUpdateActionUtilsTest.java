package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.utils.PlainEnumValueUpdateActionUtils.buildChangeLabelAction;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static AttributePlainEnumValue old;
  private static AttributePlainEnumValue newSame;
  private static AttributePlainEnumValue newDifferent;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newSame = AttributePlainEnumValueBuilder.of().key("key1").label("label1").build();
    newDifferent = AttributePlainEnumValueBuilder.of().key("key1").label("label2").build();
  }

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<ProductTypeUpdateAction> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName("attribute_definition_name_1")
                .newValue(newDifferent)
                .build());
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<ProductTypeUpdateAction> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }
}
