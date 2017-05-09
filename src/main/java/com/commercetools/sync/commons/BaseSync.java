package com.commercetools.sync.commons;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.models.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

import static java.lang.String.format;


public abstract class BaseSync<T, S extends Resource<S>, U extends BaseSyncStatistics, V extends BaseSyncOptions> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySync.class);
    protected final U statistics;
    protected final V syncOptions;

    protected BaseSync(@Nonnull final U statistics, @Nonnull final V syncOptions) {
        this.statistics = statistics;
        this.syncOptions = syncOptions;
    }

    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * @param resourceDrafts the list of new resources as drafts.
     */
    protected abstract void processDrafts(@Nonnull final List<T> resourceDrafts);


    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * <p>The time before and after the actual sync process starts is recorded in the {@link BaseSyncStatistics}
     * container so that the total processing time is computed in the statistics.
     *
     * @param resourceDrafts the list of new resources as drafts.
     */
    public void syncDrafts(@Nonnull final List<T> resourceDrafts) {
        LOGGER.info(format("About to sync %d drafts into CTP project with key '%s'.", resourceDrafts.size(),
            this.syncOptions.getCtpClient().getClientConfig().getProjectKey()));
        this.statistics.startTimer();
        this.processDrafts(resourceDrafts);
        this.statistics.calculateProcessingTime();
    }

    /**
     * Given a list of resources (e.g. categories, products, etc..). This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * <p>The time before and after the actual sync process starts is recorded in the {@link BaseSyncStatistics}
     * container so that the total processing time is computed in the statistics.
     *
     * @param resources the list of new resources.
     */
    public void sync(@Nonnull final List<S> resources) {
        LOGGER.info(format("About to sync %d resources into CTP project with key '%s'.", resources.size(),
            this.syncOptions.getCtpClient().getClientConfig().getProjectKey()));
        this.statistics.startTimer();
        this.process(resources);
        this.statistics.calculateProcessingTime();
    }

    /**
     * Given a list of resources (e.g. categories, products, etc..). This method compares each new resource in this
     * list with it's corresponding old resource in a given CTP project, and in turn it either issues update actions on
     * the existing resource if it exists or create it if it doesn't.
     *
     * @param resources the list of new resources.
     */
    protected abstract void process(@Nonnull final List<S> resources);

    /**
     * Returns an instance of type U which is a subclass of {@link BaseSyncStatistics} containing all the stats of the
     * sync process; which includes a report message, the total number of update, created, failed, processed resources
     * and the processing time of the sync in different time units and in a human readable format.
     *
     * @return a statistics object for the sync process.
     */
    @Nonnull
    public U getStatistics() {
        return this.statistics;
    }
}
