package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.models.Address;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_ARE_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.ADDRESSES_THAT_KEYS_NOT_UNIQUE;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.BILLING_ADDRESSES_ARE_NOT_VALID;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(CUSTOMER_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn(EMPTY);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn(EMPTY);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullAddressList_ShouldNotHaveValidationError() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(null);

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).containsExactly(customerDraft);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyAddressList_ShouldNotHaveValidationError() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(emptyList());

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).containsExactly(customerDraft);
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(asList(null, Address.of(CountryCode.DE), null));

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_ARE_NULL, "key", "[0, 2]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithBlankKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key1"),
            Address.of(CountryCode.DE).withKey(null),
            Address.of(CountryCode.US).withKey("address-key2"),
            Address.of(CountryCode.AC).withKey("  ")));

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_THAT_KEYS_NOT_SET, "key", "[1, 3]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithDuplicateKeysInAddresses_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key1"),
            Address.of(CountryCode.FR).withKey("address-key2"),
            Address.of(CountryCode.DE).withKey("address-key3"),
            Address.of(CountryCode.US).withKey("address-key1"),
            Address.of(CountryCode.US).withKey("address-key3")));

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(ADDRESSES_THAT_KEYS_NOT_UNIQUE, "key", "[0, 2, 3, 4]"));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullBillingIndexes_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn("email");
        when(customerDraft.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key1"),
            Address.of(CountryCode.FR).withKey("address-key2"),
            Address.of(CountryCode.US).withKey("address-key3")));
        when(customerDraft.getBillingAddresses()).thenReturn(asList(null, -1,  1, 2));

        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(BILLING_ADDRESSES_ARE_NOT_VALID, "key", "[null, -1]"));
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
