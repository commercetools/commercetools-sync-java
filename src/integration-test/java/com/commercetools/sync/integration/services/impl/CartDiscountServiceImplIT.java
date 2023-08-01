package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyCartDiscountsGet;
import com.commercetools.api.client.ByProjectKeyCartDiscountsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateAction;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetKeyAction;
import com.commercetools.api.models.cart_discount.CartDiscountSetKeyActionBuilder;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.sync.integration.commons.utils.CartDiscountITUtils;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.CartDiscountService;
import com.commercetools.sync.sdk2.services.impl.CartDiscountServiceImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountServiceImplIT {

  private CartDiscountService cartDiscountService;

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource();
  }

  /**
   * Deletes types & cart discounts from the target CTP project, then it populates the project with
   * test data.
   */
  @BeforeEach
  void setup() {
    CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource();

    CartDiscountITUtils.populateTargetProject();

    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();

    cartDiscountService = new CartDiscountServiceImpl(cartDiscountSyncOptions);
  }

  @Test
  void fetchCartDiscount_WithExistingCartDiscountKey_ShouldFetchCartDiscount() {
    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .withKey(CartDiscountITUtils.CART_DISCOUNT_KEY_1)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    assertThat(cartDiscount).isNotNull();

    final Optional<CartDiscount> fetchedCartDiscountOptional =
        cartDiscountService.fetchCartDiscount(CartDiscountITUtils.CART_DISCOUNT_KEY_1).toCompletableFuture().join();

    assertThat(fetchedCartDiscountOptional.get()).isEqualTo(cartDiscount);
  }

  @Test
  void fetchMatchingCartDiscountsByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<String> cartDiscountKeys = new HashSet<>();
    final Set<CartDiscount> matchingCartDiscounts =
        cartDiscountService
            .fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
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
        cartDiscountService
            .fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingCartDiscounts).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCartDiscountsByKeys_WithAnyExistingKeys_ShouldReturnASetOfCartDiscounts() {
    final Set<String> cartDiscountKeys = new HashSet<>();
    cartDiscountKeys.add(CartDiscountITUtils.CART_DISCOUNT_KEY_1);

    final Set<CartDiscount> matchingCartDiscounts =
        cartDiscountService
            .fetchMatchingCartDiscountsByKeys(cartDiscountKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingCartDiscounts).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCartDiscountsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    final ByProjectKeyCartDiscountsRequestBuilder byProjectKeyCartDiscountsRequestBuilder = mock();
    when(spyClient.cartDiscounts()).thenReturn(byProjectKeyCartDiscountsRequestBuilder);
    final ByProjectKeyCartDiscountsGet byProjectKeyCartDiscountsGet = mock();
    when(byProjectKeyCartDiscountsRequestBuilder.get()).thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withPredicateVar(anyString(), any()))
        .thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withLimit(anyInt())).thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();

    final CartDiscountSyncOptions spyOptions =
        CartDiscountSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();

    final CartDiscountService spyCartDiscountService = new CartDiscountServiceImpl(spyOptions);

    final Set<String> cartDiscountKeys = new HashSet<>();
    cartDiscountKeys.add(CartDiscountITUtils.CART_DISCOUNT_KEY_1);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyCartDiscountService.fetchMatchingCartDiscountsByKeys(cartDiscountKeys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void createCartDiscount_WithValidCartDiscount_ShouldCreateCartDiscount() {
    // preparation
    final CartDiscountDraft newCartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(CartDiscountITUtils.CART_DISCOUNT_NAME_2)
            .cartPredicate(CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2)
            .value(CartDiscountITUtils.CART_DISCOUNT_VALUE_DRAFT_2)
            .target(CartDiscountITUtils.CART_DISCOUNT_TARGET_2)
            .sortOrder(CartDiscountITUtils.SORT_ORDER_2)
            .requiresDiscountCode(false)
            .key(CartDiscountITUtils.CART_DISCOUNT_KEY_2)
            .isActive(false)
            .build();

    final CartDiscountSyncOptions spyOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();

    final CartDiscountService spyCartDiscountService = new CartDiscountServiceImpl(spyOptions);

    // test
    final Optional<CartDiscount> createdCartDiscount =
        spyCartDiscountService
            .createCartDiscount(newCartDiscountDraft)
            .toCompletableFuture()
            .join();

    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .withKey(CartDiscountITUtils.CART_DISCOUNT_KEY_2)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(createdCartDiscount)
        .hasValueSatisfying(
            created -> {
              assertThat(created.getKey()).isEqualTo(cartDiscount.getKey());
              assertThat(created.getName()).isEqualTo(cartDiscount.getName());
              assertThat(created.getCartPredicate()).isEqualTo(cartDiscount.getCartPredicate());
              assertThat(created.getValue()).isEqualTo(cartDiscount.getValue());
              assertThat(created.getTarget()).isEqualTo(cartDiscount.getTarget());
              assertThat(created.getSortOrder()).isEqualTo(cartDiscount.getSortOrder());
            });
  }

  @Test
  void createCartDiscount_WithDuplicateCartDiscountKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final CartDiscountDraft newCartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(CartDiscountITUtils.CART_DISCOUNT_NAME_2)
            .cartPredicate(CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2)
            .value(CartDiscountITUtils.CART_DISCOUNT_VALUE_DRAFT_2)
            .target(CartDiscountITUtils.CART_DISCOUNT_TARGET_2)
            .sortOrder(CartDiscountITUtils.SORT_ORDER_2)
            .requiresDiscountCode(false)
            .key(CartDiscountITUtils.CART_DISCOUNT_KEY_1)
            .isActive(false)
            .build();

    // test
    final Optional<CartDiscount> result =
        cartDiscountService.createCartDiscount(newCartDiscountDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            String.format("A duplicate value '\\\"%s\\\"' exists for field 'key'.", CartDiscountITUtils.CART_DISCOUNT_KEY_1));

    ensureErrorCallbackIsDuplicateFieldError();
  }

  private void ensureErrorCallbackIsDuplicateFieldError() {
    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception.getCause().getCause()).isInstanceOf(BadRequestException.class);
              final BadRequestException badRequestException =
                  (BadRequestException) exception.getCause().getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());

              return fieldErrors.size() == 1;
            });
  }

  @Test
  void createCartDiscount_WithDuplicateSortOrder_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final CartDiscountDraft newCartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(CartDiscountITUtils.CART_DISCOUNT_NAME_2)
            .cartPredicate(CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2)
            .value(CartDiscountITUtils.CART_DISCOUNT_VALUE_DRAFT_2)
            .target(CartDiscountITUtils.CART_DISCOUNT_TARGET_2)
            .sortOrder(CartDiscountITUtils.SORT_ORDER_1)
            .requiresDiscountCode(false)
            .key(CartDiscountITUtils.CART_DISCOUNT_KEY_2)
            .isActive(false)
            .build();

    // test
    final Optional<CartDiscount> result =
        cartDiscountService.createCartDiscount(newCartDiscountDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            String.format("A duplicate value '\\\"%s\\\"' exists for field 'sortOrder'.", CartDiscountITUtils.SORT_ORDER_1));

    ensureErrorCallbackIsDuplicateFieldError();
  }

  @Test
  void updateCartDiscount_WithValidChanges_ShouldUpdateCartDiscountCorrectly() {
    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .withKey(CartDiscountITUtils.CART_DISCOUNT_KEY_1)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final CartDiscountChangeCartPredicateAction changeCartPredicateUpdateAction =
        CartDiscountChangeCartPredicateActionBuilder.of()
            .cartPredicate(CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2)
            .build();

    final CartDiscount updatedCartDiscount =
        cartDiscountService
            .updateCartDiscount(cartDiscount, singletonList(changeCartPredicateUpdateAction))
            .toCompletableFuture()
            .join();

    assertThat(updatedCartDiscount)
        .satisfies(
            updated -> {
              assertThat(updated.getKey()).isEqualTo(cartDiscount.getKey());
              assertThat(updated.getName()).isEqualTo(cartDiscount.getName());
              assertThat(updated.getValue()).isEqualTo(cartDiscount.getValue());
              assertThat(updated.getTarget()).isEqualTo(cartDiscount.getTarget());
              assertThat(updated.getCartPredicate()).isEqualTo(CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2);
            });
  }

  @Test
  void updateCartDiscount_WithInvalidChanges_ShouldCompleteExceptionally() {
    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .withKey(CartDiscountITUtils.CART_DISCOUNT_KEY_1)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final CartDiscountSetKeyAction invalidAction =
        CartDiscountSetKeyActionBuilder.of().key("").build();

    cartDiscountService
        .updateCartDiscount(cartDiscount, singletonList(invalidAction))
        .handle(
            (result, throwable) -> {
              assertThat(result).isNull();
              assertThat(throwable).hasMessageContaining("Invalid key");
              return null;
            })
        .toCompletableFuture()
        .join();
  }
}
