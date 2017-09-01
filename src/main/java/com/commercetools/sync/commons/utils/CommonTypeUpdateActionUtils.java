package com.commercetools.sync.commons.utils;


import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public final class CommonTypeUpdateActionUtils {

    /**
     * Compares two {@link Object} and returns a supplied {@link UpdateAction} as a result in an
     * {@link Optional}. If both the {@link Object}s have the same values, then no update action is needed and hence an
     * empty {@link Optional} is returned.
     *
     * @param oldObject            the object which should be updated.
     * @param newObject            the object with the new information.
     * @param updateActionSupplier the supplier that returns the update action to return in the optional.
     * @param <T>                  the type of the {@link UpdateAction}
     * @return A filled optional with the update action or an empty optional if the object values are identical.
     */
    @Nonnull
    public static <T, S> Optional<UpdateAction<T>> buildUpdateAction(@Nullable final S oldObject,
                                                                  @Nullable final S newObject,
                                                                  @Nonnull final Supplier<UpdateAction<T>>
                                                                    updateActionSupplier) {
        return !Objects.equals(oldObject, newObject)
            ? Optional.ofNullable(updateActionSupplier.get()) : Optional.empty();
    }

    @Nonnull
    public static <T, S> List<UpdateAction<T>> buildUpdateActions(@Nullable final S oldObject,
                                                                  @Nullable final S newObject,
                                                                  @Nonnull final Function<S, List<UpdateAction<T>>>
                                                                      updateActionSupplier) {
        return !Objects.equals(oldObject, newObject) ? updateActionSupplier.apply(oldObject) : emptyList();
    }
}
