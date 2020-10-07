package com.commercetools.sync.customers.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddStore;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
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
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.ResourceImpl;
import io.sphere.sdk.stores.Store;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

// todo (ahmetoz) add jvm sdk support ticket for anonymous id update actions.
public final class CustomerUpdateActionUtils {

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
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new customer number.
     * @return optional containing update action or empty optional if customer numbers are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCustomerNumberUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getCustomerNumber(), newCustomer.getCustomerNumber(),
            () -> SetCustomerNumber.of(newCustomer.getCustomerNumber()));
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
            .orElseGet(() -> {
                if (oldStores != null && newStores != null) {
                    final List<UpdateAction<Customer>> updateActions =
                        buildRemoveStoreUpdateActions(oldStores, newStores);

                    updateActions.addAll(buildAddStoreUpdateActions(oldStores, newStores));
                    return updateActions;
                }

                return emptyList();
            });
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
            final List<ResourceIdentifier<Store>> stores =
                newStores.stream()
                         .filter(Objects::nonNull)
                         .collect(toList());
            if (!stores.isEmpty()) {
                return Optional.of(SetStores.of(stores));
            }
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
    private static List<UpdateAction<Customer>> buildRemoveStoreUpdateActions(
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
    private static List<UpdateAction<Customer>> buildAddStoreUpdateActions(
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
}
