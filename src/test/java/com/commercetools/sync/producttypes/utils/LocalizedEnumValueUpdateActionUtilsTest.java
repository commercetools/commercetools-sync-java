package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildChangeLabelAction;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocalizedEnumValueUpdateActionUtilsTest {
  private static LocalizedEnumValue old =
      LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label1"));
  private static LocalizedEnumValue newSame =
      LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label1"));
  private static LocalizedEnumValue newDifferent =
      LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label2"));

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<ProductType>> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newDifferent);

    assertThat(result)
        .contains(ChangeLocalizedEnumValueLabel.of("attribute_definition_name_1", newDifferent));
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<ProductType>> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }
}
