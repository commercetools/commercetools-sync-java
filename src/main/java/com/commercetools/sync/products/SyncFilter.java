package com.commercetools.sync.products;

import javax.annotation.Nonnull;
import java.util.List;

public class SyncFilter {

    /**
     * Defines which attributes to calculate update actions for.
     */
    private final List<UpdateFilter> filters;

    /**
     * Defines the filter type: blacklist or whitelist.
     */
    private final UpdateFilterType filterType;

    private SyncFilter(@Nonnull final List<UpdateFilter> filters, @Nonnull final UpdateFilterType filterType) {
        this.filters = filters;
        this.filterType = filterType;
    }

    public static SyncFilter of(@Nonnull final List<UpdateFilter> filters, @Nonnull final UpdateFilterType filterType) {
        return new SyncFilter(filters, filterType);
    }

    public UpdateFilterType getFilterType() {
        return filterType;
    }

    public List<UpdateFilter> getFilters() {
        return filters;
    }
}
