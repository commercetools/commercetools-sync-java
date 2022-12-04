package com.commercetools.sync.sdk2.products.models;

import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.sdk2.commons.models.CustomDraft;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Adapt AssetDraft with {@link CustomDraft} interface to be used on {@link CustomUpdateActionUtils}
 */
public final class AssetDraftCustomTypeAdapter implements CustomDraft {

  private final AssetDraft assetDraft;

  private AssetDraftCustomTypeAdapter(AssetDraft assetDraft) {
    this.assetDraft = assetDraft;
  }

  /**
   * Get custom fields of the {@link AssetDraft}
   *
   * @return the {@link CustomFieldsDraft}
   */
  @Nullable
  @Override
  public CustomFieldsDraft getCustom() {
    return this.assetDraft.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link
   * AssetDraft}
   *
   * @param assetDraft the {@link AssetDraft}
   * @return the {@link AssetDraftCustomTypeAdapter}
   */
  public static AssetDraftCustomTypeAdapter of(AssetDraft assetDraft) {
    return new AssetDraftCustomTypeAdapter(assetDraft);
  }
}
