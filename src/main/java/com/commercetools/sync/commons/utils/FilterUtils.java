package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.SyncFilter;
import com.commercetools.sync.products.UpdateFilterType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public final class FilterUtils {

    /** TODO
     * @param syncFilter
     * @param filter
     * @param resultIfFilteredIn
     * @param resultIfFilteredOut
     * @param <T>
     * @return
     */
    @Nonnull
    public static <T> T executeSupplierIfPassesFilter(
        @Nullable final SyncFilter syncFilter, @Nonnull final ActionGroup filter,
        @Nonnull final Supplier<T> resultIfFilteredIn, @Nonnull final Supplier<T> resultIfFilteredOut) {
        if (syncFilter == null) {
            // If there is no filter at all, then execute filter by default.
            return resultIfFilteredIn.get();
        }
        final UpdateFilterType filterType = syncFilter.getFilterType();
        if (filterType.equals(UpdateFilterType.BLACKLIST)) {
            final List<ActionGroup> blackList = syncFilter.getFilters();
            return !blackList.contains(filter) ? resultIfFilteredIn.get() : resultIfFilteredOut.get();
        }

        final List<ActionGroup> whiteList = syncFilter.getFilters();
        return whiteList.contains(filter) ? resultIfFilteredIn.get() : resultIfFilteredOut.get();
    }

    private FilterUtils() {
    }
}
