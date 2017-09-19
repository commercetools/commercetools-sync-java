package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public final class CollectionUtils {

    /**
     * Create a new collection which contains only elements which satisfy {@code includeCondition} predicate.
     *
     * @param collection       {@link Collection} to be filtered
     * @param includeCondition condition which verifies whether the value should be included to the final result.
     * @param <T>              type of the collection items.
     * @return new filtered stream with items which satisfy {@code includeCondition} predicate. If {@code collection}
     *     is <b>null</b> or empty - empty stream is returned.
     */
    @Nonnull
    public static <T> Stream<T> filterCollection(@Nullable final Collection<T> collection,
                                                 @Nonnull final Predicate<T> includeCondition) {

        return collection == null ? Stream.empty()
                : collection.stream()
                .filter(includeCondition);
    }

    /**
     * Convert a {@code collection} to a set of values using {@code entityToKey} mapping.
     *
     * <p>If the collection has duplicate keys - only one will be stored, which one is not defined though.
     *
     * @param collection {@link Collection} to convert
     * @param entityToKey function which converts the collection entry to a key.
     * @param <T> type of collection entries
     * @param <K> type of Set key
     * @return new {@link Set} which consists of items, converted from &lt;T&gt; entries to keys using
     * {@code entryToKey} function.
     */
    @Nonnull
    public static <T, K> Set<K> collectionToSet(@Nullable final Collection<T> collection,
                                                @Nonnull final Function<T, K> entityToKey) {
        return collection == null ? Collections.emptySet()
                : collection.stream()
                .map(entityToKey)
                .collect(toSet());
    }

    private CollectionUtils() {
    }
}
