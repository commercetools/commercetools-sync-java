package com.commercetools.sync.cartdiscounts;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CartDiscountService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CartDiscountServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountSyncTest {

  private static final String KEY = "cart-discount-key";
  private CartDiscountDraft newCartDiscount;
  final List<String> errorMessages = new ArrayList<>();
  final List<Throwable> exceptions = new ArrayList<>();
  private CartDiscountSyncOptions syncOptions;
  private CartDiscount errorCallbackOldResource;
  private CartDiscountDraft errorCallbackNewResource;
  private List<UpdateAction<CartDiscount>> errorCallbackUpdateActions;

  @BeforeEach
  void setup() {
    syncOptions =
        CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, newResource, oldResource, updateActions) -> {
                  this.errorCallbackOldResource = oldResource.orElse(null);
                  this.errorCallbackNewResource = newResource.orElse(null);
                  this.errorCallbackUpdateActions = updateActions;
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    newCartDiscount =
        CartDiscountDraftBuilder.of(
                LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name"),
                CartPredicate.of("totalPrice >= \"50 EUR\""),
                CartDiscountValue.ofRelative(1000),
                ShippingCostTarget.of(),
                "0.25",
                false)
            .key(KEY)
            .active(false)
            .description(
                LocalizedString.of(Locale.GERMAN, "Beschreibung", Locale.ENGLISH, "description"))
            .validFrom(ZonedDateTime.parse("2019-05-05T00:00:00.000Z"))
            .validUntil(ZonedDateTime.parse("2019-05-15T00:00:00.000Z"))
            .build();
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final CartDiscountService mockCartDiscountService = mock(CartDiscountService.class);

    when(mockCartDiscountService.fetchMatchingCartDiscountsByKeys(singleton(KEY)))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final CartDiscountSync cartDiscountSync =
        new CartDiscountSync(syncOptions, getMockTypeService(), mockCartDiscountService);

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(singletonList(newCartDiscount)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(format("Failed to fetch existing cart discounts with keys: '[%s]'.", KEY));

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(SyncException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
              assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(SphereException.class);
              return true;
            });

    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final CartDiscountService cartDiscountService = mock(CartDiscountService.class);
    when(cartDiscountService.fetchMatchingCartDiscountsByKeys(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(cartDiscountService.createCartDiscount(any()))
        .thenReturn(completedFuture(Optional.empty()));

    final CartDiscountSyncOptions spyCartDiscountSyncOptions = spy(syncOptions);

    // test
    new CartDiscountSync(spyCartDiscountSyncOptions, getMockTypeService(), cartDiscountService)
        .sync(singletonList(newCartDiscount))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCartDiscountSyncOptions).applyBeforeCreateCallback(newCartDiscount);
    verify(spyCartDiscountSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final CartDiscount mockedExistingCartDiscount = mock(CartDiscount.class);
    when(mockedExistingCartDiscount.getKey()).thenReturn(newCartDiscount.getKey());

    final CartDiscountService cartDiscountService = mock(CartDiscountService.class);
    when(cartDiscountService.fetchMatchingCartDiscountsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingCartDiscount)));

    when(cartDiscountService.updateCartDiscount(any(), any()))
        .thenReturn(completedFuture(mockedExistingCartDiscount));

    final CartDiscountSyncOptions spyCartDiscountSyncOptions = spy(syncOptions);

    // test
    new CartDiscountSync(spyCartDiscountSyncOptions, getMockTypeService(), cartDiscountService)
        .sync(singletonList(newCartDiscount))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCartDiscountSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyCartDiscountSyncOptions, never()).applyBeforeCreateCallback(newCartDiscount);
  }

  @Test
  void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final CartDiscountDraft newCartDiscountDraft = null;

    final CartDiscountSync cartDiscountSync =
        new CartDiscountSync(syncOptions, getMockTypeService(), mock(CartDiscountService.class));

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(singletonList(newCartDiscountDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("CartDiscountDraft is null.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable.getCause()).isNull();
              return true;
            });

    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final CartDiscountDraft newCartDiscountDraftWithoutKey = mock(CartDiscountDraft.class);
    when(newCartDiscountDraftWithoutKey.getKey()).thenReturn(null);

    final CartDiscountSync cartDiscountSync =
        new CartDiscountSync(syncOptions, getMockTypeService(), mock(CartDiscountService.class));

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(
            "CartDiscountDraft with name: null doesn't have a key. "
                + "Please make sure all cart discount drafts have keys.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable.getCause()).isNull();
              return true;
            });

    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
    // preparation
    final TypeService typeService = spy(new TypeServiceImpl(syncOptions));
    when(typeService.cacheKeysToIds(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final CartDiscountSync cartDiscountSync =
        new CartDiscountSync(syncOptions, typeService, mock(CartDiscountService.class));

    final CartDiscountDraft newCartDiscountDraftWithCustomType = mock(CartDiscountDraft.class);
    when(newCartDiscountDraftWithCustomType.getKey()).thenReturn("cart-discount-key");
    when(newCartDiscountDraftWithCustomType.getCustom())
        .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()));

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithCustomType))
            .toCompletableFuture()
            .join();

    // assertions
    AssertionsForStatistics.assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable.getCause()).isExactlyInstanceOf(CompletionException.class);
              assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(SphereException.class);
              return true;
            });
  }

  @Test
  void
      sync_WithErrorUpdatingCartDiscountAndCustomErrorCallback_ShouldCallErrorCallbackAndContainResourceName() {
    // preparation
    final CartDiscountService mockCartDiscountService = mock(CartDiscountServiceImpl.class);
    final TypeService mockTypeService = mock(TypeServiceImpl.class);
    final CartDiscount existingCartDiscount = mock(CartDiscount.class);
    when(existingCartDiscount.getKey()).thenReturn(newCartDiscount.getKey());
    when(mockCartDiscountService.fetchMatchingCartDiscountsByKeys(any()))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingCartDiscount)));
    when(mockCartDiscountService.fetchMatchingCartDiscountsByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockCartDiscountService.updateCartDiscount(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));
    when(mockTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));
    when(mockCartDiscountService.createCartDiscount(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(existingCartDiscount)));
    when(mockCartDiscountService.fetchCartDiscount(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(existingCartDiscount)));

    // test
    final CartDiscountSync cartDiscountSync =
        new CartDiscountSync(syncOptions, mockTypeService, mockCartDiscountService);

    cartDiscountSync.sync(singletonList(newCartDiscount)).toCompletableFuture().join();

    // assertions
    assertThat(errorCallbackOldResource).isEqualTo(existingCartDiscount);
    assertThat(errorCallbackNewResource).isEqualTo(newCartDiscount);
    assertThat(errorCallbackUpdateActions.get(0).getAction()).isEqualTo("changeValue");

    assertThat(errorMessages.get(0))
        .contains(
            "Failed to update cart discount with key: 'cart-discount-key'. Reason: io.sphere.sdk.models.SphereException:");
  }
}
