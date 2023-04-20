package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.FilterUtils.executeSupplierIfPassesFilter;
import static com.commercetools.sync.sdk2.products.ActionGroup.*;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.ImageBuilder;
import com.commercetools.api.models.common.ImageDimensionsBuilder;
import com.commercetools.api.models.product.*;
import com.commercetools.sync.sdk2.products.SyncFilter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterUtilsTest {

  private final ProductAddExternalImageAction addExternalImageAction =
      ProductAddExternalImageActionBuilder.of()
          .image(
              ImageBuilder.of()
                  .url("url2")
                  .dimensions(ImageDimensionsBuilder.of().h(10).w(10).build())
                  .build())
          .build();

  private final List<ProductUpdateAction> passingUpdateActions =
      Arrays.asList(addExternalImageAction, addExternalImageAction);

  private static final ProductRemoveImageAction removeImageAction =
      ProductRemoveImageActionBuilder.of().imageUrl("anyUrl").build();

  private static final List<ProductUpdateAction> defaultActions = singletonList(removeImageAction);

  @Test
  void executeSupplierIfPassesFilter_WithGroupInBlackList_ShouldFilterOutOnlyThisGroup() {
    final SyncFilter syncFilter = SyncFilter.ofBlackList(IMAGES);

    final List<ProductUpdateAction> updateActionsAfterImagesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, IMAGES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterImagesFilter).hasSize(1);
    assertThat(updateActionsAfterImagesFilter).isSameAs(defaultActions);

    final List<ProductUpdateAction> updateActionsAfterPricesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterPricesFilter).hasSize(2);
    assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

    final List<ProductUpdateAction> updateActionsAfterCategoriesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterCategoriesFilter).hasSize(2);
    assertThat(updateActionsAfterCategoriesFilter).isSameAs(passingUpdateActions);
  }

  @Test
  void executeSupplierIfPassesFilter_WithGroupNotInBlackList_ShouldFilterInThisGroup() {
    final SyncFilter syncFilter = SyncFilter.ofBlackList(PRICES);
    final List<ProductUpdateAction> updateActionsAfterImagesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, IMAGES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterImagesFilter).hasSize(2);
    assertThat(updateActionsAfterImagesFilter).isSameAs(passingUpdateActions);

    final List<ProductUpdateAction> updateActionsAfterPricesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterPricesFilter).hasSize(1);
    assertThat(updateActionsAfterPricesFilter).isSameAs(defaultActions);
  }

  @Test
  void executeSupplierIfPassesFilter_WithGroupInWhiteList_ShouldFilterInOnlyThisGroup() {
    final SyncFilter syncFilter = SyncFilter.ofWhiteList(PRICES);

    final List<ProductUpdateAction> updateActionsAfterPricesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterPricesFilter).hasSize(2);
    assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

    final List<ProductUpdateAction> updateActionsAfterImagesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, IMAGES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterImagesFilter).hasSize(1);
    assertThat(updateActionsAfterImagesFilter).isSameAs(defaultActions);

    final List<ProductUpdateAction> updateActionsAfterCategoriesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterCategoriesFilter).hasSize(1);
    assertThat(updateActionsAfterCategoriesFilter).isSameAs(defaultActions);
  }

  @Test
  void executeSupplierIfPassesFilter_WithDefault_ShouldFilterInEveryThing() {
    final SyncFilter syncFilter = SyncFilter.of();

    final List<ProductUpdateAction> updateActionsAfterPricesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterPricesFilter).hasSize(2);
    assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

    final List<ProductUpdateAction> updateActionsAfterImagesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, IMAGES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterImagesFilter).hasSize(2);
    assertThat(updateActionsAfterImagesFilter).isSameAs(passingUpdateActions);

    final List<ProductUpdateAction> updateActionsAfterCategoriesFilter =
        executeSupplierIfPassesFilter(
            syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
    assertThat(updateActionsAfterCategoriesFilter).hasSize(2);
    assertThat(updateActionsAfterCategoriesFilter).isSameAs(passingUpdateActions);
  }
}
