package com.commercetools.sync.sdk2.categories;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CategorySyncOptionsBuilderTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private CategorySyncOptionsBuilder categorySyncOptionsBuilderWithClientOnly =
      CategorySyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateCategorySyncOptionsBuilder() {
    final CategorySyncOptionsBuilder builder = CategorySyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildCategorySyncOptions() {
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(categorySyncOptions).isNotNull();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNull();
    assertThat(categorySyncOptions.getBeforeCreateCallback()).isNull();
    assertThat(categorySyncOptions.getErrorCallback()).isNull();
    assertThat(categorySyncOptions.getWarningCallback()).isNull();
    assertThat(categorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    assertThat(categorySyncOptions.getBatchSize())
        .isEqualTo(com.commercetools.sync.categories.CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(categorySyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
    categorySyncOptionsBuilderWithClientOnly.beforeUpdateCallback(beforeUpdateCallback);

    final CategorySyncOptions categorySyncOptions =
        categorySyncOptionsBuilderWithClientOnly.build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final Function<CategoryDraft, CategoryDraft> draftFunction = categoryDraft -> null;
    categorySyncOptionsBuilderWithClientOnly.beforeCreateCallback(draftFunction);

    final CategorySyncOptions categorySyncOptions =
        categorySyncOptionsBuilderWithClientOnly.build();
    assertThat(categorySyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  public void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException, Optional<CategoryDraft>, Optional<Category>, List<CategoryUpdateAction>>
        mockErrorCallback = (exception, newDraft, old, actions) -> {};
    categorySyncOptionsBuilderWithClientOnly.errorCallback(mockErrorCallback);

    final CategorySyncOptions categorySyncOptions =
        categorySyncOptionsBuilderWithClientOnly.build();
    assertThat(categorySyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  public void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        mockWarningCallBack = (exception, newDraft, old) -> {};
    categorySyncOptionsBuilderWithClientOnly.warningCallback(mockWarningCallBack);

    final CategorySyncOptions categorySyncOptions =
        categorySyncOptionsBuilderWithClientOnly.build();
    assertThat(categorySyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final CategorySyncOptionsBuilder instance = categorySyncOptionsBuilderWithClientOnly.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(CategorySyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(categorySyncOptionsBuilderWithClientOnly);
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
        .isEqualTo(com.commercetools.sync.categories.CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

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

    final List<CategoryUpdateAction> updateActions =
        singletonList(CategoryChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<CategoryUpdateAction> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));
    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CategoryUpdateAction> updateActions =
        singletonList(CategoryChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<CategoryUpdateAction> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CategoryUpdateAction> updateActions =
        singletonList(CategoryChangeNameActionBuilder.of().name(ofEnglish("name")).build());
    final List<CategoryUpdateAction> filteredList =
        categorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CategoryDraft.class), mock(Category.class));

    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CategoryUpdateAction> updateActions = emptyList();
    final List<CategoryUpdateAction> filteredList =
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
                .name(
                    LocalizedString.ofEnglish(
                        categoryDraft.getName().get(Locale.ENGLISH) + "_filterPostFix"))
                .slug(categoryDraft.getSlug())
                .build();

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(syncOptions.getBeforeCreateCallback()).isNotNull();

    final CategoryDraft resourceDraft = mock(CategoryDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getName()).thenReturn(LocalizedString.ofEnglish("myName"));
    when(resourceDraft.getSlug()).thenReturn(LocalizedString.ofEnglish("mySlug"));

    final Optional<CategoryDraft> filteredDraft =
        syncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filterPostFix");
    assertThat(filteredDraft.get().getName().get(Locale.ENGLISH)).isEqualTo("myName_filterPostFix");
    assertThat(filteredDraft.get().getSlug().get(Locale.ENGLISH)).isEqualTo("mySlug");
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
