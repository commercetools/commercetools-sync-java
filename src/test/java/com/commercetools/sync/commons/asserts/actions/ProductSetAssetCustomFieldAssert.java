package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProductSetAssetCustomFieldAssert
    extends AbstractSetCustomFieldAssert<ProductSetAssetCustomFieldAssert, SetAssetCustomField> {

  ProductSetAssetCustomFieldAssert(@Nullable final SetAssetCustomField actual) {
    super(actual, ProductSetAssetCustomFieldAssert.class);
  }

  /**
   * Verifies that the actual {@link SetAssetCustomField} value has identical fields as the ones
   * supplied.
   *
   * @param actionName the update action name.
   * @param assetId the asset Id the action is performed on.
   * @param variantSku the variant sku that has the asset.
   * @param variantId the variant id that has the asset.
   * @param staged the staged flag of the action.
   * @param customFieldName the custom field name to update.
   * @param customFieldValue the new custom field name to update.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public ProductSetAssetCustomFieldAssert hasValues(
      @Nonnull final String actionName,
      @Nullable final String assetId,
      @Nullable final String variantSku,
      @Nullable final Integer variantId,
      @Nullable final Boolean staged,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {

    super.hasValues(actionName, customFieldName, customFieldValue);

    assertThat(actual.getAssetId()).isEqualTo(assetId);
    assertThat(actual.getSku()).isEqualTo(variantSku);
    assertThat(actual.getVariantId()).isEqualTo(variantId);
    assertThat(actual.getStaged()).isEqualTo(staged);
    return myself;
  }
}
