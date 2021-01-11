package com.commercetools.sync.taxcategories;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import javax.annotation.Nonnull;

public final class TaxCategorySyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        TaxCategorySyncOptionsBuilder, TaxCategorySyncOptions, TaxCategory, TaxCategoryDraft> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private TaxCategorySyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link TaxCategorySyncOptionsBuilder} given a {@link SphereClient}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link TaxCategorySyncOptionsBuilder}
   */
  public static TaxCategorySyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
    return new TaxCategorySyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link TaxCategorySyncOptions} enriched with all attributes provided to
   * {@code this} builder.
   *
   * @return new instance of {@link TaxCategorySyncOptions}
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
