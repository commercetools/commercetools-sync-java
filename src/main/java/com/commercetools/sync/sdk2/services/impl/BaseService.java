package com.commercetools.sync.sdk2.services.impl;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import io.sphere.sdk.commands.UpdateCommand;
import io.sphere.sdk.models.ResourceView;
import org.apache.commons.lang3.StringUtils;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

/**
 * @param <T> Resource Draft (e.g. {@link io.sphere.sdk.products.ProductDraft}, {@link
 *     io.sphere.sdk.categories.CategoryDraft}, etc..
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link
 *     io.sphere.sdk.categories.Category}, etc..
 * @param <UQ> Resource returned by the query Q (e.g. {@link
 *     io.sphere.sdk.products.ProductProjection})
 * @param <S> Subclass of {@link com.commercetools.sync.commons.BaseSyncOptions}
 * @param <M> Query Model (e.g. {@link io.sphere.sdk.products.queries.ProductQueryModel}, {@link
 *     io.sphere.sdk.categories.queries.CategoryQueryModel}, etc..
 * @param <E> Expansion Model (e.g. {@link io.sphere.sdk.products.expansion.ProductExpansionModel},
 *     {@link io.sphere.sdk.categories.expansion.CategoryExpansionModel}, etc..
 */
abstract class BaseService<
    T,
    U extends ResourceView<U, U>,
    UQ extends ResourceView<UQ, U>,
    S extends BaseSyncOptions,
    M,
    E> {

  final S syncOptions;
  protected final Cache<String, String> keyToIdCache;

  protected static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;
  static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

  /*
   * To be more practical, considering 41 characters as an average for key and sku fields
   * (key and sku field doesn't have limit except for ProductType(256)) We chunk them in 250
   * (keys or sku) we will have a query around 11.000 characters(also considered some
   * conservative space for headers). Above this size it could return - Error 414 (Request-URI Too Large)
   */
  static final int CHUNK_SIZE = 250;

  BaseService(@Nonnull final S syncOptions) {
    this.syncOptions = syncOptions;
    this.keyToIdCache =
        Caffeine.newBuilder()
            .maximumSize(syncOptions.getCacheSize())
            .executor(Runnable::run)
            .build();
  }

  /**
   * Given a set of keys this method collects all keys which aren't already contained in the cache
   * {@code keyToIdCache}
   *
   * @param keys {@link Set} of keys
   * @return a {@link Set} of keys which aren't already contained in the cache or empty
   */
  @Nonnull
  protected Set<String> getKeysNotCached(@Nonnull final Set<String> keys) {
    return keys.stream()
        .filter(StringUtils::isNotBlank)
        .filter(key -> !keyToIdCache.asMap().containsKey(key))
        .collect(Collectors.toSet());
  }

  /**
   * Executes update request(s) on the {@code resource} with all the {@code updateActions} using the
   * {@code updateCommandFunction} while taking care of the CTP constraint of 500 update actions per
   * request by batching update actions into requests of 500 actions each.
   *
   * @param resource The resource to update.
   * @param updateCommandFunction a {@link BiFunction} used to compute the update command required
   *     to update the resource.
   * @param updateActions the update actions to execute on the resource.
   * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an
   *     instance of the resource {@link U} after all the update actions have been executed.
   */
  @Nonnull
  CompletionStage<U> updateResource(
      @Nonnull final U resource,
      @Nonnull
      final BiFunction<U, List<? extends ResourceUpdateAction>, ? extends ResourceUpdateAction>
          updateCommandFunction,
      @Nonnull final List<ResourceUpdateAction> updateActions) {

    final List<List<ResourceUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);
    return updateBatches(
        CompletableFuture.completedFuture(resource), updateCommandFunction, actionBatches);
  }

  /**
   * Given a list of update actions batches represented by a {@link List}&lt;{@link List}&gt; of
   * {@link ResourceUpdateAction}, this method executes the update command, computed by {@code
   * updateCommandFunction}, on each batch.
   *
   * @param result in the first call of this method, this result is normally a completed future
   *     containing the resource to update, it is then used within each iteration of batch execution
   *     to have the latest resource (with version) once the previous batch has finished execution.
   * @param updateCommandFunction a {@link BiFunction} used to compute the update command required
   *     to update the resource.
   * @param batches the batches of update actions to execute.
   * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an
   *     instance of the resource {@link U} after all the update actions in all batches have been
   *     executed.
   */
  @Nonnull
  private CompletionStage<U> updateBatches(
      @Nonnull final CompletionStage<U> result,
      @Nonnull
      final BiFunction<U, List<? extends ResourceUpdateAction>, ? extends ResourceUpdateAction>
          updateCommandFunction,
      @Nonnull final List<List<ResourceUpdateAction>> batches) {

    CompletionStage<U> resultStage = result;
    for (final List<ResourceUpdateAction> batch : batches) {
      resultStage =
          resultStage.thenCompose(
              updatedProduct ->
                  syncOptions
                      .getCtpClient()
                      .post(updateCommandFunction.apply(updatedProduct, batch))
                      .execute(updateCommandFunction.apply(updatedProduct, batch)));
    }
    return resultStage;
  }
}
