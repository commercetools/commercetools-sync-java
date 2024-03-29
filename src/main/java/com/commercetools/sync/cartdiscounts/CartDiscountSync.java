package com.commercetools.sync.cartdiscounts;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountBatchValidator;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountReferenceResolver;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.cartdiscounts.utils.CartDiscountSyncUtils;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.CartDiscountService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CartDiscountServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * This class syncs cart discount drafts with the corresponding cart discounts in the CTP project.
 */
public class CartDiscountSync
    extends BaseSync<
        CartDiscount,
        CartDiscountDraft,
        CartDiscountUpdateAction,
        CartDiscountSyncStatistics,
        CartDiscountSyncOptions> {

  private static final String CTP_CART_DISCOUNT_FETCH_FAILED =
      "Failed to fetch existing cart discounts with keys: '%s'.";
  private static final String CTP_CART_DISCOUNT_UPDATE_FAILED =
      "Failed to update cart discount with key: '%s'. Reason: %s";
  private static final String FAILED_TO_PROCESS =
      "Failed to process the CartDiscountDraft with key:'%s'. Reason: %s";

  private final CartDiscountService cartDiscountService;
  private final TypeService typeService;
  private final CartDiscountReferenceResolver referenceResolver;
  private final CartDiscountBatchValidator batchValidator;

  /**
   * Takes a {@link CartDiscountSyncOptions} to instantiate a new {@link CartDiscountSync} instance
   * that could be used to sync cart discount drafts in the CTP project specified in the injected
   * {@link CartDiscountSyncOptions} instance.
   *
   * @param cartDiscountSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   */
  public CartDiscountSync(@Nonnull final CartDiscountSyncOptions cartDiscountSyncOptions) {
    this(
        cartDiscountSyncOptions,
        new TypeServiceImpl(cartDiscountSyncOptions),
        new CartDiscountServiceImpl(cartDiscountSyncOptions));
  }

  /**
   * Takes a {@link CartDiscountSyncOptions} and a {@link CartDiscountService} instances to
   * instantiate a new {@link CartDiscountSync} instance that could be used to sync cart discount
   * drafts in the CTP project specified in the injected {@link CartDiscountSyncOptions} instance.
   *
   * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and
   * passed to.
   *
   * @param cartDiscountSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   * @param typeService the type service which is responsible for fetching/caching the Types from
   *     the CTP project.
   * @param cartDiscountService the cart discount service which is responsible for fetching/caching
   *     the CartDiscounts from the CTP project.
   */
  CartDiscountSync(
      @Nonnull final CartDiscountSyncOptions cartDiscountSyncOptions,
      @Nonnull final TypeService typeService,
      @Nonnull final CartDiscountService cartDiscountService) {
    super(new CartDiscountSyncStatistics(), cartDiscountSyncOptions);
    this.cartDiscountService = cartDiscountService;
    this.typeService = typeService;
    this.referenceResolver = new CartDiscountReferenceResolver(getSyncOptions(), typeService);
    this.batchValidator = new CartDiscountBatchValidator(getSyncOptions(), getStatistics());
  }

  /**
   * Iterates through the whole {@code cartDiscountDrafts} list and accumulates its valid drafts to
   * batches. Every batch is then processed by {@link
   * CartDiscountSync#processBatch(java.util.List)}.
   *
   * <p><strong>Inherited doc:</strong> {@inheritDoc}
   *
   * @param cartDiscountDrafts {@link java.util.List} of {@link CartDiscountDraft}'s that would be
   *     synced into CTP project.
   * @return {@link java.util.concurrent.CompletionStage} with {@link CartDiscountSyncStatistics}
   *     holding statistics of all sync processes performed by this sync instance.
   */
  @Override
  protected CompletionStage<CartDiscountSyncStatistics> process(
      @Nonnull final List<CartDiscountDraft> cartDiscountDrafts) {

    final List<List<CartDiscountDraft>> batches =
        batchElements(cartDiscountDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  @Override
  protected CompletionStage<CartDiscountSyncStatistics> processBatch(
      @Nonnull final List<CartDiscountDraft> batch) {

    final ImmutablePair<Set<CartDiscountDraft>, Set<String>> result =
        batchValidator.validateAndCollectReferencedKeys(batch);

    final Set<CartDiscountDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return completedFuture(statistics);
    }

    return typeService
        .cacheKeysToIds(result.getRight())
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getValue();
              if (cachingException != null) {
                handleError(
                    "Failed to build a cache of keys to ids.",
                    cachingException,
                    null,
                    null,
                    null,
                    validDrafts.size());
                return CompletableFuture.completedFuture(null);
              }

              final Set<String> keys =
                  validDrafts.stream().map(CartDiscountDraft::getKey).collect(toSet());

              return cartDiscountService
                  .fetchMatchingCartDiscountsByKeys(keys)
                  .handle(ImmutablePair::new)
                  .thenCompose(
                      fetchResponse -> {
                        final Set<CartDiscount> fetchedCartDiscounts = fetchResponse.getKey();
                        final Throwable exception = fetchResponse.getValue();

                        if (exception != null) {
                          final String errorMessage = format(CTP_CART_DISCOUNT_FETCH_FAILED, keys);
                          handleError(errorMessage, exception, null, null, null, keys.size());
                          return CompletableFuture.completedFuture(null);
                        } else {
                          return syncBatch(fetchedCartDiscounts, validDrafts);
                        }
                      });
            })
        .thenApply(
            ignoredResult -> {
              statistics.incrementProcessed(batch.size());
              return statistics;
            });
  }

  /**
   * Given a set of cart discount drafts, attempts to sync the drafts with the existing cart
   * discounts in the CTP project. The cart discount and the draft are considered to match if they
   * have the same key.
   *
   * @param oldCartDiscounts old cart discounts.
   * @param newCartDiscounts drafts that need to be synced.
   * @return a {@link java.util.concurrent.CompletionStage} which contains an empty result after
   *     execution of the update
   */
  @Nonnull
  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<CartDiscount> oldCartDiscounts,
      @Nonnull final Set<CartDiscountDraft> newCartDiscounts) {

    final Map<String, CartDiscount> oldCartDiscountMap =
        oldCartDiscounts.stream().collect(toMap(CartDiscount::getKey, identity()));

    return CompletableFuture.allOf(
        newCartDiscounts.stream()
            .map(
                newCartDiscount ->
                    referenceResolver
                        .resolveReferences(newCartDiscount)
                        .thenCompose(resolvedDraft -> syncDraft(oldCartDiscountMap, resolvedDraft))
                        .exceptionally(
                            completionException -> {
                              final String errorMessage =
                                  format(
                                      FAILED_TO_PROCESS,
                                      newCartDiscount.getKey(),
                                      completionException.getMessage());
                              handleError(
                                  errorMessage,
                                  completionException,
                                  null,
                                  newCartDiscount,
                                  null,
                                  1);
                              return null;
                            }))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  @Nonnull
  private CompletionStage<Void> syncDraft(
      @Nonnull final Map<String, CartDiscount> oldCartDiscountMap,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    final CartDiscount oldCartDiscount = oldCartDiscountMap.get(newCartDiscount.getKey());

    return ofNullable(oldCartDiscount)
        .map(cartDiscount -> buildActionsAndUpdate(oldCartDiscount, newCartDiscount))
        .orElseGet(() -> applyCallbackAndCreate(newCartDiscount));
  }

  @Nonnull
  private CompletionStage<Void> buildActionsAndUpdate(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    final List<CartDiscountUpdateAction> updateActions =
        CartDiscountSyncUtils.buildActions(oldCartDiscount, newCartDiscount, syncOptions);

    final List<CartDiscountUpdateAction> updateActionsAfterCallback =
        syncOptions.applyBeforeUpdateCallback(updateActions, newCartDiscount, oldCartDiscount);

    if (!updateActionsAfterCallback.isEmpty()) {
      return updateCartDiscount(oldCartDiscount, newCartDiscount, updateActionsAfterCallback);
    }

    return completedFuture(null);
  }

  /**
   * Given a cart discount draft, this method applies the beforeCreateCallback and then issues a
   * create request to the CTP project to create the corresponding CartDiscount.
   *
   * @param cartDiscountDraft the cart discount draft to create the cart discount from.
   * @return a {@link java.util.concurrent.CompletionStage} which contains an empty result after
   *     execution of the create.
   */
  @Nonnull
  private CompletionStage<Void> applyCallbackAndCreate(
      @Nonnull final CartDiscountDraft cartDiscountDraft) {

    return syncOptions
        .applyBeforeCreateCallback(cartDiscountDraft)
        .map(
            draft ->
                cartDiscountService
                    .createCartDiscount(draft)
                    .thenAccept(
                        cartDiscountOptional -> {
                          if (cartDiscountOptional.isPresent()) {
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                        }))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  /**
   * Given an existing {@link CartDiscount} and a new {@link CartDiscountDraft}, the method
   * calculates all the update actions required to synchronize the existing cart discount to be the
   * same as the new one. If there are update actions found, a request is made to CTP to update the
   * existing cart discount, otherwise it doesn't issue a request.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,the
   * error handling method is called.
   *
   * @param oldCartDiscount existing cart discount that could be updated.
   * @param newCartDiscount draft containing data that could differ from data in {@code
   *     oldCartDiscount}.
   * @param updateActions the update actions to update the {@link CartDiscount} with.
   * @return a {@link java.util.concurrent.CompletionStage} which contains an empty result after
   *     execution of the update.
   */
  @Nonnull
  private CompletionStage<Void> updateCartDiscount(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount,
      @Nonnull final List<CartDiscountUpdateAction> updateActions) {

    return cartDiscountService
        .updateCartDiscount(oldCartDiscount, updateActions)
        .handle(ImmutablePair::new)
        .thenCompose(
            updateResponse -> {
              final Throwable throwable = updateResponse.getValue();
              if (throwable != null) {
                return executeSupplierIfConcurrentModificationException(
                    throwable,
                    () -> fetchAndUpdate(oldCartDiscount, newCartDiscount),
                    () -> {
                      final String errorMessage =
                          format(
                              CTP_CART_DISCOUNT_UPDATE_FAILED,
                              newCartDiscount.getKey(),
                              throwable.getMessage());
                      handleError(
                          errorMessage,
                          throwable,
                          oldCartDiscount,
                          newCartDiscount,
                          updateActions,
                          1);
                      return CompletableFuture.completedFuture(null);
                    });
              } else {
                statistics.incrementUpdated();
                return CompletableFuture.completedFuture(null);
              }
            });
  }

  @Nonnull
  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final CartDiscount oldCartDiscount,
      @Nonnull final CartDiscountDraft newCartDiscount) {

    final String key = oldCartDiscount.getKey();
    return cartDiscountService
        .fetchCartDiscount(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Optional<CartDiscount> fetchedCartDiscountOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        CTP_CART_DISCOUNT_UPDATE_FAILED,
                        key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                handleError(errorMessage, exception, oldCartDiscount, newCartDiscount, null, 1);
                return CompletableFuture.completedFuture(null);
              }

              return fetchedCartDiscountOptional
                  .map(
                      fetchedCartDiscount ->
                          buildActionsAndUpdate(fetchedCartDiscount, newCartDiscount))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                CTP_CART_DISCOUNT_UPDATE_FAILED,
                                key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, oldCartDiscount, newCartDiscount, null, 1);
                        return CompletableFuture.completedFuture(null);
                      });
            });
  }
}
