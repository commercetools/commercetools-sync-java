package com.commercetools.sync.sdk2.customers;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class CustomerSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        CustomerSyncOptionsBuilder,
        CustomerSyncOptions,
        Customer,
        CustomerDraft,
        CustomerUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private CustomerSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link CustomerSyncOptionsBuilder} given a {@link ProjectApiRoot}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link ProjectApiRoot} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link CustomerSyncOptionsBuilder}
   */
  public static CustomerSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new CustomerSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates a new instance of {@link CustomerSyncOptions} enriched with all attributes provided to
   * {@code this} builder.
   *
   * @return new instance of {@link CustomerSyncOptions}
   */
  @Override
  public CustomerSyncOptions build() {
    return new CustomerSyncOptions(
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
  protected CustomerSyncOptionsBuilder getThis() {
    return this;
  }
}
