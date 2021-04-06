package com.commercetools.sync.customers;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import javax.annotation.Nonnull;

public final class CustomerSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        CustomerSyncOptionsBuilder, CustomerSyncOptions, Customer, CustomerDraft, Customer> {
  public static final int BATCH_SIZE_DEFAULT = 50;

  private CustomerSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link CustomerSyncOptionsBuilder} given a {@link SphereClient}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link CustomerSyncOptionsBuilder}
   */
  public static CustomerSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
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
