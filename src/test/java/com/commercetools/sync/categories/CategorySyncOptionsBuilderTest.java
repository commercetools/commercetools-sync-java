package com.commercetools.sync.categories;

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
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CategorySyncOptionsBuilderTest {
  private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
  private CategorySyncOptionsBuilder categorySyncOptionsBuilder =
      CategorySyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateCategorySyncOptionsBuilder() {
    final CategorySyncOptionsBuilder builder = CategorySyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildCategorySyncOptions() {
    final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
    assertThat(categorySyncOptions).isNotNull();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNull();
    assertThat(categorySyncOptions.getBeforeCreateCallback()).isNull();
    assertThat(categorySyncOptions.getErrorCallback()).isNull();
    assertThat(categorySyncOptions.getWarningCallback()).isNull();
    assertThat(categorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    assertThat(categorySyncOptions.getBatchSize())
        .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(categorySyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
    categorySyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final Function<CategoryDraft, CategoryDraft> draftFunction = categoryDraft -> null;
    categorySyncOptionsBuilder.beforeCreateCallback(draftFunction);

    final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
    assertThat(categorySyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  public void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<CategoryDraft>,
            Optional<Category>,
            List<UpdateAction<Category>>>
        mockErrorCallback = (exception, newDraft, old, actions) -> {};
    categorySyncOptionsBuilder.errorCallback(mockErrorCallback);

    final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
    assertThat(categorySyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  public void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        mockWarningCallBack = (exception, newDraft, old) -> {};
    categorySyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
    assertThat(categorySyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final CategorySyncOptionsBuilder instance = categorySyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(CategorySyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(categorySyncOptionsBuilder);
  }

  @Test
  void categorySyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeUpdateCallback((updateActions, newCategory, oldCategory) -> emptyList())
            .beforeCreateCallback(newCategoryDraft -> null)
            .build();
    assertThat(categorySyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    assertThat(categorySyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CategorySyncOptions categorySyncOptionsWithZeroBatchSize =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();
    assertThat(categorySyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final CategorySyncOptions categorySyncOptionsWithNegativeBatchSize =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    assertThat(categorySyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNull();

    final List<UpdateAction<Category>> updateActions =
        singletonList(ChangeName.of(ofEnglish("name")));
    final List<UpdateAction<Category>> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));
    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Category>> updateActions =
        singletonList(ChangeName.of(ofEnglish("name")));
    final List<UpdateAction<Category>> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Category>> updateActions =
        singletonList(ChangeName.of(ofEnglish("name")));
    final List<UpdateAction<Category>> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));

    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<UpdateAction<Category>> updateActions = emptyList();
    final List<UpdateAction<Category>> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraft() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(categorySyncOptions.getBeforeCreateCallback()).isNull();

    final CategoryDraft resourceDraft = mock(CategoryDraft.class);
    final Optional<CategoryDraft> filteredDraft =
        categorySyncOptions.applyBeforeCreateCallback(resourceDraft);
    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredList() {
    final Function<CategoryDraft, CategoryDraft> draftFunction =
        categoryDraft ->
            CategoryDraftBuilder.of(categoryDraft)
                .key(categoryDraft.getKey() + "_filterPostFix")
                .build();

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(syncOptions.getBeforeCreateCallback()).isNotNull();

    final CategoryDraft resourceDraft = mock(CategoryDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");

    final Optional<CategoryDraft> filteredDraft =
        syncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filterPostFix");
  }

  @Test
  void applyBeforeCreateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final Function<CategoryDraft, CategoryDraft> draftFunction = categoryDraft -> null;

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(syncOptions.getBeforeCreateCallback()).isNotNull();

    final CategoryDraft resourceDraft = mock(CategoryDraft.class);
    final Optional<CategoryDraft> filteredDraft =
        syncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    assertThat(categorySyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CategorySyncOptions categorySyncOptionsWithZeroCacheSize =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();
    assertThat(categorySyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final CategorySyncOptions categorySyncOptionsWithNegativeCacheSize =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();
    assertThat(categorySyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
  }
}
