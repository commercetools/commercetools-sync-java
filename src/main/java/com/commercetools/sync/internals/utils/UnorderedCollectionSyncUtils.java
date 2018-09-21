package com.commercetools.sync.internals.utils;

import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static java.util.stream.Collectors.toList;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 */
public final class UnorderedCollectionSyncUtils {

    /**
     * Compares a list of {@code newDrafts} with a list of {@code oldResources} and for every missing matching draft
     * in the {@code oldResources}: a remove update action is created using the {@code removeUpdateActionMapper}.
     * The final result is a list of all the remove update actions.
     *
     * <p>The resulting list of update actions is ensured to contain no null values.
     * <p>If the draft has null key as defined by {@code draftKeyMapper}, the method will ignore generating a remove
     * update action for it.
     *
     * @param oldResources             a list of the old resource to compare to the new collection.
     * @param newDrafts                a list of the new drafts to compare to the old collection.
     * @param oldResourceKeyMapper     a function that uses the old resource to get its key matcher.
     * @param draftKeyMapper           a function that uses the draft to get its key matcher.
     * @param removeUpdateActionMapper a function that uses the old resource to build a remove update action.
     * @param <T>                      type of the resulting update actions.
     * @param <S>                      type of the new resource key.
     * @param <U>                      type of the old resource.
     * @param <V>                      type of the new resource.
     * @return a list of all the remove update actions. If there are no missing matching drafts, an empty list is
     *         returned.
     */
    @Nonnull
    public static <T, S, U, V> List<UpdateAction<T>> buildRemoveUpdateActions(
        @Nonnull final List<U> oldResources,
        @Nullable final List<V> newDrafts,
        @Nonnull final Function<U, S> oldResourceKeyMapper,
        @Nonnull final Function<V, S> draftKeyMapper,
        @Nonnull final Function<U, UpdateAction<T>> removeUpdateActionMapper) {

        final Map<S, U> oldResourcesMap = collectionToMap(oldResources, oldResourceKeyMapper);
        oldResourcesMap.remove(null);

        final Map<S, U> resourcesToRemove = new HashMap<>(oldResourcesMap);

        emptyIfNull(newDrafts).stream()
                              .map(draftKeyMapper)
                              .filter(Objects::nonNull)
                              .forEach(resourcesToRemove::remove);

        return resourcesToRemove.values()
                                .stream()
                                .map(removeUpdateActionMapper)
                                .filter(Objects::nonNull)
                                .collect(toList());
    }

    private UnorderedCollectionSyncUtils() {
    }
}