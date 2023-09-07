package com.commercetools.sync.customers.helpers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AddressDraftBuilder;
import com.commercetools.api.models.common.BaseAddress;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CustomerBatchValidatorTest {

  private CustomerSyncOptions syncOptions;
  private CustomerSyncStatistics syncStatistics;
  private List<String> errorCallBackMessages;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);

    syncOptions =
        CustomerSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallBackMessages.add(exception.getMessage()))
            .build();
    syncStatistics = mock(CustomerSyncStatistics.class);
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<CustomerDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullCustomerDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").build();
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key(EMPTY).build();
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  @Disabled
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithNullEmail_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email(null).password("pass").key("key").build();
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyEmail_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email(EMPTY).password("pass").key("key").build();
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  @Disabled
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithNullPassword_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password(null).key("key").build();
    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_PASSWORD_NOT_SET, customerDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyPassword_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password(EMPTY).key("key").build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_PASSWORD_NOT_SET, customerDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithNullAddressList_ShouldNotHaveValidationError() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses((BaseAddress) null)
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).containsExactly(customerDraft);
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyAddressList_ShouldNotHaveValidationError() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(emptyList())
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).containsExactly(customerDraft);
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    null,
                    AddressDraftBuilder.of().country(CountryCode.DE.toString()).build(),
                    null))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(String.format(CustomerBatchValidator.ADDRESSES_ARE_NULL, "key", "[0, 2]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithBlankKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of().country(CountryCode.DE.toString()).key(null).build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of().country(CountryCode.AC.toString()).key("  ").build()))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_SET, "key", "[1, 3]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithDuplicateKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key3")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_UNIQUE, "key", "[0, 2, 3, 4]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithInvalidBillingAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .billingAddresses(asList(null, -1, 1, 2))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.BILLING_ADDRESSES_ARE_NOT_VALID, "key", "[null, -1]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithInvalidShippingAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .shippingAddresses(asList(1, 2, 3, 4, null))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.SHIPPING_ADDRESSES_ARE_NOT_VALID, "key", "[3, 4, null]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithAllValidAddresses_ShouldNotHaveValidationError() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .defaultShippingAddress(0)
            .shippingAddresses(asList(0, 1))
            .defaultBillingAddress(1)
            .billingAddresses(asList(1, 2))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).containsExactly(customerDraft);
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithEmptyBillingAndShippingAddresses_ShouldNotHaveValidationError() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .defaultShippingAddress(0)
            .shippingAddresses(emptyList())
            .defaultBillingAddress(1)
            .billingAddresses(emptyList())
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).containsExactly(customerDraft);
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithIndexesWithoutAddresses_ShouldHaveValidationErrorAndEmptyResult() {
    final CustomerDraft customerDraft1 =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(emptyList())
            .shippingAddresses(asList(0, 1))
            .build();

    final CustomerDraft customerDraft2 =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("key")
            .addresses(emptyList())
            .billingAddresses(asList(0, 1))
            .build();

    final Set<CustomerDraft> validDrafts = getValidDrafts(asList(customerDraft1, customerDraft2));

    assertThat(errorCallBackMessages).hasSize(2);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.SHIPPING_ADDRESSES_ARE_NOT_VALID, "key", "[0, 1]"));
    assertThat(errorCallBackMessages.get(1))
        .isEqualTo(
            String.format(CustomerBatchValidator.BILLING_ADDRESSES_ARE_NOT_VALID, "key", "[0, 1]"));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithMixedDrafts_ShouldReturnCorrectResults() {
    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("customerKey")
            .key("customerKey")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().key("customerGroupKey").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("typeKey").build())
                    .build())
            .stores(
                asList(
                    StoreResourceIdentifierBuilder.of().key("storeKey1").build(),
                    StoreResourceIdentifierBuilder.of().key("storeKey1").build(),
                    StoreResourceIdentifierBuilder.of().id("storeId3").build()))
            .build();

    final CustomerDraft customerDraft2 =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("customerKey2")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().id("customerGroupId2").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().id("typeId2").build())
                    .build())
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.FR.toString())
                        .key("address-key2")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.US.toString())
                        .key("address-key3")
                        .build()))
            .build();

    final CustomerDraft customerDraft3 =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("  ")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().key("customerGroupKey3").build())
            .build();

    final CustomerDraft customerDraft4 =
        CustomerDraftBuilder.of()
            .email("")
            .password("pass")
            .key("customerKey4")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().id("typeId4").build())
                    .build())
            .build();

    final CustomerBatchValidator customerBatchValidator =
        new CustomerBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomerDraft>, CustomerBatchValidator.ReferencedKeys> pair =
        customerBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(customerDraft, customerDraft2, customerDraft3, customerDraft4));

    assertThat(errorCallBackMessages).hasSize(2);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft3.getEmail()));
    assertThat(errorCallBackMessages.get(1))
        .isEqualTo(
            String.format(
                CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft4.getKey()));

    assertThat(pair.getLeft()).containsExactlyInAnyOrder(customerDraft, customerDraft2);
    assertThat(pair.getRight().getTypeKeys()).containsExactlyInAnyOrder("typeKey");
    assertThat(pair.getRight().getCustomerGroupKeys())
        .containsExactlyInAnyOrder("customerGroupKey");
  }

  @Nonnull
  private Set<CustomerDraft> getValidDrafts(@Nonnull final List<CustomerDraft> customerDrafts) {
    final CustomerBatchValidator customerBatchValidator =
        new CustomerBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomerDraft>, CustomerBatchValidator.ReferencedKeys> pair =
        customerBatchValidator.validateAndCollectReferencedKeys(customerDrafts);
    return pair.getLeft();
  }
}
