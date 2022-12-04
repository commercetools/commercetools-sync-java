package com.commercetools.sync.sdk2.products.models;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import org.jetbrains.annotations.Nullable;

/** Adapt Asset with {@link Custom} interface to be used on {@link CustomUpdateActionUtils} */
public final class AssetCustomTypeAdapter implements Custom {

  private final Asset asset;

  private AssetCustomTypeAdapter(Asset asset) {
    this.asset = asset;
  }

  /**
   * Get Id of the {@link Asset}
   *
   * @return the {@link Asset#getId()}
   */
  @Override
  public String getId() {
    return this.asset.getId();
  }

  /**
   * Get typeId of the {@link Asset} see: https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "asset"
   */
  @Override
  public String getTypeId() {
    return "asset";
  }

  /**
   * Get custom fields of the {@link Asset}
   *
   * @return the {@link CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.asset.getCustom();
  }

  /**
   * Get Key of the {@link Asset}
   *
   * @return the {@link Asset#getKey()}
   */
  public String getKey() {
    return this.asset.getKey();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link Asset}
   *
   * @param asset the {@link Asset}
   * @return the {@link AssetCustomTypeAdapter}
   */
  public static AssetCustomTypeAdapter of(Asset asset) {
    return new AssetCustomTypeAdapter(asset);
  }
}
