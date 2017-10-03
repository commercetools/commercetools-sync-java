package com.commercetools.sync.products;

import javax.annotation.Nonnull;
import java.util.List;

public class SyncFilter {

    /**
     * Defines which attributes to calculate update actions for.
     */
    private final List<ActionGroup> filters;

    /**
     * Defines the filter type: blacklist or whitelist.
     */
    private final UpdateFilterType filterType;

    private SyncFilter(@Nonnull final UpdateFilterType filterType, @Nonnull final List<ActionGroup> filters) {
        this.filterType = filterType;
        this.filters = filters;
    }

    public static SyncFilter of(@Nonnull final UpdateFilterType filterType, @Nonnull final List<ActionGroup> filters) {
        return new SyncFilter(filterType, filters);
    }

    public UpdateFilterType getFilterType() {
        return filterType;
    }

    public List<ActionGroup> getFilters() {
        return filters;
    }
}
