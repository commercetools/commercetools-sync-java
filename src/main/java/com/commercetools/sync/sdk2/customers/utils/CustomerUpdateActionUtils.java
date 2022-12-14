package com.commercetools.sync.sdk2.customers.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.common.BaseAddress;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerAddAddressAction;
import com.commercetools.api.models.customer.CustomerAddAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerAddBillingAddressIdAction;
import com.commercetools.api.models.customer.CustomerAddBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerAddShippingAddressIdAction;
import com.commercetools.api.models.customer.CustomerAddShippingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerAddStoreAction;
import com.commercetools.api.models.customer.CustomerAddStoreActionBuilder;
import com.commercetools.api.models.customer.CustomerChangeAddressAction;
import com.commercetools.api.models.customer.CustomerChangeAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerChangeEmailAction;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerRemoveAddressAction;
import com.commercetools.api.models.customer.CustomerRemoveAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveBillingAddressIdAction;
import com.commercetools.api.models.customer.CustomerRemoveBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveShippingAddressIdAction;
import com.commercetools.api.models.customer.CustomerRemoveShippingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveStoreAction;
import com.commercetools.api.models.customer.CustomerRemoveStoreActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCompanyNameAction;
import com.commercetools.api.models.customer.CustomerSetCompanyNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomerGroupAction;
import com.commercetools.api.models.customer.CustomerSetCustomerGroupActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomerNumberAction;
import com.commercetools.api.models.customer.CustomerSetCustomerNumberActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDateOfBirthAction;
import com.commercetools.api.models.customer.CustomerSetDateOfBirthActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultBillingAddressAction;
import com.commercetools.api.models.customer.CustomerSetDefaultBillingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultShippingAddressAction;
import com.commercetools.api.models.customer.CustomerSetDefaultShippingAddressActionBuilder;
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
import com.commercetools.api.models.customer.CustomerSetStoresAction;
import com.commercetools.api.models.customer.CustomerSetStoresActionBuilder;
import com.commercetools.api.models.customer.CustomerSetTitleAction;
import com.commercetools.api.models.customer.CustomerSetTitleActionBuilder;
import com.commercetools.api.models.customer.CustomerSetVatIdAction;
import com.commercetools.api.models.customer.CustomerSetVatIdActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.store.StoreKeyReference;
import com.commercetools.api.models.store.StoreResourceIdentifier;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomerUpdateActionUtils {

  public static final String CUSTOMER_NUMBER_EXISTS_WARNING =
      "Customer with key: \"%s\" has "
          + "already a customer number: \"%s\", once it's set it cannot be changed. "
          + "Hereby, the update action is not created.";

  private CustomerUpdateActionUtils() {}

  /**
   * Compares the {@code email} values of a {@link Customer} and a {@link CustomerDraft} and returns
   * an {@link Optional} of update action, which would contain the {@code "changeEmail"} {@link
   * CustomerChangeEmailAction}. If both {@link Customer} and {@link CustomerDraft} have the same
   * {@code email} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new email.
   * @return optional containing update action or empty optional if emails are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildChangeEmailUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getEmail(),
        newCustomer.getEmail(),
        () -> CustomerChangeEmailActionBuilder.of().email(newCustomer.getEmail()).build());
  }

  /**
   * Compares the {@code firstName} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setFirstName"}
   * {@link CustomerSetFirstNameAction}. If both {@link Customer} and {@link CustomerDraft} have the
   * same {@code firstName} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new first name.
   * @return optional containing update action or empty optional if first names are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetFirstNameUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getFirstName(),
        newCustomer.getFirstName(),
        () -> CustomerSetFirstNameActionBuilder.of().firstName(newCustomer.getFirstName()).build());
  }

  /**
   * Compares the {@code lastName} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setLastName"}
   * {@link CustomerSetLastNameAction}. If both {@link Customer} and {@link CustomerDraft} have the
   * same {@code lastName} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new last name.
   * @return optional containing update action or empty optional if last names are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetLastNameUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getLastName(),
        newCustomer.getLastName(),
        () -> CustomerSetLastNameActionBuilder.of().lastName(newCustomer.getLastName()).build());
  }

  /**
   * Compares the {@code middleName} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setMiddleName"}
   * {@link CustomerSetMiddleNameAction}. If both {@link Customer} and {@link CustomerDraft} have
   * the same {@code middleName} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new middle name.
   * @return optional containing update action or empty optional if middle names are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetMiddleNameUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getMiddleName(),
        newCustomer.getMiddleName(),
        () ->
            CustomerSetMiddleNameActionBuilder.of()
                .middleName(newCustomer.getMiddleName())
                .build());
  }

  /**
   * Compares the {@code title} values of a {@link Customer} and a {@link CustomerDraft} and returns
   * an {@link Optional} of update action, which would contain the {@code "setTitle"} {@link
   * CustomerSetTitleAction}. If both {@link Customer} and {@link CustomerDraft} have the same
   * {@code title} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new title.
   * @return optional containing update action or empty optional if titles are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetTitleUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getTitle(),
        newCustomer.getTitle(),
        () -> CustomerSetTitleActionBuilder.of().title(newCustomer.getTitle()).build());
  }

  /**
   * Compares the {@code salutation} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "SetSalutation"}
   * {@link CustomerSetSalutationAction}. If both {@link Customer} and {@link CustomerDraft} have
   * the same {@code salutation} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the Customer that should be updated.
   * @param newCustomer the Customer draft that contains the new salutation.
   * @return optional containing update action or empty optional if salutations are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetSalutationUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getSalutation(),
        newCustomer.getSalutation(),
        () ->
            CustomerSetSalutationActionBuilder.of()
                .salutation(newCustomer.getSalutation())
                .build());
  }

  /**
   * Compares the {@code customerNumber} values of a {@link Customer} and a {@link CustomerDraft}
   * and returns an {@link Optional} of update action, which would contain the {@code
   * "setCustomerNumber"} {@link CustomerSetCustomerNumberAction}. If both {@link Customer} and
   * {@link CustomerDraft} have the same {@code customerNumber} values, then no update action is
   * needed and empty optional will be returned.
   *
   * <p>Note: Customer number should be unique across a project. Once it's set it cannot be changed.
   * For this case, warning callback will be triggered and an empty optional will be returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new customer number.
   * @param syncOptions responsible for supplying the sync options to the sync utility method. It is
   *     used for triggering the warning callback when trying to change an existing customer number.
   * @return optional containing update action or empty optional if customer numbers are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetCustomerNumberUpdateAction(
      @Nonnull final Customer oldCustomer,
      @Nonnull final CustomerDraft newCustomer,
      @Nonnull final CustomerSyncOptions syncOptions) {

    final Optional<CustomerUpdateAction> setCustomerNumberAction =
        buildUpdateAction(
            oldCustomer.getCustomerNumber(),
            newCustomer.getCustomerNumber(),
            () ->
                CustomerSetCustomerNumberActionBuilder.of()
                    .customerNumber(newCustomer.getCustomerNumber())
                    .build());

    if (setCustomerNumberAction.isPresent() && !isBlank(oldCustomer.getCustomerNumber())) {

      syncOptions.applyWarningCallback(
          new SyncException(
              format(
                  CUSTOMER_NUMBER_EXISTS_WARNING,
                  oldCustomer.getKey(),
                  oldCustomer.getCustomerNumber())),
          oldCustomer,
          newCustomer);

      return Optional.empty();
    }

    return setCustomerNumberAction;
  }

  /**
   * Compares the {@code externalId} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setExternalId"}
   * {@link CustomerSetExternalIdAction}. If both {@link Customer} and {@link CustomerDraft} have
   * the same {@code externalId} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new external id.
   * @return optional containing update action or empty optional if external ids are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetExternalIdUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getExternalId(),
        newCustomer.getExternalId(),
        () ->
            CustomerSetExternalIdActionBuilder.of()
                .externalId(newCustomer.getExternalId())
                .build());
  }

  /**
   * Compares the {@code companyName} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setCompanyName"}
   * {@link CustomerSetCompanyNameAction}. If both {@link Customer} and {@link CustomerDraft} have
   * the same {@code companyName} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new company name.
   * @return optional containing update action or empty optional if company names are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetCompanyNameUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getCompanyName(),
        newCustomer.getCompanyName(),
        () ->
            CustomerSetCompanyNameActionBuilder.of()
                .companyName(newCustomer.getCompanyName())
                .build());
  }

  /**
   * Compares the {@code dateOfBirth} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setDateOfBirth"}
   * {@link CustomerSetDateOfBirthAction}. If both {@link Customer} and {@link CustomerDraft} have
   * the same {@code dateOfBirth} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new date of birth.
   * @return optional containing update action or empty optional if dates of birth are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetDateOfBirthUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getDateOfBirth(),
        newCustomer.getDateOfBirth(),
        () ->
            CustomerSetDateOfBirthActionBuilder.of()
                .dateOfBirth(newCustomer.getDateOfBirth())
                .build());
  }

  /**
   * Compares the {@code vatId} values of a {@link Customer} and a {@link CustomerDraft} and returns
   * an {@link Optional} of update action, which would contain the {@code "setVatId"} {@link
   * CustomerSetVatIdAction}. If both {@link Customer} and {@link CustomerDraft} have the same
   * {@code vatId} values, then no update action is needed and empty optional will be returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new vat id.
   * @return optional containing update action or empty optional if vat ids are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetVatIdUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getVatId(),
        newCustomer.getVatId(),
        () -> CustomerSetVatIdActionBuilder.of().vatId(newCustomer.getVatId()).build());
  }

  /**
   * Compares the {@code locale} values of a {@link Customer} and a {@link CustomerDraft} and
   * returns an {@link Optional} of update action, which would contain the {@code "setLocale"}
   * {@link CustomerSetLocaleAction}. If both {@link Customer} and {@link CustomerDraft} have the
   * same {@code locale} values, then no update action is needed and empty optional will be
   * returned.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft that contains the new locale.
   * @return optional containing update action or empty optional if locales are identical.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetLocaleUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateAction(
        oldCustomer.getLocale(),
        newCustomer.getLocale(),
        () -> CustomerSetLocaleActionBuilder.of().locale(newCustomer.getLocale()).build());
  }

  /**
   * Compares the {@link CustomerGroup} references of an old {@link Customer} and new {@link
   * CustomerDraft}. If they are different - return {@link CustomerSetCustomerGroupAction} update
   * action.
   *
   * <p>If the old value is set, but the new one is empty - the command will unset the customer
   * group.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft with new {@link CustomerGroup} reference.
   * @return An optional with {@link CustomerUpdateAction} update action.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetCustomerGroupUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    return buildUpdateActionForReferences(
        oldCustomer.getCustomerGroup(),
        newCustomer.getCustomerGroup(),
        () ->
            CustomerSetCustomerGroupActionBuilder.of()
                .customerGroup(newCustomer.getCustomerGroup())
                .build());
  }

  /**
   * Compares the stores of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link
   * List} of {@link CustomerUpdateAction} as a result. If no update action is needed, for example
   * in case where both the {@link Customer} and the {@link CustomerDraft} have the identical
   * stores, an empty {@link List} is returned.
   *
   * <p>Note: Null values of the stores are filtered out.
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new data.
   * @return A list of customer store-related update actions.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildStoreUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final List<StoreKeyReference> oldStores = oldCustomer.getStores();
    final List<StoreResourceIdentifier> newStores = newCustomer.getStores();

    return buildSetStoreUpdateAction(oldStores, newStores)
        .map(Collections::singletonList)
        .orElseGet(() -> prepareStoreActions(oldStores, newStores));
  }

  private static List<CustomerUpdateAction> prepareStoreActions(
      @Nullable final List<StoreKeyReference> oldStores,
      @Nullable final List<StoreResourceIdentifier> newStores) {

    if (oldStores != null && newStores != null) {
      final List<CustomerUpdateAction> removeStoreUpdateActions =
          buildRemoveStoreUpdateActions(oldStores, newStores);

      final List<CustomerUpdateAction> addStoreUpdateActions =
          buildAddStoreUpdateActions(oldStores, newStores);

      if (!removeStoreUpdateActions.isEmpty() && !addStoreUpdateActions.isEmpty()) {
        return buildSetStoreUpdateAction(newStores)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
      }

      return removeStoreUpdateActions.isEmpty() ? addStoreUpdateActions : removeStoreUpdateActions;
    }

    return emptyList();
  }

  /**
   * Compares the {@link List} of {@link StoreKeyReference}s and {@link StoreResourceIdentifier}s of
   * a {@link CustomerDraft} and a {@link Customer}. It returns a {@link CustomerSetStoresAction}
   * update action as a result. If both the {@link Customer} and the {@link CustomerDraft} have the
   * same set of stores, then no update actions are needed and hence an empty {@link List} is
   * returned.
   *
   * <p>Note: Null values of the stores are filtered out.
   *
   * @param oldStores the stores which should be updated.
   * @param newStores the stores where we get the new store.
   * @return A list containing the update actions or an empty list if the store references are
   *     identical.
   */
  @Nonnull
  private static Optional<CustomerUpdateAction> buildSetStoreUpdateAction(
      @Nullable final List<StoreKeyReference> oldStores,
      @Nullable final List<StoreResourceIdentifier> newStores) {

    if (oldStores != null && !oldStores.isEmpty()) {
      if (newStores == null || newStores.isEmpty()) {
        return Optional.of(CustomerSetStoresActionBuilder.of().stores(emptyList()).build());
      }
    } else if (newStores != null && !newStores.isEmpty()) {
      return buildSetStoreUpdateAction(newStores);
    }

    return Optional.empty();
  }

  private static Optional<CustomerUpdateAction> buildSetStoreUpdateAction(
      @Nonnull final List<StoreResourceIdentifier> newStores) {

    final List<StoreResourceIdentifier> stores =
        newStores.stream().filter(Objects::nonNull).collect(toList());

    if (!stores.isEmpty()) {
      return Optional.of(CustomerSetStoresActionBuilder.of().stores(stores).build());
    }

    return Optional.empty();
  }

  /**
   * Compares the {@link List} of {@link StoreKeyReference}s and {@link StoreResourceIdentifier}s of
   * a {@link CustomerDraft} and a {@link Customer}. It returns a {@link List} of {@link
   * CustomerRemoveStoreAction} update actions as a result, if the old store needs to be removed
   * from a customer to have the same set of stores as the new customer. If both the {@link
   * Customer} and the {@link CustomerDraft} have the same set of stores, then no update actions are
   * needed and hence an empty {@link List} is returned.
   *
   * <p>Note: Null values of the stores are filtered out.
   *
   * @param oldStores the stores which should be updated.
   * @param newStores the stores where we get the new store.
   * @return A list containing the update actions or an empty list if the store references are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildRemoveStoreUpdateActions(
      @Nonnull final List<StoreKeyReference> oldStores,
      @Nonnull final List<StoreResourceIdentifier> newStores) {

    final Map<String, StoreResourceIdentifier> newStoreKeyToStoreMap =
        newStores.stream()
            .filter(Objects::nonNull)
            .filter(storeResourceIdentifier -> storeResourceIdentifier.getKey() != null)
            .collect(toMap(StoreResourceIdentifier::getKey, identity()));

    return oldStores.stream()
        .filter(Objects::nonNull)
        .filter(storeKeyReference -> !newStoreKeyToStoreMap.containsKey(storeKeyReference.getKey()))
        .map(
            storeKeyReference ->
                CustomerRemoveStoreActionBuilder.of()
                    .store(
                        StoreResourceIdentifierBuilder.of().key(storeKeyReference.getKey()).build())
                    .build())
        .collect(toList());
  }

  /**
   * Compares the {@link List} of {@link StoreKeyReference}s and {@link StoreResourceIdentifier}s of
   * a {@link CustomerDraft} and a {@link Customer}. It returns a {@link List} of {@link
   * CustomerAddStoreAction} update actions as a result, if the old store needs to be added to a
   * customer to have the same set of stores as the new customer. If both the {@link Customer} and
   * the {@link CustomerDraft} have the same set of stores, then no update actions are needed and
   * hence an empty {@link List} is returned.
   *
   * <p>Note: Null values of the stores are filtered out.
   *
   * @param oldStores the stores which should be updated.
   * @param newStores the stores where we get the new store.
   * @return A list containing the update actions or an empty list if the store references are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildAddStoreUpdateActions(
      @Nonnull final List<StoreKeyReference> oldStores,
      @Nonnull final List<StoreResourceIdentifier> newStores) {

    final Map<String, StoreKeyReference> oldStoreKeyToStoreMap =
        oldStores.stream()
            .filter(Objects::nonNull)
            .collect(toMap(StoreKeyReference::getKey, identity()));

    return newStores.stream()
        .filter(Objects::nonNull)
        .filter(
            storeResourceIdentifier ->
                !oldStoreKeyToStoreMap.containsKey(storeResourceIdentifier.getKey()))
        .map(
            storeResourceIdentifier ->
                CustomerAddStoreActionBuilder.of().store(storeResourceIdentifier).build())
        .collect(toList());
  }

  /**
   * Compares the addresses of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link
   * List} of {@link CustomerUpdateAction} as a result. If both the {@link Customer} and the {@link
   * CustomerDraft} have the same set of addresses, then no update actions are needed and hence an
   * empty {@link List} is returned.
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new data.
   * @return A list of customer address-related update actions.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildAllAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final List<CustomerUpdateAction> addressActions = new ArrayList<>();

    final List<CustomerUpdateAction> removeAddressActions =
        buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    addressActions.addAll(removeAddressActions);
    addressActions.addAll(buildChangeAddressUpdateActions(oldCustomer, newCustomer));
    addressActions.addAll(buildAddAddressUpdateActions(oldCustomer, newCustomer));

    addressActions.addAll(
        collectAndFilterRemoveShippingAndBillingActions(
            removeAddressActions, oldCustomer, newCustomer));

    buildSetDefaultShippingAddressUpdateAction(oldCustomer, newCustomer)
        .ifPresent(addressActions::add);
    buildSetDefaultBillingAddressUpdateAction(oldCustomer, newCustomer)
        .ifPresent(addressActions::add);

    addressActions.addAll(buildAddShippingAddressUpdateActions(oldCustomer, newCustomer));
    addressActions.addAll(buildAddBillingAddressUpdateActions(oldCustomer, newCustomer));

    return addressActions;
  }

  @Nonnull
  private static List<CustomerUpdateAction> collectAndFilterRemoveShippingAndBillingActions(
      @Nonnull final List<CustomerUpdateAction> removeAddressActions,
      @Nonnull final Customer oldCustomer,
      @Nonnull final CustomerDraft newCustomer) {

    /* An action combination like below will cause a bad request error in API, so we need to filter out
    to avoid such cases:

       {
           "version": 1,
           "actions": [
               {
                   "action" : "removeAddress",
                   "addressId": "-FWSGZNy"
               },
               {
                   "action" : "removeBillingAddressId",
                   "addressId" : "-FWSGZNy"
               }
           ]
       }

       {
           "statusCode": 400,
           "message": "The customers billingAddressIds don't contain id '-FWSGZNy'.",
           "errors": [
               {
                   "code": "InvalidOperation",
                   "message": "The customers billingAddressIds don't contain id '-FWSGZNy'.",
                   "action": {
                       "action": "removeBillingAddressId",
                       "addressId": "-FWSGZNy"
                   },
                   "actionIndex": 2
               }
           ]
       }
    */
    final Set<String> addressIdsToRemove =
        removeAddressActions.stream()
            .map(customerUpdateAction -> (CustomerRemoveAddressAction) customerUpdateAction)
            .map(CustomerRemoveAddressAction::getAddressId)
            .collect(toSet());

    final List<CustomerUpdateAction> removeActions = new ArrayList<>();

    removeActions.addAll(
        buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer).stream()
            .map(
                customerUpdateAction ->
                    (CustomerRemoveShippingAddressIdAction) customerUpdateAction)
            .filter(action -> !addressIdsToRemove.contains(action.getAddressId()))
            .collect(toList()));

    removeActions.addAll(
        buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer).stream()
            .map(
                customerUpdateAction -> (CustomerRemoveBillingAddressIdAction) customerUpdateAction)
            .filter(action -> !addressIdsToRemove.contains(action.getAddressId()))
            .collect(toList()));

    return removeActions;
  }

  /**
   * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link
   * Customer#getAddresses()}. It returns a {@link List} of {@link CustomerRemoveAddressAction}
   * update actions as a result, if the old address needs to be removed from the {@code oldCustomer}
   * to have the same set of addresses as the {@code newCustomer}. If both the {@link Customer} and
   * the {@link CustomerDraft} have the same set of addresses, then no update actions are needed and
   * hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Null values of the new addresses are filtered out.
   *   <li>Address values without keys are filtered out.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new addresses.
   * @return A list containing the update actions or an empty list if the addresses are identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildRemoveAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    if (oldCustomer.getAddresses().isEmpty()) {
      return Collections.emptyList();
    }

    if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {

      return oldCustomer.getAddresses().stream()
          .map(
              address -> CustomerRemoveAddressActionBuilder.of().addressId(address.getId()).build())
          .collect(Collectors.toList());
    }

    final Set<String> newAddressKeys =
        newCustomer.getAddresses().stream()
            .filter(Objects::nonNull)
            .map(BaseAddress::getKey)
            .filter(key -> !isBlank(key))
            .collect(toSet());

    return oldCustomer.getAddresses().stream()
        .filter(
            oldAddress ->
                isBlank(oldAddress.getKey()) || !newAddressKeys.contains(oldAddress.getKey()))
        .map(
            address -> {
              if (address.getId() != null) {
                return CustomerRemoveAddressActionBuilder.of().addressId(address.getId()).build();
              }

              return CustomerRemoveAddressActionBuilder.of().addressKey(address.getKey()).build();
            })
        .collect(toList());
  }

  /**
   * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link
   * Customer#getAddresses()}. It returns a {@link List} of {@link CustomerChangeAddressAction}
   * update actions as a result, if the old address needs to be changed/updated from the {@code
   * oldCustomer} to have the same set of addresses as the {@code newCustomer}. If both the {@link
   * Customer} and the {@link CustomerDraft} have the same set of addresses, then no update actions
   * are needed and hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Null values of the new addresses are filtered out.
   *   <li>Address values without keys are filtered out.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new addresses.
   * @return A list containing the update actions or an empty list if the addresses are identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildChangeAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {
      return Collections.emptyList();
    }

    final Map<String, Address> oldAddressKeyToAddressMap =
        oldCustomer.getAddresses().stream()
            .filter(address -> !isBlank(address.getKey()))
            .collect(toMap(BaseAddress::getKey, identity()));

    return newCustomer.getAddresses().stream()
        .filter(Objects::nonNull)
        .filter(newAddress -> !isBlank(newAddress.getKey()))
        .filter(newAddress -> oldAddressKeyToAddressMap.containsKey(newAddress.getKey()))
        .map(
            newAddress -> {
              final Address oldAddress = oldAddressKeyToAddressMap.get(newAddress.getKey());
              if (!newAddress.equalsIgnoreId(oldAddress)) {
                return CustomerChangeAddressActionBuilder.of()
                    .addressId(oldAddress.getId())
                    .address(newAddress)
                    .build();
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  /**
   * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link
   * Customer#getAddresses()}. It returns a {@link List} of {@link CustomerAddAddressAction} update
   * actions as a result, if the new address needs to be added to have the same set of addresses as
   * the {@code newCustomer}. If both the {@link Customer} and the {@link CustomerDraft} have the
   * same set of addresses, then no update actions are needed and hence an empty {@link List} is
   * returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Null values of the new addresses are filtered out.
   *   <li>Address values without keys are filtered out.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new addresses.
   * @return A list containing the update actions or an empty list if the addresses are identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildAddAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {
      return Collections.emptyList();
    }

    final Map<String, Address> oldAddressKeyToAddressMap =
        oldCustomer.getAddresses().stream()
            .filter(address -> !isBlank(address.getKey()))
            .collect(toMap(Address::getKey, identity()));

    return newCustomer.getAddresses().stream()
        .filter(Objects::nonNull)
        .filter(newAddress -> !isBlank(newAddress.getKey()))
        .filter(newAddress -> !oldAddressKeyToAddressMap.containsKey(newAddress.getKey()))
        .map(newAddress -> CustomerAddAddressActionBuilder.of().address(newAddress).build())
        .collect(toList());
  }

  /**
   * Compares the {@link Customer#getDefaultShippingAddressId()} and {@link
   * CustomerDraft#getDefaultShippingAddress()}. If they are different - return {@link
   * CustomerSetDefaultShippingAddressAction} update action. If the old shipping address is set, but
   * the new one is empty - the command will unset the default shipping address.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft with new default shipping address.
   * @return An optional with {@link CustomerSetDefaultShippingAddressAction} update action.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetDefaultShippingAddressUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final Optional<Address> oldAddress =
        oldCustomer.getAddresses().stream()
            .filter(
                address ->
                    address.getId() != null
                        && address.getId().equals(oldCustomer.getDefaultShippingAddressId()))
            .findFirst();
    final String newAddressKey =
        getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultShippingAddress());

    if (newAddressKey != null) {
      if (!oldAddress.isPresent() || !Objects.equals(oldAddress.get().getKey(), newAddressKey)) {
        return Optional.of(
            CustomerSetDefaultShippingAddressActionBuilder.of().addressKey(newAddressKey).build());
      }
    } else if (oldAddress.isPresent()) { // unset
      return Optional.of(CustomerSetDefaultShippingAddressActionBuilder.of().build());
    }

    return Optional.empty();
  }

  /**
   * Compares the {@link Customer#getDefaultBillingAddressId()} and {@link
   * CustomerDraft#getDefaultBillingAddress()}. If they are different - return {@link
   * CustomerSetDefaultBillingAddressAction} update action. If the old billing address id value is
   * set, but the new one is empty - the command will unset the default billing address.
   *
   * @param oldCustomer the customer that should be updated.
   * @param newCustomer the customer draft with new default billing address.
   * @return An optional with {@link CustomerSetDefaultBillingAddressAction} update action.
   */
  @Nonnull
  public static Optional<CustomerUpdateAction> buildSetDefaultBillingAddressUpdateAction(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final Optional<Address> oldAddress =
        oldCustomer.getAddresses().stream()
            .filter(
                address ->
                    address.getId() != null
                        && address.getId().equals(oldCustomer.getDefaultBillingAddressId()))
            .findFirst();

    final String newAddressKey =
        getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultBillingAddress());

    if (newAddressKey != null) {
      if (!oldAddress.isPresent() || !Objects.equals(oldAddress.get().getKey(), newAddressKey)) {
        return Optional.of(
            CustomerSetDefaultBillingAddressActionBuilder.of().addressKey(newAddressKey).build());
      }
    } else if (oldAddress.isPresent()) { // unset
      return Optional.of(CustomerSetDefaultBillingAddressActionBuilder.of().build());
    }

    return Optional.empty();
  }

  @Nullable
  private static String getAddressKeyAt(
      @Nullable final List<BaseAddress> addressList, @Nullable final Integer index) {

    if (index == null) {
      return null;
    }

    if (addressList == null || index < 0 || index >= addressList.size()) {
      throw new IllegalArgumentException(
          format("Addresses list does not contain an address at the index: %s", index));
    }

    final BaseAddress address = addressList.get(index);
    if (address == null) {
      throw new IllegalArgumentException(
          format("Address is null at the index: %s of the addresses list.", index));
    } else if (isBlank(address.getKey())) {
      throw new IllegalArgumentException(
          format("Address does not have a key at the index: %s of the addresses list.", index));
    } else {
      return address.getKey();
    }
  }

  /**
   * Compares the {@link List} of a {@link Customer#getShippingAddressIds()} and a {@link
   * CustomerDraft#getShippingAddresses()}. It returns a {@link List} of {@link
   * CustomerAddShippingAddressIdAction} update actions as a result, if the new shipping address
   * needs to be added to have the same set of addresses as the {@code newCustomer}. If both the
   * {@link Customer} and the {@link CustomerDraft} have the same set of shipping addresses, then no
   * update actions are needed and hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Old address values without keys are filtered out.
   *   <li><b>Each address</b> in the new addresses list satisfies the following conditions:
   *       <ol>
   *         <li>It is not null
   *         <li>It has a key which is not blank (null/empty)
   *       </ol>
   *       Otherwise, a {@link IllegalArgumentException} will be thrown.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new shipping addresses.
   * @return A list containing the update actions or an empty list if the shipping addresses are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildAddShippingAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    if (newCustomer.getShippingAddresses() == null
        || newCustomer.getShippingAddresses().isEmpty()) {
      return Collections.emptyList();
    }

    final Map<String, Address> oldAddressKeyToAddressMap =
        getShippingAddresses(oldCustomer).stream()
            .filter(address -> !isBlank(address.getKey()))
            .collect(toMap(Address::getKey, identity()));

    final Set<String> newAddressKeys =
        newCustomer.getShippingAddresses().stream()
            .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
            .collect(toSet());

    return newAddressKeys.stream()
        .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
        .map(key -> CustomerAddShippingAddressIdActionBuilder.of().addressKey(key).build())
        .collect(toList());
  }

  @Nonnull
  private static List<Address> getShippingAddresses(@Nonnull final Customer oldCustomer) {
    final Set<String> ids = new HashSet<>(oldCustomer.getShippingAddressIds());
    return oldCustomer.getAddresses().stream()
        .filter(id -> ids.contains(id.getId()))
        .collect(toList());
  }

  @Nonnull
  private static List<Address> getBillingAddresses(@Nonnull final Customer oldCustomer) {
    final Set<String> ids = new HashSet<>(oldCustomer.getBillingAddressIds());
    return oldCustomer.getAddresses().stream()
        .filter(id -> ids.contains(id.getId()))
        .collect(toList());
  }

  /**
   * Compares the {@link List} of a {@link Customer#getShippingAddressIds()} and a {@link
   * CustomerDraft#getShippingAddresses()}. It returns a {@link List} of {@link
   * CustomerRemoveShippingAddressIdAction} update actions as a result, if the old shipping address
   * needs to be removed to have the same set of addresses as the {@code newCustomer}. If both the
   * {@link Customer} and the {@link CustomerDraft} have the same set of shipping addresses, then no
   * update actions are needed and hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Old shipping addresses without keys will be removed.
   *   <li><b>Each address</b> in the new addresses list satisfies the following conditions:
   *       <ol>
   *         <li>It exists in the given index.
   *         <li>It has a key which is not blank (null/empty)
   *       </ol>
   *       Otherwise, a {@link IllegalArgumentException} will be thrown.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new shipping addresses.
   * @return A list containing the update actions or an empty list if the shipping addresses are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildRemoveShippingAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final List<Address> shippingAddresses = getShippingAddresses(oldCustomer);
    if (shippingAddresses.isEmpty()) {
      return Collections.emptyList();
    }

    if (newCustomer.getShippingAddresses() == null
        || newCustomer.getShippingAddresses().isEmpty()) {

      return shippingAddresses.stream()
          .map(
              address ->
                  CustomerRemoveShippingAddressIdActionBuilder.of()
                      .addressId(address.getId())
                      .build())
          .collect(Collectors.toList());
    }

    final Set<String> newAddressKeys =
        newCustomer.getShippingAddresses().stream()
            .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
            .collect(toSet());

    return shippingAddresses.stream()
        .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
        .map(
            address ->
                CustomerRemoveShippingAddressIdActionBuilder.of()
                    .addressId(address.getId())
                    .build())
        .collect(toList());
  }

  /**
   * Compares the {@link List} of a {@link Customer#getBillingAddressIds()} ()} and a {@link
   * CustomerDraft#getBillingAddresses()}. It returns a {@link List} of {@link
   * CustomerAddBillingAddressIdAction} update actions as a result, if the new billing address needs
   * to be added to have the same set of addresses as the {@code newCustomer}. If both the {@link
   * Customer} and the {@link CustomerDraft} have the same set of billing addresses, then no update
   * actions are needed and hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Old address values without keys are filtered out.
   *   <li><b>Each address</b> in the new addresses list satisfies the following conditions:
   *       <ol>
   *         <li>It is not null
   *         <li>It has a key which is not blank (null/empty)
   *       </ol>
   *       Otherwise, a {@link IllegalArgumentException} will be thrown.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new billing addresses.
   * @return A list containing the update actions or an empty list if the billing addresses are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildAddBillingAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final List<Address> billingAddresses = getBillingAddresses(oldCustomer);
    if (newCustomer.getBillingAddresses() == null || newCustomer.getBillingAddresses().isEmpty()) {
      return Collections.emptyList();
    }

    final Map<String, Address> oldAddressKeyToAddressMap =
        billingAddresses.stream()
            .filter(address -> !isBlank(address.getKey()))
            .collect(toMap(Address::getKey, identity()));

    final Set<String> newAddressKeys =
        newCustomer.getBillingAddresses().stream()
            .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
            .collect(toSet());

    return newAddressKeys.stream()
        .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
        .map(key -> CustomerAddBillingAddressIdActionBuilder.of().addressKey(key).build())
        .collect(toList());
  }

  /**
   * Compares the {@link List} of a {@link Customer#getBillingAddressIds()} ()} and a {@link
   * CustomerDraft#getBillingAddresses()}. It returns a {@link List} of {@link
   * CustomerRemoveBillingAddressIdAction} update actions as a result, if the old billing address
   * needs to be removed to have the same set of addresses as the {@code newCustomer}. If both the
   * {@link Customer} and the {@link CustomerDraft} have the same set of billing addresses, then no
   * update actions are needed and hence an empty {@link List} is returned.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>Addresses are matching by their keys.
   *   <li>Null values of the old addresses are filtered out.
   *   <li>Old shipping address values without keys are filtered out.
   *   <li><b>Each address</b> in the new addresses list satisfies the following conditions:
   *       <ol>
   *         <li>It exists in the given index.
   *         <li>It has a key which is not blank (null/empty)
   *       </ol>
   *       Otherwise, a {@link IllegalArgumentException} will be thrown.
   * </ul>
   *
   * @param oldCustomer the customer which should be updated.
   * @param newCustomer the customer draft where we get the new shipping addresses.
   * @return A list containing the update actions or an empty list if the shipping addresses are
   *     identical.
   */
  @Nonnull
  public static List<CustomerUpdateAction> buildRemoveBillingAddressUpdateActions(
      @Nonnull final Customer oldCustomer, @Nonnull final CustomerDraft newCustomer) {

    final List<Address> billingAddresses = getBillingAddresses(oldCustomer);
    if (billingAddresses.isEmpty()) {

      return Collections.emptyList();
    }

    if (newCustomer.getBillingAddresses() == null || newCustomer.getBillingAddresses().isEmpty()) {

      return billingAddresses.stream()
          .map(
              address ->
                  CustomerRemoveBillingAddressIdActionBuilder.of()
                      .addressId(address.getId())
                      .build())
          .collect(Collectors.toList());
    }

    final Set<String> newAddressKeys =
        newCustomer.getBillingAddresses().stream()
            .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
            .collect(toSet());

    return billingAddresses.stream()
        .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
        .map(
            address ->
                CustomerRemoveBillingAddressIdActionBuilder.of().addressId(address.getId()).build())
        .collect(toList());
  }
}
