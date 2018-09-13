package com.commercetools.sync.types;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.types.utils.TypeSyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class syncs type drafts with the corresponding types in the CTP project.
 */
public class TypeSync extends BaseSync<TypeDraft, TypeSyncStatistics, TypeSyncOptions> {

    private static final String CTP_TYPE_FETCH_FAILED = "Failed to fetch existing types of keys '%s'.";
    private static final String CTP_TYPE_UPDATE_FAILED = "Failed to update type of key '%s'.";
    private static final String CTP_TYPE_CREATE_FAILED = "Failed to create type of key '%s'.";
    private static final String TYPE_DRAFT_HAS_NO_KEY = "Failed to process type draft without key.";
    private static final String TYPE_DRAFT_IS_NULL = "Failed to process null type draft.";

    private final TypeService typeService;

    public TypeSync(@Nonnull final TypeSyncOptions typeSyncOptions) {
        super(new TypeSyncStatistics(), typeSyncOptions);
        this.typeService = new TypeServiceImpl(typeSyncOptions);
    }

    public TypeSync(@Nonnull final TypeSyncOptions typeSyncOptions,
                    @Nonnull final TypeService typeService) {
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
    protected CompletionStage<TypeSyncStatistics> process(
            @Nonnull final List<TypeDraft> typeDrafts) {

        final List<List<TypeDraft>> batches = batchElements(typeDrafts, syncOptions.getBatchSize());

        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    @Override
    protected CompletionStage<TypeSyncStatistics> syncBatches(
            @Nonnull final List<List<TypeDraft>> batches,
            @Nonnull final CompletionStage<TypeSyncStatistics> result) {

        if (batches.isEmpty()) {
            return result;
        }

        final List<TypeDraft> firstBatch = batches.remove(0);
        return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
    }

    /**
     * Fetches existing {@link Type} objects from CTP project that correspond to passed {@code batch}.
     * Having existing product types fetched, {@code batch} is compared and synced with fetched objects by
     * {@link TypeSync#syncBatch(List, List)} function. When fetching existing types results in
     * an empty optional then {@code batch} isn't processed.
     *
     * @param batch batch of drafts that need to be synced
     * @return {@link CompletionStage} of {@link Void} that indicates method progress.
     */
    @Override
    protected CompletionStage<TypeSyncStatistics> processBatch(@Nonnull final List<TypeDraft> batch) {
        final List<TypeDraft> validTypeDrafts = batch.stream()
                                                                   .filter(this::validateDraft)
                                                                   .collect(toList());

        if (validTypeDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validTypeDrafts.stream().map(TypeDraft::getKey).collect(toSet());

            return fetchExistingTypes(keys)
                .thenCompose(oldTypes -> syncBatch(oldTypes, validTypeDrafts))
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
     * Given a set of type keys, fetches the corresponding types from CTP if they exist.
     *
     * @param keys the keys of the types that are wanted to be fetched.
     * @return a future which contains the list of types corresponding to the keys.
     */
    private CompletionStage<List<Type>> fetchExistingTypes(@Nonnull final Set<String> keys) {
        return typeService
                .fetchMatchingTypesByKeys(keys)
                .exceptionally(exception -> {
                    final String errorMessage = format(CTP_TYPE_FETCH_FAILED, keys);
                    handleError(errorMessage, exception, keys.size());

                    return emptyList();
                });
    }

    /**
     * Given a list of {@link Type} or {@link TypeDraft}, returns a map of keys to the
     * {@link Type}/{@link TypeDraft} instances.
     *
     * @param types list of {@link Type}/{@link TypeDraft}
     * @param <T>          a type that extends of {@link WithKey}.
     * @return the map of keys to {@link Type}/{@link TypeDraft} instances.
     */
    private <T extends WithKey> Map<String, T> getKeysProductTypeMap(@Nonnull final List<T> types) {
        return types.stream().collect(Collectors.toMap(WithKey::getKey, p -> p,
            (productTypeA, productTypeB) -> productTypeB));
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed product types to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed product types counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {

        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a list of type drafts, attempts to sync the drafts with the existing types in the CTP
     * project. The type and the draft are considered to match if they have the same key.
     *
     * @param oldTypes old types.
     * @param newTypes drafts that need to be synced.
     * @return a future which contains an empty result after execution of the update
     */
    private CompletionStage<TypeSyncStatistics> syncBatch(
            @Nonnull final List<Type> oldTypes,
            @Nonnull final List<TypeDraft> newTypes) {
        final Map<String, Type> oldTypeMap = getKeysProductTypeMap(oldTypes);

        return CompletableFuture.allOf(newTypes
            .stream()
            .map(newType -> {
                final Type oldType = oldTypeMap.get(newType.getKey());

                return ofNullable(oldType)
                    .map(type -> updateProductType(oldType, newType))
                    .orElseGet(() -> createType(newType));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new)).thenApply(result -> statistics);
    }

    /**
     * Given a type draft, issues a request to the CTP project to create a corresponding Type.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param typeDraft the type draft to create the product type from.
     * @return a future which contains an empty result after execution of the create.
     */
    private CompletionStage<Void> createType(@Nonnull final TypeDraft typeDraft) {
        return syncOptions.applyBeforeCreateCallBack(typeDraft)
                .map(typeService::createType)
                .map(creationFuture -> creationFuture
                        .thenAccept(createdType -> statistics.incrementCreated())
                        .exceptionally(exception -> {
                            final String errorMessage = format(CTP_TYPE_CREATE_FAILED,
                                    typeDraft.getKey());
                            handleError(errorMessage, exception, 1);

                            return null;
                        }))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    /**
     * Given an existing {@link Type} and a new {@link TypeDraft}, the method calculates all the
     * update actions required to synchronize the existing product type to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing product type, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param oldType existing type that could be updated.
     * @param newType draft containing data that could differ from data in {@code oldType}.
     * @return a future which contains an empty result after execution of the update.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> updateProductType(@Nonnull final Type oldType,
                                                    @Nonnull final TypeDraft newType) {

        final List<UpdateAction<Type>> updateActions = buildActions(oldType, newType, syncOptions);

        final List<UpdateAction<Type>> updateActionsAfterCallback = syncOptions.applyBeforeUpdateCallBack(
            updateActions,
            newType,
            oldType
        );

        if (!updateActionsAfterCallback.isEmpty()) {
            return typeService.updateType(oldType, updateActionsAfterCallback)
                    .thenAccept(updatedProductType -> statistics.incrementUpdated())
                    .exceptionally(exception -> {
                        final String errorMessage = format(CTP_TYPE_UPDATE_FAILED, newType.getKey());
                        handleError(errorMessage, exception, 1);

                        return null;
                    });
        }

        return completedFuture(null);
    }
}
