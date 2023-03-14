package com.commercetools.sync.sdk2.producttypes;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class ProductTypeSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        ProductTypeSyncOptionsBuilder,
        ProductTypeSyncOptions,
        ProductType,
        ProductTypeDraft,
        ProductTypeUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private ProductTypeSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  public static ProductTypeSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new ProductTypeSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link com.commercetools.sync.producttypes.ProductTypeSyncOptions}
   * enriched with all attributes provided to {@code this} builder.
   *
   * @return new instance of {@link com.commercetools.sync.producttypes.ProductTypeSyncOptions}
   */
  @Override
  public ProductTypeSyncOptions build() {
    return new ProductTypeSyncOptions(
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
  protected ProductTypeSyncOptionsBuilder getThis() {
    return this;
  }
}
