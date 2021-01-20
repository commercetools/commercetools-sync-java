package com.commercetools.sync.categories.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CategorySyncStatistics extends BaseSyncStatistics {

  /**
   * The following {@link Map} ({@code categoryKeysWithMissingParents}) represents categories with
   * missing parents.
   *
   * <ul>
   *   <li>key: key of the missing parent category
   *   <li>value: a set of the parent's children category keys
   * </ul>
   *
   * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}) because it is
   * accessed/modified in a concurrent context, specifically when updating products in parallel in
   * {@link com.commercetools.sync.categories.CategorySync#updateCategory(Category, CategoryDraft,
   * List)}.
   */
  private ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents =
      new ConcurrentHashMap<>();

  public CategorySyncStatistics() {
    super();
  }

  /**
   * Builds a summary of the category sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 categories were processed in total (0 created, 0 updated and 0 categories failed
   * to sync)."
   *
   * @return a summary message of the category sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return format(
        "Summary: %s categories were processed in total "
            + "(%s created, %s updated, %s failed to sync and %s categories with a missing parent).",
        getProcessed(),
        getCreated(),
        getUpdated(),
        getFailed(),
        getNumberOfCategoriesWithMissingParents());
  }

  /**
   * Returns the total number of categories with missing parents.
   *
   * @return the total number of categories with missing parents.
   */
  public int getNumberOfCategoriesWithMissingParents() {
    return categoryKeysWithMissingParents.values().stream().mapToInt(Set::size).sum();
  }

  public Map<String, Set<String>> getCategoryKeysWithMissingParents() {
    return Collections.unmodifiableMap(categoryKeysWithMissingParents);
  }

  public void setCategoryKeysWithMissingParents(
      @Nonnull final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents) {
    this.categoryKeysWithMissingParents = categoryKeysWithMissingParents;
  }

  /**
   * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in
   * the {@code categoryKeysWithMissingParents}, if there isn't it creates a new entry with this
   * parent key and as a value a new set containing the {@code childKey}. Otherwise, if there is
   * already, it just adds the {@code categoryKey} to the existing set.
   *
   * @param missingParentCategoryKey the key of the missing parent.
   * @param childKey the key of the category with a missing parent.
   */
  public void putMissingParentCategoryChildKey(
      @Nonnull final String missingParentCategoryKey, @Nonnull final String childKey) {
    final Set<String> missingParentCategoryChildrenKeys =
        categoryKeysWithMissingParents.get(missingParentCategoryKey);
    if (missingParentCategoryChildrenKeys != null) {
      missingParentCategoryChildrenKeys.add(childKey);
    } else {
      final Set<String> newChildCategoryKeys = new HashSet<>();
      newChildCategoryKeys.add(childKey);
      categoryKeysWithMissingParents.put(missingParentCategoryKey, newChildCategoryKeys);
    }
  }

  /**
   * Given a {@code childCategoryKey} this method, checks in the {@code
   * categoryKeysWithMissingParents} if it exists as a child to a missing parent, and returns the
   * key of first found (since a category can have only one parent) missing parent in an optional.
   * Otherwise, it returns an empty optional.
   *
   * @param childCategoryKey key of the category to find it's has a missing parent key.
   * @return the key of that missing parent in an optional, if it exists. Otherwise, it returns an
   *     empty optional.
   */
  @Nonnull
  public Optional<String> getMissingParentKey(@Nonnull final String childCategoryKey) {
    return categoryKeysWithMissingParents.entrySet().stream()
        .filter(missingParentEntry -> missingParentEntry.getValue().contains(childCategoryKey))
        .findFirst()
        .map(Map.Entry::getKey);
  }

  /**
   * Given a child {@code categoryKey} this method removes its occurrences from the map {@code
   * categoryKeysWithMissingParents}.
   *
   * <p>NOTE: When all the children keys of a missing parent are removed, the value of the map entry
   * will be an empty list. i.e. the entry itself will not be removed. However, this could be
   * investigated whether removing the entry at all when the list is empty will affect the
   * algorithm. TODO: RELATED BUT NOT SAME AS GITHUB ISSUE#77
   *
   * @param childCategoryKey the child category key to remove from {@code
   *     categoryKeysWithMissingParents}
   */
  public void removeChildCategoryKeyFromMissingParentsMap(@Nonnull final String childCategoryKey) {
    categoryKeysWithMissingParents.forEach((key, value) -> value.remove(childCategoryKey));
  }

  @Nullable
  public Set<String> removeAndGetChildrenKeys(@Nonnull final String key) {
    return categoryKeysWithMissingParents.remove(key);
  }
}
