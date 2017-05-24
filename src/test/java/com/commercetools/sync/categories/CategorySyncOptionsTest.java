package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
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
import java.util.function.Function;

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
        final Function<List<UpdateAction<Category>>,
            List<UpdateAction<Category>>> clearListFilter = (updateActions -> Collections.emptyList());
        categorySyncOptionsBuilder.setUpdateActionsFilter(clearListFilter);
        final CategorySyncOptions syncOptions = categorySyncOptionsBuilder.build();

        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        updateActions.add(ChangeName.of(LocalizedString.of(Locale.ENGLISH, "name")));

        final List<UpdateAction<Category>> resultantList = syncOptions.getUpdateActionsFilter().apply(updateActions);

        assertThat(updateActions).isNotEmpty();
        assertThat(resultantList).isEmpty();
    }
}
