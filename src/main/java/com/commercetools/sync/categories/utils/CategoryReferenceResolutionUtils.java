package com.commercetools.sync.categories.utils;

import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CategoryReferenceResolutionUtils {

  private CategoryReferenceResolutionUtils() {}

  /**
   * Returns an {@link java.util.List}&lt;{@link CategoryDraft}&gt; consisting of the results of
   * applying the mapping from {@link Category} to {@link CategoryDraft} with considering reference
   * resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>parent</td>
   *       <td>{@link com.commercetools.api.models.category.CategoryReference}</td>
   *       <td>{@link com.commercetools.api.models.category.CategoryResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>asset.custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link Category} and {@link com.commercetools.api.models.type.Type}
   * references should contain Id in the map(cache) with a key value. Any reference that is not
   * available in the map will have its id in place and not replaced by the key. This reference will
   * be considered as existing resources on the target commercetools project and the library will
   * issues an update/create API request without reference resolution.
   *
   * @param categories the categories without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link CategoryDraft} built from the supplied {@link
   *     java.util.List} of {@link Category}.
   */
  @Nonnull
  public static List<CategoryDraft> mapToCategoryDrafts(
      @Nonnull final List<Category> categories,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return categories.stream()
        .map(category -> mapToCategoryDraft(category, referenceIdToKeyCache))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static CategoryDraft mapToCategoryDraft(
      @Nonnull final Category category,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final CategoryResourceIdentifier resourceIdentifierWithKey =
        getResourceIdentifierWithKey(
            category.getParent(),
            referenceIdToKeyCache,
            (id, key) -> CategoryResourceIdentifierBuilder.of().key(key).id(id).build());
    return CategoryDraftBuilder.of()
        .key(category.getKey())
        .slug(category.getSlug())
        .name(category.getName())
        .description(category.getDescription())
        .externalId(category.getExternalId())
        .metaDescription(category.getMetaDescription())
        .metaKeywords(category.getMetaKeywords())
        .metaTitle(category.getMetaTitle())
        .orderHint(category.getOrderHint())
        .custom(mapToCustomFieldsDraft(category, referenceIdToKeyCache))
        .assets(mapToAssetDrafts(category.getAssets(), referenceIdToKeyCache))
        .parent(resourceIdentifierWithKey)
        .build();
  }
}
