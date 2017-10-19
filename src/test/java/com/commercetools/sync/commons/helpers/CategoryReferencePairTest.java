package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class CategoryReferencePairTest {

    @Test
    public void of_WithBothReferencesAndCategoryOrderHints_ShouldSetBoth() {
        final List<Reference<Category>> categoryReferences =
            Arrays.asList(Category.referenceOfId("cat1"), Category.referenceOfId("cat2"));
        final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of("cat1", "0.12");
        final CategoryReferencePair categoryReferencePair =
            CategoryReferencePair.of(categoryReferences, categoryOrderHints);

        assertThat(categoryReferencePair).isNotNull();
        assertThat(categoryReferencePair.getCategoryReferences()).isEqualTo(categoryReferences);
        assertThat(categoryReferencePair.getCategoryOrderHints()).isEqualTo(categoryOrderHints);
    }

    @Test
    public void of_WithNullCategoryOrderHints_ShouldSetReferencesOnly() {
        final List<Reference<Category>> categoryReferences =
            Arrays.asList(Category.referenceOfId("cat1"), Category.referenceOfId("cat2"));
        final CategoryReferencePair categoryReferencePair =
            CategoryReferencePair.of(categoryReferences, null);

        assertThat(categoryReferencePair).isNotNull();
        assertThat(categoryReferencePair.getCategoryReferences()).isEqualTo(categoryReferences);
        assertThat(categoryReferencePair.getCategoryOrderHints()).isNull();
    }
}
