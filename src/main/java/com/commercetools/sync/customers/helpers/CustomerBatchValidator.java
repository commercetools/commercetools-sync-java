package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.customers.CustomerSyncOptions;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.models.Address;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CustomerBatchValidator
    extends BaseBatchValidator<CustomerDraft, CustomerSyncOptions, BaseSyncStatistics> {

    static final String CUSTOMER_DRAFT_IS_NULL = "CustomerDraft is null.";
    static final String CUSTOMER_DRAFT_KEY_NOT_SET = "CustomerDraft with email: %s doesn't have a key. "
        + "Please make sure all customer drafts have keys.";
    static final String CUSTOMER_DRAFT_EMAIL_NOT_SET = "CustomerDraft with key: %s doesn't have an email. "
        + "Please make sure all customer drafts have emails.";

    static final String ADDRESSES_ARE_NULL = "CustomerDraft with key: '%s' has null addresses on indexes: '%s'. "
        + "Please make sure each address is set in the addresses list.";
    static final String ADDRESSES_THAT_KEYS_NOT_SET = "CustomerDraft with key: '%s' has blank address keys on indexes: "
        + "'%s'. Please make sure each address has a key in the addresses list";
    static final String ADDRESSES_THAT_KEYS_NOT_UNIQUE = "CustomerDraft with key: '%s' has duplicate address keys on "
        + "indexes: '%s'. Please make sure each address key is unique in the addresses list.";

    static final String DEFAULT_BILLING_ADDRESS_IS_NOT_VALID = "CustomerDraft with key: '%s' does not contain an "
        + "index: '%s' of the 'defaultBillingAddress' in the addresses array."
        + "Please make sure all customer drafts have a valid index value for the 'defaultBillingAddress'."
        + "Note: When The 'defaultBillingAddressId' of the customer will be set to the ID of that address.";

    static final String BILLING_ADDRESSES_ARE_NOT_VALID = "CustomerDraft with key: '%s' does not contain indices: '%s' "
        + "of the 'billingAddresses' in the addresses list. "
        + "Please make sure all customer drafts have valid index values for the 'billingAddresses'."
        + "Note: The 'billingAddressIds' of the customer will be set to the IDs of that addresses.";

    static final String DEFAULT_SHIPPING_ADDRESS_IS_NOT_VALID = "CustomerDraft with key: '%s' does not contain an "
        + "index: '%s' of the 'defaultShippingAddress' in the addresses array."
        + "Please make sure all customer drafts have a valid index value for the 'defaultShippingAddress'."
        + "Note: The 'defaultShippingAddressId' of the customer will be set to the ID of that address.";

    static final String SHIPPING_ADDRESSES_ARE_NOT_VALID = "CustomerDraft with key: '%s' does not contain indices: '%s'"
        + " of the 'shippingAddresses' in the addresses list. "
        + "Please make sure all customer drafts have valid index values for the 'shippingAddresses'."
        + "Note: The 'shippingAddressIds' of the customer will be set to the IDs of that addresses.";

    public CustomerBatchValidator(@Nonnull final CustomerSyncOptions syncOptions,
                                  @Nonnull final BaseSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link CustomerDraft}&gt; of drafts this method attempts to validate
     * drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;,
     * {@link CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     * referenced keys within a wrapper.
     *
     * <p>A valid customer draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has a email which is not blank (null/empty)</li>
     * <li>It has not null addresses</li>
     * <li>It has valid addresses with keys is not blank (null/empty)</li>
     * <li>It has unique address keys</li>
     * <li>It has valid valid 'defaultBillingAddress', 'billingAddresses',
     * 'defaultShippingAddress', 'shippingAddresses'</li>
     * </ol>
     *
     * @param customerDrafts the customer drafts to validate and collect referenced keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;,
     *      {@link CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     *      referenced keys within a wrapper.
     */
    @Override
    public ImmutablePair<Set<CustomerDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<CustomerDraft> customerDrafts) {

        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<CustomerDraft> validDrafts = customerDrafts
            .stream()
            .filter(this::isValidCustomerDraft)
            .peek(customerDraft -> collectReferencedKeys(referencedKeys, customerDraft))
            .collect(toSet());

        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidCustomerDraft(@Nullable final CustomerDraft customerDraft) {

        if (customerDraft == null) {
            handleError(CUSTOMER_DRAFT_IS_NULL);
        } else if (isBlank(customerDraft.getKey())) {
            handleError(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        } else if (isBlank(customerDraft.getEmail())) {
            handleError(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        } else if (hasValidAddresses(customerDraft)) {
            // todo (ahmetoz)
            // what to do for password ?
            return hasValidBillingAndShippingAddresses(customerDraft);
        }
        return false;
    }

    private boolean hasValidAddresses(@Nonnull final CustomerDraft customerDraft) {

        final List<Address> addressList = customerDraft.getAddresses();
        if (addressList == null || addressList.isEmpty()) {
            return true;
        }

        final List<Integer> indexesOfNullAddresses = getIndexes(addressList, Objects::isNull);
        if (!indexesOfNullAddresses.isEmpty()) {
            handleError(format(ADDRESSES_ARE_NULL, customerDraft.getKey(), indexesOfNullAddresses));
            return false;
        }

        final List<Integer> indexesOfBlankAddressKeys = getIndexes(addressList, address -> isBlank(address.getKey()));
        if (!indexesOfBlankAddressKeys.isEmpty()) {
            handleError(format(ADDRESSES_THAT_KEYS_NOT_SET, customerDraft.getKey(), indexesOfBlankAddressKeys));
            return false;
        }

        final Predicate<Address> searchDuplicateKeys = (Address address) ->
            addressList.stream().filter(a -> Objects.equals(a.getKey(), address.getKey())).count() > 1;

        final List<Integer> indexesOfDuplicateAddressKeys = getIndexes(addressList, searchDuplicateKeys);
        if (!indexesOfDuplicateAddressKeys.isEmpty()) {
            handleError(format(ADDRESSES_THAT_KEYS_NOT_UNIQUE, customerDraft.getKey(), indexesOfDuplicateAddressKeys));
            return false;
        }

        return true;
    }

    private List<Integer> getIndexes(@Nonnull final List<Address> list, @Nonnull final Predicate<Address> predicate) {

        final List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private boolean hasValidBillingAndShippingAddresses(@Nonnull final CustomerDraft customerDraft) {
        /* An example error response from the API, when the indexes are not valid:
           {
                "statusCode": 400,
                "message": "Customer does not contain an address at index '1'.",
                "errors": [{
                    "code": "InvalidOperation",
                    "message": "Customer does not contain an address at index '-1'."
                },
                {
                    "code": "InvalidOperation",
                    "message": "Customer does not contain an address at index '1'."
                }]
            }
         */

        final List<Address> addressList = customerDraft.getAddresses();
        final boolean hasAddresses = addressList != null && !addressList.isEmpty();

        final Predicate<Integer> isMissingIndex = index -> index == null
            || (hasAddresses && (index < 0 || index > addressList.size() - 1));

        if (customerDraft.getDefaultBillingAddress() != null
            && isMissingIndex.test(customerDraft.getDefaultBillingAddress())) {
            handleError(format(DEFAULT_BILLING_ADDRESS_IS_NOT_VALID,
                customerDraft.getKey(), customerDraft.getDefaultBillingAddress()));
            return false;
        }

        if (customerDraft.getBillingAddresses() != null && !customerDraft.getBillingAddresses().isEmpty()) {
            final List<Integer> inValidIndexes = getIndexValues(customerDraft.getBillingAddresses(), isMissingIndex);
            if (!inValidIndexes.isEmpty()) {
                handleError(format(BILLING_ADDRESSES_ARE_NOT_VALID, customerDraft.getKey(), inValidIndexes));
                return false;
            }
        }

        if (customerDraft.getDefaultShippingAddress() != null
            && isMissingIndex.test(customerDraft.getDefaultShippingAddress())) {
            handleError(format(DEFAULT_SHIPPING_ADDRESS_IS_NOT_VALID,
                customerDraft.getKey(), customerDraft.getDefaultShippingAddress()));
            return false;
        }

        if (customerDraft.getShippingAddresses() != null && !customerDraft.getShippingAddresses().isEmpty()) {
            final List<Integer> inValidIndexes = getIndexValues(customerDraft.getBillingAddresses(), isMissingIndex);
            if (!inValidIndexes.isEmpty()) {
                handleError(format(SHIPPING_ADDRESSES_ARE_NOT_VALID, customerDraft.getKey(), inValidIndexes));
                return false;
            }
        }

        return true;
    }

    private List<Integer> getIndexValues(@Nonnull final List<Integer> list,
        @Nonnull final Predicate<Integer> predicate) {

        final List<Integer> indexes = new ArrayList<>();
        for (Integer integer : list) {
            if (predicate.test(integer)) {
                indexes.add(integer);
            }
        }
        return indexes;
    }

    private void collectReferencedKeys(
        @Nonnull final CustomerBatchValidator.ReferencedKeys referencedKeys,
        @Nonnull final CustomerDraft customerDraft) {

        collectReferencedKeyFromResourceIdentifier(customerDraft.getCustomerGroup(),
            referencedKeys.customerGroupKeys::add);
        collectReferencedKeyFromCustomFieldsDraft(customerDraft.getCustom(),
            referencedKeys.typeKeys::add);
        // todo (ahmetoz) stores.
    }

    public static class ReferencedKeys {
        private final Set<String> customerGroupKeys = new HashSet<>();
        private final Set<String> typeKeys = new HashSet<>();

        public Set<String> getTypeKeys() {
            return typeKeys;
        }

        public Set<String> getCustomerGroupKeys() {
            return customerGroupKeys;
        }
    }
}
