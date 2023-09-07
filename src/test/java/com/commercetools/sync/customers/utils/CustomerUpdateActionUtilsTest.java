package com.commercetools.sync.customers.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerChangeEmailAction;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerSetCompanyNameAction;
import com.commercetools.api.models.customer.CustomerSetCompanyNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomerGroupAction;
import com.commercetools.api.models.customer.CustomerSetCustomerNumberAction;
import com.commercetools.api.models.customer.CustomerSetCustomerNumberActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDateOfBirthAction;
import com.commercetools.api.models.customer.CustomerSetDateOfBirthActionBuilder;
import com.commercetools.api.models.customer.CustomerSetExternalIdAction;
import com.commercetools.api.models.customer.CustomerSetExternalIdActionBuilder;
import com.commercetools.api.models.customer.CustomerSetFirstNameAction;
import com.commercetools.api.models.customer.CustomerSetFirstNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetLastNameAction;
import com.commercetools.api.models.customer.CustomerSetLastNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetLocaleAction;
import com.commercetools.api.models.customer.CustomerSetLocaleActionBuilder;
import com.commercetools.api.models.customer.CustomerSetMiddleNameAction;
import com.commercetools.api.models.customer.CustomerSetMiddleNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetSalutationAction;
import com.commercetools.api.models.customer.CustomerSetSalutationActionBuilder;
import com.commercetools.api.models.customer.CustomerSetTitleAction;
import com.commercetools.api.models.customer.CustomerSetTitleActionBuilder;
import com.commercetools.api.models.customer.CustomerSetVatIdAction;
import com.commercetools.api.models.customer.CustomerSetVatIdActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupReferenceBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerUpdateActionUtilsTest {

  private static Customer old;
  private static CustomerDraft newSame;
  private static CustomerDraft newDifferent;

  @BeforeEach
  void setup() {
    final String key = "key1";
    final String companyName = "companyName1";
    final String salutation = "salutation1";
    final String vatId = "vatId1";
    final String locale = "DE";
    final String birthDate = "1990-10-01";
    final String externalId = "externalId1";
    final String customerNumber = "1234";

    old = mock(Customer.class);
    when(old.getKey()).thenReturn(key);
    when(old.getEmail()).thenReturn("old-email");
    when(old.getFirstName()).thenReturn("old-firstName");
    when(old.getMiddleName()).thenReturn("old-middleName");
    when(old.getLastName()).thenReturn("old-lastName");
    when(old.getTitle()).thenReturn("old-title");
    when(old.getSalutation()).thenReturn(salutation);
    when(old.getCustomerNumber()).thenReturn(customerNumber);
    when(old.getExternalId()).thenReturn(externalId);
    when(old.getCompanyName()).thenReturn(companyName);
    when(old.getDateOfBirth()).thenReturn(LocalDate.parse(birthDate));
    when(old.getVatId()).thenReturn(vatId);
    when(old.getLocale()).thenReturn(Locale.forLanguageTag(locale).toLanguageTag());

    newSame =
        CustomerDraftBuilder.of()
            .email("old-email")
            .password("oldPW")
            .firstName("old-firstName")
            .middleName("old-middleName")
            .lastName("old-lastName")
            .title("old-title")
            .key(key)
            .companyName(companyName)
            .salutation(salutation)
            .dateOfBirth(LocalDate.parse(birthDate))
            .locale(Locale.forLanguageTag(locale).toLanguageTag())
            .vatId(vatId)
            .externalId(externalId)
            .customerNumber(customerNumber)
            .build();

    newDifferent =
        CustomerDraftBuilder.of()
            .email("new-email")
            .password("newPW")
            .firstName("new-firstName")
            .middleName("new-middleName")
            .lastName("new-lastName")
            .companyName("companyName2")
            .salutation("salutation2")
            .dateOfBirth(LocalDate.parse("1990-10-02"))
            .locale(Locale.US.toLanguageTag())
            .vatId("vatId2")
            .externalId("externalId2")
            .customerNumber("customerNumber2")
            .key("key2")
            .build();
  }

  @Test
  void buildChangeEmailUpdateAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildChangeEmailUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerChangeEmailAction.class);
    assertThat(result)
        .contains(CustomerChangeEmailActionBuilder.of().email(newDifferent.getEmail()).build());
  }

  @Test
  void buildChangeEmailUpdateAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildChangeEmailUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetFirstNameUpdateAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetFirstNameUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetFirstNameAction.class);
    assertThat(result)
        .contains(
            CustomerSetFirstNameActionBuilder.of().firstName(newDifferent.getFirstName()).build());
  }

  @Test
  void buildSetFirstNameUpdateAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetFirstNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetLastNameUpdateAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetLastNameUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetLastNameAction.class);
    assertThat(result)
        .contains(
            CustomerSetLastNameActionBuilder.of().lastName(newDifferent.getLastName()).build());
  }

  @Test
  void buildSetLastNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetLastNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetMiddleNameUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetMiddleNameUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetMiddleNameAction.class);
    assertThat(result)
        .contains(
            CustomerSetMiddleNameActionBuilder.of()
                .middleName(newDifferent.getMiddleName())
                .build());
  }

  @Test
  void buildSetMiddleNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetMiddleNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTitleUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetTitleUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetTitleAction.class);
    assertThat(result)
        .contains(CustomerSetTitleActionBuilder.of().title(newDifferent.getTitle()).build());
  }

  @Test
  void buildSetTitleUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetTitleUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetSalutationUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetSalutationUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetSalutationAction.class);
    assertThat(result)
        .contains(
            CustomerSetSalutationActionBuilder.of()
                .salutation(newDifferent.getSalutation())
                .build());
  }

  @Test
  void buildSetSalutationUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetSalutationUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void
      buildSetCustomerNumberUpdateAction_withDifferentValues_ShouldNotReturnActionAndAddWarningCallback() {
    final List<String> warningMessages = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction(
            old, newDifferent, customerSyncOptions);

    assertThat(result).isEmpty();
    assertThat(warningMessages)
        .containsExactly(
            String.format(
                CustomerUpdateActionUtils.CUSTOMER_NUMBER_EXISTS_WARNING, old.getKey(), "1234"));
  }

  @Test
  void buildSetCustomerNumberUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final List<String> warningMessages = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction(
            old, newSame, customerSyncOptions);

    assertThat(result).isEmpty();
    assertThat(warningMessages).isEmpty();
  }

  @Test
  void buildSetCustomerNumberUpdateAction_withEmptyOldValueAndANewValue_ShouldReturnAction() {
    final List<String> warningMessages = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .build();

    Customer oldCustomer = mock(Customer.class);
    when(oldCustomer.getCustomerNumber()).thenReturn(" ");

    CustomerDraft newCustomer = mock(CustomerDraft.class);
    when(newCustomer.getCustomerNumber()).thenReturn("customer-number");

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction(
            oldCustomer, newCustomer, customerSyncOptions);

    assertThat(result).containsInstanceOf(CustomerSetCustomerNumberAction.class);
    assertThat(result)
        .contains(
            CustomerSetCustomerNumberActionBuilder.of().customerNumber("customer-number").build());
    assertThat(warningMessages).isEmpty();
  }

  @Test
  void buildSetCustomerNumberUpdateAction_withNullOldValueAndANewValue_ShouldReturnAction() {
    final List<String> warningMessages = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .build();

    Customer oldCustomer = mock(Customer.class);
    when(oldCustomer.getCustomerNumber()).thenReturn(null);

    CustomerDraft newCustomer = mock(CustomerDraft.class);
    when(newCustomer.getCustomerNumber()).thenReturn("customer-number");

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction(
            oldCustomer, newCustomer, customerSyncOptions);

    assertThat(result).containsInstanceOf(CustomerSetCustomerNumberAction.class);
    assertThat(result)
        .contains(
            CustomerSetCustomerNumberActionBuilder.of().customerNumber("customer-number").build());
    assertThat(warningMessages).isEmpty();
  }

  @Test
  void buildSetExternalIdUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetExternalIdUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetExternalIdAction.class);
    assertThat(result)
        .contains(
            CustomerSetExternalIdActionBuilder.of()
                .externalId(newDifferent.getExternalId())
                .build());
  }

  @Test
  void buildSetExternalIdUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetExternalIdUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetCompanyNameUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCompanyNameUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetCompanyNameAction.class);
    assertThat(result)
        .contains(
            CustomerSetCompanyNameActionBuilder.of()
                .companyName(newDifferent.getCompanyName())
                .build());
  }

  @Test
  void buildSetCompanyNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCompanyNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDateOfBirthUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetDateOfBirthUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetDateOfBirthAction.class);
    assertThat(result)
        .contains(
            CustomerSetDateOfBirthActionBuilder.of()
                .dateOfBirth(newDifferent.getDateOfBirth())
                .build());
  }

  @Test
  void buildSetDateOfBirthUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetDateOfBirthUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetVatIdUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetVatIdUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetVatIdAction.class);
    assertThat(result)
        .contains(CustomerSetVatIdActionBuilder.of().vatId(newDifferent.getVatId()).build());
  }

  @Test
  void buildSetVatIdUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetVatIdUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetLocaleUpdateAction_withDifferentValues_ShouldReturnAction() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetLocaleUpdateAction(old, newDifferent);

    assertThat(result).containsInstanceOf(CustomerSetLocaleAction.class);
    assertThat(result)
        .contains(CustomerSetLocaleActionBuilder.of().locale(newDifferent.getLocale()).build());
  }

  @Test
  void buildSetLocaleUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetLocaleUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetCustomerGroupAction_WithSameReference_ShouldNotReturnAction() {
    final String customerGroupId = UUID.randomUUID().toString();

    final CustomerGroupReference customerGroupReference =
        CustomerGroupReferenceBuilder.of().id(customerGroupId).build();

    final Customer oldCustomer = mock(Customer.class);
    when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .customerGroup(CustomerGroupResourceIdentifierBuilder.of().id(customerGroupId).build())
            .build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
    assertThat(result).isNotPresent();
  }

  @Test
  void buildSetCustomerGroupAction_WithDifferentReference_ShouldReturnAction() {
    final String customerGroupId = UUID.randomUUID().toString();
    final CustomerGroupReference customerGroupReference =
        CustomerGroupReferenceBuilder.of().id(customerGroupId).build();

    final Customer oldCustomer = mock(Customer.class);
    when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

    final String resolvedCustomerGroupId = UUID.randomUUID().toString();
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().id(resolvedCustomerGroupId).build())
            .build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
    assertThat(result).isPresent();
    assertThat(result).containsInstanceOf(CustomerSetCustomerGroupAction.class);
    assertThat(((CustomerSetCustomerGroupAction) result.get()).getCustomerGroup())
        .isEqualTo(CustomerGroupResourceIdentifierBuilder.of().id(resolvedCustomerGroupId).build());
  }

  @Test
  void buildSetCustomerGroupAction_WithOnlyNewReference_ShouldReturnAction() {
    final Customer oldCustomer = mock(Customer.class);
    final String newCustomerGroupId = UUID.randomUUID().toString();
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().id(newCustomerGroupId).build())
            .build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
    assertThat(result).isPresent();
    assertThat(result).containsInstanceOf(CustomerSetCustomerGroupAction.class);
    assertThat(((CustomerSetCustomerGroupAction) result.get()).getCustomerGroup())
        .isEqualTo(CustomerGroupResourceIdentifierBuilder.of().id(newCustomerGroupId).build());
  }

  @Test
  void buildSetCustomerGroupAction_WithoutNewReference_ShouldReturnUnsetAction() {
    final String customerGroupId = UUID.randomUUID().toString();
    final CustomerGroupReference customerGroupReference =
        CustomerGroupReferenceBuilder.of().id(customerGroupId).build();

    final Customer oldCustomer = mock(Customer.class);
    when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final Optional<CustomerUpdateAction> result =
        CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
    assertThat(result).isPresent();
    assertThat(result).containsInstanceOf(CustomerSetCustomerGroupAction.class);
    // Note: If the old value is set, but the new one is empty - the command will unset the customer
    // group.
    assertThat(((CustomerSetCustomerGroupAction) result.get()).getCustomerGroup()).isNull();
  }
}
