package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.models.Address;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class AddressesUpdateActionUtils {

    static List<UpdateAction<Customer>> buildAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
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
    private static List<UpdateAction<Customer>> buildUpdateActions(@Nonnull final List<Address> oldAddresses,
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
    private static List<UpdateAction<Customer>> buildRemoveAddressUpdateActions(
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
    private static List<UpdateAction<Customer>> buildAddAddressUpdateActions(
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

    private AddressesUpdateActionUtils() {
    }
}
