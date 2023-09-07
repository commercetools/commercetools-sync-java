package com.commercetools.sync.cartdiscounts;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class CartDiscountSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        CartDiscountSyncOptionsBuilder,
        CartDiscountSyncOptions,
        CartDiscount,
        CartDiscountDraft,
        CartDiscountUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private CartDiscountSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link CartDiscountSyncOptionsBuilder} given a {@link
   * com.commercetools.api.client.ProjectApiRoot} responsible for interaction with the target CTP
   * project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link com.commercetools.api.client.ProjectApiRoot}
   *     responsible for interaction with the target CTP project.
   * @return new instance of {@link CartDiscountSyncOptionsBuilder}
   */
  public static CartDiscountSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new CartDiscountSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link CartDiscountSyncOptions} enriched with all attributes provided
   * to {@code this} builder.
   *
   * @return new instance of {@link CartDiscountSyncOptions}
   */
  @Override
  public CartDiscountSyncOptions build() {
    return new CartDiscountSyncOptions(
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
  protected CartDiscountSyncOptionsBuilder getThis() {
    return this;
  }
}
