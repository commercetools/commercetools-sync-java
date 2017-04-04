package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.helpers.SyncResult;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CommonTypeUpdateActionUtils {

    /**
     * Compares two {@link Object} and returns a {@link SyncResult} containing the supplied {@link UpdateAction} as a
     * result. If no update action is needed, for example in case where both the {@link Object}
     * have the same values, a sync result with containing an empty list of update actions is returned.
     *
     * @param oldObject    the object which should be updated.
     * @param newObject    the object with the new information.
     * @param updateAction the update action to return in the optional.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the object values are identical.
     */
    @Nonnull
    public static <T> SyncResult<T> buildUpdateAction(@Nullable final Object oldObject,
                                                      @Nullable final Object newObject,
                                                      @Nullable final UpdateAction<T> updateAction) {
        return !Objects.equals(oldObject, newObject) && updateAction != null
                ? SyncResult.of(updateAction) : SyncResult.emptyResult();
    }
}
