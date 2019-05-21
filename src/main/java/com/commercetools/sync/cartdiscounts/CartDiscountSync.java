package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.CartDiscountService;
import com.commercetools.sync.services.impl.CartDiscountServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.commands.UpdateAction;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountSyncUtils.buildActions;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class syncs cart discount drafts with the corresponding cart discounts in the CTP project.
 */
public class CartDiscountSync extends BaseSync<CartDiscountDraft, CartDiscountSyncStatistics, CartDiscountSyncOptions> {

    private static final String CTP_CART_DISCOUNT_FETCH_FAILED =
        "Failed to fetch existing cart discounts with keys: '%s'.";
    private static final String CTP_CART_DISCOUNT_UPDATE_FAILED =
        "Failed to update cart discount with key: '%s'. Reason: %s";
    private static final String CART_DISCOUNT_DRAFT_HAS_NO_KEY =
        "Failed to process cart discount draft without key.";
    private static final String CART_DISCOUNT_DRAFT_IS_NULL =
        "Failed to process null cart discount draft.";


    private final CartDiscountService cartDiscountService;

    public CartDiscountSync(@Nonnull final CartDiscountSyncOptions cartDiscountSyncOptions) {
        this(cartDiscountSyncOptions, new CartDiscountServiceImpl(cartDiscountSyncOptions));
    }

    /**
     * Takes a {@link CartDiscountSyncOptions} and a {@link CartDiscountService} instances to instantiate
     * a new {@link CartDiscountSync} instance that could be used to sync cart discount drafts in the CTP project specified
     * in the injected {@link CartDiscountSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param cartDiscountSyncOptions the container of all the options of the sync process including the CTP project
     *                                client and/or configuration and other sync-specific options.
     * @param cartDiscountService     the cart discount service which is responsible for fetching/caching
     *                                the CartDiscounts from the CTP project.
     */
    CartDiscountSync(@Nonnull final CartDiscountSyncOptions cartDiscountSyncOptions,
                     @Nonnull final CartDiscountService cartDiscountService) {
        super(new CartDiscountSyncStatistics(), cartDiscountSyncOptions);
        this.cartDiscountService = cartDiscountService;
    }

    /**
     * Iterates through the whole {@code cartDiscountDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link CartDiscountSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param cartDiscountDrafts {@link List} of {@link CartDiscountDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link CartDiscountSyncStatistics} holding statistics of all sync
     * processes performed by this sync instance.
     */
    @Override
    protected CompletionStage<CartDiscountSyncStatistics> process(
        @Nonnull final List<CartDiscountDraft> cartDiscountDrafts) {

        final List<List<CartDiscountDraft>> batches = batchElements(cartDiscountDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, completedFuture(statistics));
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed cart discounts to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     */
    private void handleError(@Nonnull final String errorMessage,
                             @Nullable final Throwable exception,
                             final int failedTimes) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    private String getKey(@Nonnull final CartDiscount cartDiscount) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscount.getName().get(Locale.ENGLISH);
    }

