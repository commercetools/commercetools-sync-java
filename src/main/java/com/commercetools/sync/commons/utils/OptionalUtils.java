package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class OptionalUtils {

    /**
     *  Takes a {@link Collection} of {@link Optional}s containing some elements of type {@link T}, this method filters
     *  out any empty {@link Optional} and returns a new collection containing the values of the non-empty optionals.
     *
     * @param optionals the collection of optionals that should be filtered out of empty optionals.
     * @param <T> The type of the elements in the Optionals in the supplied collection {@code optionals}.
     * @return a new collection containing the values of the non-empty optionals from the supplied {@code optionals}
     *         collection.
     */
    @Nonnull
    public static <T> List<T> filterEmptyOptionals(@Nonnull final Collection<Optional<T>> optionals) {
        return filterEmptyOptionals(optionals, ArrayList::new);
    }

    /**
     *  Takes a {@link Collection} of {@link Optional}s containing some elements of type {@link T}, this method filters
     *  out any empty {@link Optional} and returns a new collection, of a type based on the supplied
     *  {@code collectionFactory}, containing the values of the non-empty optionals.
     *
     * @param optionals the collection of optionals that should be filtered out of empty optionals.
     * @param collectionFactory the factory which decided on the type of the returned collection.
     * @param <T> The type of the elements in the Optionals in the supplied collection {@code optionals}.
     * @param <C> The type of the collection that should be returned containing the values of the non-empty optionals.
     * @return a new collection, of a type based on the supplied {@code collectionFactory},
     *         containing the values of the non-empty optionals from the supplied {@code optionals} collection.
     */
    @Nonnull
    private static <T, C extends Collection<T>> C filterEmptyOptionals(@Nonnull final Collection<Optional<T>> optionals,
                                                                      @Nonnull final Supplier<C> collectionFactory) {

        return optionals.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toCollection(collectionFactory));
    }

    private OptionalUtils() {
    }
}
