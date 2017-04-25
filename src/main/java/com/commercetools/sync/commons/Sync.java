package com.commercetools.sync.commons;


import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.models.Resource;

import javax.annotation.Nonnull;
import java.util.List;

public interface Sync<T, S extends Resource<S>> {
    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * @param resourceDrafts the list of new resources as drafts.
     */
    void syncDrafts(@Nonnull final List<T> resourceDrafts);

    /**
     * Given a list of resources (e.g. categories, products, etc..). This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * @param resources the list of new resources.
     */
    void sync(@Nonnull final List<S> resources);

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
