package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils;
import io.sphere.sdk.carts.commands.updateactions.UpdateItemShippingAddress;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.AddBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.AddShippingAddressId;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.RemoveBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.RemoveShippingAddressId;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerGroup;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultBillingAddress;
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
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static java.util.stream.Collectors.toList;

//TODO: write Javadoc
//TODO: write unit tests
public class CustomerUpdateActionUtils {

    public Optional<UpdateAction<Customer>> changeEmailUpdateAction(@Nonnull final Customer oldCustomer,
                                                                    @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getEmail(), newCustomer.getEmail(),
            () -> ChangeEmail.of(
                newCustomer.getEmail()));
    }

    public Optional<UpdateAction<Customer>> setFirstNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                     @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getFirstName(), newCustomer.getFirstName(),
            () -> SetFirstName.of(newCustomer.getFirstName()));
    }

    public Optional<UpdateAction<Customer>> setLastNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                    @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getLastName(), newCustomer.getLastName(),
            () -> SetLastName.of(newCustomer.getLastName()));
    }

    public Optional<UpdateAction<Customer>> setMiddleNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                      @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getMiddleName(), newCustomer.getMiddleName(),
            () -> SetMiddleName.of(newCustomer.getMiddleName()));
    }

    public Optional<UpdateAction<Customer>> setTitleUpdateAction(@Nonnull final Customer oldCustomer,
                                                                 @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getTitle(), newCustomer.getTitle(),
            () -> SetTitle.of(newCustomer.getTitle()));
    }

    public Optional<UpdateAction<Customer>> setSalutationUpdateAction(@Nonnull final Customer oldCustomer,
                                                                      @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getSalutation(), newCustomer.getSalutation(),
            () -> SetSalutation.of(newCustomer.getSalutation()));
    }

    //TODO: all address related update actions
    public Optional<UpdateAction<Customer>> addAddressUpdateAction(@Nonnull final Customer oldCustomer,
                                                                   @Nonnull final CustomerDraft newCustomer) {

        final List<Address> oldAddresses = oldCustomer.getAddresses();
        final List<Address> newAddresses = newCustomer.getAddresses();

        return CommonTypeUpdateActionUtils.buildUpdateActions(oldAddresses, newAddresses,
            () -> {
                final List<UpdateAction<Customer>> updateActions = new ArrayList<>();
                final List<ResourceIdentifier<Address>> newAddressesToBeAdded = filterCollection(newAddresses,
                    newAddressReference ->
                        oldAddresses.stream()
                                    .map(ResourceIdentifier::getKey)
                                    .noneMatch(oldResourceIdentifier ->
                                        oldResourceIdentifier.equals(newAddressReference)))
                    .collect(toList());

            });
    }

    public Optional<UpdateAction<Customer>> setDefaultShippingAddressUpdateAction(@Nonnull final Customer oldCustomer,
                                                                                  @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getDefaultShippingAddress(),
            newCustomer.getDefaultShippingAddress(), () -> SetDefaultShippingAddress.of(
                String.valueOf(newCustomer.getDefaultShippingAddress())));

    }

    public Optional<UpdateAction<Customer>> addShippingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getShippingAddresses(),
            newCustomer.getShippingAddresses(), () -> AddShippingAddressId.of(
                String.valueOf(newCustomer.getShippingAddresses())));
    }

    //TODO
    public Optional<UpdateAction<Customer>> removeShippingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getShippingAddresses(),
            newCustomer.getShippingAddresses(), () -> RemoveShippingAddressId.of(
                String.valueOf(newCustomer.getShippingAddresses())));
    }

    public Optional<UpdateAction<Customer>> setDefaultBillingAddressUpdateAction(@Nonnull final Customer oldCustomer,
                                                                                 @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getBillingAddresses(),
            newCustomer.getBillingAddresses(), () -> SetDefaultBillingAddress.of(
                String.valueOf(newCustomer.getBillingAddresses())));
    }

    public Optional<UpdateAction<Customer>> addBillingAddressIdentifierUpdateAction(@Nonnull final Customer oldCustomer,
                                                                                    @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getBillingAddressIds(),
            newCustomer.getBillingAddresses(), () -> AddBillingAddressId.of(
                String.valueOf(newCustomer.getBillingAddresses())));
    }

    public Optional<UpdateAction<Customer>> removeBillingAddressIdentifier(@Nonnull final Customer oldCustomer,
                                                                           @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getBillingAddressIds(),
            newCustomer.getBillingAddresses(), () -> RemoveBillingAddressId.of(
                String.valueOf(oldCustomer.getBillingAddressIds())));
    }

    //TODO
    /*public Optional<UpdateAction<Customer>> setCustomerGroupUpdateAction(@Nonnull final Customer oldCustomer,
                                                                         @Nonnull final CustomerDraft newCustomer) {

        final Reference<CustomerGroup> oldCustomerGroup = oldCustomer.getCustomerGroup();
        final ResourceIdentifier<CustomerGroup> newCustomerGroup = newCustomer.getCustomerGroup();
        return buildUpdateActions(oldCustomerGroup, newCustomerGroup, () -> SetCustomerGroup.of(newCustomerGroup)


            )


        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getCustomerGroup(),
            newCustomer.getCustomerGroup(), () -> SetCustomerGroup.of(newCustomer.getCustomerGroup()));
    }*/

    public Optional<UpdateAction<Customer>> setCustomerNumberUpdateAction(@Nonnull final Customer oldCustomer,
                                                                          @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getCustomerNumber(),
            newCustomer.getCustomerNumber(), () -> SetCustomerNumber.of(newCustomer.getCustomerNumber()));
    }

    public Optional<UpdateAction<Customer>> setExternalIdUpdateAction(@Nonnull final Customer oldCustomer,
                                                                      @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction((oldCustomer.getExternalId(), newCustomer.getExternalId(),
            () -> SetExternalId.of(newCustomer.getExternalId()));
    }

    public Optional<UpdateAction<Customer>> setCompanyNameUpdateAction(@Nonnull final Customer oldCustomer,
                                                                       @Nonnull final CustomerDraft newCustomer) {

        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getCompanyName(), newCustomer.getCompanyName(),
            () -> SetCompanyName.of(
                newCustomer.getCompanyName()));
    }

    public Optional<UpdateAction<Customer>> setDateOfBirthUpdateAction(@Nonnull final Customer oldCustomer,
                                                                       @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getDateOfBirth(), newCustomer.getDateOfBirth(),
            () -> SetDateOfBirth.of(newCustomer.getDateOfBirth()));
    }

    public Optional<UpdateAction<Customer>> setVatIdUpdateAction(@Nonnull final Customer oldCustomer,
                                                                 @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getVatId(), newCustomer.getVatId(),
            () -> SetVatId.of(newCustomer.getVatId()));
    }

    public Optional<UpdateAction<Customer>> setLocaleUpdateAction(@Nonnull final Customer oldCustomer,
                                                                  @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getLocale(), newCustomer.getLocale(),
            () -> SetLocale.of(newCustomer.getLocale()));
    }

    public Optional<UpdateAction<Customer>> setKeyUpdateAction(@Nonnull final Customer oldCustomer,
                                                               @Nonnull final CustomerDraft newCustomer) {
        return CommonTypeUpdateActionUtils.buildUpdateAction(oldCustomer.getKey(), newCustomer.getKey(),
            () -> SetKey.of(newCustomer.getKey()));
    }


}
