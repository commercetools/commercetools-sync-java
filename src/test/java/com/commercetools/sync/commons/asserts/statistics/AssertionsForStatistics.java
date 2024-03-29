package com.commercetools.sync.commons.asserts.statistics;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssertionsForStatistics {
  private AssertionsForStatistics() {}

  /**
   * Create assertion for {@link com.commercetools.sync.customers.helpers.CustomerSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CustomerSyncStatisticsAssert assertThat(
      @Nullable final CustomerSyncStatistics statistics) {
    return new CustomerSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link com.commercetools.sync.products.helpers.ProductSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductSyncStatisticsAssert assertThat(
      @Nullable final ProductSyncStatistics statistics) {
    return new ProductSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link com.commercetools.sync.categories.helpers.CategorySyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CategorySyncStatisticsAssert assertThat(
      @Nullable final CategorySyncStatistics statistics) {
    return new CategorySyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link
   * com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static CustomObjectSyncStatisticsAssert assertThat(
      @Nullable final CustomObjectSyncStatistics statistics) {
    return new CustomObjectSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link
   * com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ProductTypeSyncStatisticsAssert assertThat(
      @Nullable final ProductTypeSyncStatistics statistics) {
    return new ProductTypeSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link com.commercetools.sync.types.helpers.TypeSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static TypeSyncStatisticsAssert assertThat(@Nullable final TypeSyncStatistics statistics) {
    return new TypeSyncStatisticsAssert(statistics);
  }

  @Nonnull
  public static CartDiscountSyncStatisticsAssert assertThat(
      @Nullable final CartDiscountSyncStatistics statistics) {
    return new CartDiscountSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link
   * com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static TaxCategorySyncStatisticsAssert assertThat(
      @Nullable final TaxCategorySyncStatistics statistics) {
    return new TaxCategorySyncStatisticsAssert(statistics);
  }
  /**
   * Create assertion for {@link com.commercetools.sync.states.helpers.StateSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static StateSyncStatisticsAssert assertThat(
      @Nullable final StateSyncStatistics statistics) {
    return new StateSyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link
   * com.commercetools.sync.inventories.helpers.InventorySyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static InventorySyncStatisticsAssert assertThat(
      @Nullable final InventorySyncStatistics statistics) {
    return new InventorySyncStatisticsAssert(statistics);
  }

  /**
   * Create assertion for {@link
   * com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics}.
   *
   * @param statistics the actual value.
   * @return the created assertion object.
   */
  @Nonnull
  public static ShoppingListSyncStatisticsAssert assertThat(
      @Nullable final ShoppingListSyncStatistics statistics) {
    return new ShoppingListSyncStatisticsAssert(statistics);
  }
}
