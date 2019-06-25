package com.commercetools.sync.integration.services.impl;


import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.services.CartDiscountService;
import com.commercetools.sync.services.impl.CartDiscountServiceImpl;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeCartPredicate;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetDescription;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.SORT_ORDER_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.SORT_ORDER_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CartDiscountServiceImplIT {

    private CartDiscountService cartDiscountService;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteCartDiscountsFromTargetAndSource();
    }

    /**
     * Deletes types & cart discounts from the target CTP project, then it populates the project with test data.
     */
    @BeforeEach
    void setup() {
        deleteCartDiscountsFromTargetAndSource();

        populateTargetProject();

        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .build();

        cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);
    }

    @Test
    void fetchCartDiscount_WithExistingCartDiscountKey_ShouldFetchCartDiscount() {
        final Optional<CartDiscount> cartDiscountOptional = CTP_TARGET_CLIENT
                .execute(CartDiscountQuery.of()
                        .withPredicates(cartDiscountQueryModel ->
                                cartDiscountQueryModel.key().is(CART_DISCOUNT_KEY_1)))
                .toCompletableFuture().join().head();
        assertThat(cartDiscountOptional).isNotNull();

        final Optional<CartDiscount> fetchedCartDiscountOptional =
                cartDiscountService.fetchCartDiscount(CART_DISCOUNT_KEY_1).toCompletableFuture().join();

        assertThat(fetchedCartDiscountOptional).isEqualTo(cartDiscountOptional);
    }

    @Test
    void fetchMatchingCartDiscountsByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<String> cartDiscountKeys = new HashSet<>();
        final Set<CartDiscount> matchingCartDiscounts =
                cartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
                        .toCompletableFuture()
                        .join();

        assertThat(matchingCartDiscounts).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingCartDiscountsByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
        final Set<String> cartDiscountKeys = new HashSet<>();
        cartDiscountKeys.add("cart_discount_key_1");
        cartDiscountKeys.add("cart_discount_key_2");

        final Set<CartDiscount> matchingCartDiscounts =
                cartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
                        .toCompletableFuture()
                        .join();

        assertThat(matchingCartDiscounts).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingCartDiscountsByKeys_WithAnyExistingKeys_ShouldReturnASetOfCartDiscounts() {
        final Set<String> cartDiscountKeys = new HashSet<>();
        cartDiscountKeys.add(CART_DISCOUNT_KEY_1);

        final Set<CartDiscount> matchingCartDiscounts =
                cartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
                        .toCompletableFuture()
                        .join();

        assertThat(matchingCartDiscounts).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingCartDiscountsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CartDiscountQuery.class)))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()))
                .thenCallRealMethod();

        final CartDiscountSyncOptions spyOptions =
                CartDiscountSyncOptionsBuilder.of(spyClient)
                        .errorCallback((errorMessage, exception) -> {
                            errorCallBackMessages.add(errorMessage);
                            errorCallBackExceptions.add(exception);
                        })
                        .build();

        final CartDiscountService spyCartDiscountService = new CartDiscountServiceImpl(spyOptions);


        final Set<String> cartDiscountKeys = new HashSet<>();
        cartDiscountKeys.add(CART_DISCOUNT_KEY_1);

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyCartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(BadGatewayException.class);
    }

    @Test
    void createCartDiscount_WithValidCartDiscount_ShouldCreateCartDiscount() {
        //preparation
        final CartDiscountDraft newCartDiscountDraft =
                CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
                        CART_DISCOUNT_CART_PREDICATE_2,
                        CART_DISCOUNT_VALUE_2,
                        CART_DISCOUNT_TARGET_2,
                        SORT_ORDER_2,
                        false)
                        .key(CART_DISCOUNT_KEY_2)
                        .active(false)
                        .build();

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CartDiscountSyncOptions spyOptions =
                CartDiscountSyncOptionsBuilder.of(spyClient)
                        .errorCallback((errorMessage, exception) -> {
                            errorCallBackMessages.add(errorMessage);
                            errorCallBackExceptions.add(exception);
                        })
                        .build();

        final CartDiscountService spyCartDiscountService = new CartDiscountServiceImpl(spyOptions);

        //test
        final Optional<CartDiscount> createdCartDiscount = spyCartDiscountService
                .createCartDiscount(newCartDiscountDraft)
                .toCompletableFuture().join();

        final Optional<CartDiscount> queriedOptional = CTP_TARGET_CLIENT
                .execute(CartDiscountQuery.of().withPredicates(cartDiscountQueryModel ->
                        cartDiscountQueryModel.key().is(CART_DISCOUNT_KEY_2)))
                .toCompletableFuture().join().head();

        assertThat(queriedOptional).hasValueSatisfying(queried ->
                assertThat(createdCartDiscount).hasValueSatisfying(created -> {
                    assertThat(created.getKey()).isEqualTo(queried.getKey());
                    assertThat(created.getName()).isEqualTo(queried.getName());
                    assertThat(created.getCartPredicate()).isEqualTo(queried.getCartPredicate());
                    assertThat(created.getValue()).isEqualTo(queried.getValue());
                    assertThat(created.getTarget()).isEqualTo(queried.getTarget());
                    assertThat(created.getSortOrder()).isEqualTo(queried.getSortOrder());
                }));

    }

    @Test
    void createCartDiscount_WithDuplicateCartDiscountKey_ShouldHaveEmptyOptionalAsAResult() {
        //preparation
        final CartDiscountDraft newCartDiscountDraft =
                CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
                        CART_DISCOUNT_CART_PREDICATE_2,
                        CART_DISCOUNT_VALUE_2,
                        CART_DISCOUNT_TARGET_2,
                        SORT_ORDER_2,
                        false)
                        .key(CART_DISCOUNT_KEY_1)
                        .active(false)
                        .build();

        final CartDiscountSyncOptions options = CartDiscountSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(options);

        // test
        final Optional<CartDiscount> result =
                cartDiscountService.createCartDiscount(newCartDiscountDraft)
                        .toCompletableFuture().join();

        // assertion
        assertThat(result).isEmpty();
        assertThat(errorCallBackMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                        .contains(format("A duplicate value '\"%s\" exists for field 'key'.", CART_DISCOUNT_KEY_1)));

        ensureErrorCallbackIsDuplicateFieldError();
    }

    private void ensureErrorCallbackIsDuplicateFieldError() {
        assertThat(errorCallBackExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception -> {
                    assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                    final ErrorResponseException errorResponseException = (ErrorResponseException) exception;

                    final List<DuplicateFieldError> fieldErrors = errorResponseException
                            .getErrors()
                            .stream()
                            .map(sphereError -> {
                                assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                                return sphereError.as(DuplicateFieldError.class);
                            })
                            .collect(toList());
                    assertThat(fieldErrors).hasSize(1);
                });
    }

    @Test
    void createCartDiscount_WithDuplicateSortOrder_ShouldHaveEmptyOptionalAsAResult() {
        //preparation
        final CartDiscountDraft newCartDiscountDraft =
                CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
                        CART_DISCOUNT_CART_PREDICATE_2,
                        CART_DISCOUNT_VALUE_2,
                        CART_DISCOUNT_TARGET_2,
                        SORT_ORDER_1,
                        false)
                        .key(CART_DISCOUNT_KEY_2)
                        .active(false)
                        .build();

        final CartDiscountSyncOptions options = CartDiscountSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .build();

        final CartDiscountService cartDiscountService = new CartDiscountServiceImpl(options);

        // test
        final Optional<CartDiscount> result =
                cartDiscountService.createCartDiscount(newCartDiscountDraft)
                        .toCompletableFuture().join();

        // assertion
        assertThat(result).isEmpty();
        assertThat(errorCallBackMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                    .contains(format("A duplicate value '\"%s\"' exists for field 'sortOrder'.", SORT_ORDER_1)));

        ensureErrorCallbackIsDuplicateFieldError();
    }

    @Test
    void updateCartDiscount_WithValidChanges_ShouldUpdateCartDiscountCorrectly() {
        final Optional<CartDiscount> cartDiscountOptional = CTP_TARGET_CLIENT
                .execute(CartDiscountQuery.of()
                        .withPredicates(cartDiscountQueryModel ->
                                cartDiscountQueryModel.key().is(CART_DISCOUNT_KEY_1)))
                .toCompletableFuture().join().head();
        assertThat(cartDiscountOptional).isNotNull();

        final ChangeCartPredicate changeCartPredicateUpdateAction =
                ChangeCartPredicate.of(CART_DISCOUNT_CART_PREDICATE_2);

        final CartDiscount updatedCartDiscount = cartDiscountService.updateCartDiscount(
                cartDiscountOptional.get(), singletonList(changeCartPredicateUpdateAction))
                .toCompletableFuture().join();
        assertThat(updatedCartDiscount).isNotNull();

        final Optional<CartDiscount> updatedCartDiscountOptional = CTP_TARGET_CLIENT
                .execute(CartDiscountQuery.of()
                        .withPredicates(cartDiscountQueryModel ->
                                cartDiscountQueryModel.key().is(CART_DISCOUNT_KEY_1)))
                .toCompletableFuture().join().head();

        assertThat(cartDiscountOptional).isNotEmpty();
        final CartDiscount fetchedCartDiscount = updatedCartDiscountOptional.get();
        assertThat(fetchedCartDiscount.getKey()).isEqualTo(updatedCartDiscount.getKey());
        assertThat(fetchedCartDiscount.getName()).isEqualTo(updatedCartDiscount.getName());
        assertThat(fetchedCartDiscount.getValue()).isEqualTo(updatedCartDiscount.getValue());
        assertThat(fetchedCartDiscount.getTarget()).isEqualTo(updatedCartDiscount.getTarget());
        assertThat(fetchedCartDiscount.getCartPredicate()).isEqualTo(updatedCartDiscount.getCartPredicate());
    }

    @Test
    void updateCartDiscount_WithInvalidChanges_ShouldCompleteExceptionally() {
        final Optional<CartDiscount> cartDiscountOptional = CTP_TARGET_CLIENT
                .execute(CartDiscountQuery.of()
                        .withPredicates(cartDiscountQueryModel ->
                                cartDiscountQueryModel.key().is(CART_DISCOUNT_KEY_1)))
                .toCompletableFuture().join().head();
        assertThat(cartDiscountOptional).isNotNull();

        final SetDescription setDescriptionUpdateAction = SetDescription.of(null);

        cartDiscountService.updateCartDiscount(cartDiscountOptional.get(),
                singletonList(setDescriptionUpdateAction))
                .exceptionally(exception -> {
                    assertThat(exception).isNotNull();
                    assertThat(exception.getMessage()).contains("Request body does not contain valid JSON.");
                    return null;
                })
                .toCompletableFuture().join();
    }

}
