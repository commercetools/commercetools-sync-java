package com.commercetools.sync.commons.utils;


import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class CommonTypeUpdateActionUtils {

    /**
     * Compares two {@link Object} and returns a supplied {@link UpdateAction} as a result in an
     * {@link Optional}. If both the {@link Object}s have the same values, then no update action is needed and hence an
     * empty {@link Optional} is returned.
     *
     * @param oldObject            the object which should be updated
     * @param newObject            the object with the new information
     * @param updateActionSupplier the supplier that returns the update action to return in the optional
     * @param <T>                  the type of the {@link UpdateAction}
     * @param <S>                  the type of the objects to compare
     * @param <U>                  certain {@link UpdateAction} implementation type
     * @return A filled optional with the update action or an empty optional if the object values are identical
     */
    @Nonnull
    public static <T, S, U extends UpdateAction<T>> Optional<U> buildUpdateAction(
            @Nullable final S oldObject,
            @Nullable final S newObject,
            @Nonnull final Supplier<U> updateActionSupplier) {

        return !Objects.equals(oldObject, newObject)
                ? Optional.ofNullable(updateActionSupplier.get())
                : Optional.empty();
    }

    /**
     * Compares two objects that are of type {@link ResourceIdentifier} (or a type that extends it) and returns a
     * supplied {@link UpdateAction} as a result in an {@link Optional}. If both the {@link Object}s have the same
     * values, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldResourceIdentifier            the old resource identifier
     * @param newResourceIdentifier            the new resource identifier
     * @param updateActionSupplier             the supplier that returns the update action to return in the optional
     * @param <T>                              the type of the {@link UpdateAction}
     * @param <S>                              the type of the old resource identifier
     * @param <U>                              the type of the new resource identifier
     * @param <V>                              concrete {@link UpdateAction} implementation type
     * @return A filled optional with the update action or an empty optional if the object values are identical
     */
    @Nonnull
    public static <T, S extends ResourceIdentifier, U extends ResourceIdentifier, V extends UpdateAction<T>> Optional<V>
    buildUpdateActionForReferences(
        @Nullable final S oldResourceIdentifier,
        @Nullable final U newResourceIdentifier,
        @Nonnull final Supplier<V> updateActionSupplier) {

        final String oldParentId = ofNullable(oldResourceIdentifier).map(ResourceIdentifier::getId).orElse(null);
        final String newParentId = ofNullable(newResourceIdentifier).map(ResourceIdentifier::getId).orElse(null);

        return buildUpdateAction(oldParentId, newParentId, updateActionSupplier);
    }

    /**
     * Compares two {@link Object} and returns a supplied list of {@link UpdateAction} as a result.
     * If both the {@link Object}s have the same values, then no update action is needed
     * and hence an empty list is returned.
     *
     * @param oldObject            the object which should be updated
     * @param newObject            the object with the new information
     * @param updateActionSupplier the supplier that returns a list of update actions if the objects are different
     * @param <T>                  the type of the {@link UpdateAction}
     * @param <S>                  the type of the objects to compare
     * @param <U>                  certain {@link UpdateAction} implementation type
     * @return A filled optional with the update action or an empty optional if the object values are identical
     */
    @Nonnull
    public static <T, S, U extends UpdateAction<T>> List<U> buildUpdateActions(
            @Nullable final S oldObject,
            @Nullable final S newObject,
            @Nonnull final Supplier<List<U>> updateActionSupplier) {

        return !Objects.equals(oldObject, newObject)
                ? updateActionSupplier.get()
                : emptyList();
    }

    private CommonTypeUpdateActionUtils() {
    }
}
