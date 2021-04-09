package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.service.CategoryTransformService;
import com.commercetools.sync.categories.service.impl.CategoryTransformServiceImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

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
   *     <p>TODO: Move the implementation from service class to this util class.
   */
  @Nonnull
  public static CompletableFuture<List<CategoryDraft>> toCategoryDrafts(
      @Nonnull final SphereClient client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<Category> categories) {

    final CategoryTransformService categoryTransformService =
        new CategoryTransformServiceImpl(client, referenceIdToKeyCache);
    return categoryTransformService.toCategoryDrafts(categories);
  }
}
