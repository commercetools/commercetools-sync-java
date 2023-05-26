package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValueUpdateActions;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.List;
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
  void buildLocalizedEnumValueUpdateActions_WithDifferentValues_ShouldReturnAction() {
    final List<ProductTypeUpdateAction> result =
        buildLocalizedEnumValueUpdateActions("attribute_definition_name_1", old, newDifferent);

    assertThat(result)
        .contains(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName("attribute_definition_name_1")
                .newValue(newDifferent)
                .build());
  }

  @Test
  void buildLocalizedEnumValueUpdateActions_WithSameValues_ShouldReturnEmptyOptional() {
    final List<ProductTypeUpdateAction> result =
        buildLocalizedEnumValueUpdateActions("attribute_definition_name_1", old, newSame);

    assertThat(result).isEmpty();
  }
}
