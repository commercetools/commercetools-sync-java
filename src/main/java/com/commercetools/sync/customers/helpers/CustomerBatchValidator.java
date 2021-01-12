package com.commercetools.sync.customers.helpers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.customers.CustomerSyncOptions;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.models.Address;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CustomerBatchValidator
    extends BaseBatchValidator<CustomerDraft, CustomerSyncOptions, CustomerSyncStatistics> {

  public static final String CUSTOMER_DRAFT_IS_NULL = "CustomerDraft is null.";
  public static final String CUSTOMER_DRAFT_KEY_NOT_SET =
      "CustomerDraft with email: %s doesn't have a key. "
          + "Please make sure all customer drafts have keys.";
  public static final String CUSTOMER_DRAFT_EMAIL_NOT_SET =
      "CustomerDraft with key: %s doesn't have an email. "
          + "Please make sure all customer drafts have emails.";
  static final String CUSTOMER_DRAFT_PASSWORD_NOT_SET =
      "CustomerDraft with key: %s doesn't have a password. "
          + "Please make sure all customer drafts have passwords.";

  static final String ADDRESSES_ARE_NULL =
      "CustomerDraft with key: '%s' has null addresses on indexes: '%s'. "
          + "Please make sure each address is set in the addresses list.";
  static final String ADDRESSES_THAT_KEYS_NOT_SET =
      "CustomerDraft with key: '%s' has blank address keys on indexes: "
          + "'%s'. Please make sure each address has a key in the addresses list";
  static final String ADDRESSES_THAT_KEYS_NOT_UNIQUE =
      "CustomerDraft with key: '%s' has duplicate address keys on "
          + "indexes: '%s'. Please make sure each address key is unique in the addresses list.";

  static final String BILLING_ADDRESSES_ARE_NOT_VALID =
      "CustomerDraft with key: '%s' does not contain indices: '%s' "
          + "of the 'billingAddresses' in the addresses list. "
          + "Please make sure all customer drafts have valid index values for the 'billingAddresses'.";

  static final String SHIPPING_ADDRESSES_ARE_NOT_VALID =
      "CustomerDraft with key: '%s' does not contain indices: '%s'"
          + " of the 'shippingAddresses' in the addresses list. "
          + "Please make sure all customer drafts have valid index values for the 'shippingAddresses'.";

  public CustomerBatchValidator(
      @Nonnull final CustomerSyncOptions syncOptions,
      @Nonnull final CustomerSyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link CustomerDraft}&gt; of drafts this method attempts to validate
   * drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;, {@link
   * CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
   * referenced keys within a wrapper.
   *
   * <p>A valid customer draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   *   <li>It has a email which is not blank (null/empty)
   *   <li><b>Each address</b> in the addresses list satisfies the following conditions:
   *       <ol>
   *         <li>It is not null
   *         <li>It has a key which is not blank (null/empty)
   *         <li>It has a unique key
   *       </ol>
   *   <li><b>Each address index</b> in the 'billing' and 'shipping' addresses list are valid and
   *       contained in the addresses list.
   * </ol>
   *
   * @param customerDrafts the customer drafts to validate and collect referenced keys.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;, {@link
   *     CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts
   *     and referenced keys within a wrapper.
   */
  @Override
  public ImmutablePair<Set<CustomerDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
      @Nonnull final List<CustomerDraft> customerDrafts) {

    final ReferencedKeys referencedKeys = new ReferencedKeys();
    final Set<CustomerDraft> validDrafts =
        customerDrafts.stream()
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
    } else if (isBlank(customerDraft.getPassword())) {
      handleError(format(CUSTOMER_DRAFT_PASSWORD_NOT_SET, customerDraft.getKey()));
    } else if (hasValidAddresses(customerDraft)) {
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

    final List<Integer> indexesOfBlankAddressKeys =
        getIndexes(addressList, address -> isBlank(address.getKey()));
    if (!indexesOfBlankAddressKeys.isEmpty()) {
      handleError(
          format(ADDRESSES_THAT_KEYS_NOT_SET, customerDraft.getKey(), indexesOfBlankAddressKeys));
      return false;
    }

    final Predicate<Address> searchDuplicateKeys =
        (Address address) ->
            addressList.stream().filter(a -> Objects.equals(a.getKey(), address.getKey())).count()
                > 1;

    final List<Integer> indexesOfDuplicateAddressKeys =
        getIndexes(addressList, searchDuplicateKeys);
    if (!indexesOfDuplicateAddressKeys.isEmpty()) {
      handleError(
          format(
              ADDRESSES_THAT_KEYS_NOT_UNIQUE,
              customerDraft.getKey(),
              indexesOfDuplicateAddressKeys));
      return false;
    }

    return true;
  }

  @Nonnull
  private List<Integer> getIndexes(
      @Nonnull final List<Address> list, @Nonnull final Predicate<Address> predicate) {

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
    final Predicate<Integer> isInvalidIndex =
        index ->
            index == null
                || addressList == null
                || addressList.isEmpty()
                || index < 0
                || index > addressList.size() - 1;

    if (customerDraft.getBillingAddresses() != null
        && !customerDraft.getBillingAddresses().isEmpty()) {
      final List<Integer> invalidIndexes =
          getIndexValues(customerDraft.getBillingAddresses(), isInvalidIndex);
      if (!invalidIndexes.isEmpty()) {
        handleError(
            format(BILLING_ADDRESSES_ARE_NOT_VALID, customerDraft.getKey(), invalidIndexes));
        return false;
      }
    }

    if (customerDraft.getShippingAddresses() != null
        && !customerDraft.getShippingAddresses().isEmpty()) {
      final List<Integer> invalidIndexes =
          getIndexValues(customerDraft.getShippingAddresses(), isInvalidIndex);
      if (!invalidIndexes.isEmpty()) {
        handleError(
            format(SHIPPING_ADDRESSES_ARE_NOT_VALID, customerDraft.getKey(), invalidIndexes));
        return false;
      }
    }

    return true;
  }

  @Nonnull
  private List<Integer> getIndexValues(
      @Nonnull final List<Integer> list, @Nonnull final Predicate<Integer> predicate) {

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

    collectReferencedKeyFromResourceIdentifier(
        customerDraft.getCustomerGroup(), referencedKeys.customerGroupKeys::add);
    collectReferencedKeyFromCustomFieldsDraft(
        customerDraft.getCustom(), referencedKeys.typeKeys::add);
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
