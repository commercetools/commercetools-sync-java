package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.CustomFieldsUtils.createCustomFieldsDraft;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import java.util.List;
import java.util.stream.Collectors;

public class AssetUtils {

  public static List<AssetDraft> createAssetDraft(List<Asset> assets) {
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
