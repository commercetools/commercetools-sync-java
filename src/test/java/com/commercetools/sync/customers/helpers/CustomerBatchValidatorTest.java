package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.Address;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_ARE_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_UNIQUE;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.BILLING_ADDRESSES_ARE_NOT_VALID;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.SHIPPING_ADDRESSES_ARE_NOT_VALID;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CustomerBatchValidatorTest {

    private CustomerSyncOptions syncOptions;
    private CustomerSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = CustomerSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
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
    void validateAndCollectReferencedKeys_WithNullCustomerDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(CUSTOMER_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of("email", "pass").build();
        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of("email", "pass")
                                                                .key(EMPTY)
                                                                .build();
        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of(null, "pass")
                                                                .key("key")
                                                                .build();
        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of(EMPTY, "pass")
                                                                .key("key")
                                                                .build();
        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullAddressList_ShouldNotHaveValidationError() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of("email", "pass")
                                                                .key("key")
                                                                .addresses(null)
                                                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).containsExactly(customerDraft);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyAddressList_ShouldNotHaveValidationError() {
        final CustomerDraft customerDraft = CustomerDraftBuilder.of("email", "pass")
                                                                .key("key")
                                                                .addresses(emptyList())
                                                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).containsExactly(customerDraft);
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("key")
                                .addresses(asList(null, Address.of(CountryCode.DE), null))
                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_ARE_NULL, "key", "[0, 2]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithBlankKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("key")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key1"),
                                    Address.of(CountryCode.DE).withKey(null),
                                    Address.of(CountryCode.US).withKey("address-key2"),
                                    Address.of(CountryCode.AC).withKey("  ")))
                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_THAT_KEYS_NOT_SET, "key", "[1, 3]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithDuplicateKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("key")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key1"),
                                    Address.of(CountryCode.FR).withKey("address-key2"),
                                    Address.of(CountryCode.DE).withKey("address-key3"),
                                    Address.of(CountryCode.US).withKey("address-key1"),
                                    Address.of(CountryCode.US).withKey("address-key3")))
                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_THAT_KEYS_NOT_UNIQUE, "key", "[0, 2, 3, 4]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithInvalidBillingAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("key")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key1"),
                                    Address.of(CountryCode.FR).withKey("address-key2"),
                                    Address.of(CountryCode.US).withKey("address-key3")))
                                .billingAddresses(asList(null, -1, 1, 2))
                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(BILLING_ADDRESSES_ARE_NOT_VALID, "key", "[null, -1]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithInvalidShippingAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("key")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key1"),
                                    Address.of(CountryCode.FR).withKey("address-key2"),
                                    Address.of(CountryCode.US).withKey("address-key3")))
                                .shippingAddresses(asList(1, 2, 3, 4, null))
                                .build();

        final Set<CustomerDraft> validDrafts = getValidDrafts(singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHIPPING_ADDRESSES_ARE_NOT_VALID, "key", "[3, 4, null]"));
        assertThat(validDrafts).isEmpty();
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
