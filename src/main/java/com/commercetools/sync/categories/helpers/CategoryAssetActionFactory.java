package com.commercetools.sync.categories.helpers;

import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildActions;

import com.commercetools.api.models.category.CategoryAddAssetActionBuilder;
import com.commercetools.api.models.category.CategoryChangeAssetOrderActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryRemoveAssetActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.helpers.AssetActionFactory;
import java.util.List;
import javax.annotation.Nonnull;

public final class CategoryAssetActionFactory
    extends AssetActionFactory<CategoryUpdateAction, CategoryDraft> {

  public CategoryAssetActionFactory(@Nonnull final CategorySyncOptions syncOptions) {
    this.syncOptions = syncOptions;
  }

  @Override
  public List<CategoryUpdateAction> buildAssetActions(
      @Nonnull final CategoryDraft newResource,
      @Nonnull final Asset oldAsset,
      @Nonnull final AssetDraft newAssetDraft) {
    return buildActions(newResource, oldAsset, newAssetDraft, (CategorySyncOptions) syncOptions);
  }

  @Override
  public CategoryUpdateAction buildRemoveAssetAction(@Nonnull final String assetKey) {
    return CategoryRemoveAssetActionBuilder.of().assetKey(assetKey).build();
  }

  @Override
  public CategoryUpdateAction buildChangeAssetOrderAction(
      @Nonnull final List<String> newAssetOrder) {
    return CategoryChangeAssetOrderActionBuilder.of().assetOrder(newAssetOrder).build();
  }

  @Override
  public CategoryUpdateAction buildAddAssetAction(
      @Nonnull final AssetDraft assetDraft, @Nonnull final Integer position) {
    return CategoryAddAssetActionBuilder.of().asset(assetDraft).position(position).build();
  }
}
