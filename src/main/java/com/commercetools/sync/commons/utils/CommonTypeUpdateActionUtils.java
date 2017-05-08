package com.commercetools.sync.commons.utils;


import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class CommonTypeUpdateActionUtils {

    /**
     * Compares two {@link Object} and returns a supplied {@link UpdateAction} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link Object}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param oldObject            the object which should be updated.
     * @param newObject            the object with the new information.
     * @param updateActionSupplier the supplier that returns the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the object values are identical.
     */
    @Nonnull
    public static <T> Optional<UpdateAction<T>> buildUpdateAction(@Nullable final Object oldObject,
                                                                  @Nullable final Object newObject,
                                                                  @Nonnull final Supplier<UpdateAction<T>>
                                                                    updateActionSupplier) {
        return !Objects.equals(oldObject, newObject)
            ? Optional.ofNullable(updateActionSupplier.get()) : Optional.empty();
    }
}
