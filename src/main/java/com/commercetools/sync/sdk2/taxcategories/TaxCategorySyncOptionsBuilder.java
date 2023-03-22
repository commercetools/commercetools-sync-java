package com.commercetools.sync.sdk2.taxcategories;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class TaxCategorySyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        TaxCategorySyncOptionsBuilder,
        TaxCategorySyncOptions,
        TaxCategory,
        TaxCategoryDraft,
        TaxCategoryUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private TaxCategorySyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link
   * com.commercetools.sync.sdk2.taxcategories.TaxCategorySyncOptionsBuilder} given a {@link
   * io.sphere.sdk.client.SphereClient} responsible for interaction with the target CTP project,
   * with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link io.sphere.sdk.client.SphereClient} responsible for
   *     interaction with the target CTP project.
   * @return new instance of {@link
   *     com.commercetools.sync.sdk2.taxcategories.TaxCategorySyncOptionsBuilder}
   */
  public static TaxCategorySyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new TaxCategorySyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link com.commercetools.sync.taxcategories.TaxCategorySyncOptions}
   * enriched with all attributes provided to {@code this} builder.
   *
   * @return new instance of {@link com.commercetools.sync.taxcategories.TaxCategorySyncOptions}
   */
  @Override
  public TaxCategorySyncOptions build() {
    return new TaxCategorySyncOptions(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }

  /**
   * Returns an instance of this class to be used in the superclass's generic methods. Please see
   * the JavaDoc in the overridden method for further details.
   *
   * @return an instance of this class.
   */
  @Override
  protected TaxCategorySyncOptionsBuilder getThis() {
    return this;
  }
}
