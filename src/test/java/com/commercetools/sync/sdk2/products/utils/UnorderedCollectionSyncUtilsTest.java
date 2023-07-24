package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class UnorderedCollectionSyncUtilsTest {

  @Test
  void buildRemoveUpdateActions_withNullNewDraftsAndEmptyOldCollection_ShouldNotBuildActions() {
    // preparation
    final List<ProductVariant> oldVariants = new ArrayList<>();
    final List<ProductVariantDraft> newDrafts = null;

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions).isEmpty();
  }

  @Test
  void buildRemoveUpdateActions_withNullNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);
    when(productVariant.getKey()).thenReturn("1");
    final List<ProductVariant> oldVariants = singletonList(productVariant);
    final List<ProductVariantDraft> newDrafts = null;

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions)
        .containsExactly(
            ProductRemoveVariantActionBuilder.of().id(productVariant.getId()).staged(true).build());
  }

  @Test
  void buildRemoveUpdateActions_withEmptyNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);
    when(productVariant.getKey()).thenReturn("1");
    final List<ProductVariant> oldVariants = singletonList(productVariant);
    final List<ProductVariantDraft> newDrafts = new ArrayList<>();

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions)
        .containsExactly(
            ProductRemoveVariantActionBuilder.of().id(productVariant.getId()).staged(true).build());
  }

  @Test
  void
      buildRemoveUpdateActions_withSomeMatchingDrafts_ShouldBuildRemoveActionsForEveryMissingDraft() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);
    when(productVariant.getKey()).thenReturn("1");

    final ProductVariant productVariant2 = mock(ProductVariant.class);
    when(productVariant2.getId()).thenReturn(2L);
    when(productVariant2.getKey()).thenReturn("2");

    final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
    final List<ProductVariantDraft> newDrafts =
        singletonList(ProductVariantDraftBuilder.of().key(productVariant.getKey()).build());

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions)
        .containsExactly(
            ProductRemoveVariantActionBuilder.of()
                .id(productVariant2.getId())
                .staged(true)
                .build());
  }

  @Test
  void
      buildRemoveUpdateActions_withDraftsWithNullKeys_ShouldNotBuildRemoveActionsForDraftsWithNullKeys() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);

    final ProductVariant productVariant2 = mock(ProductVariant.class);
    when(productVariant2.getId()).thenReturn(2L);

    final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
    final List<ProductVariantDraft> newDrafts =
        singletonList(ProductVariantDraftBuilder.of().key(productVariant.getKey()).build());

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions).isEmpty();
  }

  @Test
  void buildRemoveUpdateActions_withNullDrafts_ShouldNotBuildRemoveActions() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);

    final ProductVariant productVariant2 = mock(ProductVariant.class);
    when(productVariant2.getId()).thenReturn(2L);

    final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
    final List<ProductVariantDraft> newDrafts = singletonList(null);

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions).isEmpty();
  }

  @Test
  void
      buildRemoveUpdateActions_withNullReturningActionMapper_ShouldBuildActionsWithNoNullElement() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);

    final ProductVariant productVariant2 = mock(ProductVariant.class);
    when(productVariant2.getId()).thenReturn(2L);
    when(productVariant2.getKey()).thenReturn("2");

    final ProductVariant productVariant3 = mock(ProductVariant.class);
    when(productVariant2.getId()).thenReturn(3L);
    when(productVariant2.getKey()).thenReturn("3");

    final List<ProductVariant> oldVariants =
        asList(productVariant, productVariant2, productVariant3);
    final List<ProductVariantDraft> newDrafts =
        singletonList(ProductVariantDraftBuilder.of().key(productVariant.getKey()).build());

    // test
    final Function<ProductVariant, ProductUpdateAction> onlyRemoveV2Mapper =
        variant -> {
          if (Objects.equals(variant.getKey(), productVariant2.getKey())) {
            return ProductRemoveVariantActionBuilder.of()
                .id(productVariant2.getId())
                .staged(true)
                .build();
          }
          return null;
        };
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            onlyRemoveV2Mapper);

    // assertion
    assertThat(removeUpdateActions).doesNotContainNull();
  }

  @Test
  void buildRemoveUpdateActions_withIdenticalNewDraftsAndOldMap_ShouldNotBuildRemoveActions() {
    // preparation
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getId()).thenReturn(1L);
    when(productVariant.getKey()).thenReturn("1");
    final List<ProductVariant> oldVariants = singletonList(productVariant);
    final List<ProductVariantDraft> newDrafts =
        singletonList(ProductVariantDraftBuilder.of().key(productVariant.getKey()).build());

    // test
    final List<ProductUpdateAction> removeUpdateActions =
        buildRemoveUpdateActions(
            oldVariants,
            newDrafts,
            ProductVariant::getKey,
            ProductVariantDraft::getKey,
            p -> ProductRemoveVariantActionBuilder.of().id(p.getId()).staged(true).build());

    // assertion
    assertThat(removeUpdateActions).isEmpty();
  }
}
