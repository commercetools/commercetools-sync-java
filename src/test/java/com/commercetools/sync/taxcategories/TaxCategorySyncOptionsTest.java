package com.commercetools.sync.taxcategories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.SetKey;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TaxCategorySyncOptionsTest {

  private static SphereClient CTP_CLIENT = mock(SphereClient.class);

  @Test
  void applyBeforeUpdateCallback_WithNullCallback_ShouldReturnIdenticalList() {
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<UpdateAction<TaxCategory>> updateActions = singletonList(SetKey.of("key"));
    final List<UpdateAction<TaxCategory>> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallback_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<UpdateAction<TaxCategory>>,
            TaxCategoryDraft,
            TaxCategory,
            List<UpdateAction<TaxCategory>>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<UpdateAction<TaxCategory>> updateActions = singletonList(SetKey.of("key"));

    final List<UpdateAction<TaxCategory>> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<UpdateAction<TaxCategory>>,
          TaxCategoryDraft,
          TaxCategory,
          List<UpdateAction<TaxCategory>>> {}

  @Test
  void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final TaxCategorySyncOptionsTest.MockTriFunction beforeUpdateCallback =
        mock(TaxCategorySyncOptionsTest.MockTriFunction.class);
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<UpdateAction<TaxCategory>> filteredList =
        taxCategorySyncOptions.applyBeforeUpdateCallback(
            emptyList(), mock(TaxCategoryDraft.class), mock(TaxCategory.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallback_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<UpdateAction<TaxCategory>>,
            TaxCategoryDraft,
            TaxCategory,
            List<UpdateAction<TaxCategory>>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<UpdateAction<TaxCategory>> updateActions = singletonList(SetKey.of("key"));

    final List<UpdateAction<TaxCategory>> filteredList =
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
