package com.commercetools.sync.types;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import com.commercetools.sync.types.utils.TypeSyncUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
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
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class syncs type drafts with the corresponding types in the CTP project.
 */
public class TypeSync extends BaseSync<TypeDraft, TypeSyncStatistics, TypeSyncOptions> {

    private static final String CTP_TYPE_FETCH_FAILED = "Failed to fetch existing types with keys: '%s'.";
    private static final String CTP_TYPE_UPDATE_FAILED = "Failed to update type with key: '%s'. Reason: %s" ;
    private static final String TYPE_DRAFT_HAS_NO_KEY = "Failed to process type draft without key.";
    private static final String TYPE_DRAFT_IS_NULL = "Failed to process null type draft.";

    private final TypeService typeService;

    public TypeSync(@Nonnull final TypeSyncOptions typeSyncOptions) {
        this(typeSyncOptions, new TypeServiceImpl(typeSyncOptions));
    }

    /**
     * Takes a {@link TypeSyncOptions} and a {@link TypeService} instances to instantiate
     * a new {@link TypeSync} instance that could be used to sync type drafts in the CTP project specified
     * in the injected {@link TypeSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param typeSyncOptions the container of all the options of the sync process including the CTP project
     *                        client and/or configuration and other sync-specific options.
     * @param typeService     the type service which is responsible for fetching/caching the Types from the CTP
     *                        project.
     */
    TypeSync(@Nonnull final TypeSyncOptions typeSyncOptions, @Nonnull final TypeService typeService) {
        super(new TypeSyncStatistics(), typeSyncOptions);
        this.typeService = typeService;
    }

    /**
     * Iterates through the whole {@code typeDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link TypeSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param typeDrafts {@link List} of {@link TypeDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link TypeSyncStatistics} holding statistics of all sync
     *         processes performed by this sync instance.
     */
    @Override
    protected CompletionStage<TypeSyncStatistics> process(@Nonnull final List<TypeDraft> typeDrafts) {
        final List<List<TypeDraft>> batches = batchElements(typeDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    /**
     * This method first creates a new {@link Set} of valid {@link TypeDraft} elements. For more on the rules of
     * validation, check: {@link TypeSync#validateDraft(TypeDraft)}. Using the resulting set of
     * {@code validTypeDrafts}, the matching types in the target CTP project are fetched then the method
     * {@link TypeSync#syncBatch(Set, Set)} is called to perform the sync (<b>update</b> or <b>create</b>
     * requests accordingly) on the target project.
     *
     * <p> In case of error during of fetching of existing types, the error callback will be triggered.
     * And the sync process would stop for the given batch.
     * </p>
     *
     * @param batch batch of drafts that need to be synced
     * @return a {@link CompletionStage} containing an instance
     *         of {@link TypeSyncStatistics} which contains information about the result of syncing the supplied
     *         batch to the target project.
     */
    @Override
    protected CompletionStage<TypeSyncStatistics> processBatch(@Nonnull final List<TypeDraft> batch) {

        final Set<TypeDraft> validTypeDrafts = batch.stream().filter(this::validateDraft).collect(toSet());

        if (validTypeDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validTypeDrafts.stream().map(TypeDraft::getKey).collect(toSet());

            return typeService
                .fetchMatchingTypesByKeys(keys)
                .handle(ImmutablePair::new)
                .thenCompose(fetchResponse -> {
                    final Set<Type> fetchedTypes = fetchResponse.getKey();
                    final Throwable exception = fetchResponse.getValue();

                    if (exception != null) {
                        final String errorMessage = format(CTP_TYPE_FETCH_FAILED, keys);
                        handleError(errorMessage, exception, keys.size());
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return syncBatch(fetchedTypes, validTypeDrafts);
                    }
                })
                .thenApply(ignored -> {
                    statistics.incrementProcessed(batch.size());
                    return statistics;
                });
        }
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}. Otherwise handles an error
     * and returns {@code false}. A valid draft is a {@link TypeDraft} object that is not {@code null} and its
     * key is not empty.
     *
     * @param draft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final TypeDraft draft) {
        if (draft == null) {
            handleError(TYPE_DRAFT_IS_NULL, null, 1);
        } else if (isBlank(draft.getKey())) {
            handleError(TYPE_DRAFT_HAS_NO_KEY, null, 1);
        } else {
            return true;
        }

        return false;
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed types to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed types counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a set of type drafts, attempts to sync the drafts with the existing types in the CTP
     * project. The type and the draft are considered to match if they have the same key.
     *
     * @param oldTypes old types.
     * @param newTypes drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
            @Nonnull final Set<Type> oldTypes,
            @Nonnull final Set<TypeDraft> newTypes) {

        final Map<String, Type> oldTypeMap = oldTypes.stream().collect(toMap(Type::getKey, identity()));

        return CompletableFuture.allOf(newTypes
                .stream()
                .map(newType -> {
                    final Type oldType = oldTypeMap.get(newType.getKey());

                    return ofNullable(oldType)
                        .map(type -> buildActionsAndUpdate(oldType, newType))
                        .orElseGet(() -> applyCallbackAndCreate(newType));
                })
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new));
    }

    /**
     * Given a type draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding Type.
     *
     * @param typeDraft the type draft to create the type from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Optional<Type>> applyCallbackAndCreate(
        @Nonnull final TypeDraft typeDraft) {

        return syncOptions
            .applyBeforeCreateCallBack(typeDraft)
            .map(draft -> typeService
                .createType(draft)
                .thenApply(typeOptional -> {
                    if (typeOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                    return typeOptional;
                })
            )
            .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Optional<Type>> buildActionsAndUpdate(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType) {

        final List<UpdateAction<Type>> updateActions = TypeSyncUtils.buildActions(oldType, newType, syncOptions);

        final List<UpdateAction<Type>> updateActionsAfterCallback =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newType, oldType);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateType(oldType, newType, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    /**
     * Given an existing {@link Type} and a new {@link TypeDraft}, the method calculates all the
     * update actions required to synchronize the existing type to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing type, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP,the error handling method
     * is called.
     *
     * @param oldType       existing type that could be updated.
     * @param newType       draft containing data that could differ from data in {@code oldType}.
     * @param updateActions the update actions to update the {@link Type} with.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<Type>> updateType(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType,
        @Nonnull final List<UpdateAction<Type>> updateActions) {

        return typeService
            .updateType(oldType, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final Type updatedType = updateResponse.getKey();
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldType, newType),
                        () -> {
                            final String errorMessage =
                                format(CTP_TYPE_UPDATE_FAILED, newType.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return CompletableFuture.completedFuture(Optional.empty());
                        });
                } else {
                    statistics.incrementUpdated();
                    return CompletableFuture.completedFuture(Optional.of(updatedType));
                }
            });
    }

    private CompletionStage<Optional<Type>> fetchAndUpdate(
        @Nonnull final Type oldType,
        @Nonnull final TypeDraft newType) {

        final String key = oldType.getKey();
        return typeService
            .fetchType(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<Type> fetchedTypeOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_TYPE_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedTypeOptional
                    .map(fetchedType -> buildActionsAndUpdate(fetchedType, newType))
                    .orElseGet(() -> {
                        final String errorMessage =
                            format(CTP_TYPE_UPDATE_FAILED, key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, 1);
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }
}
