package com.commercetools.sync.sdk2.categories.utils;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.Reference;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public final class CategoryTransformUtils {

  /**
   * Transforms categories by resolving the references and map them to CategoryDrafts.
   *
   * <p>This method replaces the ids on unexpanded references of the category{@link Category} by
   * using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the Category to CategoryDraft by performing reference resolution considering
   * idToKey value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param categories the categories to replace the references id's with keys.
   * @return a new list which contains categoryDrafts which have all their references resolved and
   *     already replaced with keys.
   */
  @Nonnull
  public static CompletableFuture<List<CategoryDraft>> toCategoryDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<Category> categories) {

    final CategoryTransformServiceImpl categoryTransformService =
        new CategoryTransformServiceImpl(client, referenceIdToKeyCache);
    return categoryTransformService.toCategoryDrafts(categories);
  }

  private static class CategoryTransformServiceImpl extends BaseTransformServiceImpl {

    public CategoryTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<CategoryDraft>> toCategoryDrafts(
        @Nonnull final List<Category> categories) {

      /*
       * This method will fill already-fetched category keys (as it's a native field of category) it
       * means it could fill the cache with category id to category key without an extra query (or at
       * least a minimum amount) because a category could be a parent to another category or a child of
       * another parent category, this way we could optimize the query if the parent and child are in
       * the same batch/page (which is highly probable).
       *
       * <p>Simply iterate and fill all category keys with their ids, which means more keys stored in
       * the cache, which might not be an issue as the internal cache is fast and not putting much
       * memory overhead.
       */
      categories.forEach(
          category -> fillReferenceIdToKeyCache(category.getId(), category.getKey()));

      final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();

      transformReferencesToRunParallel.add(this.transformParentCategoryReference(categories));
      transformReferencesToRunParallel.add(this.transformCustomTypeReference(categories));

      return CompletableFuture.allOf(
              transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
          .thenApply(
              ignore ->
                  CategoryReferenceResolutionUtils.mapToCategoryDrafts(
                      categories, super.referenceIdToKeyCache));
    }

    private void fillReferenceIdToKeyCache(String id, String key) {
      final String keyValue = StringUtils.isBlank(key) ? KEY_IS_NOT_SET_PLACE_HOLDER : key;
      super.referenceIdToKeyCache.add(id, keyValue);
    }

    @Nonnull
    private CompletableFuture<Void> transformParentCategoryReference(
        @Nonnull final List<Category> categories) {

      final Set<String> parentCategoryIds =
          categories.stream()
              .map(Category::getParent)
              .filter(Objects::nonNull)
              .map(Reference::getId)
              .collect(Collectors.toSet());

      return super.fetchAndFillReferenceIdToKeyCache(
          parentCategoryIds, GraphQlQueryResource.CATEGORIES);
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomTypeReference(
        @Nonnull final List<Category> categories) {

      final Set<String> setOfTypeIds = new HashSet<>();
      setOfTypeIds.addAll(
          categories.stream()
              .map(Category::getCustom)
              .filter(Objects::nonNull)
              .map(customFields -> customFields.getType().getId())
              .collect(Collectors.toSet()));

      setOfTypeIds.addAll(
          categories.stream()
              .map(Category::getAssets)
              .map(
                  assets ->
                      assets.stream()
                          .filter(Objects::nonNull)
                          .map(Asset::getCustom)
                          .filter(Objects::nonNull)
                          .map(customFields -> customFields.getType().getId())
                          .collect(toList()))
              .flatMap(Collection::stream)
              .collect(toSet()));

      return super.fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
    }
  }
}
