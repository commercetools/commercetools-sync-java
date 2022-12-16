package com.commercetools.sync.sdk2.customers;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomerSyncOptions
    extends BaseSyncOptions<Customer, CustomerDraft, CustomerUpdateAction> {

  CustomerSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<CustomerDraft>,
                  Optional<Customer>,
                  List<CustomerUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<CustomerUpdateAction>, CustomerDraft, Customer, List<CustomerUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<CustomerDraft, CustomerDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
