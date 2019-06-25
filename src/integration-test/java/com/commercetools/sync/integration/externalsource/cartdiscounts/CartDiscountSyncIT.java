package com.commercetools.sync.integration.externalsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_FROM;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_UNTIL;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.SORT_ORDER_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getCartDiscountByKey;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getSortOrders;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CartDiscountSyncIT {

    /**
     * Deletes cart discounts from the target CTP projects.
     * Populates the target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        deleteCartDiscountsFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewCartPredicate_ShouldUpdateCartDiscountWithNewCartPredicate() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_2,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getCartPredicate()).isEqualTo(PREDICATE_2));
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewValue_ShouldUpdateCartDiscountWithNewValue() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                CART_DISCOUNT_VALUE_2,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_VALUE_2));
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewTarget_ShouldUpdateCartDiscountWithNewTarget() {
        // preparation
        final Optional<CartDiscount> oldCartDiscountBefore =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);
        assertThat(oldCartDiscountBefore).isNotEmpty();

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_2,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<CartDiscount> oldCartDiscountAfter =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_1);

        assertThat(oldCartDiscountAfter).isNotEmpty();
        assertThat(oldCartDiscountAfter).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getTarget()).isEqualTo(CART_DISCOUNT_TARGET_2));
    }

    @Test
    void sync_WithNewCartDiscount_ShouldCreateNewDiscount() {
        //preparation
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(CART_DISCOUNT_DRAFT_2))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 1, 0, 0);

        final Optional<CartDiscount> cartDiscountAfterCreation =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_2);

        assertThat(cartDiscountAfterCreation).isNotEmpty();
        assertThat(cartDiscountAfterCreation).hasValueSatisfying(cartDiscount -> {
            assertThat(cartDiscount.getName()).isEqualTo(CART_DISCOUNT_NAME_2);
            assertThat(cartDiscount.getDescription()).isEqualTo(CART_DISCOUNT_DESC_2);
            assertThat(cartDiscount.getCartPredicate()).isEqualTo(PREDICATE_2);
            assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_VALUE_2);
        });
    }

    @Test
    void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //prepare
        final CartDiscountDraft newCartDiscountDraftWithoutKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(null)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutKey))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to process cart discount draft without key.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> assertThat(throwable).isNull());

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithoutCartPredicate_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //prepare
        // Draft without "cartPredicate" throws a commercetools exception because "cartPredicate" is a required value
        final CartDiscountDraft newCartDiscountDraftWithoutName =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                (String) null,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutName))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update cart discount with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("cartPredicate: Missing required value");
            });

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithoutValue_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //prepare
        // Draft without "value" throws a commercetools exception because "value" is a required value
        final CartDiscountDraft newCartDiscountDraftWithoutValue =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                null,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutValue))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update cart discount with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("value: Missing required value");
            });

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // preparation
        final List<String> sortOrders = getSortOrders(100);
        // Default batch size is 50 (check CartDiscountSyncOptionsBuilder) so we have 2 batches of 50
        final List<CartDiscountDraft> cartDiscountDrafts = IntStream
            .range(0, 100)
            .mapToObj(i ->
                CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
                    CART_DISCOUNT_CART_PREDICATE_2,
                    CART_DISCOUNT_VALUE_2,
                    CART_DISCOUNT_TARGET_2,
                    sortOrders.get(i),
                    false)
                                        .key(format("key__%s", Integer.toString(i)))
                                        .active(false)
                                        .build())
            .collect(Collectors.toList());

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture()
            .join();


        //assertion
        assertThat(cartDiscountSyncStatistics).hasValues(100, 100, 0, 0);
    }

    @Test
    void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCartDiscountWithSuccess() {
        // Preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        // assertion
        assertThat(statistics).hasValues(1, 0, 1, 0);

        // Assert CTP state.
        final PagedQueryResult<CartDiscount> queryResult =
            CTP_TARGET_CLIENT.execute(CartDiscountQuery.of().plusPredicates(queryModel ->
                queryModel.key().is(CART_DISCOUNT_KEY_1)))
                             .toCompletableFuture()
                             .join();

        assertThat(queryResult.head()).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getKey()).isEqualTo(CART_DISCOUNT_KEY_1));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdate() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        //preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        //assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);

        assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update cart discount with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", CART_DISCOUNT_KEY_2));

    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CartDiscountQuery.class)))
                .thenCallRealMethod() // Call real fetch on fetching matching cart discounts
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        //preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(CART_DISCOUNT_DRAFT_2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update cart discount with key: '%s'. Reason: Not found when attempting to fetch while "
                + "retrying after concurrency modification.", CART_DISCOUNT_KEY_2));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final CartDiscountQuery anyCartDiscountQuery = any(CartDiscountQuery.class);

        when(spyClient.execute(anyCartDiscountQuery))
                .thenCallRealMethod() // Call real fetch on fetching matching cart discounts
                .thenReturn(completedFuture(PagedQueryResult.empty()));

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

}
