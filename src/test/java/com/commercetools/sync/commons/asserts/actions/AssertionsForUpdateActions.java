package com.commercetools.sync.commons.asserts.actions;

import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.types.customupdateactions.SetCustomFieldBase;
import io.sphere.sdk.types.customupdateactions.SetCustomTypeBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssertionsForUpdateActions {
  private AssertionsForUpdateActions() {}

  /**
   * Create assertion for {@link SetCustomTypeBase}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static SetCustomTypeAssert assertThat(@Nullable final SetCustomTypeBase updateAction) {
    return new SetCustomTypeAssert(updateAction);
  }

  /**
   * Create assertion for {@link SetCustomFieldBase}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static AbstractSetCustomFieldAssert assertThat(
      @Nullable final SetCustomFieldBase updateAction) {
    return new SetCustomFieldAssert(updateAction);
  }

  /**
   * Create assertion for {@link
   * io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CategorySetAssetCustomTypeAssert assertThat(
      @Nullable
          final io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType updateAction) {
    return new CategorySetAssetCustomTypeAssert(updateAction);
  }

  /**
   * Create assertion for {@link
   * io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CategorySetAssetCustomFieldAssert assertThat(
      @Nullable
          final io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField updateAction) {
    return new CategorySetAssetCustomFieldAssert(updateAction);
  }

  /**
   * Create assertion for {@link io.sphere.sdk.products.commands.updateactions.SetAssetCustomType}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductSetAssetCustomTypeAssert assertThat(
      @Nullable
          final io.sphere.sdk.products.commands.updateactions.SetAssetCustomType updateAction) {
    return new ProductSetAssetCustomTypeAssert(updateAction);
  }

  /**
   * Create assertion for {@link io.sphere.sdk.products.commands.updateactions.SetAssetCustomField}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductSetAssetCustomFieldAssert assertThat(
      @Nullable
          final io.sphere.sdk.products.commands.updateactions.SetAssetCustomField updateAction) {
    return new ProductSetAssetCustomFieldAssert(updateAction);
  }

  /**
   * Create assertion for {@link SetProductPriceCustomType}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static SetPriceCustomTypeAssert assertThat(
      @Nullable final SetProductPriceCustomType updateAction) {
    return new SetPriceCustomTypeAssert(updateAction);
  }

  /**
   * Create assertion for {@link SetProductPriceCustomField}.
   *
   * @param updateAction the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static SetPriceCustomFieldAssert assertThat(
      @Nullable final SetProductPriceCustomField updateAction) {
    return new SetPriceCustomFieldAssert(updateAction);
  }
}
