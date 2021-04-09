package com.commercetools.sync.categories.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.categories.service.CategoryTransformService;
import com.commercetools.sync.categories.utils.CategoryReferenceResolutionUtils;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class CategoryTransformServiceImpl extends BaseTransformServiceImpl
    implements CategoryTransformService {

  public CategoryTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
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
    cacheResourceReferenceKeys(categories);

    final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();

    transformReferencesToRunParallel.add(this.transformParentCategoryReference(categories));
    transformReferencesToRunParallel.add(this.transformCustomTypeReference(categories));

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
        .thenApply(
            ignore ->
                CategoryReferenceResolutionUtils.mapToCategoryDrafts(
                    categories, referenceIdToKeyCache));
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

    return fetchAndFillReferenceIdToKeyCache(parentCategoryIds, GraphQlQueryResources.CATEGORIES);
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

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
  }
}
