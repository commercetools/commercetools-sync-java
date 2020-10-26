package com.commercetools.sync.customers;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CustomerSyncOptions extends BaseSyncOptions<Customer, CustomerDraft> {

    CustomerSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>,
            List<UpdateAction<Customer>>> errorCallback,
        @Nullable final TriConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>>
            warningCallback,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer,
            List<UpdateAction<Customer>>> beforeUpdateCallback,
        @Nullable final Function<CustomerDraft, CustomerDraft> beforeCreateCallback) {
        super(ctpClient,
            errorCallback,
            warningCallback,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
    }
}
