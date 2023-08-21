package com.commercetools.sync.sdk2.taxcategories.utils;

import static com.commercetools.sync.sdk2.taxcategories.utils.TaxCategorySyncUtils.buildActions;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.tax_category.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxCategorySyncUtilsTest {

  @Test
  void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
    final String name = "test name";
    final String description = "test description";

    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getName()).thenReturn(name);
    when(taxCategory.getKey()).thenReturn("tax-cat-key");
    when(taxCategory.getDescription()).thenReturn(description);

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name(name)
            .rates(emptyList())
            .description(description)
            .key("tax-cat-key")
            .build();

    final List<TaxCategoryUpdateAction> result = buildActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValues_ShouldBuildAllUpdateActions() {
    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getKey()).thenReturn("tax-cat-key");
    when(taxCategory.getDescription()).thenReturn("description");

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("different name")
            .description("different description")
            .key("tax-cat-key")
            .build();

    final List<TaxCategoryUpdateAction> result = buildActions(taxCategory, taxCategoryDraft);

    assertAll(
        () ->
            assertThat(result)
                .contains(
                    TaxCategoryChangeNameActionBuilder.of()
                        .name(taxCategoryDraft.getName())
                        .build()),
        () ->
            assertThat(result)
                .contains(
                    TaxCategorySetDescriptionActionBuilder.of()
                        .description(taxCategoryDraft.getDescription())
                        .build()));
  }
}
