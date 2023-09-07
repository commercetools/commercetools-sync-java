package com.commercetools.sync.taxcategories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.*;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TaxCategorySyncOptionsTest {

  private static final TaxCategorySetKeyAction TAX_CATEGORY_SET_KEY_ACTION =
      TaxCategorySetKeyActionBuilder.of().key("key").build();
  private static ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);

  @Test
  void applyBeforeUpdateCallback_WithNullCallback_ShouldReturnIdenticalList() {
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<TaxCategoryUpdateAction> updateActions =
        singletonList(TaxCategorySetKeyActionBuilder.of().key("key").build());
    final List<TaxCategoryUpdateAction> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallback_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<TaxCategoryUpdateAction>,
            TaxCategoryDraft,
            TaxCategory,
            List<TaxCategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<TaxCategoryUpdateAction> updateActions =
        singletonList(TaxCategorySetKeyActionBuilder.of().key("key").build());

    final List<TaxCategoryUpdateAction> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<TaxCategoryUpdateAction>,
          TaxCategoryDraft,
          TaxCategory,
          List<TaxCategoryUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<TaxCategoryUpdateAction> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            emptyList(), mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallback_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<TaxCategoryUpdateAction>,
            TaxCategoryDraft,
            TaxCategory,
            List<TaxCategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<TaxCategoryUpdateAction> updateActions = singletonList(TAX_CATEGORY_SET_KEY_ACTION);

    final List<TaxCategoryUpdateAction> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {
    final Function<TaxCategoryDraft, TaxCategoryDraft> draftFunction =
        taxCategoryDraft ->
            TaxCategoryDraftBuilder.of(taxCategoryDraft)
                .key(taxCategoryDraft.getKey() + "_filteredKey")
                .build();
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final TaxCategoryDraft resourceDraft = mock(TaxCategoryDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getName()).thenReturn("myName");

    final Optional<TaxCategoryDraft> filteredDraft =
        taxCategorySyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            taxCategoryDraft ->
                assertThat(taxCategoryDraft.getKey()).isEqualTo("myKey_filteredKey"));
  }

  @Test
  void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    final TaxCategoryDraft resourceDraft = mock(TaxCategoryDraft.class);

    final Optional<TaxCategoryDraft> filteredDraft =
        taxCategorySyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<TaxCategoryDraft, TaxCategoryDraft> draftFunction = taxCategoryDraft -> null;
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final TaxCategoryDraft resourceDraft = mock(TaxCategoryDraft.class);

    final Optional<TaxCategoryDraft> filteredDraft =
        taxCategorySyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }
}
