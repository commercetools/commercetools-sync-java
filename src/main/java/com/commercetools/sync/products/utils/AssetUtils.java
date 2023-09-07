package com.commercetools.sync.products.utils;

import static com.commercetools.sync.products.utils.CustomFieldsUtils.createCustomFieldsDraft;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class AssetUtils {

  public static List<AssetDraft> createAssetDraft(@Nullable List<Asset> assets) {
    if (assets == null) {
      return List.of();
    }
    return assets.stream()
        .map(
            asset ->
                AssetDraftBuilder.of()
                    .custom(createCustomFieldsDraft(asset.getCustom()))
                    .description(asset.getDescription())
                    .name(asset.getName())
                    .key(asset.getKey())
                    .sources(asset.getSources())
                    .tags(asset.getTags())
                    .build())
        .collect(Collectors.toList());
  }
}
