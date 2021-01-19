package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.types.Type;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProductSetAssetCustomTypeAssert
    extends AbstractSetCustomTypeAssert<ProductSetAssetCustomTypeAssert, SetAssetCustomType> {

  ProductSetAssetCustomTypeAssert(@Nullable final SetAssetCustomType actual) {
    super(actual, ProductSetAssetCustomTypeAssert.class);
  }

  /**
   * Verifies that the actual {@link
   * io.sphere.sdk.products.commands.updateactions.SetAssetCustomType} value has identical fields as
   * the ones supplied.
   *
   * @param actionName the update action name.
   * @param assetId the asset Id the action is performed on.
   * @param variantSku the variant sku that has the asset.
   * @param variantId the variant id that has the asset.
   * @param staged the staged flag of the action.
   * @param customFields the new custom type fields.
   * @param type the new custom type.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public ProductSetAssetCustomTypeAssert hasValues(
      @Nonnull final String actionName,
      @Nullable final String assetId,
      @Nullable final String variantSku,
      @Nullable final Integer variantId,
      @Nullable final Boolean staged,
      @Nullable final Map<String, JsonNode> customFields,
      @Nullable final ResourceIdentifier<Type> type) {

    super.hasValues(actionName, customFields, type);

    assertThat(actual.getAssetId()).isEqualTo(assetId);
    assertThat(actual.getSku()).isEqualTo(variantSku);
    assertThat(actual.getVariantId()).isEqualTo(variantId);
    assertThat(actual.getStaged()).isEqualTo(staged);
    return myself;
  }
}