    private String getKey(@Nonnull final CartDiscountDraft cartDiscountDraft) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscountDraft.getName().get(Locale.ENGLISH);
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}. Otherwise handles an error
     * and returns {@code false}. A valid draft is a {@link CartDiscountDraft} object that is not {@code null} and its
     * key is not empty.
     *
     * @param cartDiscountDraft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final CartDiscountDraft cartDiscountDraft) {
        if (cartDiscountDraft == null) {
            handleError(CART_DISCOUNT_DRAFT_IS_NULL, null, 1);
        } else if (isBlank(getKey(cartDiscountDraft))) {
            //todo: SUPPORT-4443 need to be merged from name to key.
            handleError(CART_DISCOUNT_DRAFT_HAS_NO_KEY, null, 1);
        } else {
            return true;
        }

        return false;
    }

    @Nonnull
    private CompletionStage<Optional<CartDiscount>> fetchAndUpdate(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {

        final String key = getKey(oldCartDiscount);
        return cartDiscountService
            .fetchCartDiscount(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Optional<CartDiscount> fetchedCartDiscountOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_CART_DISCOUNT_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedCartDiscountOptional
                    .map(fetchedCartDiscount -> buildActionsAndUpdate(fetchedCartDiscount, newCartDiscount))
                    .orElseGet(() -> {
                        final String errorMessage =
                            format(CTP_CART_DISCOUNT_UPDATE_FAILED, key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(errorMessage, null, 1);
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }

    /**
     * Given an existing {@link CartDiscount} and a new {@link CartDiscountDraft}, the method calculates all the
     * update actions required to synchronize the existing cart discount to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing cart discount, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP,the error handling method
     * is called.
     *
     * @param oldCartDiscount existing cart discount that could be updated.
     * @param newCartDiscount draft containing data that could differ from data in {@code oldCartDiscount}.
     * @param updateActions   the update actions to update the {@link CartDiscount} with.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<CartDiscount>> updateCartDiscount(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount,
        @Nonnull final List<UpdateAction<CartDiscount>> updateActions) {

        return cartDiscountService
            .updateCartDiscount(oldCartDiscount, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final CartDiscount updatedCartDiscount = updateResponse.getKey();
                final Throwable sphereException = updateResponse.getValue();
                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldCartDiscount, newCartDiscount),
                        () -> {
                            final String errorMessage =
                                format(CTP_CART_DISCOUNT_UPDATE_FAILED, getKey(newCartDiscount),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return CompletableFuture.completedFuture(Optional.empty());
                        });
                } else {
                    statistics.incrementUpdated();
                    return CompletableFuture.completedFuture(Optional.of(updatedCartDiscount));
                }
            });
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Optional<CartDiscount>> buildActionsAndUpdate(
        @Nonnull final CartDiscount oldCartDiscount,
        @Nonnull final CartDiscountDraft newCartDiscount) {

        final List<UpdateAction<CartDiscount>> updateActions =
            buildActions(oldCartDiscount, newCartDiscount, syncOptions);

        final List<UpdateAction<CartDiscount>> updateActionsAfterCallback =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newCartDiscount, oldCartDiscount);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateCartDiscount(oldCartDiscount, newCartDiscount, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    /**
     * Given a cart discount draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding CartDiscount.
     *
     * @param cartDiscountDraft the cart discount draft to create the cart discount from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @Nonnull
    private CompletionStage<Optional<CartDiscount>> applyCallbackAndCreate(
        @Nonnull final CartDiscountDraft cartDiscountDraft) {

        return syncOptions
            .applyBeforeCreateCallBack(cartDiscountDraft)
            .map(draft -> cartDiscountService
                .createCartDiscount(draft)
                .thenApply(cartDiscountOptional -> {
                    if (cartDiscountOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                    return cartDiscountOptional;
                })
            )
            .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * Given a set of cart discount drafts, attempts to sync the drafts with the existing cart discounts in the CTP
     * project. The cart discount and the draft are considered to match if they have the same key.
     *
     * @param oldCartDiscounts old cart discounts.
     * @param newCartDiscounts drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<CartDiscount> oldCartDiscounts,
        @Nonnull final Set<CartDiscountDraft> newCartDiscounts) {

        final Map<String, CartDiscount> oldCartDiscountMap = oldCartDiscounts
            .stream()
            .collect(toMap(this::getKey, identity()));

        return CompletableFuture.allOf(newCartDiscounts
            .stream()
            .map(newCartDiscount -> {
                final String key = getKey(newCartDiscount);
                final CartDiscount oldCartDiscount = oldCartDiscountMap.get(key);

                return ofNullable(oldCartDiscount)
                    .map(cartDiscount -> buildActionsAndUpdate(oldCartDiscount, newCartDiscount))
                    .orElseGet(() -> applyCallbackAndCreate(newCartDiscount));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    @Override
    protected CompletionStage<CartDiscountSyncStatistics> processBatch(@Nonnull final List<CartDiscountDraft> batch) {

        final Set<CartDiscountDraft> validCartDiscountDrafts =
            batch.stream().filter(this::validateDraft).collect(toSet());
        if (validCartDiscountDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        }

        final Set<String> keys = validCartDiscountDrafts.stream().map(this::getKey).collect(toSet());


        return cartDiscountService
            .fetchMatchingCartDiscountsByKeys(keys)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                final Set<CartDiscount> fetchedCartDiscounts = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_CART_DISCOUNT_FETCH_FAILED, keys);
                    handleError(errorMessage, exception, keys.size());
                    return CompletableFuture.completedFuture(null);
                } else {
                    return syncBatch(fetchedCartDiscounts, validCartDiscountDrafts);
                }
            })
            .thenApply(ignored -> {
                statistics.incrementProcessed(batch.size());
                return statistics;
            });
    }


}
