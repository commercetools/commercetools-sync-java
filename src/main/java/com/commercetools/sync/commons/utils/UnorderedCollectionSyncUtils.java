package com.commercetools.sync.commons.utils;

import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public final class UnorderedCollectionSyncUtils {

    /**
     *
     * @param oldResourcesMap
     * @param newResources
     * @param keyMapper
     * @param removeUpdateActionMapper
     * @param <T> type of the resulting update actions.
     * @param <S> type of the new resource key.
     * @param <U> type of the old resource.
     * @param <V> type of the new resource.
     * @return
     */
    public static <T, S, U, V> List<UpdateAction<T>> buildRemoveUpdateActions(
        @Nonnull final Map<S, U> oldResourcesMap,
        @Nullable final List<V> newResources,
        @Nonnull final Function<V, S> keyMapper,
        @Nonnull final Function<U, UpdateAction<T>> removeUpdateActionMapper) {

        final Map<S, U> resourcesToRemove = new HashMap<>(oldResourcesMap);

        emptyIfNull(newResources).stream()
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
