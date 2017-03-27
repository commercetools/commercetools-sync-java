package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DraftTransformer {

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param categories
     * @return
     */
    public static List<CategoryDraft> getCategoryDrafts(@Nonnull final List<Category> categories) {
        return categories.stream()
                .map(DraftTransformer::getCategoryDraft)
                .collect(Collectors.toList());
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param categories
     * @param categoryTypeKeyMap
     * @return
     */
    public static List<CategoryDraft> getCategoryDrafts(@Nonnull final List<Category> categories,
                                                 @Nonnull final Map<String, String> categoryTypeKeyMap) {
        return categories.stream()
                .map(category -> {
                    CustomFields categoryCustomFields = category.getCustom();
                    if (categoryCustomFields != null) {
                        String categoryCustomTypeKey = categoryTypeKeyMap.get(categoryCustomFields.getType().getId());
                        if (categoryCustomTypeKey != null) {
                            return getCategoryDraft(category, categoryCustomTypeKey);
                        } else {
                            // TODO: Throw problem that there is no key set for this category!!
                            return null;
                        }
                    } else {
                        return getCategoryDraft(category);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param category
     * @return
     */
    public static CategoryDraft getCategoryDraft(@Nonnull final Category category) {
        return CategoryDraftBuilder.of(category.getName(), category.getSlug())
                .externalId(category.getExternalId())
                .description(category.getDescription())
                .parent(category.getParent())
                .orderHint(category.getOrderHint())
                .metaTitle(category.getMetaTitle())
                .metaDescription(category.getDescription())
                .metaKeywords(category.getMetaKeywords())
                .custom(getCustomFieldsDraft(category.getCustom()))
                .assets(getAssetDrafts(category.getAssets()))
                .build();
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param category
     * @param categoryTypeKey
     * @return
     */
    public static CategoryDraft getCategoryDraft(@Nonnull final Category category,
                                          @Nonnull final String categoryTypeKey) {
        return null;
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param customFields
     * @return
     */
    public static CustomFieldsDraft getCustomFieldsDraft(final CustomFields customFields) {
        Reference<Type> typeReference = customFields.getType();
        String customFieldsTypeKey = typeReference.getKey(); // this would be null if no reference expansion was used!
        if (customFieldsTypeKey == null) {
            return CustomFieldsDraft.ofTypeIdAndJson(typeReference.getId(), customFields.getFieldsJsonMap());
        }
        return CustomFieldsDraft.ofTypeKeyAndJson(customFieldsTypeKey, customFields.getFieldsJsonMap());
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param customFields
     * @param categoryTypeKey
     * @return
     */
    public static CustomFieldsDraft getCustomFieldsDraft(@Nonnull final CustomFields customFields,
                                                  @Nonnull final String categoryTypeKey) {
        return CustomFieldsDraft.ofTypeKeyAndJson(categoryTypeKey, customFields.getFieldsJsonMap());
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param assets
     * @return
     */
    public static List<AssetDraft> getAssetDrafts(@Nonnull final List<Asset> assets) {
        return assets.stream()
                .map(DraftTransformer::getAssetDraft)
                .collect(Collectors.toList());
    }

    /**
     * TODO: UNIT TEST
     * TODO: DOCUMENT
     * TODO: REFACTOR
     * @param asset
     * @return
     */
    public static AssetDraft getAssetDraft(@Nonnull final Asset asset) {
        return AssetDraftBuilder.of(asset.getSources(), asset.getName())
                .description(asset.getDescription())
                .custom(getCustomFieldsDraft(asset.getCustom()))
                .tags(asset.getTags())
                .build();
    }
}
