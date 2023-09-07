package com.commercetools.sync.services.impl;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.only;

import com.commercetools.api.client.ByProjectKeyCartDiscountsByIDPost;
import com.commercetools.api.client.ByProjectKeyCartDiscountsByIDRequestBuilder;
import com.commercetools.api.client.ByProjectKeyCartDiscountsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCartDiscountsKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyCartDiscountsPost;
import com.commercetools.api.client.ByProjectKeyCartDiscountsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountSetDescriptionActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdate;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateBuilder;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CartDiscountService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CartDiscountServiceImplTest {

  private final ByProjectKeyCartDiscountsKeyByKeyGet byProjectKeyCartDiscountsKeyByKeyGet = mock();

  private final ByProjectKeyCartDiscountsPost byProjectKeyCartDiscountsPost = mock();
  private final ByProjectKeyCartDiscountsByIDPost byProjectKeyCartDiscountsByIDPost = mock();

  private final ByProjectKeyCartDiscountsByIDRequestBuilder
      byProjectKeyCartDiscountsByIDRequestBuilder = mock();

  @Test
  void fetchCartDiscount_WithEmptyKey_ShouldNotFetchAnyCartDiscount() {
    // preparation
    final ProjectApiRoot ctpClient = mockProjectApiRoot();
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(ctpClient).build();
    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.fetchCartDiscount("");

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    verify(byProjectKeyCartDiscountsKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchCartDiscount_WithNullKey_ShouldNotFetchAnyCartDiscount() {
    // preparation
    final ProjectApiRoot ctpClient = mockProjectApiRoot();
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(ctpClient).build();
    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.fetchCartDiscount(null);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    verify(byProjectKeyCartDiscountsKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchCartDiscount_WithValidKey_ShouldReturnMockCartDiscount() {
    // preparation
    final ProjectApiRoot ctpClient = mockProjectApiRoot();
    final CartDiscount mockCartDiscount = mock(CartDiscount.class);
    when(mockCartDiscount.getId()).thenReturn("testId");
    when(mockCartDiscount.getKey()).thenReturn("any_key");
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(ctpClient).build();
    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    final ApiHttpResponse<CartDiscount> apiHttpResponse = mock(ApiHttpResponse.class);
    when(byProjectKeyCartDiscountsKeyByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(apiHttpResponse.getBody()).thenReturn(mockCartDiscount);

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.fetchCartDiscount("any_key");

    // assertions
    assertThat(result).isCompletedWithValue(Optional.of(mockCartDiscount));
    verify(byProjectKeyCartDiscountsKeyByKeyGet, only()).execute();
  }

  @Test
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
      justification = "See: https://github.com/spotbugs/spotbugs/issues/872")
  void createCartDiscount_WithNullCartDiscountKey_ShouldNotCreateCartDiscount() {
    // preparation
    final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(mockCartDiscountDraft.getKey()).thenReturn(null);

    final ProjectApiRoot projectApiRoot = mockProjectApiRoot();
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();
    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.createCartDiscount(mockCartDiscountDraft);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .containsExactly("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    verify(projectApiRoot, times(0)).cartDiscounts();
  }

  @Test
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
      justification = "See: https://github.com/spotbugs/spotbugs/issues/872")
  void createCartDiscount_WithEmptyCartDiscountKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final ProjectApiRoot projectApiRoot = mockProjectApiRoot();
    final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(mockCartDiscountDraft.getKey()).thenReturn("");

    final CartDiscountSyncOptions options =
        CartDiscountSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();

    final CartDiscountServiceImpl cartDiscountService = new CartDiscountServiceImpl(options);

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.createCartDiscount(mockCartDiscountDraft);

    // assertion
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
    verify(projectApiRoot, times(0)).cartDiscounts();
  }

  @Test
  void createCartDiscount_WithUnsuccessfulMockCtpResponse_ShouldNotCreateCartDiscount() {
    // preparation
    final CartDiscountDraft mockCartDiscountDraft = mock(CartDiscountDraft.class);
    final Map<String, Throwable> errors = new HashMap<>();
    when(mockCartDiscountDraft.getKey()).thenReturn("cartDiscountKey");

    final ProjectApiRoot projectApiRoot = mockProjectApiRoot();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.put(exception.getMessage(), exception))
            .build();

    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    when(byProjectKeyCartDiscountsPost.execute())
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));

    // test
    final CompletionStage<Optional<CartDiscount>> result =
        cartDiscountService.createCartDiscount(mockCartDiscountDraft);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    assertThat(errors.keySet())
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to create draft with key: 'cartDiscountKey'.");

    assertThat(errors.values())
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(SyncException.class);
              assertThat(exception.getCause()).isExactlyInstanceOf(BadGatewayException.class);
              return true;
            });
  }

  @Test
  void updateCartDiscount_WithMockSuccessfulCtpResponse_ShouldCallCartDiscountUpdateCommand() {
    // preparation
    final ProjectApiRoot projectApiRoot = mockProjectApiRoot();
    final CartDiscount mockCartDiscount = mock(CartDiscount.class);
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(projectApiRoot).build();

    final ApiHttpResponse<CartDiscount> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mockCartDiscount);
    when(byProjectKeyCartDiscountsByIDPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    final List<CartDiscountUpdateAction> updateActions =
        singletonList(
            CartDiscountSetDescriptionActionBuilder.of()
                .description(ofEnglish("new_desc"))
                .build());
    // test
    final CompletionStage<CartDiscount> result =
        cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

    // assertions
    assertThat(result).isCompletedWithValue(mockCartDiscount);
    verify(byProjectKeyCartDiscountsByIDRequestBuilder)
        .post(
            eq(
                CartDiscountUpdateBuilder.of()
                    .actions(updateActions)
                    .version(mockCartDiscount.getVersion())
                    .build()));
  }

  @Test
  void updateCartDiscount_WithMockUnsuccessfulCtpResponse_ShouldCompleteExceptionally() {
    // preparation
    final CartDiscount mockCartDiscount = mock(CartDiscount.class);
    final ProjectApiRoot mockProjectApiRoot = mockProjectApiRoot();
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(mockProjectApiRoot).build();

    when(byProjectKeyCartDiscountsByIDPost.execute())
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));

    final CartDiscountService cartDiscountService =
        new CartDiscountServiceImpl(cartDiscountSyncOptions);

    final List<CartDiscountUpdateAction> updateActions =
        singletonList(
            CartDiscountSetDescriptionActionBuilder.of()
                .description(ofEnglish("new_desc"))
                .build());
    // test
    final CompletionStage<CartDiscount> result =
        cartDiscountService.updateCartDiscount(mockCartDiscount, updateActions);

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  private ProjectApiRoot mockProjectApiRoot() {
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ByProjectKeyCartDiscountsRequestBuilder byProjectKeyCartDiscountsRequestBuilder = mock();
    when(ctpClient.cartDiscounts()).thenReturn(byProjectKeyCartDiscountsRequestBuilder);
    final ByProjectKeyCartDiscountsKeyByKeyRequestBuilder
        byProjectKeyCartDiscountsKeyByKeyRequestBuilder = mock();
    when(byProjectKeyCartDiscountsRequestBuilder.withKey(any()))
        .thenReturn(byProjectKeyCartDiscountsKeyByKeyRequestBuilder);
    when(byProjectKeyCartDiscountsRequestBuilder.withId(any()))
        .thenReturn(byProjectKeyCartDiscountsByIDRequestBuilder);
    when(byProjectKeyCartDiscountsKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyCartDiscountsKeyByKeyGet);
    final CompletableFuture<ApiHttpResponse<CartDiscount>> apiHttpResponseCompletableFuture =
        mock();
    when(byProjectKeyCartDiscountsKeyByKeyGet.execute())
        .thenReturn(apiHttpResponseCompletableFuture);
    when(byProjectKeyCartDiscountsRequestBuilder.post(any(CartDiscountDraft.class)))
        .thenReturn(byProjectKeyCartDiscountsPost);
    when(byProjectKeyCartDiscountsByIDRequestBuilder.post(any(CartDiscountUpdate.class)))
        .thenReturn(byProjectKeyCartDiscountsByIDPost);
    when(byProjectKeyCartDiscountsPost.execute()).thenReturn(apiHttpResponseCompletableFuture);
    when(byProjectKeyCartDiscountsByIDPost.execute()).thenReturn(apiHttpResponseCompletableFuture);
    return ctpClient;
  }
}
