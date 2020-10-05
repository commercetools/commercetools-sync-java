package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultShippingAddress;
import io.sphere.sdk.customers.commands.updateactions.SetExternalId;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetKey;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;

import javax.annotation.Nonnull;
import java.util.Optional;

public final class CustomerUpdateActionUtils {

    /**
     * Compares the {@code email} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeEmail"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code email} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new email.
     * @return optional containing update action or empty optional if emails are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> changeEmailUpdateAction(@Nonnull final Customer oldCustomer,
                                                                           @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getEmail(), newCustomer.getEmail(),
            () -> ChangeEmail.of(
                newCustomer.getEmail()));
    }

    /**
     * Compares the {@code FirstName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setFirstName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code FirstName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setFirstNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                            @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getFirstName(), newCustomer.getFirstName(),
            () -> SetFirstName.of(newCustomer.getFirstName()));
    }

    /**
     * Compares the {@code LastName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLastName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code LastName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setLastNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                           @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getLastName(), newCustomer.getLastName(),
            () -> SetLastName.of(newCustomer.getLastName()));
    }

    /**
     * Compares the {@code MiddleName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setMiddleName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code MiddleName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new name.
     * @return optional containing update action or empty optional if names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setMiddleNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                             @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getMiddleName(), newCustomer.getMiddleName(),
            () -> SetMiddleName.of(newCustomer.getMiddleName()));
    }

    /**
     * Compares the {@code title} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setTitle"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code title} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new title.
     * @return optional containing update action or empty optional if titles are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setTitleUpdateAction(@Nonnull final Customer oldCustomer,
                                                                        @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getTitle(), newCustomer.getTitle(),
            () -> SetTitle.of(newCustomer.getTitle()));
    }

    /**
     * Compares the {@code Salutation} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "SetSalutation"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code Salutation} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new salutation.
     * @return optional containing update action or empty optional if salutations are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setSalutationUpdateAction(@Nonnull final Customer oldCustomer,
                                                                             @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getSalutation(), newCustomer.getSalutation(),
            () -> SetSalutation.of(newCustomer.getSalutation()));
    }

    public Optional<UpdateAction<Customer>> setDefaultShippingAddressUpdateAction
        (@Nonnull final Customer oldCustomer,
         @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getDefaultShippingAddress(),
            newCustomer.getDefaultShippingAddress(), () -> SetDefaultShippingAddress.of(
                String.valueOf(newCustomer.getDefaultShippingAddress())));

    }

    //TODO implement addShippingAddressIdentifierUpdateAction
    public Optional<UpdateAction<Customer>> addShippingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

//        final List<Address> oldShippingAddresses = oldCustomer.getShippingAddresses();
//        final List<Integer> newShippingAddresses = newCustomer.getShippingAddresses();
//
//        final Map<String, Customer> oldShippingAddressMap = oldShippingAddresses
//            .stream()
//            .collect();
        return null;
    }

    //TODO implement removeShippingAddressIdentifierUpdateAction
    public Optional<UpdateAction<Customer>> removeShippingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

//        final List<Address> oldAddresses = oldCustomer.getAddresses();
//        final List<Address> newAddresses = newCustomer.getAddresses();
//
//        return buildUpdateActions(oldAddresses, newAddresses, () -> {
//            final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
//            filterCollection(oldAddresses, oldAddressReference ->
//                !newAddresses.contains(oldAddressReference.getId()))
//                .forEach(addressReference ->
//                    updateActions.add(RemoveShippingAddressId.of(addressReference.getId())));

//            return updateActions;
//        });
        return null;
    }

    //TODO implement setDefaultBillingAddressUpdateAction
    public Optional<UpdateAction<Customer>> setDefaultBillingAddressUpdateAction
    (@Nonnull final Customer oldCustomer,
     @Nonnull final CustomerDraft newCustomer) {

        return null;
    }

    //TODO implement addBillingAddressIdentifierUpdateAction
    public Optional<UpdateAction<Customer>> addBillingAddressIdentifierUpdateAction(@Nonnull final Customer oldCustomer,
                                                                                    @Nonnull final CustomerDraft newCustomer) {

//        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getBillingAddressIds(),
//            newCustomer.getBillingAddresses(), () -> AddBillingAddressId.of(
//                newCustomer.getBillingAddresses()));
        return null;
    }

    //TODO implement removeBillingAddressIdentifier
    public Optional<UpdateAction<Customer>> removeBillingAddressIdentifier(@Nonnull final Customer oldCustomer,
                                                                           @Nonnull final CustomerDraft newCustomer) {

//        final List<Address> oldAddresses = oldCustomer.getAddresses();
//        final List<Address> newAddresses = newCustomer.getAddresses();
//
//        return buildUpdateActions(oldAddresses, newAddresses, () -> {
//            final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
//            filterCollection(oldAddresses, oldAddressReference ->
//                !newAddresses.contains(oldAddressReference.))
//                .forEach(addressReference ->
//                    updateActions.add(RemoveShippingAddressId.of(addressReference.getId())));
//
//            return updateActions;
//        });
        return null;
    }

    //TODO implement setCustomerGroupUpdateAction
    public static Optional<UpdateAction<Customer>> setCustomerGroupUpdateAction
        (@Nonnull final Customer oldCustomer,
         @Nonnull final CustomerDraft newCustomer) {

//        final Referenceable<CustomerGroup> oldCustomerGroup = oldCustomer.getCustomerGroup();
//        final ResourceIdentifier<CustomerGroup> newCustomerGroup = newCustomer.getCustomerGroup();
//
//        return CommonTypeUpdateActionUtils.buildUpdateActions(oldCustomerGroup, newCustomerGroup,
//            () -> SetCustomerGroup.of(
//                newCustomer);
        return null;

    }

    /**
     * Compares the {@code CustomerNumber} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCustomerNumber"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code CustomerNumber} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new customer number.
     * @return optional containing update action or empty optional if customer numbers are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setCustomerNumberUpdateAction
    (@Nonnull final Customer oldCustomer,
     @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getCustomerNumber(),
            newCustomer.getCustomerNumber(), () -> SetCustomerNumber.of(newCustomer.getCustomerNumber()));
    }

    /**
     * Compares the {@code ExternalId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setExternalId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code ExternalId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new external Id.
     * @return optional containing update action or empty optional if external Ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setExternalIdUpdateAction(@Nonnull final Customer oldCustomer,
                                                                             @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction((oldCustomer.getExternalId()), newCustomer.getExternalId(),
            () -> SetExternalId.of(newCustomer.getExternalId()));
    }

    /**
     * Compares the {@code CompanyName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCompanyName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code CompanyName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new company name.
     * @return optional containing update action or empty optional if company names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setCompanyNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                              @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getCompanyName(), newCustomer.getCompanyName(),
            () -> SetCompanyName.of(
                newCustomer.getCompanyName()));
    }

    /**
     * Compares the {@code DateOfBirth} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setDateOfBirth"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code DateOfBirth} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new date of birth.
     * @return optional containing update action or empty optional if dates of birth are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setDateOfBirthUpdateAction(@Nonnull final Customer oldCustomer,
                                                                              @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getDateOfBirth(), newCustomer.getDateOfBirth(),
            () -> SetDateOfBirth.of(newCustomer.getDateOfBirth()));
    }

    /**
     * Compares the {@code VatId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setVatId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code VatId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new vat Id.
     * @return optional containing update action or empty optional if vat Ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setVatIdUpdateAction(@Nonnull final Customer oldCustomer,
                                                                        @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getVatId(), newCustomer.getVatId(),
            () -> SetVatId.of(newCustomer.getVatId()));
    }

    /**
     * Compares the {@code Locale} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLocale"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code Locale} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new locale.
     * @return optional containing update action or empty optional if locales are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setLocaleUpdateAction(@Nonnull final Customer oldCustomer,
                                                                         @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getLocale(), newCustomer.getLocale(),
            () -> SetLocale.of(newCustomer.getLocale()));
    }

    /**
     * Compares the {@code Key} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setKey"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code Key} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new key.
     * @return optional containing update action or empty optional if keys are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> setKeyUpdateAction(@Nonnull final Customer oldCustomer,
                                                                      @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getKey(), newCustomer.getKey(),
            () -> SetKey.of(newCustomer.getKey()));
    }

    //TODO implement setCustomTypeUpdateAction
    //TODO implement setCustomFieldUpdateAction
    //TODO implement setStoresUpdateAction
    //TODO implement addStoreUpdateAction
    //TODO implement removeStoreUpdateAction


}
