package com.commercetools.sync.customobjects;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.customobjects.helpers.CustomObjectBatchValidator;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.customobjects.utils.CustomObjectSyncUtils;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.impl.CustomObjectServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * This class syncs custom object drafts with the corresponding custom objects in the CTP project.
 */
public class CustomObjectSync extends BaseSync<CustomObjectDraft<JsonNode>,
    CustomObjectSyncStatistics, CustomObjectSyncOptions> {

    private static final String CTP_CUSTOM_OBJECT_FETCH_FAILED =
        "Failed to fetch existing custom objects with keys: '%s'.";
    private static final String CTP_CUSTOM_OBJECT_UPDATE_FAILED =
        "Failed to update custom object with key: '%s'. Reason: %s";
    private static final String CTP_CUSTOM_OBJECT_CREATE_FAILED =
        "Failed to create custom object with key: '%s'. Reason: %s";

    private final CustomObjectService customObjectService;
    private final CustomObjectBatchValidator batchValidator;

    public CustomObjectSync(@Nonnull final CustomObjectSyncOptions syncOptions) {
        this(syncOptions, new CustomObjectServiceImpl(syncOptions));
    }

    /**
     * Takes a {@link CustomObjectSyncOptions} and a {@link CustomObjectService} instances to instantiate
     * a new {@link CustomObjectSync} instance that could be used to sync customObject drafts in the CTP project
     * specified in the injected {@link CustomObjectSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param syncOptions         the container of all the options of the sync process including the CTP project
     *                            client and/or configuration and other sync-specific options.
     * @param customObjectService the custom object service which is responsible for fetching/caching the
     */
    CustomObjectSync(
        @Nonnull final CustomObjectSyncOptions syncOptions,
        @Nonnull final CustomObjectService customObjectService) {

        super(new CustomObjectSyncStatistics(), syncOptions);
        this.customObjectService = customObjectService;
        this.batchValidator = new CustomObjectBatchValidator(getSyncOptions(), getStatistics());
    }

    /**
     * Iterates through the whole {@code customObjectDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link CustomObjectSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param customObjectDrafts {@link List} of {@link CustomObjectDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link CustomObjectSyncStatistics} holding statistics of all sync
     *     processes performed by this sync instance.
     */
    protected CompletionStage<CustomObjectSyncStatistics> process(
        @Nonnull final List<CustomObjectDraft<JsonNode>> customObjectDrafts) {
        final List<List<CustomObjectDraft<JsonNode>>> batches = batchElements(
            customObjectDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    /**
     * This method first creates a new {@link Set} of valid {@link CustomObjectDraft} elements. For more on the rules of
     * validation, check: {@link CustomObjectBatchValidator#validateAndCollectReferencedKeys(List)}. Using the resulting
     * set of {@code validCustomObjectDrafts}, the matching custom objects in the target CTP project are fetched then
     * the method {@link CustomObjectSync#syncBatch(Set, Set)} is called to perform the sync (<b>update</b> or
     * <b>create</b> requests accordingly) on the target project.
     *
     * <p> In case of error during of fetching of existing custom objects, the error callback will be triggered.
     * And the sync process would stop for the given batch.
     * </p>
     *
     * @param batch batch of drafts that need to be synced
     * @return a {@link CompletionStage} containing an instance
     *      of {@link CustomObjectSyncStatistics} which contains information about the result of syncing the supplied
     *      batch to the target project.
     */
    protected CompletionStage<CustomObjectSyncStatistics> processBatch(
        @Nonnull final List<CustomObjectDraft<JsonNode>> batch) {

        final ImmutablePair<Set<CustomObjectDraft<JsonNode>>, Set<CustomObjectCompositeIdentifier>> result =
            batchValidator.validateAndCollectReferencedKeys(batch);

        final Set<CustomObjectDraft<JsonNode>> validDrafts = result.getLeft();
        if (validDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return CompletableFuture.completedFuture(statistics);
        }
        final Set<CustomObjectCompositeIdentifier> validIdentifiers = result.getRight();

        return customObjectService
            .fetchMatchingCustomObjects(validIdentifiers)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Set<CustomObject<JsonNode>> fetchedCustomObjects = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_CUSTOM_OBJECT_FETCH_FAILED, validIdentifiers);
                    handleError(errorMessage, exception, validIdentifiers.size());
                    return CompletableFuture.completedFuture(null);
                } else {
                    return syncBatch(fetchedCustomObjects, validDrafts);
                }
            })
            .thenApply(ignored -> {
                statistics.incrementProcessed(batch.size());
                return statistics;
            });
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed custom objects to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed custom objects counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nonnull final Throwable exception,
                             final int failedTimes) {
        SyncException syncException = new SyncException(errorMessage, exception);
        syncOptions.applyErrorCallback(syncException);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed custom objects to sync.
     *
     * @param errorMessage         The error message describing the reason(s) of failure.
     * @param exception            The exception that called caused the failure, if any.
     * @param failedTimes          The number of times that the failed custom objects counter is incremented.
     * @param oldCustomObject      existing custom object that could be updated.
     * @param newCustomObjectDraft draft containing data that could differ from data in {@code oldCustomObject}.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes, @Nullable final CustomObject<JsonNode> oldCustomObject,
                             @Nullable final CustomObjectDraft<JsonNode> newCustomObjectDraft) {

        SyncException syncException = exception != null ? new SyncException(errorMessage, exception)
            : new SyncException(errorMessage);
        syncOptions.applyErrorCallback(syncException, oldCustomObject, newCustomObjectDraft, null);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a set of custom object drafts, attempts to sync the drafts with the existing custom objects in the CTP
     * project. The custom object and the draft are considered to match if they have the same key and container.
     *
     * @param oldCustomObjects      old custom objects.
     * @param newCustomObjectDrafts drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<CustomObject<JsonNode>> oldCustomObjects,
        @Nonnull final Set<CustomObjectDraft<JsonNode>> newCustomObjectDrafts) {

        final Map<String, CustomObject<JsonNode>> oldCustomObjectMap =
            oldCustomObjects.stream().collect(
                toMap(customObject -> CustomObjectCompositeIdentifier.of(
                    customObject.getKey(), customObject.getContainer()).toString(), identity()));

        return CompletableFuture.allOf(newCustomObjectDrafts
            .stream()
            .map(newCustomObjectDraft -> {
                final CustomObject<JsonNode> oldCustomObject = oldCustomObjectMap.get(
                    CustomObjectCompositeIdentifier.of(newCustomObjectDraft).toString());
                return ofNullable(oldCustomObject)
                    .map(customObject -> updateCustomObject(oldCustomObject, newCustomObjectDraft))
                    .orElseGet(() -> applyCallbackAndCreate(newCustomObjectDraft));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    /**
     * Given a custom object draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding CustomObject.
     *
     * @param customObjectDraft the custom object draft to create the custom object from.
     * @return a {@link CompletionStage} which contains created custom object after success execution of the create.
     *      Otherwise it contains an empty result in case of failure.
     */
    @Nonnull
    private CompletionStage<Optional<CustomObject<JsonNode>>> applyCallbackAndCreate(
        @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

        return syncOptions
            .applyBeforeCreateCallback(customObjectDraft)
                .map(draft -> customObjectService
                    .upsertCustomObject(draft)
                    .thenApply(customObjectOptional -> {
                        if (customObjectOptional.isPresent()) {
                            statistics.incrementCreated();
                        } else {
                            statistics.incrementFailed();
                        }
                        return customObjectOptional;
                    }).exceptionally(sphereException -> {
                        final String errorMessage =
                            format(CTP_CUSTOM_OBJECT_CREATE_FAILED,
                                CustomObjectCompositeIdentifier.of(customObjectDraft).toString(),
                                sphereException.getMessage());
                        handleError(errorMessage, sphereException, 1,
                            null, customObjectDraft);
                        return Optional.empty();
                    })
                ).orElse(completedFuture(Optional.empty()));
    }

    /**
     * Given an existing {@link CustomObject} and a new {@link CustomObjectDraft}, the method first checks whether
     * existing {@link CustomObject} and a new {@link CustomObjectDraft} are identical. If so, the method aborts update
     * the new draft and return an empty result, otherwise a request is made to CTP to update the existing custom
     * object.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP,the error handling method
     * is called.
     *
     * @param oldCustomObject existing custom object that could be updated.
     * @param newCustomObject draft containing data that could differ from data in {@code oldCustomObject}.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<CustomObject<JsonNode>>> updateCustomObject(
        @Nonnull final CustomObject<JsonNode> oldCustomObject,
        @Nonnull final CustomObjectDraft<JsonNode> newCustomObject) {

        if (!CustomObjectSyncUtils.hasIdenticalValue(oldCustomObject, newCustomObject)) {
            return customObjectService
                .upsertCustomObject(newCustomObject)
                .handle(ImmutablePair::new)
                .thenCompose(updatedResponseEntry -> {
                    final Optional<CustomObject<JsonNode>> updateCustomObjectOptional = updatedResponseEntry.getKey();
                    final Throwable sphereException = updatedResponseEntry.getValue();
                    if (sphereException != null) {
                        return executeSupplierIfConcurrentModificationException(sphereException,
                            () -> fetchAndUpdate(oldCustomObject, newCustomObject),
                            () -> {
                                final String errorMessage =
                                    format(CTP_CUSTOM_OBJECT_UPDATE_FAILED,
                                        CustomObjectCompositeIdentifier.of(newCustomObject).toString(),
                                        sphereException.getMessage());
                                handleError(errorMessage, sphereException, 1,
                                    oldCustomObject, newCustomObject);
                                return CompletableFuture.completedFuture(Optional.empty());
                            });
                    } else {
                        statistics.incrementUpdated();
                        return CompletableFuture.completedFuture(Optional.of(updateCustomObjectOptional.get()));
                    }

                });
        }
        return completedFuture(Optional.empty());
    }

    @Nonnull
    private CompletionStage<Optional<CustomObject<JsonNode>>> fetchAndUpdate(
        @Nonnull final CustomObject<JsonNode> oldCustomObject,
        @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

        final CustomObjectCompositeIdentifier identifier = CustomObjectCompositeIdentifier.of(oldCustomObject);

        return customObjectService
            .fetchCustomObject(identifier)
            .handle(ImmutablePair::new)
            .thenCompose(fetchedResponseEntry -> {
                final Optional<CustomObject<JsonNode>> fetchedCustomObjectOptional = fetchedResponseEntry.getKey();
                final Throwable exception = fetchedResponseEntry.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_CUSTOM_OBJECT_UPDATE_FAILED, identifier.toString(),
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1, oldCustomObject, customObjectDraft);
                    return CompletableFuture.completedFuture(Optional.empty());
                }
                return fetchedCustomObjectOptional
                    .map(fetchedCustomObject -> updateCustomObject(fetchedCustomObject, customObjectDraft))
                    .orElseGet(() -> {
                        final String errorMessage =
                            format(CTP_CUSTOM_OBJECT_UPDATE_FAILED, identifier.toString(),
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, 1,
                            oldCustomObject, customObjectDraft);
                        return CompletableFuture.completedFuture(null);
                    });

            });
    }
}
