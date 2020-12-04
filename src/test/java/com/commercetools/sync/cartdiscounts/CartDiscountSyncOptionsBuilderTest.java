package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeName;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartDiscountSyncOptionsBuilderTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
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
        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNull();
        assertThat(cartDiscountSyncOptions.getErrorCallback()).isNull();
        assertThat(cartDiscountSyncOptions.getWarningCallback()).isNull();
        assertThat(cartDiscountSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(cartDiscountSyncOptions.getBatchSize()).isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
        assertThat(cartDiscountSyncOptions.getCacheSize()).isEqualTo(10_000);
    }

    @Test
    void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft, CartDiscount,
            List<UpdateAction<CartDiscount>>> beforeUpdateCallback =
                (updateActions, newCartDiscount, oldCartDiscount) -> emptyList();

        cartDiscountSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        cartDiscountSyncOptionsBuilder.beforeCreateCallback((newCartDiscount) -> null);

        final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
        assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final QuadConsumer<SyncException, Optional<CartDiscountDraft>,
                Optional<CartDiscount>, List<UpdateAction<CartDiscount>>> mockErrorCallBack
                = (syncException, draft, cartDiscount, updateActions) -> {
                };
        cartDiscountSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
        assertThat(cartDiscountSyncOptions.getErrorCallback()).isNotNull();
    }

    @Test
    void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>> mockWarningCallBack
                = (syncException, draft, cartDiscount) -> { };
        cartDiscountSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final CartDiscountSyncOptions cartDiscountSyncOptions = cartDiscountSyncOptionsBuilder.build();
        assertThat(cartDiscountSyncOptions.getWarningCallback()).isNotNull();
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
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCartDiscount) -> null)
            .beforeUpdateCallback((updateActions, newCartDiscount, oldCartDiscount) -> emptyList())
            .build();
        assertThat(cartDiscountSyncOptions).isNotNull();
    }

    @Test
    void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                      .batchSize(10)
                                                                                      .build();
        assertThat(cartDiscountSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CartDiscountSyncOptions cartDiscountSyncOptionsWithZeroBatchSize =
            CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                          .batchSize(0)
                                          .build();

        assertThat(cartDiscountSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final CartDiscountSyncOptions cartDiscountSyncOptionsWithNegativeBatchSize = CartDiscountSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();
        assertThat(cartDiscountSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(CartDiscountSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .build();
        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<CartDiscount>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));

        final List<UpdateAction<CartDiscount>> filteredList =
            cartDiscountSyncOptions.applyBeforeUpdateCallback(
                updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));

        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft,
            CartDiscount, List<UpdateAction<CartDiscount>>> beforeUpdateCallback =
                (updateActions, newCartDiscount, oldCartDiscount) -> null;

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                      .beforeUpdateCallback(
                                                                                          beforeUpdateCallback)
                                                                                      .build();
        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<CartDiscount>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<CartDiscount>> filteredList =
            cartDiscountSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    private interface MockTriFunction extends
        TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft,
            CartDiscount, List<UpdateAction<CartDiscount>>> {
    }

    @Test
    void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
        final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

        final CartDiscountSyncOptions cartDiscountSyncOptions =
            CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                          .beforeUpdateCallback(beforeUpdateCallback)
                                          .build();

        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<CartDiscount>> updateActions = emptyList();
        final List<UpdateAction<CartDiscount>> filteredList =
            cartDiscountSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));

        assertThat(filteredList).isEmpty();
        verify(beforeUpdateCallback, never()).apply(any(), any(), any());
    }

    @Test
    void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft,
            CartDiscount, List<UpdateAction<CartDiscount>>> beforeUpdateCallback =
                (updateActions, newCartDiscount, oldCartDiscount) -> emptyList();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                      .beforeUpdateCallback(
                                                                                          beforeUpdateCallback)
                                                                                      .build();
        assertThat(cartDiscountSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<CartDiscount>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<CartDiscount>> filteredList =
            cartDiscountSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(CartDiscountDraft.class), mock(CartDiscount.class));
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

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();

        assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();

        final CartDiscountDraft resourceDraft = mock(CartDiscountDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");


        final Optional<CartDiscountDraft> filteredDraft = cartDiscountSyncOptions
                .applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).hasValueSatisfying(cartDiscountDraft ->
            assertThat(cartDiscountDraft.getKey()).isEqualTo("myKey_filteredKey"));
    }

    @Test
    void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNull();

        final CartDiscountDraft resourceDraft = mock(CartDiscountDraft.class);
        final Optional<CartDiscountDraft> filteredDraft = cartDiscountSyncOptions.applyBeforeCreateCallback(
            resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<CartDiscountDraft, CartDiscountDraft> draftFunction = cartDiscountDraft -> null;
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .beforeCreateCallback(
                                                                                                  draftFunction)
                                                                                              .build();
        assertThat(cartDiscountSyncOptions.getBeforeCreateCallback()).isNotNull();

        final CartDiscountDraft resourceDraft = mock(CartDiscountDraft.class);
        final Optional<CartDiscountDraft> filteredDraft =
            cartDiscountSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }

    @Test
    void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                              .cacheSize(10)
                                                                                              .build();
        assertThat(cartDiscountSyncOptions.getCacheSize()).isEqualTo(10);
    }

    @Test
    void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CartDiscountSyncOptions cartDiscountSyncOptionsWithZeroCacheSize = CartDiscountSyncOptionsBuilder
            .of(CTP_CLIENT)
            .cacheSize(0)
            .build();

        assertThat(cartDiscountSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

        final CartDiscountSyncOptions cartDiscountSyncOptionsWithNegativeCacheSize = CartDiscountSyncOptionsBuilder
            .of(CTP_CLIENT)
            .cacheSize(-100)
            .build();

        assertThat(cartDiscountSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
    }
}
