package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.commands.updateactions.AddBillingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.AddShippingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultBillingAddressWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultShippingAddressWithKey;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.AddStore;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.RemoveShippingAddressId;
import io.sphere.sdk.customers.commands.updateactions.RemoveStore;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerGroup;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetExternalId;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetStores;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.ResourceImpl;
import io.sphere.sdk.stores.Store;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class CustomerUpdateActionUtils {

    public static final String CUSTOMER_NUMBER_EXISTS_WARNING = "Customer with key: \"%s\" has "
        + "already a customer number: \"%s\", once it's set it cannot be changed. "
        + "Hereby, the update action is not created.";

    private CustomerUpdateActionUtils() {
    }

    /**
     * Compares the {@code email} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeEmail"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code email} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new email.
     * @return optional containing update action or empty optional if emails are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildChangeEmailUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getEmail(), newCustomer.getEmail(),
            () -> ChangeEmail.of(newCustomer.getEmail()));
    }

    /**
     * Compares the {@code firstName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setFirstName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code firstName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new first name.
     * @return optional containing update action or empty optional if first names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetFirstNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getFirstName(), newCustomer.getFirstName(),
            () -> SetFirstName.of(newCustomer.getFirstName()));
    }

    /**
     * Compares the {@code lastName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLastName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code lastName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new last name.
     * @return optional containing update action or empty optional if last names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetLastNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getLastName(), newCustomer.getLastName(),
            () -> SetLastName.of(newCustomer.getLastName()));
    }

    /**
     * Compares the {@code middleName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setMiddleName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code middleName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new middle name.
     * @return optional containing update action or empty optional if middle names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetMiddleNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getMiddleName(), newCustomer.getMiddleName(),
            () -> SetMiddleName.of(newCustomer.getMiddleName()));
    }

    /**
     * Compares the {@code title} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setTitle"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code title} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new title.
     * @return optional containing update action or empty optional if titles are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetTitleUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getTitle(), newCustomer.getTitle(),
            () -> SetTitle.of(newCustomer.getTitle()));
    }

    /**
     * Compares the {@code salutation} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "SetSalutation"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code salutation} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new salutation.
     * @return optional containing update action or empty optional if salutations are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetSalutationUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getSalutation(), newCustomer.getSalutation(),
            () -> SetSalutation.of(newCustomer.getSalutation()));
    }

    /**
     * Compares the {@code customerNumber} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCustomerNumber"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code customerNumber} values, then no update action is needed and empty optional will be returned.
     *
     * <p>Note: Customer number should be unique across a project. Once it's set it cannot be changed. For this case,
     * warning callback will be triggered and an empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new customer number.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the warning callback when trying to change an existing customer number.
     * @return optional containing update action or empty optional if customer numbers are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCustomerNumberUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer,
        @Nonnull final CustomerSyncOptions syncOptions) {

        final Optional<UpdateAction<Customer>> setCustomerNumberAction =
            buildUpdateAction(oldCustomer.getCustomerNumber(), newCustomer.getCustomerNumber(),
                () -> SetCustomerNumber.of(newCustomer.getCustomerNumber()));

        if (setCustomerNumberAction.isPresent() && !isBlank(oldCustomer.getCustomerNumber())) {

            syncOptions.applyWarningCallback(
                new SyncException(format(CUSTOMER_NUMBER_EXISTS_WARNING, oldCustomer.getKey(),
                    oldCustomer.getCustomerNumber())), oldCustomer, newCustomer);

            return Optional.empty();
        }

        return setCustomerNumberAction;
    }

    /**
     * Compares the {@code externalId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setExternalId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code externalId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new external id.
     * @return optional containing update action or empty optional if external ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetExternalIdUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getExternalId(), newCustomer.getExternalId(),
            () -> SetExternalId.of(newCustomer.getExternalId()));
    }

    /**
     * Compares the {@code companyName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCompanyName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code companyName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new company name.
     * @return optional containing update action or empty optional if company names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCompanyNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getCompanyName(), newCustomer.getCompanyName(),
            () -> SetCompanyName.of(newCustomer.getCompanyName()));
    }

    /**
     * Compares the {@code dateOfBirth} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setDateOfBirth"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code dateOfBirth} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new date of birth.
     * @return optional containing update action or empty optional if dates of birth are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetDateOfBirthUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getDateOfBirth(), newCustomer.getDateOfBirth(),
            () -> SetDateOfBirth.of(newCustomer.getDateOfBirth()));
    }

    /**
     * Compares the {@code vatId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setVatId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code vatId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new vat id.
     * @return optional containing update action or empty optional if vat ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetVatIdUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getVatId(), newCustomer.getVatId(),
            () -> SetVatId.of(newCustomer.getVatId()));
    }

    /**
     * Compares the {@code locale} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLocale"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code locale} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new locale.
     * @return optional containing update action or empty optional if locales are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetLocaleUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getLocale(), newCustomer.getLocale(),
            () -> SetLocale.of(newCustomer.getLocale()));
    }

    /**
     * Compares the {@link CustomerGroup} references of an old {@link Customer} and
     * new {@link CustomerDraft}. If they are different - return {@link SetCustomerGroup} update action.
     *
     * <p>If the old value is set, but the new one is empty - the command will unset the customer group.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft with new {@link CustomerGroup} reference.
     * @return An optional with {@link SetCustomerGroup} update action.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCustomerGroupUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateActionForReferences(oldCustomer.getCustomerGroup(), newCustomer.getCustomerGroup(),
            () -> SetCustomerGroup.of(mapResourceIdentifierToReferenceable(newCustomer.getCustomerGroup())));
    }

    @Nullable
    private static Referenceable<CustomerGroup> mapResourceIdentifierToReferenceable(
        @Nullable final ResourceIdentifier<CustomerGroup> resourceIdentifier) {

        if (resourceIdentifier == null) {
            return null; // unset
        }

        // TODO (JVM-SDK), see: SUPPORT-10261 SetCustomerGroup could be created with a ResourceIdentifier
        // https://github.com/commercetools/commercetools-jvm-sdk/issues/2072
        return new ResourceImpl<CustomerGroup>(null, null, null, null) {
            @Override
            public Reference<CustomerGroup> toReference() {
                return Reference.of(CustomerGroup.referenceTypeId(), resourceIdentifier.getId());
            }
        };
    }

    /**
     * Compares the stores of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Customer}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Customer} and the {@link CustomerDraft} have the identical stores, an empty
     * {@link List} is returned.
     *
     * <p>Note: Null values of the stores are filtered out.
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new data.
     * @return A list of customer store-related update actions.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildStoreUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final List<KeyReference<Store>> oldStores = oldCustomer.getStores();
        final List<ResourceIdentifier<Store>> newStores = newCustomer.getStores();

        return buildSetStoreUpdateAction(oldStores, newStores)
            .map(Collections::singletonList)
            .orElseGet(() -> prepareStoreActions(oldStores, newStores));
    }

    private static List<UpdateAction<Customer>> prepareStoreActions(
        @Nullable final List<KeyReference<Store>> oldStores,
        @Nullable final List<ResourceIdentifier<Store>> newStores) {

        if (oldStores != null && newStores != null) {
            final List<UpdateAction<Customer>> removeStoreUpdateActions =
                buildRemoveStoreUpdateActions(oldStores, newStores);

            final List<UpdateAction<Customer>> addStoreUpdateActions =
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
     * Compares the {@link List} of {@link Store} {@link KeyReference}s and {@link Store} {@link ResourceIdentifier}s
     * of a {@link CustomerDraft} and a {@link Customer}. It returns a {@link SetStores} update action as a result.
     * If both the {@link Customer} and the {@link CustomerDraft} have the same set of stores, then no update actions
     * are needed and hence an empty {@link List} is returned.
     *
     * <p>Note: Null values of the stores are filtered out.
     *
     * @param oldStores the stores which should be updated.
     * @param newStores the stores where we get the new store.
     * @return A list containing the update actions or an empty list if the store references are identical.
     */
    @Nonnull
    private static Optional<UpdateAction<Customer>> buildSetStoreUpdateAction(
        @Nullable final List<KeyReference<Store>> oldStores,
        @Nullable final List<ResourceIdentifier<Store>> newStores) {

        if (oldStores != null && !oldStores.isEmpty()) {
            if (newStores == null || newStores.isEmpty()) {
                return Optional.of(SetStores.of(emptyList()));
            }
        } else if (newStores != null && !newStores.isEmpty()) {
            return buildSetStoreUpdateAction(newStores);
        }

        return Optional.empty();
    }

    private static Optional<UpdateAction<Customer>> buildSetStoreUpdateAction(
        @Nonnull final List<ResourceIdentifier<Store>> newStores) {

        final List<ResourceIdentifier<Store>> stores =
            newStores.stream()
                     .filter(Objects::nonNull)
                     .collect(toList());

        if (!stores.isEmpty()) {
            return Optional.of(SetStores.of(stores));
        }

        return Optional.empty();
    }

    /**
     * Compares the {@link List} of {@link Store} {@link KeyReference}s and {@link Store} {@link ResourceIdentifier}s
     * of a {@link CustomerDraft} and a {@link Customer}. It returns a {@link List} of {@link RemoveStore} update
     * actions as a result, if the old store needs to be removed from a customer to have the same set of stores as
     * the new customer. If both the {@link Customer} and the {@link CustomerDraft} have the same set of stores,
     * then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Note: Null values of the stores are filtered out.
     *
     * @param oldStores the stores which should be updated.
     * @param newStores the stores where we get the new store.
     * @return A list containing the update actions or an empty list if the store references are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildRemoveStoreUpdateActions(
        @Nonnull final List<KeyReference<Store>> oldStores,
        @Nonnull final List<ResourceIdentifier<Store>> newStores) {

        final Map<String, ResourceIdentifier<Store>> newStoreKeyToStoreMap =
            newStores.stream()
                     .filter(Objects::nonNull)
                     .filter(storeResourceIdentifier -> storeResourceIdentifier.getKey() != null)
                     .collect(toMap(ResourceIdentifier::getKey, identity()));

        return oldStores
            .stream()
            .filter(Objects::nonNull)
            .filter(storeKeyReference -> !newStoreKeyToStoreMap.containsKey(storeKeyReference.getKey()))
            .map(storeKeyReference -> RemoveStore.of(ResourceIdentifier.ofKey(storeKeyReference.getKey())))
            .collect(toList());
    }

    /**
     * Compares the {@link List} of {@link Store} {@link KeyReference}s and {@link Store} {@link ResourceIdentifier}s
     * of a {@link CustomerDraft} and a {@link Customer}. It returns a {@link List} of {@link AddStore} update actions
     * as a result, if the old store needs to be added to a customer to have the same set of stores as the new customer.
     * If both the {@link Customer} and the {@link CustomerDraft} have the same set of stores, then no update actions
     * are needed and hence an empty {@link List} is returned.
     *
     * <p>Note: Null values of the stores are filtered out.
     *
     * @param oldStores the stores which should be updated.
     * @param newStores the stores where we get the new store.
     * @return A list containing the update actions or an empty list if the store references are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAddStoreUpdateActions(
        @Nonnull final List<KeyReference<Store>> oldStores,
        @Nonnull final List<ResourceIdentifier<Store>> newStores) {

        final Map<String, KeyReference<Store>> oldStoreKeyToStoreMap =
            oldStores.stream()
                     .filter(Objects::nonNull)
                     .collect(toMap(KeyReference::getKey, identity()));

        return newStores
            .stream()
            .filter(Objects::nonNull)
            .filter(storeResourceIdentifier -> !oldStoreKeyToStoreMap.containsKey(storeResourceIdentifier.getKey()))
            .map(AddStore::of)
            .collect(toList());
    }

    /**
     * Compares the addresses of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Customer}&gt; as a result. If both the {@link Customer} and the
     * {@link CustomerDraft} have the same set of addresses, then no update actions are needed and hence an empty
     * {@link List} is returned.
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new data.
     * @return A list of customer address-related update actions.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAllAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final List<UpdateAction<Customer>> addressActions = new ArrayList<>();

        final List<UpdateAction<Customer>> removeAddressActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        addressActions.addAll(removeAddressActions);
        addressActions.addAll(buildChangeAddressUpdateActions(oldCustomer, newCustomer));
        addressActions.addAll(buildAddAddressUpdateActions(oldCustomer, newCustomer));

        addressActions.addAll(
            collectAndFilterRemoveShippingAndBillingActions(removeAddressActions, oldCustomer, newCustomer));

        buildSetDefaultShippingAddressUpdateAction(oldCustomer, newCustomer).ifPresent(addressActions::add);
        buildSetDefaultBillingAddressUpdateAction(oldCustomer, newCustomer).ifPresent(addressActions::add);

        addressActions.addAll(buildAddShippingAddressUpdateActions(oldCustomer, newCustomer));
        addressActions.addAll(buildAddBillingAddressUpdateActions(oldCustomer, newCustomer));

        return addressActions;
    }

    @Nonnull
    private static List<UpdateAction<Customer>> collectAndFilterRemoveShippingAndBillingActions(
        @Nonnull final List<UpdateAction<Customer>> removeAddressActions,
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
                                .map(customerUpdateAction -> (RemoveAddress)customerUpdateAction)
                                .map(RemoveAddress::getAddressId)
                                .collect(toSet());


        final List<UpdateAction<Customer>> removeActions = new ArrayList<>();

        removeActions.addAll(buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer)
            .stream()
            .map(customerUpdateAction -> (RemoveShippingAddressId)customerUpdateAction)
            .filter(action -> !addressIdsToRemove.contains(action.getAddressId()))
            .collect(toList()));

        removeActions.addAll(buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer)
            .stream()
            .map(customerUpdateAction -> (RemoveBillingAddressId)customerUpdateAction)
            .filter(action -> !addressIdsToRemove.contains(action.getAddressId()))
            .collect(toList()));

        return removeActions;
    }

    /**
     * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link Customer#getAddresses()}.
     * It returns a {@link List} of {@link RemoveAddress} update actions as a result, if the old address needs to be
     * removed from the {@code oldCustomer} to have the same set of addresses as the {@code newCustomer}.
     * If both the {@link Customer} and the {@link CustomerDraft} have the same set of addresses,
     * then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Null values of the new addresses are filtered out.</li>
     *  <li>Address values without keys are filtered out.</li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new addresses.
     * @return A list containing the update actions or an empty list if the addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildRemoveAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (oldCustomer.getAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {

            return oldCustomer.getAddresses()
                              .stream()
                              .map(address -> RemoveAddress.of(address.getId()))
                              .collect(Collectors.toList());
        }

        final Set<String> newAddressKeys =
            newCustomer.getAddresses()
                       .stream()
                       .filter(Objects::nonNull)
                       .filter(newAddress -> !isBlank(newAddress.getKey()))
                       .map(Address::getKey)
                       .collect(toSet());

        return oldCustomer.getAddresses()
                          .stream()
                          .filter(oldAddress -> isBlank(oldAddress.getKey())
                              || !newAddressKeys.contains(oldAddress.getKey()))
                          .map(RemoveAddress::of)
                          .collect(toList());
    }

    /**
     * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link Customer#getAddresses()}.
     * It returns a {@link List} of {@link ChangeAddress} update actions as a result, if the old address needs to be
     * changed/updated from the {@code oldCustomer} to have the same set of addresses as the {@code newCustomer}.
     * If both the {@link Customer} and the {@link CustomerDraft} have the same set of addresses,
     * then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Null values of the new addresses are filtered out.</li>
     *  <li>Address values without keys are filtered out.</li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new addresses.
     * @return A list containing the update actions or an empty list if the addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildChangeAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Address> oldAddressKeyToAddressMap =
            oldCustomer.getAddresses()
                       .stream()
                       .filter(address -> !isBlank(address.getKey()))
                       .collect(toMap(Address::getKey, identity()));

        return newCustomer.getAddresses()
                          .stream()
                          .filter(Objects::nonNull)
                          .filter(newAddress -> !isBlank(newAddress.getKey()))
                          .filter(newAddress -> oldAddressKeyToAddressMap.containsKey(newAddress.getKey()))
                          .map(newAddress -> {
                              final Address oldAddress = oldAddressKeyToAddressMap.get(newAddress.getKey());
                              if (!newAddress.equalsIgnoreId(oldAddress)) {
                                  return ChangeAddress.of(oldAddress.getId(), newAddress);
                              }
                              return null;
                          })
                          .filter(Objects::nonNull)
                          .collect(toList());
    }

    /**
     * Compares the {@link List} of a {@link CustomerDraft#getAddresses()} and a {@link Customer#getAddresses()}.
     * It returns a {@link List} of {@link AddAddress} update actions as a result, if the new address needs to be
     * added to have the same set of addresses as the {@code newCustomer}.
     * If both the {@link Customer} and the {@link CustomerDraft} have the same set of addresses,
     * then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Null values of the new addresses are filtered out.</li>
     *  <li>Address values without keys are filtered out.</li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new addresses.
     * @return A list containing the update actions or an empty list if the addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAddAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (newCustomer.getAddresses() == null || newCustomer.getAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Address> oldAddressKeyToAddressMap =
            oldCustomer.getAddresses()
                       .stream()
                       .filter(address -> !isBlank(address.getKey()))
                       .collect(toMap(Address::getKey, identity()));

        return newCustomer.getAddresses()
                          .stream()
                          .filter(Objects::nonNull)
                          .filter(newAddress -> !isBlank(newAddress.getKey()))
                          .filter(newAddress -> !oldAddressKeyToAddressMap.containsKey(newAddress.getKey()))
                          .map(AddAddress::of)
                          .collect(toList());
    }

    /**
     * Compares the {@link Customer#getDefaultShippingAddress()} and {@link CustomerDraft#getDefaultShippingAddress()}.
     * If they are different - return {@link SetDefaultShippingAddressWithKey} update action. If the old shipping
     * address is set, but the new one is empty - the command will unset the default shipping address.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft with new default shipping address.
     * @return An optional with {@link SetDefaultShippingAddressWithKey} update action.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetDefaultShippingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final Address oldAddress = oldCustomer.getDefaultShippingAddress();
        final String newAddressKey =
            getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultShippingAddress());

        if (newAddressKey != null) {
            if (oldAddress == null || !Objects.equals(oldAddress.getKey(), newAddressKey)) {
                return Optional.of(SetDefaultShippingAddressWithKey.of(newAddressKey));
            }
        } else if (oldAddress != null) { // unset
            return Optional.of(SetDefaultShippingAddressWithKey.of(null));
        }

        return Optional.empty();
    }

    /**
     * Compares the {@link Customer#getDefaultBillingAddress()} and {@link CustomerDraft#getDefaultBillingAddress()}.
     * If they are different - return {@link SetDefaultShippingAddressWithKey} update action. If the old billing address
     * id value is set, but the new one is empty - the command will unset the default billing address.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft with new default billing address.
     * @return An optional with {@link SetDefaultShippingAddressWithKey} update action.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetDefaultBillingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final Address oldAddress = oldCustomer.getDefaultBillingAddress();
        final String newAddressKey =
            getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultBillingAddress());

        if (newAddressKey != null) {
            if (oldAddress == null || !Objects.equals(oldAddress.getKey(), newAddressKey)) {
                return Optional.of(SetDefaultBillingAddressWithKey.of(newAddressKey));
            }
        } else if (oldAddress != null) { // unset
            return Optional.of(SetDefaultBillingAddressWithKey.of(null));
        }

        return Optional.empty();
    }

    @Nullable
    private static String getAddressKeyAt(
            @Nullable final List<Address> addressList,
            @Nullable final Integer index) {

        if (index == null)  {
            return null;
        }

        if (addressList == null || index < 0 || index >= addressList.size()) {
            throw new IllegalArgumentException(
                    format("Addresses list does not contain an address at the index: %s", index));
        }

        final Address address = addressList.get(index);
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
     * Compares the {@link List} of a {@link Customer#getShippingAddresses()} and a
     * {@link CustomerDraft#getShippingAddresses()}. It returns a {@link List} of {@link AddShippingAddressIdWithKey}
     * update actions as a result, if the new shipping address needs to be added to have the same set of addresses as
     * the {@code newCustomer}. If both the {@link Customer} and the {@link CustomerDraft} have the same set of
     * shipping addresses, then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Old address values without keys are filtered out.</li>
     *  <li><b>Each address</b> in the new addresses list satisfies the following conditions:
     *  <ol>
     *   <li>It is not null</li>
     *   <li>It has a key which is not blank (null/empty)</li>
     *  </ol>
     *  Otherwise, a {@link IllegalArgumentException} will be thrown.
     *  </li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new shipping addresses.
     * @return A list containing the update actions or an empty list if the shipping addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAddShippingAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (newCustomer.getShippingAddresses() == null
            || newCustomer.getShippingAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Address> oldAddressKeyToAddressMap =
            oldCustomer.getShippingAddresses()
                       .stream()
                       .filter(address -> !isBlank(address.getKey()))
                       .collect(toMap(Address::getKey, identity()));

        final Set<String> newAddressKeys =
            newCustomer.getShippingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(toSet());

        return newAddressKeys
            .stream()
            .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
            .map(AddShippingAddressIdWithKey::of)
            .collect(toList());
    }

    /**
     * Compares the {@link List} of a {@link Customer#getShippingAddresses()} and a
     * {@link CustomerDraft#getShippingAddresses()}. It returns a {@link List} of {@link RemoveShippingAddressId}
     * update actions as a result, if the old shipping address needs to be removed to have the same set of addresses as
     * the {@code newCustomer}. If both the {@link Customer} and the {@link CustomerDraft} have the same set of
     * shipping addresses, then no update actions are needed and hence an empty {@link List} is returned.
     *
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Old shipping addresses without keys will be removed.</li>
     *  <li><b>Each address</b> in the new addresses list satisfies the following conditions:
     *  <ol>
     *   <li>It exists in the given index.</li>
     *   <li>It has a key which is not blank (null/empty)</li>
     *  </ol>
     *  Otherwise, a {@link IllegalArgumentException} will be thrown.
     *  </li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new shipping addresses.
     * @return A list containing the update actions or an empty list if the shipping addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildRemoveShippingAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if ((oldCustomer.getShippingAddresses() == null
            || oldCustomer.getShippingAddresses().isEmpty())) {

            return Collections.emptyList();
        }

        if (newCustomer.getShippingAddresses() == null
            || newCustomer.getShippingAddresses().isEmpty()) {

            return oldCustomer.getShippingAddresses()
                              .stream()
                              .map(address -> RemoveShippingAddressId.of(address.getId()))
                              .collect(Collectors.toList());
        }

        final Set<String> newAddressKeys =
            newCustomer.getShippingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(toSet());

        return oldCustomer.getShippingAddresses()
                          .stream()
                          .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
                          .map(address -> RemoveShippingAddressId.of(address.getId()))
                          .collect(toList());
    }

    /**
     * Compares the {@link List} of a {@link Customer#getBillingAddresses()} and a
     * {@link CustomerDraft#getBillingAddresses()}. It returns a {@link List} of {@link AddBillingAddressIdWithKey}
     * update actions as a result, if the new billing address needs to be added to have the same set of addresses as
     * the {@code newCustomer}. If both the {@link Customer} and the {@link CustomerDraft} have the same set of
     * billing addresses, then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Old address values without keys are filtered out.</li>
     *  <li><b>Each address</b> in the new addresses list satisfies the following conditions:
     *  <ol>
     *   <li>It is not null</li>
     *   <li>It has a key which is not blank (null/empty)</li>
     *  </ol>
     *  Otherwise, a {@link IllegalArgumentException} will be thrown.
     *  </li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new billing addresses.
     * @return A list containing the update actions or an empty list if the billing addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAddBillingAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (newCustomer.getBillingAddresses() == null
            || newCustomer.getBillingAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Address> oldAddressKeyToAddressMap =
            oldCustomer.getBillingAddresses()
                       .stream()
                       .filter(address -> !isBlank(address.getKey()))
                       .collect(toMap(Address::getKey, identity()));


        final Set<String> newAddressKeys =
            newCustomer.getBillingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(toSet());

        return newAddressKeys
            .stream()
            .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
            .map(AddBillingAddressIdWithKey::of)
            .collect(toList());
    }

    /**
     * Compares the {@link List} of a {@link Customer#getBillingAddresses()} and a
     * {@link CustomerDraft#getBillingAddresses()}. It returns a {@link List} of {@link RemoveBillingAddressId}
     * update actions as a result, if the old billing address needs to be removed to have the same set of addresses as
     * the {@code newCustomer}. If both the {@link Customer} and the {@link CustomerDraft} have the same set of
     * billing addresses, then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>Notes:
     * <ul>
     *  <li>Addresses are matching by their keys. </li>
     *  <li>Null values of the old addresses are filtered out.</li>
     *  <li>Old shipping address values without keys are filtered out.</li>
     *  <li><b>Each address</b> in the new addresses list satisfies the following conditions:
     *  <ol>
     *   <li>It exists in the given index.</li>
     *   <li>It has a key which is not blank (null/empty)</li>
     *  </ol>
     *  Otherwise, a {@link IllegalArgumentException} will be thrown.
     *  </li>
     * </ul>
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new shipping addresses.
     * @return A list containing the update actions or an empty list if the shipping addresses are identical.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildRemoveBillingAddressUpdateActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if ((oldCustomer.getBillingAddresses() == null
            || oldCustomer.getBillingAddresses().isEmpty())) {

            return Collections.emptyList();
        }

        if (newCustomer.getBillingAddresses() == null
            || newCustomer.getBillingAddresses().isEmpty()) {

            return oldCustomer.getBillingAddresses()
                              .stream()
                              .map(address -> RemoveBillingAddressId.of(address.getId()))
                              .collect(Collectors.toList());
        }

        final Set<String> newAddressKeys =
            newCustomer.getBillingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(toSet());

        return oldCustomer.getBillingAddresses()
                          .stream()
                          .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
                          .map(address -> RemoveBillingAddressId.of(address.getId()))
                          .collect(toList());
    }
}
