package com.commercetools.sync.commons.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryResourceIdentifierPairTest {

  @Test
  void of_WithBothReferencesAndCategoryOrderHints_ShouldSetBoth() {
    final List<CategoryResourceIdentifier> categoryReferences =
        Arrays.asList(
            CategoryResourceIdentifierBuilder.of().id("cat1").build(),
            CategoryResourceIdentifierBuilder.of().id("cat2").build());
    final CategoryOrderHints categoryOrderHints =
        CategoryOrderHintsBuilder.of().addValue("cat1", "0.12").build();
    final CategoryResourceIdentifierPair categoryReferencePair =
        CategoryResourceIdentifierPair.of(categoryReferences, categoryOrderHints);

    assertThat(categoryReferencePair).isNotNull();
    assertThat(categoryReferencePair.getCategoryResourceIdentifiers())
        .containsExactlyInAnyOrderElementsOf(categoryReferences);
    assertThat(categoryReferencePair.getCategoryOrderHints()).isEqualTo(categoryOrderHints);
  }

  @Test
  void of_WithNullCategoryOrderHints_ShouldSetReferencesOnly() {
    final List<CategoryResourceIdentifier> categoryReferences =
        Arrays.asList(
            CategoryResourceIdentifierBuilder.of().id("cat1").build(),
            CategoryResourceIdentifierBuilder.of().id("cat2").build());
    final CategoryResourceIdentifierPair categoryReferencePair =
        CategoryResourceIdentifierPair.of(categoryReferences, null);

    assertThat(categoryReferencePair).isNotNull();
    assertThat(categoryReferencePair.getCategoryResourceIdentifiers())
        .containsExactlyInAnyOrderElementsOf(categoryReferences);
    assertThat(categoryReferencePair.getCategoryOrderHints()).isNull();
  }
}
