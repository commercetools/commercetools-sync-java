package com.commercetools.sync.commons.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryReferencePairTest {

  @Test
  void of_WithBothReferencesAndCategoryOrderHints_ShouldSetBoth() {
    final List<Reference<Category>> categoryReferences =
        Arrays.asList(Category.referenceOfId("cat1"), Category.referenceOfId("cat2"));
    final CategoryOrderHints categoryOrderHints = CategoryOrderHints.of("cat1", "0.12");
    final CategoryReferencePair categoryReferencePair =
        CategoryReferencePair.of(new HashSet<>(categoryReferences), categoryOrderHints);

    assertThat(categoryReferencePair).isNotNull();
    assertThat(categoryReferencePair.getCategoryResourceIdentifiers())
        .containsExactlyInAnyOrderElementsOf(categoryReferences);
    assertThat(categoryReferencePair.getCategoryOrderHints()).isEqualTo(categoryOrderHints);
  }

  @Test
  void of_WithNullCategoryOrderHints_ShouldSetReferencesOnly() {
    final List<Reference<Category>> categoryReferences =
        Arrays.asList(Category.referenceOfId("cat1"), Category.referenceOfId("cat2"));
    final CategoryReferencePair categoryReferencePair =
        CategoryReferencePair.of(new HashSet<>(categoryReferences), null);

    assertThat(categoryReferencePair).isNotNull();
    assertThat(categoryReferencePair.getCategoryResourceIdentifiers())
        .containsExactlyInAnyOrderElementsOf(categoryReferences);
    assertThat(categoryReferencePair.getCategoryOrderHints()).isNull();
  }
}
