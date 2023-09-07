package com.commercetools.sync.cartdiscounts;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeNameActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CartDiscountSyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private CartDiscountSyncOptionsBuilder cartDiscountSyncOptionsBuilder =
      CartDiscountSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateCartDiscountSyncOptionsBuilder() {
    assertThat(cartDiscountSyncOptionsBuilder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
    assertThat(cartDiscountSyncOptions).isNotNull();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNull();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNull();
    Assertions.assertThat(cartDiscountSyncOptions.getErrorCallback()).isNull();
    Assertions.assertThat(cartDiscountSyncOptions.getWarningCallback()).isNull();
    Assertions.assertThat(cartDiscountSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    Assertions.assertThat(cartDiscountSyncOptions.getBatchSize())
        .isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    Assertions.assertThat(cartDiscountSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<CartDiscountUpdateAction>,
            CartDiscountDraft,
            CartDiscount,
            List<CartDiscountUpdateAction>>
        beforeUpdateCallback = (updateActions, newCartDiscount, oldCartDiscount) -> emptyList();

    cartDiscountSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    cartDiscountSyncOptionsBuilder.beforeCreateCallback((newCartDiscount) -> null);

    final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<CartDiscountDraft>,
            Optional<CartDiscount>,
            List<CartDiscountUpdateAction>>
        mockErrorCallBack = (syncException, draft, cartDiscount, updateActions) -> {};
    cartDiscountSyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
    Assertions.assertThat(cartDiscountSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>>
        mockWarningCallBack = (syncException, draft, cartDiscount) -> {};
    cartDiscountSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
    Assertions.assertThat(cartDiscountSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final CartDiscountSyncOptionsBuilder instance = cartDiscountSyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(CartDiscountSyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(cartDiscountSyncOptionsBuilder);
  }

  @Test
  void cartDiscountSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCartDiscount) -> null)
            .beforeUpdateCallback((updateActions, newCartDiscount, oldCartDiscount) -> emptyList())
            .build();
    assertThat(cartDiscountSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    Assertions.assertThat(cartDiscountSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CartDiscountSyncOptions cartDiscountSyncOptionsWithZeroBatchSize =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();

    Assertions.assertThat(cartDiscountSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final CartDiscountSyncOptions cartDiscountSyncOptionsWithNegativeBatchSize =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    Assertions.assertThat(cartDiscountSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<CartDiscountUpdateAction> updateActions =
        singletonList(CartDiscountChangeNameActionBuilder.of().name(ofEnglish("name")).build());

    final List<CartDiscountUpdateAction> filteredList =
        cartDiscountSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<CartDiscountUpdateAction>,
            CartDiscountDraft,
            CartDiscount,
            List<CartDiscountUpdateAction>>
        beforeUpdateCallback = (updateActions, newCartDiscount, oldCartDiscount) -> null;

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CartDiscountUpdateAction> updateActions =
        singletonList(CartDiscountChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<CartDiscountUpdateAction> filteredList =
        cartDiscountSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<CartDiscountUpdateAction>,
          CartDiscountDraft,
          CartDiscount,
          List<CartDiscountUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CartDiscountUpdateAction> updateActions = emptyList();
    final List<CartDiscountUpdateAction> filteredList =
        cartDiscountSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<CartDiscountUpdateAction>,
            CartDiscountDraft,
            CartDiscount,
            List<CartDiscountUpdateAction>>
        beforeUpdateCallback = (updateActions, newCartDiscount, oldCartDiscount) -> emptyList();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CartDiscountUpdateAction> updateActions =
        singletonList(CartDiscountChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<CartDiscountUpdateAction> filteredList =
        cartDiscountSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<CartDiscountDraft, CartDiscountDraft> draftFunction =
        cartDiscountDraft ->
            CartDiscountDraftBuilder.of(cartDiscountDraft)
                .key(format("%s_filteredKey", cartDiscountDraft.getKey()))
                .build();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();

    Assertions.assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();

    final CartDiscountDraft resourceDraft =
        CartDiscountDraftBuilder.of()
            .name(ofEnglish("name"))
            .value(
                cartDiscountValueDraftBuilder ->
                    cartDiscountValueDraftBuilder
                        .absoluteBuilder()
                        .money(MoneyBuilder.of().centAmount(10L).currencyCode("EUR").build()))
            .cartPredicate("1=1")
            .sortOrder("test")
            .key("myKey")
            .build();

    final Optional<CartDiscountDraft> filteredDraft =
        cartDiscountSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            cartDiscountDraft ->
                assertThat(cartDiscountDraft.getKey()).isEqualTo("myKey_filteredKey"));
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNull();

    final CartDiscountDraft resourceDraft = mock(CartDiscountDraft.class);
    final Optional<CartDiscountDraft> filteredDraft =
        cartDiscountSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<CartDiscountDraft, CartDiscountDraft> draftFunction = cartDiscountDraft -> null;
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();

    final CartDiscountDraft resourceDraft = mock(CartDiscountDraft.class);
    final Optional<CartDiscountDraft> filteredDraft =
        cartDiscountSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    Assertions.assertThat(cartDiscountSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CartDiscountSyncOptions cartDiscountSyncOptionsWithZeroCacheSize =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();

    Assertions.assertThat(cartDiscountSyncOptionsWithZeroCacheSize.getCacheSize())
        .isEqualTo(10_000);

    final CartDiscountSyncOptions cartDiscountSyncOptionsWithNegativeCacheSize =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();

    Assertions.assertThat(cartDiscountSyncOptionsWithNegativeCacheSize.getCacheSize())
        .isEqualTo(10_000);
  }
}
