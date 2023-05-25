package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildChangeLabelAction;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocalizedEnumValueUpdateActionUtilsTest {
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
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<ProductTypeUpdateAction> result =
        buildChangeLabelAction("attribute_definition_name_1", old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
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
