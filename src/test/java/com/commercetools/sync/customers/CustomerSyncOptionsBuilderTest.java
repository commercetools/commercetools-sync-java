package com.commercetools.sync.customers;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerSyncOptionsBuilderTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private final CustomerSyncOptionsBuilder customerSyncOptionsBuilder = CustomerSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    void of_WithClient_ShouldCreateCustomerSyncOptionsBuilder() {
        final CustomerSyncOptionsBuilder builder = CustomerSyncOptionsBuilder.of(CTP_CLIENT);
        assertThat(builder).isNotNull();
    }

    @Test
    void build_WithClient_ShouldBuildSyncOptions() {
        final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
        assertThat(customerSyncOptions).isNotNull();
        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(customerSyncOptions.getBeforeCreateCallback()).isNull();
        assertThat(customerSyncOptions.getErrorCallback()).isNull();
        assertThat(customerSyncOptions.getWarningCallback()).isNull();
        assertThat(customerSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(customerSyncOptions.getBatchSize()).isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer, List<UpdateAction<Customer>>>
            beforeUpdateCallback = (updateActions, newCustomer, oldCustomer) -> emptyList();

        customerSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        customerSyncOptionsBuilder.beforeCreateCallback((newCustomer) -> null);

        final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
        assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final QuadConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>, List<UpdateAction<Customer>>>
            mockErrorCallBack = (exception, newResource, oldResource, updateActions) -> { };
        customerSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
        assertThat(customerSyncOptions.getErrorCallback()).isNotNull();
    }

    @Test
    void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final TriConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>> mockWarningCallBack =
            (exception, newResource, oldResource) -> {
            };
        customerSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
        assertThat(customerSyncOptions.getWarningCallback()).isNotNull();
    }

    @Test
    void getThis_ShouldReturnCorrectInstance() {
        final CustomerSyncOptionsBuilder builder = customerSyncOptionsBuilder.getThis();
        assertThat(builder).isNotNull();
        assertThat(builder).isInstanceOf(CustomerSyncOptionsBuilder.class);
        assertThat(builder).isEqualTo(customerSyncOptionsBuilder);
    }

    @Test
    void customerSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCustomer) -> null)
            .beforeUpdateCallback((updateActions, newCustomer, oldCustomer) -> emptyList())
            .build();
        assertThat(customerSyncOptions).isNotNull();
    }

    @Test
    void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(10)
            .build();
        assertThat(customerSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CustomerSyncOptions customerSyncOptionsWithZeroBatchSize = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(0)
            .build();
        assertThat(customerSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final CustomerSyncOptions customerSyncOptionsWithNegativeBatchSize = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();
        assertThat(customerSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .build();
        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<Customer>> updateActions = singletonList(ChangeEmail.of("mail@mail.com"));

        final List<UpdateAction<Customer>> filteredList =
            customerSyncOptions.applyBeforeUpdateCallback(updateActions, mock(CustomerDraft.class),
                mock(Customer.class));

        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer, List<UpdateAction<Customer>>>
            beforeUpdateCallback = (updateActions, newCustomer, oldCustomer) -> null;
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Customer>> updateActions = singletonList(ChangeEmail.of("mail@mail.com"));
        final List<UpdateAction<Customer>> filteredList =
            customerSyncOptions.applyBeforeUpdateCallback(updateActions, mock(CustomerDraft.class),
                mock(Customer.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    private interface MockTriFunction extends
        TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer, List<UpdateAction<Customer>>> {
    }

    @Test
    void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
        final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

        final CustomerSyncOptions customerSyncOptions =
            CustomerSyncOptionsBuilder.of(CTP_CLIENT)
                                      .beforeUpdateCallback(beforeUpdateCallback)
                                      .build();

        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Customer>> updateActions = emptyList();
        final List<UpdateAction<Customer>> filteredList =
            customerSyncOptions.applyBeforeUpdateCallback(updateActions, mock(CustomerDraft.class),
                mock(Customer.class));

        assertThat(filteredList).isEmpty();
        verify(beforeUpdateCallback, never()).apply(any(), any(), any());
    }

    @Test
    void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer, List<UpdateAction<Customer>>>
            beforeUpdateCallback = (updateActions, newType, oldType) -> emptyList();

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
        assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Customer>> updateActions = singletonList(ChangeEmail.of("mail@mail.com"));
        final List<UpdateAction<Customer>> filteredList =
            customerSyncOptions
                .applyBeforeUpdateCallback(updateActions, mock(CustomerDraft.class), mock(Customer.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
        final Function<CustomerDraft, CustomerDraft> draftFunction =
            customerDraft -> CustomerDraftBuilder.of(customerDraft)
                                                 .key(customerDraft.getKey() + "_filteredKey")
                                                 .build();

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .beforeCreateCallback(draftFunction)
                                                                                  .build();

        assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();

        final CustomerDraft resourceDraft = mock(CustomerDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");
        when(resourceDraft.getDefaultBillingAddress()).thenReturn(null);
        when(resourceDraft.getDefaultShippingAddress()).thenReturn(null);


        final Optional<CustomerDraft> filteredDraft = customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isNotEmpty();
        assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
    }

    @Test
    void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(customerSyncOptions.getBeforeCreateCallback()).isNull();

        final CustomerDraft resourceDraft = mock(CustomerDraft.class);
        final Optional<CustomerDraft> filteredDraft = customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<CustomerDraft, CustomerDraft> draftFunction = customerDraft -> null;
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .beforeCreateCallback(draftFunction)
                                                                                  .build();
        assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();

        final CustomerDraft resourceDraft = mock(CustomerDraft.class);
        final Optional<CustomerDraft> filteredDraft = customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }

}
