package com.commercetools.sync.customers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CustomerSyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final CustomerSyncOptionsBuilder customerSyncOptionsBuilder =
      CustomerSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateCustomerSyncOptionsBuilder() {
    final CustomerSyncOptionsBuilder builder = CustomerSyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
    assertThat(customerSyncOptions).isNotNull();
    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNull();
    Assertions.assertThat(customerSyncOptions.getBeforeCreateCallback()).isNull();
    Assertions.assertThat(customerSyncOptions.getErrorCallback()).isNull();
    Assertions.assertThat(customerSyncOptions.getWarningCallback()).isNull();
    Assertions.assertThat(customerSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    Assertions.assertThat(customerSyncOptions.getBatchSize())
        .isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    Assertions.assertThat(customerSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<CustomerUpdateAction>, CustomerDraft, Customer, List<CustomerUpdateAction>>
        beforeUpdateCallback = (updateActions, newCustomer, oldCustomer) -> emptyList();

    customerSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    customerSyncOptionsBuilder.beforeCreateCallback((newCustomer) -> null);

    final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
    Assertions.assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException, Optional<CustomerDraft>, Optional<Customer>, List<CustomerUpdateAction>>
        mockErrorCallBack = (exception, newResource, oldResource, updateActions) -> {};
    customerSyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
    Assertions.assertThat(customerSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>>
        mockWarningCallBack = (exception, newResource, oldResource) -> {};
    customerSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final CustomerSyncOptions customerSyncOptions = customerSyncOptionsBuilder.build();
    Assertions.assertThat(customerSyncOptions.getWarningCallback()).isNotNull();
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
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCustomer) -> null)
            .beforeUpdateCallback((updateActions, newCustomer, oldCustomer) -> emptyList())
            .build();
    assertThat(customerSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    Assertions.assertThat(customerSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CustomerSyncOptions customerSyncOptionsWithZeroBatchSize =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();
    Assertions.assertThat(customerSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final CustomerSyncOptions customerSyncOptionsWithNegativeBatchSize =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    Assertions.assertThat(customerSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(CustomerSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<CustomerUpdateAction> updateActions =
        singletonList(CustomerChangeEmailActionBuilder.of().email("mail@mail.com").build());

    final List<CustomerUpdateAction> filteredList =
        customerSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomerDraft.class), mock(Customer.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<CustomerUpdateAction>, CustomerDraft, Customer, List<CustomerUpdateAction>>
        beforeUpdateCallback = (updateActions, newCustomer, oldCustomer) -> null;
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CustomerUpdateAction> updateActions =
        singletonList(CustomerChangeEmailActionBuilder.of().email("mail@mail.com").build());
    final List<CustomerUpdateAction> filteredList =
        customerSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomerDraft.class), mock(Customer.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<CustomerUpdateAction>, CustomerDraft, Customer, List<CustomerUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CustomerUpdateAction> updateActions = emptyList();
    final List<CustomerUpdateAction> filteredList =
        customerSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomerDraft.class), mock(Customer.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<CustomerUpdateAction>, CustomerDraft, Customer, List<CustomerUpdateAction>>
        beforeUpdateCallback = (updateActions, newType, oldType) -> emptyList();

    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    Assertions.assertThat(customerSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<CustomerUpdateAction> updateActions =
        singletonList(CustomerChangeEmailActionBuilder.of().email("mail@mail.com").build());
    final List<CustomerUpdateAction> filteredList =
        customerSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomerDraft.class), mock(Customer.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<CustomerDraft, CustomerDraft> draftFunction =
        customerDraft ->
            CustomerDraftBuilder.of(customerDraft)
                .key(customerDraft.getKey() + "_filteredKey")
                .build();

    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();

    Assertions.assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();

    final CustomerDraft resourceDraft = mock(CustomerDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getEmail()).thenReturn("mail@mail.com");
    when(resourceDraft.getPassword()).thenReturn("pass");
    when(resourceDraft.getDefaultBillingAddress()).thenReturn(null);
    when(resourceDraft.getDefaultShippingAddress()).thenReturn(null);

    final Optional<CustomerDraft> filteredDraft =
        customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(customerSyncOptions.getBeforeCreateCallback()).isNull();

    final CustomerDraft resourceDraft = mock(CustomerDraft.class);
    final Optional<CustomerDraft> filteredDraft =
        customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<CustomerDraft, CustomerDraft> draftFunction = customerDraft -> null;
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(customerSyncOptions.getBeforeCreateCallback()).isNotNull();

    final CustomerDraft resourceDraft = mock(CustomerDraft.class);
    final Optional<CustomerDraft> filteredDraft =
        customerSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    Assertions.assertThat(customerSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CustomerSyncOptions customerSyncOptionsWithZeroCacheSize =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();
    Assertions.assertThat(customerSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final CustomerSyncOptions customerSyncOptionsWithNegativeCacheSize =
        CustomerSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();
    Assertions.assertThat(customerSyncOptionsWithNegativeCacheSize.getCacheSize())
        .isEqualTo(10_000);
  }
}
