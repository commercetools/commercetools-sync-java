package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.types.Type;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SetPriceCustomTypeAssert
    extends AbstractSetCustomTypeAssert<SetPriceCustomTypeAssert, SetProductPriceCustomType> {

  SetPriceCustomTypeAssert(@Nullable final SetProductPriceCustomType actual) {
    super(actual, SetPriceCustomTypeAssert.class);
  }

  /**
   * Verifies that the actual {@link
   * io.sphere.sdk.products.commands.updateactions.SetAssetCustomType} value has identical fields as
   * the ones supplied.
   *
   * @param actionName the update action name.
   * @param priceId the price id that has the custom fields.
   * @param staged the staged flag of the action.
   * @param customFields the new custom type fields.
   * @param type the new custom type.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public SetPriceCustomTypeAssert hasValues(
      @Nonnull final String actionName,
      @Nullable final String priceId,
      @Nullable final Boolean staged,
      @Nullable final Map<String, JsonNode> customFields,
      @Nullable final ResourceIdentifier<Type> type) {

    super.hasValues(actionName, customFields, type);
    assertThat(actual.getStaged()).isEqualTo(staged);
    assertThat(actual.getPriceId()).isEqualTo(priceId);
    return myself;
  }
}
