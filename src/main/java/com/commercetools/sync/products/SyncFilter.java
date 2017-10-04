package com.commercetools.sync.products;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Defines either a blacklist or a whitelist for filtering certain update action groups ({@link ActionGroup}).
 *
 * <p>The action groups can be a list of any of the values of the enum {@link ActionGroup}, namely:
 * <ul>
 * <li>NAME</li>
 * <li>DESCRIPTION</li>
 * <li>SLUG</li>
 * <li>SEARCHKEYWORDS</li>
 * <li>METATITLE</li>
 * <li>METADESCRIPTION</li>
 * <li>METAKEYWORDS</li>
 * <li>VARIANTS</li>
 * <li>ATTRIBUTES</li>
 * <li>PRICES</li>
 * <li>IMAGES</li>
 * <li>CATEGORIES</li>
 * </ul>
 *
 * <p>The {@code filterType} defines whether the list is to be blacklisted ({@link UpdateFilterType#BLACKLIST}) or
 * whitelisted ({@link UpdateFilterType#WHITELIST}). A blacklist means that <b>everything but</b> these action
 * groups will be synced. A whitelist means that <b>only</b> these action groups will be synced.
 */
public final class SyncFilter {

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

    @Nonnull
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
