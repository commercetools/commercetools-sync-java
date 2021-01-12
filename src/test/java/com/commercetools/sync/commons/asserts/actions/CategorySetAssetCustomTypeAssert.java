package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CategorySetAssetCustomTypeAssert
    extends AbstractSetCustomTypeAssert<CategorySetAssetCustomTypeAssert, SetAssetCustomType> {

  CategorySetAssetCustomTypeAssert(@Nullable final SetAssetCustomType actual) {
    super(actual, CategorySetAssetCustomTypeAssert.class);
  }

  /**
   * Verifies that the actual {@link SetAssetCustomType} value has identical fields as the ones
   * supplied.
   *
   * @param actionName the update action name.
   * @param assetId the asset Id the action is performed on.
   * @param assetKey the asset key the action is performed on.
   * @param customFields the new custom type fields.
   * @param type the new custom type.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public CategorySetAssetCustomTypeAssert hasValues(
      @Nonnull final String actionName,
      @Nullable final String assetId,
      @Nullable final String assetKey,
      @Nullable final Map<String, JsonNode> customFields,
      @Nullable final ResourceIdentifier<Type> type) {

    super.hasValues(actionName, customFields, type);
    assertThat(actual.getAssetId()).isEqualTo(assetId);
    assertThat(actual.getAssetKey()).isEqualTo(assetKey);
    return myself;
  }
}
