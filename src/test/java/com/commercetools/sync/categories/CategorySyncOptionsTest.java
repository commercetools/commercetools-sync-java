package com.commercetools.sync.categories;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategorySyncOptionsTest {
    private CategorySyncOptionsBuilder categorySyncOptionsBuilder;

    /**
     * Initializes an instance of {@link CategorySyncOptionsBuilder} to be used in the unit test methods of this test
     * class.
     */
    @Before
    public void setup() {
        categorySyncOptionsBuilder = CategorySyncOptionsBuilder.of(mock(SphereClient.class));
    }

    @Test
    public void getUpdateActionsFilter_WithFilter_ShouldApplyFilterOnList() {
        final TriFunction<List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>>
            clearListFilter = (updateActions, newCategory, oldCategory) -> Collections.emptyList();
        categorySyncOptionsBuilder.beforeUpdateCallback(clearListFilter);
        final CategorySyncOptions syncOptions = categorySyncOptionsBuilder.build();

        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        updateActions.add(ChangeName.of(LocalizedString.of(Locale.ENGLISH, "name")));

        assertThat(syncOptions.getBeforeUpdateCallback()).isNotNull();
        final List<UpdateAction<Category>> resultantList = syncOptions.getBeforeUpdateCallback()
                                                                      .apply(updateActions, mock(CategoryDraft.class),
                                                                          mock(Category.class));

        assertThat(updateActions).isNotEmpty();
        assertThat(resultantList).isEmpty();
    }

    @Test
    public void build_WithOnlyRequiredFieldsSet_ShouldReturnProperOptionsInstance() {
        final CategorySyncOptions options = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
        assertThat(options).isNotNull();
        assertThat(options.getBatchSize()).isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }
}
