package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultBillingAddress;
import io.sphere.sdk.customers.commands.updateactions.SetDefaultShippingAddress;
import io.sphere.sdk.models.Address;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class AddressesUpdateActionUtils {

    /**
     * todo: add missing javadoc.
     * @param oldAddresses a
     * @param newAddresses a
     * @return a
     * @throws BuildUpdateActionException a
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                         @Nullable final List<Address> newAddresses)
        throws BuildUpdateActionException {
        if (newAddresses != null) {
            return buildUpdateActions(oldAddresses, newAddresses
                .stream()
                .filter(Objects::nonNull)
                .collect(toList()));
        } else {
            return oldAddresses
                .stream()
                .map(Address::getId)
                .map(RemoveAddress::of)
                .collect(Collectors.toList());
        }
    }

    @Nonnull
    protected static List<UpdateAction<Customer>> buildUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                   @Nonnull final List<Address> newAddresses)
        throws BuildUpdateActionException {

        try {
            final List<UpdateAction<Customer>> updateActions =
                buildRemoveAddressUpdateActions(oldAddresses, newAddresses);

            updateActions.addAll(
                buildAddAddressUpdateActions(oldAddresses, newAddresses)
            );

            return updateActions;

        } catch (final DuplicateNameException | DuplicateKeyException exception) {
            throw new BuildUpdateActionException(exception);
        }
    }

    @Nonnull
    protected static List<UpdateAction<Customer>> buildRemoveAddressUpdateActions(
        @Nonnull final List<Address> oldAddresses,
        @Nonnull final List<Address> newAddresses) {
        final Map<String, Address> newAddressesKeyMap = newAddresses
            .stream().collect(
                toMap(Address::getKey, addressDraft -> addressDraft, (addressDraftA, addressDraftB) -> {
                        throw new DuplicateKeyException(format("Address IDs drafts have duplicated names. "
                                + "Duplicated attribute definition name: '%s'. "
                                + "Attribute definitions names are expected to be unique inside their product type.",
                            addressDraftA.getKey()));
                    }
                ));

        return oldAddresses
            .stream()
            .map(oldAddress -> {
                final String oldAddressKey = oldAddress.getKey();
                final Address matchingNewAddress =
                    newAddressesKeyMap.get(oldAddressKey);

                return ofNullable(matchingNewAddress)
                    .map(addressDraft -> {
                        if (!addressDraft.getKey().equals(oldAddressKey)) {
                            return RemoveAddress.of(oldAddress);
                        } else {
                            if (addressDraft.equals(oldAddress)) {
                                return ChangeAddress.of(oldAddress.getId(), addressDraft);
                            }
                            return null;
                        }
                    }).orElse(RemoveAddress.of(oldAddress));

            }).filter(Objects::nonNull)
            .collect(toList());
    }

    @Nonnull
    protected static List<UpdateAction<Customer>> buildAddAddressUpdateActions(
        @Nonnull final List<Address> oldAddresses,
        @Nonnull final List<Address> newAddresses) {
        final Map<String, Address> oldAddressKeyMap = oldAddresses
            .stream()
            .collect(toMap(Address::getKey, key -> key));

        return newAddresses
            .stream()
            .filter(keyDraft -> !oldAddressKeyMap.containsKey(keyDraft.getKey())
            )
            .map(AddAddress::of)
            .collect(toList());
    }


    private static Optional<UpdateAction<Customer>> setDefaultShippingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {
        // find the address in that index, be aware the null pointer exceptions
        Address address = newCustomer.getAddresses().get(newCustomer.getDefaultShippingAddress());
        String addressId = address.getId();
        //
        return buildUpdateAction(oldCustomer.getDefaultShippingAddressId(),
            addressId, () -> SetDefaultShippingAddress.of(addressId));
    }

    //TODO implement addShippingAddressIdentifierUpdateAction
    private static List<UpdateAction<Customer>> buildAddShippingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final List<Address> oldShippingAddresses = oldCustomer.getShippingAddresses();
        final List<Address> newAddresses = newCustomer.getAddresses();
        final List<Integer> newShippingAddressesIndices = newCustomer.getShippingAddresses();

        final List<Address> newShippingAddresses = new ArrayList<>();
        for (Integer newShippingAddressesIndex : newShippingAddressesIndices) {
            newShippingAddresses.add(newAddresses.get(newShippingAddressesIndex));
        }

        return AddressesUpdateActionUtils.buildAddAddressUpdateActions(oldShippingAddresses, newShippingAddresses);
    }

    //TODO implement removeShippingAddressIdentifierUpdateAction
    private static Optional<UpdateAction<Customer>> removeShippingAddressIdentifierUpdateAction(
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
        //
        //            return updateActions;
        //        });
        return Optional.empty();
    }

    //TODO implement setDefaultBillingAddressUpdateAction
    private static Optional<UpdateAction<Customer>> setDefaultBillingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        // find the address in that index, be aware the null pointer exceptions
        Address address = newCustomer.getAddresses().get(newCustomer.getDefaultBillingAddress());
        String addressId = address.getId();
        //
        return buildUpdateAction(oldCustomer.getDefaultBillingAddressId(),
            addressId, () -> SetDefaultBillingAddress.of(addressId));
    }

    //TODO implement addBillingAddressIdentifierUpdateAction
    private static List<UpdateAction<Customer>> addBillingAddressIdentifierUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final List<Address> oldShippingAddresses = oldCustomer.getBillingAddresses();
        final List<Address> newAddresses = newCustomer.getAddresses();
        final List<Integer> newBillingAddressesIndices = newCustomer.getBillingAddresses();

        final List<Address> newBillingAddresses = new ArrayList<>();
        for (Integer newShippingAddressesIndex : newBillingAddressesIndices) {
            newBillingAddresses.add(newAddresses.get(newShippingAddressesIndex));
        }

        return AddressesUpdateActionUtils.buildAddAddressUpdateActions(oldShippingAddresses, newBillingAddresses);
    }

    //TODO implement removeBillingAddressIdentifier
    private static Optional<UpdateAction<Customer>> removeBillingAddressIdentifier(
        @Nonnull final Customer oldCustomer,
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
        return Optional.empty();
    }

    private AddressesUpdateActionUtils() {
    }
}
