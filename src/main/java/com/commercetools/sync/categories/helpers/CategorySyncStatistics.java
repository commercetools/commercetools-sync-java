package com.commercetools.sync.categories.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
   * <p>The map is thread-safe (by instantiating it with {@link ConcurrentHashMap}).
   */
  private final ConcurrentHashMap<String, Set<String>> categoryKeysWithMissingParents =
      new ConcurrentHashMap<>();

  public CategorySyncStatistics() {
    super();
  }

  /**
   * Builds a summary of the category sync statistics instance that looks like the following
   * example:
   *
   * <p>"Summary: 2 categories were processed in total (1 created, 1 updated and 0 categories failed
   * to sync and 0 categories with a missing parent)."
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
    return (int)
        categoryKeysWithMissingParents.values().stream()
            .flatMap(Collection::stream)
            .distinct()
            .count();
  }

  /**
   * This method checks if there is an entry with the key of the {@code missingParentCategoryKey} in
   * the {@code categoryKeysWithMissingParents}, if there isn't it creates a new entry with this
   * parent key and as a value a new set containing the {@code childKey}. Otherwise, if there is
   * already, it just adds the {@code childKey} to the existing set.
   *
   * @param missingParentCategoryKey the key of the missing parent.
   * @param childKey the key of the state with a missing parent.
   */
  public void addMissingDependency(
      @Nonnull final String missingParentCategoryKey, @Nonnull final String childKey) {

    categoryKeysWithMissingParents
        .computeIfAbsent(missingParentCategoryKey, ign -> new HashSet<>())
        .add(childKey);
  }

  /**
   * Given a child {@code childKey} this method removes its occurrences from the map {@code
   * categoryKeysWithMissingParents}.
   *
   * <p>NOTE: When all the children keys of a missing parent are removed, the value of the map entry
   * will be removed.
   *
   * @param parentKey the key of the missing parent.
   * @param childKey the child category key to remove from {@code categoryKeysWithMissingParents}
   */
  public void removeChildCategoryKeyFromMissingParentsMap(
      @Nonnull final String parentKey, @Nonnull final String childKey) {

    categoryKeysWithMissingParents.computeIfPresent(
        parentKey,
        (key, values) -> {
          values.remove(childKey);
          return values.isEmpty() ? null : values;
        });
  }

  /**
   * Returns the children keys of given {@code parentKey}.
   *
   * @param parentKey parent category key
   * @return Returns the children keys of given {@code parentKey}.
   */
  @Nullable
  public Set<String> getChildrenKeys(@Nonnull final String parentKey) {
    return categoryKeysWithMissingParents.get(parentKey);
  }
}
