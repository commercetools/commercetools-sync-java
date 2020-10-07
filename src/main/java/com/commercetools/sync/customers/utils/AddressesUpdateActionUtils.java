package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.customers.commands.updateactions.AddBillingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.AddShippingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultBillingAddressWitKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultShippingAddressWitKey;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.RemoveShippingAddressId;
import io.sphere.sdk.models.Address;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class AddressesUpdateActionUtils {

    /**
     * todo: add missing javadoc.
     *
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
                .collect(toList());
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

    @Nonnull
    private static Optional<UpdateAction<Customer>> buildSetDefaultShippingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final Address oldAddress = oldCustomer.getDefaultShippingAddress();
        final String newAddressKey =
            getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultShippingAddress());

        if (newAddressKey != null) {
            if (oldAddress == null || !Objects.equals(oldAddress.getKey(), newAddressKey)) {
                return Optional.of(SetDefaultShippingAddressWitKey.of(newAddressKey));
            }
        }

        if (oldAddress != null) { // unset
            return Optional.of(SetDefaultBillingAddressWitKey.of(null));
        }

        return Optional.empty();
    }

    @Nonnull
    private static Optional<UpdateAction<Customer>> buildSetDefaultBillingAddressUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        final Address oldAddress = oldCustomer.getDefaultBillingAddress();
        final String newAddressKey =
            getAddressKeyAt(newCustomer.getAddresses(), newCustomer.getDefaultBillingAddress());

        if (newAddressKey != null) {
            if (oldAddress == null || !Objects.equals(oldAddress.getKey(), newAddressKey)) {
                return Optional.of(SetDefaultBillingAddressWitKey.of(newAddressKey));
            }
        }

        if (oldAddress != null) { // unset
            return Optional.of(SetDefaultBillingAddressWitKey.of(null));
        }

        return Optional.empty();
    }

    @Nullable
    private static String getAddressKeyAt(
        @Nullable final List<Address> addressList,
        @Nullable final Integer index) {

        if (index != null) {
            if (addressList != null) {
                if (index >= 0 && index < addressList.size()) {
                    final Address address = addressList.get(index);
                    return address.getKey();
                }
            }

            throw new IllegalArgumentException(format("Address list does not contain the index: %s", index));
        }

        return null;
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildAddShippingAddressesUpdateAction(
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
                       .collect(Collectors.toSet());

        return newAddressKeys
            .stream()
            .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
            .map(AddShippingAddressIdWithKey::of)
            .collect(toList());
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildRemoveShippingAddressesUpdateAction(
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
                       .collect(Collectors.toSet());

        return oldCustomer.getShippingAddresses()
                   .stream()
                   .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
                   .map(address -> RemoveShippingAddressId.of(address.getId()))
                   .collect(toList());
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildAddBillingAddressesUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if (newCustomer.getBillingAddresses() == null
            || newCustomer.getBillingAddresses().isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Address> oldAddressKeyToAddressMap =
            oldCustomer.getBillingAddresses()
                       .stream()
                       .filter(Objects::nonNull)
                       .collect(toMap(Address::getKey, identity()));


        final Set<String> newAddressKeys =
            newCustomer.getBillingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(Collectors.toSet());

        return newAddressKeys
            .stream()
            .filter(newAddressKey -> !oldAddressKeyToAddressMap.containsKey(newAddressKey))
            .map(AddBillingAddressIdWithKey::of)
            .collect(toList());
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildRemoveBillingAddressesUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        if ((oldCustomer.getBillingAddresses() == null
            || oldCustomer.getBillingAddresses().isEmpty())) {

            return Collections.emptyList();
        }

        if (newCustomer.getBillingAddresses() == null
            || newCustomer.getBillingAddresses().isEmpty()) {

            return oldCustomer.getShippingAddresses()
                              .stream()
                              .map(address -> RemoveBillingAddressId.of(address.getId()))
                              .collect(Collectors.toList());
        }

        final Set<String> newAddressKeys =
            newCustomer.getBillingAddresses()
                       .stream()
                       .map(index -> getAddressKeyAt(newCustomer.getAddresses(), index))
                       .collect(Collectors.toSet());

        return oldCustomer.getBillingAddresses()
                          .stream()
                          .filter(address -> isBlank(address.getKey()) || !newAddressKeys.contains(address.getKey()))
                          .map(address -> RemoveBillingAddressId.of(address.getId()))
                          .collect(toList());
    }

    private AddressesUpdateActionUtils() {
    }
}
