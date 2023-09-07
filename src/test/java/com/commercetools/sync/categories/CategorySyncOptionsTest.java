package com.commercetools.sync.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryChangeNameActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorySyncOptionsTest {
  private CategorySyncOptionsBuilder categorySyncOptionsBuilder;

  /**
   * Initializes an instance of {@link CategorySyncOptionsBuilder} to be used in the unit test
   * methods of this test class.
   */
  @BeforeEach
  void setup() {
    categorySyncOptionsBuilder = CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class));
  }

  @Test
  void getUpdateActionsFilter_WithFilter_ShouldApplyFilterOnList() {
    final TriFunction<
            List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>>
        clearListFilter = (updateActions, newCategory, oldCategory) -> Collections.emptyList();
    categorySyncOptionsBuilder.beforeUpdateCallback(clearListFilter);
    final CategorySyncOptions syncOptions = categorySyncOptionsBuilder.build();

    final List<CategoryUpdateAction> updateActions = new ArrayList<>();
    updateActions.add(
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .build());

    Assertions.assertThat(syncOptions.getBeforeUpdateCallback()).isNotNull();
    final List<CategoryUpdateAction> resultantList =
        syncOptions
            .getBeforeUpdateCallback()
            .apply(updateActions, mock(CategoryDraft.class), mock(Category.class));

    assertThat(updateActions).isNotEmpty();
    assertThat(resultantList).isEmpty();
  }

  @Test
  void build_WithOnlyRequiredFieldsSet_ShouldReturnProperOptionsInstance() {
    final CategorySyncOptions options =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    assertThat(options).isNotNull();
    Assertions.assertThat(options.getBatchSize())
        .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }
}
