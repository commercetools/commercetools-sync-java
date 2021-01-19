package com.commercetools.sync.types;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TypeSyncOptionsBuilderTest {

  private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
  private TypeSyncOptionsBuilder typeSyncOptionsBuilder = TypeSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateTypeSyncOptionsBuilder() {
    final TypeSyncOptionsBuilder builder = TypeSyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final TypeSyncOptions typeSyncOptions = typeSyncOptionsBuilder.build();
    assertThat(typeSyncOptions).isNotNull();
    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNull();
    assertThat(typeSyncOptions.getBeforeCreateCallback()).isNull();
    assertThat(typeSyncOptions.getErrorCallback()).isNull();
    assertThat(typeSyncOptions.getWarningCallback()).isNull();
    assertThat(typeSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    assertThat(typeSyncOptions.getBatchSize()).isEqualTo(TypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(typeSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>>
        beforeUpdateCallback = (updateActions, newType, oldType) -> emptyList();

    typeSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final TypeSyncOptions typeSyncOptions = typeSyncOptionsBuilder.build();
    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    typeSyncOptionsBuilder.beforeCreateCallback((newType) -> null);

    final TypeSyncOptions typeSyncOptions = typeSyncOptionsBuilder.build();
    assertThat(typeSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<SyncException, Optional<TypeDraft>, Optional<Type>, List<UpdateAction<Type>>>
        mockErrorCallBack = (exception, newResource, oldResource, updateActions) -> {};
    typeSyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final TypeSyncOptions typeSyncOptions = typeSyncOptionsBuilder.build();
    assertThat(typeSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<TypeDraft>, Optional<Type>> mockWarningCallBack =
        (exception, newResource, oldResource) -> {};
    typeSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final TypeSyncOptions typeSyncOptions = typeSyncOptionsBuilder.build();
    assertThat(typeSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final TypeSyncOptionsBuilder instance = typeSyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(TypeSyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(typeSyncOptionsBuilder);
  }

  @Test
  void typeSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newType) -> null)
            .beforeUpdateCallback((updateActions, newType, oldType) -> emptyList())
            .build();
    assertThat(typeSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    assertThat(typeSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final TypeSyncOptions typeSyncOptionsWithZeroBatchSize =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();
    assertThat(typeSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(TypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final TypeSyncOptions typeSyncOptionsWithNegativeBatchSize =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    assertThat(typeSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(TypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<UpdateAction<Type>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));

    final List<UpdateAction<Type>> filteredList =
        typeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TypeDraft.class), mock(Type.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>>
        beforeUpdateCallback = (updateActions, newType, oldType) -> null;
    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Type>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
    final List<UpdateAction<Type>> filteredList =
        typeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TypeDraft.class), mock(Type.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();

    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Type>> updateActions = emptyList();
    final List<UpdateAction<Type>> filteredList =
        typeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TypeDraft.class), mock(Type.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>>
        beforeUpdateCallback = (updateActions, newType, oldType) -> emptyList();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    assertThat(typeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Type>> updateActions = singletonList(ChangeName.of(ofEnglish("name")));
    final List<UpdateAction<Type>> filteredList =
        typeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TypeDraft.class), mock(Type.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<TypeDraft, TypeDraft> draftFunction =
        typeDraft ->
            TypeDraftBuilder.of(typeDraft).key(typeDraft.getKey() + "_filteredKey").build();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();

    assertThat(typeSyncOptions.getBeforeCreateCallback()).isNotNull();

    final TypeDraft resourceDraft = mock(TypeDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");

    final Optional<TypeDraft> filteredDraft =
        typeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(typeSyncOptions.getBeforeCreateCallback()).isNull();

    final TypeDraft resourceDraft = mock(TypeDraft.class);
    final Optional<TypeDraft> filteredDraft =
        typeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<TypeDraft, TypeDraft> draftFunction = typeDraft -> null;
    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(typeSyncOptions.getBeforeCreateCallback()).isNotNull();

    final TypeDraft resourceDraft = mock(TypeDraft.class);
    final Optional<TypeDraft> filteredDraft =
        typeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    assertThat(typeSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final TypeSyncOptions typeSyncOptionsWithZeroCacheSize =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();
    assertThat(typeSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final TypeSyncOptions typeSyncOptionsWithNegativeCacheSize =
        TypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();
    assertThat(typeSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
  }
}
