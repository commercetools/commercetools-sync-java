package com.commercetools.sync.categories.service;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface CategoryTransformService {

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
   * @param categories the categories to replace the references id's with keys.
   * @return a new list which contains categoryDrafts which have all their references resolved and
   *     already replaced with keys.
   */
  @Nonnull
  CompletableFuture<List<CategoryDraft>> toCategoryDrafts(@Nonnull List<Category> categories);
}
