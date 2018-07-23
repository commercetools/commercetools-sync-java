package com.commercetools.sync.commons.utils;

import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static java.util.stream.Collectors.toList;

/**
 * This utility class is only meant to be used for internal use of the library.
 */
public final class UnorderedCollectionSyncUtils {

    /**
     * Compares a list of {@code newDrafts} with a map of {@code oldResourcesMap} and for every missing matching draft
     * in the {@code oldResourcesMap}: a remove update action is created using the {@code removeUpdateActionMapper}.
     * The final result is a list of all the remove update actions.
     *
     * @param oldResourcesMap          a map that consists of entries where each entry has a key=[resource matcher] and
     *                                 the value=[the old resource itself].
     * @param newDrafts                a list of the new drafts to compare to the old collection.
     * @param keyMapper                a function that uses the draft to get its key matcher.
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
        @Nonnull final Map<S, U> oldResourcesMap,
        @Nullable final List<V> newDrafts,
        @Nonnull final Function<V, S> keyMapper,
        @Nonnull final Function<U, UpdateAction<T>> removeUpdateActionMapper) {

        final Map<S, U> resourcesToRemove = new HashMap<>(oldResourcesMap);

        emptyIfNull(newDrafts).stream()
                              .filter(Objects::nonNull)
                              .map(keyMapper)
                              .forEach(resourcesToRemove::remove);

        return resourcesToRemove.values()
                                .stream()
                                .map(removeUpdateActionMapper)
                                .collect(toList());
    }

    /**
     *
     * @param oldResourcesMap
     * @param oneToOneActionsMapper
     * @param addActionMapper
     * @param <T> type of the resulting update actions.
     * @param <S> type of the new resource key.
     * @param <U> type of the old resource.
     * @return
     */
    public static <T, S, U> List<UpdateAction<T>> buildOneToOneOrAddActions(
        @Nonnull final Map<S, U> oldResourcesMap,
        @Nonnull final S newResourceKey,
        @Nonnull final Function<U, List<UpdateAction<T>>> oneToOneActionsMapper,
        @Nonnull final Supplier<UpdateAction<T>> addActionMapper) {

        return ofNullable(oldResourcesMap.get(newResourceKey))
            .map(oneToOneActionsMapper)
            .orElseGet(() -> singletonList(addActionMapper.get()));
    }

    private UnorderedCollectionSyncUtils() {
    }
}
