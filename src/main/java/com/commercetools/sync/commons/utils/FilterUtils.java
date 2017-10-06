package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.SyncFilter;
import com.commercetools.sync.products.UpdateFilterType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public final class FilterUtils {

    /**
     * Given a {@link SyncFilter}, a {@link ActionGroup} and 2 {@link Supplier}. This method checks if the action group
     * passes the criteria of the syncFiler, i.e. one of the following cases:
     * 1. Is not blacklisted
     * 2. Is whitelisted
     * 3. There {@link SyncFilter} is null.
     *
     * <p>If the action group passes the filter, then the first supplier is executed, if not then the second supplier is
     * executed.
     *
     * @param syncFilter          defines the blacklisting/whitelisted action groups.
     * @param actionGroup         the action group under check.
     * @param resultIfFilteredIn  the supplier to be executed if the action group passes the filter.
     * @param resultIfFilteredOut the supplier to be executed if the action group doesn't pass the filter.
     * @param <T>                 the type of the result of the suppliers and, in turn, the return of this method.
     * @return the result of the executed supplier.
     */
    @Nonnull
    public static <T> T executeSupplierIfPassesFilter(
        @Nullable final SyncFilter syncFilter, @Nonnull final ActionGroup actionGroup,
        @Nonnull final Supplier<T> resultIfFilteredIn, @Nonnull final Supplier<T> resultIfFilteredOut) {
        if (syncFilter == null) {
            // If there is no filter at all, then execute first supplier by default.
            return resultIfFilteredIn.get();
        }
        final UpdateFilterType filterType = syncFilter.getFilterType();
        if (filterType.equals(UpdateFilterType.BLACKLIST)) {
            final List<ActionGroup> blackList = syncFilter.getFilters();
            return !blackList.contains(actionGroup) ? resultIfFilteredIn.get() : resultIfFilteredOut.get();
        }

        final List<ActionGroup> whiteList = syncFilter.getFilters();
        return whiteList.contains(actionGroup) ? resultIfFilteredIn.get() : resultIfFilteredOut.get();
    }

    private FilterUtils() {
    }
}
