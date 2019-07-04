package com.commercetools.sync.taxcategories.utils;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.commercetools.sync.taxcategories.utils.TaxCategorySyncUtils.buildActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategorySyncUtilsTest {

    private TaxCategory old;
    private TaxCategoryDraft draft;

    @BeforeEach
    void setup() {
        final String name = "test name";
        final String key = "test key";
        final String description = "test description";

        old = mock(TaxCategory.class);
        when(old.getName()).thenReturn(name);
        when(old.getKey()).thenReturn(key);
        when(old.getDescription()).thenReturn(description);

        draft = TaxCategoryDraftBuilder.of(name, null, description)
            .key(key)
            .build();
    }

    @Test
    void buildActions_WithSameValues_ShouldNotBuildAction() {
        List<UpdateAction<TaxCategory>> result = buildActions(old, draft, mock(TaxCategorySyncOptions.class));

        assertThat(result).as("Should be empty").isEmpty();
    }

}
