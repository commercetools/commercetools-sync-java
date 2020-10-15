package com.commercetools.sync.shoppinglists;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
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

class ShoppingListSyncOptionsBuilderTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private ShoppingListSyncOptionsBuilder shoppingListSyncOptionsBuilder =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    void of_WithClient_ShouldCreateShoppingListSyncOptionsBuilder() {
        assertThat(shoppingListSyncOptionsBuilder).isNotNull();
    }

    @Test
    void build_WithClient_ShouldBuildSyncOptions() {
        final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
        assertThat(shoppingListSyncOptions).isNotNull();
        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNull();
        assertThat(shoppingListSyncOptions.getErrorCallback()).isNull();
        assertThat(shoppingListSyncOptions.getWarningCallback()).isNull();
        assertThat(shoppingListSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(shoppingListSyncOptions.getBatchSize()).isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final TriFunction<List<UpdateAction<ShoppingList>>, ShoppingListDraft, ShoppingList,
            List<UpdateAction<ShoppingList>>> beforeUpdateCallback =
                (updateActions, newShoppingList, oldShoppingList) -> emptyList();

        shoppingListSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        shoppingListSyncOptionsBuilder.beforeCreateCallback((newShoppingList) -> null);

        final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
        assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final QuadConsumer<SyncException, Optional<ShoppingListDraft>,
                Optional<ShoppingList>, List<UpdateAction<ShoppingList>>> mockErrorCallBack
                = (syncException, draft, shoppingList, updateActions) -> {
                };
        shoppingListSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
        assertThat(shoppingListSyncOptions.getErrorCallback()).isNotNull();
    }

    @Test
    void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final TriConsumer<SyncException, Optional<ShoppingListDraft>, Optional<ShoppingList>> mockWarningCallBack
                = (syncException, draft, shoppingList) -> { };
        shoppingListSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
        assertThat(shoppingListSyncOptions.getWarningCallback()).isNotNull();
    }

    @Test
    void getThis_ShouldReturnCorrectInstance() {
        final ShoppingListSyncOptionsBuilder instance = shoppingListSyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(ShoppingListSyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(shoppingListSyncOptionsBuilder);
    }

    @Test
    void shoppingListSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final ShoppingListSyncOptions shoppingListSyncOptions =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .batchSize(30)
                                          .beforeCreateCallback((newShoppingList) -> null)
                                          .beforeUpdateCallback(
                                              (updateActions, newShoppingList, oldShoppingList) -> emptyList())
                                          .build();

        assertThat(shoppingListSyncOptions).isNotNull();
    }

    @Test
    void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        final ShoppingListSyncOptions shoppingListSyncOptions =
                ShoppingListSyncOptionsBuilder
                    .of(CTP_CLIENT)
                    .batchSize(10)
                    .build();

        assertThat(shoppingListSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final ShoppingListSyncOptions shoppingListSyncOptionsWithZeroBatchSize =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .batchSize(0)
                                          .build();

        assertThat(shoppingListSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final ShoppingListSyncOptions shoppingListSyncOptionsWithNegativeBatchSize =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .batchSize(-100)
                                          .build();

        assertThat(shoppingListSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final ShoppingListSyncOptions shoppingListSyncOptions =
                ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).build();

        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<ShoppingList>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));

        final List<UpdateAction<ShoppingList>> filteredList =
            shoppingListSyncOptions.applyBeforeUpdateCallback(
                updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));

        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<ShoppingList>>, ShoppingListDraft,
            ShoppingList, List<UpdateAction<ShoppingList>>> beforeUpdateCallback =
                (updateActions, newShoppingList, oldShoppingList) -> null;

        final ShoppingListSyncOptions shoppingListSyncOptions =
                ShoppingListSyncOptionsBuilder
                    .of(CTP_CLIENT)
                    .beforeUpdateCallback(beforeUpdateCallback)
                    .build();

        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<ShoppingList>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<ShoppingList>> filteredList =
            shoppingListSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    private interface MockTriFunction extends
        TriFunction<List<UpdateAction<ShoppingList>>, ShoppingListDraft,
            ShoppingList, List<UpdateAction<ShoppingList>>> {
    }

    @Test
    void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
        final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

        final ShoppingListSyncOptions shoppingListSyncOptions =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .beforeUpdateCallback(beforeUpdateCallback)
                                          .build();

        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<ShoppingList>> updateActions = emptyList();
        final List<UpdateAction<ShoppingList>> filteredList =
            shoppingListSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));

        assertThat(filteredList).isEmpty();
        verify(beforeUpdateCallback, never()).apply(any(), any(), any());
    }

    @Test
    void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<ShoppingList>>, ShoppingListDraft,
            ShoppingList, List<UpdateAction<ShoppingList>>> beforeUpdateCallback =
                (updateActions, newShoppingList, oldShoppingList) -> emptyList();

        final ShoppingListSyncOptions shoppingListSyncOptions =
                ShoppingListSyncOptionsBuilder
                    .of(CTP_CLIENT)
                    .beforeUpdateCallback(beforeUpdateCallback)
                    .build();

        assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<ShoppingList>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<ShoppingList>> filteredList =
            shoppingListSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
        final Function<ShoppingListDraft, ShoppingListDraft> draftFunction =
            shoppingListDraft ->
                    ShoppingListDraftBuilder.of(shoppingListDraft)
                                            .key(format("%s_filteredKey", shoppingListDraft.getKey()))
                                            .build();

        final ShoppingListSyncOptions shoppingListSyncOptions =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .beforeCreateCallback(draftFunction)
                                          .build();

        assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();

        final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");


        final Optional<ShoppingListDraft> filteredDraft = shoppingListSyncOptions
                .applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).hasValueSatisfying(shoppingListDraft ->
            assertThat(shoppingListDraft.getKey()).isEqualTo("myKey_filteredKey"));
    }

    @Test
    void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNull();

        final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
        final Optional<ShoppingListDraft> filteredDraft = shoppingListSyncOptions.applyBeforeCreateCallback(
            resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<ShoppingListDraft, ShoppingListDraft> draftFunction = shoppingListDraft -> null;
        final ShoppingListSyncOptions shoppingListSyncOptions =
            ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
                                          .beforeCreateCallback(draftFunction)
                                          .build();

        assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();

        final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
        final Optional<ShoppingListDraft> filteredDraft =
            shoppingListSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }
}
