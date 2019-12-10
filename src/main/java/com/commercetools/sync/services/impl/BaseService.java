package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.DraftBasedCreateCommand;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateCommand;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.queries.MetaModelQueryDsl;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @param <T> Resource Draft (e.g. {@link io.sphere.sdk.products.ProductDraft},
 *            {@link io.sphere.sdk.categories.CategoryDraft}, etc..
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link io.sphere.sdk.categories.Category}, etc..
 * @param <S> Subclass of {@link BaseSyncOptions}
 * @param <Q> Query (e.g. {@link io.sphere.sdk.products.queries.ProductQuery},
 *            {@link io.sphere.sdk.categories.queries.CategoryQuery}, etc..
 * @param <M> Query Model (e.g. {@link io.sphere.sdk.products.queries.ProductQueryModel},
 *            {@link io.sphere.sdk.categories.queries.CategoryQueryModel}, etc..
 * @param <E> Expansion Model (e.g. {@link io.sphere.sdk.products.expansion.ProductExpansionModel},
 *            {@link io.sphere.sdk.categories.expansion.CategoryExpansionModel}, etc..
 */
abstract class BaseService<T, U extends Resource<U> & WithKey, S extends BaseSyncOptions,
    Q extends MetaModelQueryDsl<U, Q, M, E>, M, E> {

    final S syncOptions;
    boolean isCached = false;
    final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    private static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;
    private static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

    BaseService(@Nonnull final S syncOptions) {
        this.syncOptions = syncOptions;
    }

    /**
     * Executes update request(s) on the {@code resource} with all the {@code updateActions} using the
     * {@code updateCommandFunction} while taking care of the CTP constraint of 500 update actions per request by
     * batching update actions into requests of 500 actions each.
     *
     * @param resource              The resource to update.
     * @param updateCommandFunction a {@link BiFunction} used to compute the update command required to update the
     *                              resource.
     * @param updateActions         the update actions to execute on the resource.
     * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an instance of
     *         the resource {@link U} after all the update actions have been executed.
     */
    @Nonnull
    CompletionStage<U> updateResource(
        @Nonnull final U resource,
        @Nonnull final BiFunction<U, List<? extends UpdateAction<U>>, UpdateCommand<U>> updateCommandFunction,
        @Nonnull final List<UpdateAction<U>> updateActions) {
        final List<List<UpdateAction<U>>> actionBatches = batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);
        return updateBatches(CompletableFuture.completedFuture(resource), updateCommandFunction, actionBatches);
    }

    /**
     * Given a list of update actions batches represented by a {@link List}&lt;{@link List}&gt; of {@link UpdateAction},
     * this method executes the update command, computed by {@code updateCommandFunction}, on each batch.
     *
     * @param result                in the first call of this method, this result is normally a completed
     *                              future containing the resource to update, it is then used within each iteration of
     *                              batch execution to have the latest resource (with version) once the previous batch
     *                              has finished execution.
     * @param updateCommandFunction a {@link BiFunction} used to compute the update command required to update the
     *                              resource.
     * @param batches               the batches of update actions to execute.
     * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an instance of
     *         the resource {@link U} after all the update actions in all batches have been executed.
     */
    @Nonnull
    private CompletionStage<U> updateBatches(
        @Nonnull final CompletionStage<U> result,
        @Nonnull final BiFunction<U, List<? extends UpdateAction<U>>, UpdateCommand<U>> updateCommandFunction,
        @Nonnull final List<List<UpdateAction<U>>> batches) {
        CompletionStage<U> resultStage = result;
        for (final List<UpdateAction<U>> batch : batches) {
            resultStage = resultStage.thenCompose(updatedProduct ->
                syncOptions.getCtpClient().execute(updateCommandFunction.apply(updatedProduct, batch)));
        }
        return resultStage;
    }

    /**
     * Given a resource draft of type {@code T}, this method attempts to create a resource {@code U} based on it in
     * the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty option and the error callback will be triggered in those cases:
     * <ul>
     * <li>the draft has a blank key</li>
     * <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created successfully on CTP, then the created resource's id and
     * key are cached and the method returns a {@link CompletionStage} in which the result of it's completion
     * contains an instance {@link Optional} of the resource which was created.
     *
     * @param draft         the resource draft to create a resource based off of.
     * @param keyMapper     a function to get the key from the supplied draft.
     * @param createCommand a function to get the create command using the supplied draft.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<U>> createResource(
        @Nonnull final T draft,
        @Nonnull final Function<T, String> keyMapper,
        @Nonnull final Function<T, DraftBasedCreateCommand<U, T>> createCommand) {

        final String draftKey = keyMapper.apply(draft);

        if (isBlank(draftKey)) {
            syncOptions.applyErrorCallback(format(CREATE_FAILED, draftKey, "Draft key is blank!"));
            return CompletableFuture.completedFuture(Optional.empty());
        } else {
            return syncOptions
                .getCtpClient()
                .execute(createCommand.apply(draft))
                .handle(((resource, exception) -> {
                    if (exception == null) {
                        keyToIdCache.put(draftKey, resource.getId());
                        return Optional.of(resource);
                    } else {
                        syncOptions.applyErrorCallback(
                            format(CREATE_FAILED, draftKey, exception.getMessage()), exception);
                        return Optional.empty();
                    }
                }));
        }

    }

    /**
     * Given a {@code key}, if it is blank (null/empty), a completed future with an empty optional is returned.
     * This method then checks if the cached map of resource keys -&gt; ids contains the key. If it does, then an
     * optional containing the mapped id is returned. If the cache doesn't contain the key; this method attempts to
     * fetch the id of the key from the CTP project, caches it and returns a
     * {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's completion
     * could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no resource
     * was found in the CTP project with this key.
     *
     * @param key the key by which a resource id should be fetched from the CTP project.
     * @param querySupplier supplies the query to fetch the resource with the given key.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         resource was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedResourceId(
        @Nullable final String key,
        @Nonnull final Supplier<Q> querySupplier) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.containsKey(key)) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return fetchAndCache(key, querySupplier);
    }

    private CompletionStage<Optional<String>> fetchAndCache(
        @Nullable final String key,
        @Nonnull final Supplier<Q> querySupplier) {

        final Consumer<List<U>> pageConsumer = page -> page.forEach(resource ->
            keyToIdCache.put(resource.getKey(), resource.getId()));


        return CtpQueryUtils
            .queryAll(syncOptions.getCtpClient(), querySupplier.get(), pageConsumer)
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    /**
     * Given a set of keys this method caches a mapping of the keys to ids of such keys only for the keys which are
     * not already in the cache.
     *
     * @param keys keys to cache.
     * @param keysQueryMapper function that accepts a set of keys which are not cached and maps it to a query object
     *                        representing the query to CTP on such keys.
     * @return a map of key to ids of the requested keys.
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds(
        @Nonnull final Set<String> keys,
        @Nonnull final Function<Set<String>, Q> keysQueryMapper) {

        final Set<String> keysNotCached = keys
            .stream()
            .filter(StringUtils::isNotBlank)
            .filter(key -> !keyToIdCache.containsKey(key))
            .collect(Collectors.toSet());

        if (keysNotCached.isEmpty()) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }

        final Consumer<List<U>> pageConsumer = page -> page.forEach(resource ->
            keyToIdCache.put(resource.getKey(), resource.getId()));

        return CtpQueryUtils
            .queryAll(syncOptions.getCtpClient(), keysQueryMapper.apply(keysNotCached), pageConsumer)
            .thenApply(result -> keyToIdCache);
    }

    /**
     * Given a {@link Set} of resource keys, this method fetches a set of all the resources, matching this given set of
     * keys in the CTP project, defined in an injected {@link SphereClient}. A mapping of the key to the id
     * of the fetched resources is persisted in an in-memory map.
     *
     * @param keys  set of state keys to fetch matching states by
     * @param querySupplier supplies the query to fetch the resources with the given keys.
     * @return {@link CompletionStage}&lt;{@link Set}&lt;{@code U}&gt;&gt; in which the result of it's completion
     *         contains a {@link Set} of all matching resources.
     */
    @Nonnull
    CompletionStage<Set<U>> fetchMatchingResources(
        @Nonnull final Set<String> keys,
        @Nonnull final Supplier<Q> querySupplier) {

        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        return CtpQueryUtils
            .queryAll(syncOptions.getCtpClient(), querySupplier.get(), Function.identity())
            .thenApply(fetchedResources -> fetchedResources
                .stream()
                .flatMap(List::stream)
                .peek(resource -> keyToIdCache.put(resource.getKey(), resource.getId()))
                .collect(Collectors.toSet()));
    }

    /**
     * Given a resource key, this method fetches a resource that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching resource an empty {@link Optional} will be
     * returned in the returned future. A mapping of the key to the id of the fetched category is persisted in an in
     * -memory map.
     *
     * @param key           the key of the resource to fetch
     * @param querySupplier supplies the query to fetch the resource with the given key.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@code T} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<U>> fetchResource(
        @Nullable final String key,
        @Nonnull final Supplier<Q> querySupplier) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return syncOptions
            .getCtpClient()
            .execute(querySupplier.get())
            .thenApply(pagedQueryResult -> pagedQueryResult
                .head()
                .map(resource -> {
                    keyToIdCache.put(key, resource.getId());
                    return resource;
                }));
    }

}