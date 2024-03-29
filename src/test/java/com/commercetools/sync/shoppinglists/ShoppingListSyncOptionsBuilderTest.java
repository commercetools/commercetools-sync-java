package com.commercetools.sync.shoppinglists;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ShoppingListSyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final ShoppingListSyncOptionsBuilder shoppingListSyncOptionsBuilder =
      ShoppingListSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateShoppingListSyncOptionsBuilder() {
    assertThat(shoppingListSyncOptionsBuilder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
    assertThat(shoppingListSyncOptions).isNotNull();
    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNull();
    Assertions.assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNull();
    Assertions.assertThat(shoppingListSyncOptions.getErrorCallback()).isNull();
    Assertions.assertThat(shoppingListSyncOptions.getWarningCallback()).isNull();
    Assertions.assertThat(shoppingListSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    Assertions.assertThat(shoppingListSyncOptions.getBatchSize())
        .isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<ShoppingListUpdateAction>,
            ShoppingListDraft,
            ShoppingList,
            List<ShoppingListUpdateAction>>
        beforeUpdateCallback = (updateActions, newShoppingList, oldShoppingList) -> emptyList();

    shoppingListSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    shoppingListSyncOptionsBuilder.beforeCreateCallback((newShoppingList) -> null);

    final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
    Assertions.assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<ShoppingListDraft>,
            Optional<ShoppingList>,
            List<ShoppingListUpdateAction>>
        mockErrorCallBack = (syncException, draft, shoppingList, updateActions) -> {};
    shoppingListSyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
    Assertions.assertThat(shoppingListSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<ShoppingListDraft>, Optional<ShoppingList>>
        mockWarningCallBack = (syncException, draft, shoppingList) -> {};
    shoppingListSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final ShoppingListSyncOptions shoppingListSyncOptions = shoppingListSyncOptionsBuilder.build();
    Assertions.assertThat(shoppingListSyncOptions.getWarningCallback()).isNotNull();
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
            .beforeUpdateCallback((updateActions, newShoppingList, oldShoppingList) -> emptyList())
            .build();

    assertThat(shoppingListSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();

    Assertions.assertThat(shoppingListSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ShoppingListSyncOptions shoppingListSyncOptionsWithZeroBatchSize =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();

    Assertions.assertThat(shoppingListSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final ShoppingListSyncOptions shoppingListSyncOptionsWithNegativeBatchSize =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();

    Assertions.assertThat(shoppingListSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(ShoppingListSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<ShoppingListUpdateAction> updateActions =
        singletonList(ShoppingListChangeNameActionBuilder.of().name(ofEnglish("name")).build());

    final List<ShoppingListUpdateAction> filteredList =
        shoppingListSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<ShoppingListUpdateAction>,
            ShoppingListDraft,
            ShoppingList,
            List<ShoppingListUpdateAction>>
        beforeUpdateCallback = (updateActions, newShoppingList, oldShoppingList) -> null;

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ShoppingListUpdateAction> updateActions =
        singletonList(ShoppingListChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<ShoppingListUpdateAction> filteredList =
        shoppingListSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<ShoppingListUpdateAction>,
          ShoppingListDraft,
          ShoppingList,
          List<ShoppingListUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ShoppingListUpdateAction> updateActions = emptyList();
    final List<ShoppingListUpdateAction> filteredList =
        shoppingListSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<ShoppingListUpdateAction>,
            ShoppingListDraft,
            ShoppingList,
            List<ShoppingListUpdateAction>>
        beforeUpdateCallback = (updateActions, newShoppingList, oldShoppingList) -> emptyList();

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ShoppingListUpdateAction> updateActions =
        singletonList(ShoppingListChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<ShoppingListUpdateAction> filteredList =
        shoppingListSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ShoppingListDraft.class), mock(ShoppingList.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<ShoppingListDraft, ShoppingListDraft> draftFunction =
        shoppingListDraft ->
            ShoppingListDraftBuilder.of(shoppingListDraft)
                .name(ofEnglish("test shopping list"))
                .key(format("%s_filteredKey", shoppingListDraft.getKey()))
                .build();

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");

    final Optional<ShoppingListDraft> filteredDraft =
        shoppingListSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            shoppingListDraft ->
                assertThat(shoppingListDraft.getKey()).isEqualTo("myKey_filteredKey"));
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNull();

    final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
    final Optional<ShoppingListDraft> filteredDraft =
        shoppingListSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<ShoppingListDraft, ShoppingListDraft> draftFunction = shoppingListDraft -> null;
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();

    Assertions.assertThat(shoppingListSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ShoppingListDraft resourceDraft = mock(ShoppingListDraft.class);
    final Optional<ShoppingListDraft> filteredDraft =
        shoppingListSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();

    Assertions.assertThat(shoppingListSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ShoppingListSyncOptions shoppingListSyncOptionsWithZeroCacheSize =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();

    Assertions.assertThat(shoppingListSyncOptionsWithZeroCacheSize.getCacheSize())
        .isEqualTo(10_000);

    final ShoppingListSyncOptions shoppingListSyncOptionsWithNegativeCacheSize =
        ShoppingListSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();

    Assertions.assertThat(shoppingListSyncOptionsWithNegativeCacheSize.getCacheSize())
        .isEqualTo(10_000);
  }
}
