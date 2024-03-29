package com.commercetools.sync.states.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StateSyncStatistics extends BaseSyncStatistics<StateSyncStatistics> {

  /**
   * The following {@link java.util.Map} ({@code stateKeysWithMissingParents}) represents products
   * with missing states (other referenced states).
   *
   * <ul>
   *   <li>key: key of the missing parent state
   *   <li>value: a set of the parent's children state keys
   * </ul>
   *
   * <p>The map is thread-safe (by instantiating it with {@link
   * java.util.concurrent.ConcurrentHashMap}).
   */
  private ConcurrentHashMap<String, Set<String>> stateKeysWithMissingParents =
      new ConcurrentHashMap<>();

  /**
   * Builds a summary of the state sync statistics instance that looks like the following example:
   *
   * <p>"Summary: 4 state(s) were processed in total (1 created, 1 updated, 1 failed to sync and 1
   * product(s) with a missing transition(s))."
   *
   * @return a summary message of the states sync statistics instance.
   */
  @Override
  public String getReportMessage() {
    return format(
        "Summary: %s state(s) were processed in total "
            + "(%s created, %s updated, %s failed to sync and %s state(s) with missing transition(s)).",
        getProcessed(),
        getCreated(),
        getUpdated(),
        getFailed(),
        getNumberOfStatesWithMissingParents());
  }

  @Override
  protected StateSyncStatistics getThis() {
    return this;
  }

  /**
   * Returns the total number of states with missing parents.
   *
   * @return the total number of states with missing parents.
   */
  public int getNumberOfStatesWithMissingParents() {
    return (int)
        stateKeysWithMissingParents.values().stream()
            .flatMap(Collection::stream)
            .distinct()
            .count();
  }

  /**
   * This method checks if there is an entry with the key of the {@code missingParentStateKey} in
   * the {@code stateKeysWithMissingParents}, if there isn't it creates a new entry with this parent
   * key and as a value a new set containing the {@code childKey}. Otherwise, if there is already,
   * it just adds the {@code childKey} to the existing set.
   *
   * @param parentKey the key of the missing parent.
   * @param childKey the key of the state with a missing parent.
   */
  public void addMissingDependency(
      @Nonnull final String parentKey, @Nonnull final String childKey) {
    stateKeysWithMissingParents.merge(
        parentKey,
        new HashSet<>(List.of(childKey)),
        (existingSet, newChildAsSet) -> {
          existingSet.addAll(newChildAsSet);
          return existingSet;
        });
  }

  @Nullable
  public Set<String> removeAndGetReferencingKeys(@Nonnull final String key) {
    return stateKeysWithMissingParents.remove(key);
  }
}
