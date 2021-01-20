package com.commercetools.sync.taxcategories.utils;

import static com.commercetools.sync.taxcategories.utils.TaxCategorySyncUtils.buildActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.SetDescription;
import java.util.Collections;
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
        TaxCategoryDraftBuilder.of(name, Collections.emptyList(), description)
            .key("tax-cat-key")
            .build();

    final List<UpdateAction<TaxCategory>> result = buildActions(taxCategory, taxCategoryDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValues_ShouldBuildAllUpdateActions() {
    final TaxCategory taxCategory = mock(TaxCategory.class);
    when(taxCategory.getName()).thenReturn("name");
    when(taxCategory.getKey()).thenReturn("tax-cat-key");
    when(taxCategory.getDescription()).thenReturn("description");

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of("different name", null, "different description")
            .key("tax-cat-key")
            .build();

    final List<UpdateAction<TaxCategory>> result = buildActions(taxCategory, taxCategoryDraft);

    assertAll(
        () -> assertThat(result).contains(ChangeName.of(taxCategoryDraft.getName())),
        () -> assertThat(result).contains(SetDescription.of(taxCategoryDraft.getDescription())));
  }
}
