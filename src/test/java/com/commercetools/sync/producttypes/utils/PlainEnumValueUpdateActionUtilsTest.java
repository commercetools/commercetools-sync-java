package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.producttypes.utils.PlainEnumValueUpdateActionUtils.buildChangeLabelAction;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangePlainEnumValueLabel;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlainEnumValueUpdateActionUtilsTest {
  private static EnumValue old;
  private static EnumValue newSame;
  private static EnumValue newDifferent;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    old = EnumValue.of("key1", "label1");
    newSame = EnumValue.of("key1", "label1");
    newDifferent = EnumValue.of("key1", "label2");
  }

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<ProductType>> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newDifferent);

    assertThat(result).containsInstanceOf(ChangePlainEnumValueLabel.class);
    assertThat(result)
        .contains(ChangePlainEnumValueLabel.of("attribute_definition_name_1", newDifferent));
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<ProductType>> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }
}
