package com.commercetools.sync.commons;


import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;
import java.util.List;

public interface Sync {
    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * @param resourceDrafts the list of new resources as drafts.
     * @param <T>            the type of the resource draft. For example: CategoryDraft, ProductDraft, etc..
     */
    <T> void syncDrafts(@Nonnull final List<T> resourceDrafts);

    /**
     * Given a list of resources (e.g. categories, products, etc..). This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * @param resources the list of new resources.
     * @param <T>       the type of the resource. For example: Category, Product, etc..
     */
    <T> void sync(@Nonnull final List<T> resources);

    /**
     * Builds a {@link BaseSyncStatistics} object containing all the stats of the sync process; which includes a report
     * message, the total number of update, created, failed, processed resources and the processing time of the sync in
     * different time units and in a human readable format.
     *
     * @return a statistics object for the sync process.
     */
    @Nonnull
    BaseSyncStatistics getStatistics();
}
