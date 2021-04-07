package com.commercetools.sync.categories.utils;

import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
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
   * Returns an {@link List}&lt;{@link CategoryDraft}&gt; consisting of the results of applying the
   * mapping from {@link Category} to {@link CategoryDraft} with considering reference resolution.
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
   *       <td>{@link Reference}&lt;{@link Category}&gt;</td>
   *       <td>{@link ResourceIdentifier}&lt;{@link Category}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>asset.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link Category} and {@link Type} references should contain Id in the
   * map(cache) with a key value. Any reference that is not available in the map will have its id in
   * place and not replaced by the key. This reference will be considered as existing resources on
   * the target commercetools project and the library will issues an update/create API request
   * without reference resolution.
   *
   * @param categories the categories without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link CategoryDraft} built from the supplied {@link List} of {@link
   *     Category}.
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
    return CategoryDraftBuilder.of(category)
        .custom(mapToCustomFieldsDraft(category, referenceIdToKeyCache))
        .assets(mapToAssetDrafts(category.getAssets(), referenceIdToKeyCache))
        .parent(getResourceIdentifierWithKey(category.getParent(), referenceIdToKeyCache))
        .build();
  }
}
