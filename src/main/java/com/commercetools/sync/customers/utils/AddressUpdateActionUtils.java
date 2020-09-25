package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.models.Address;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class AddressUpdateActionUtils {


    // add address, remove, update update actions for address
    static List<UpdateAction<Customer>> buildAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                  @Nullable final List<Address> newAddresses)
        throws BuildUpdateActionException {
        if (newAddresses != null) {
            return buildUpdateActions(oldAddresses, newAddresses
                .stream()
                .filter(Objects::nonNull)
                .collect(toList()));
        } else {
            return oldAddresses.stream()
                               .map((Address address) -> RemoveAddress.of(address))
                               .collect(toList());
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

            buildChangeAddressUpdateActions(oldAddresses, newAddresses
            )
                .ifPresent(updateActions::add);

            return updateActions;

        } catch (final DuplicateNameException | DuplicateKeyException exception) {
            throw new BuildUpdateActionException(exception);
        }
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildRemoveAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                                @Nonnull final List<Address> newAddresses) {
        final Map<String, Address> newAddressesIdMap = newAddresses
            .stream().collect(
                toMap(Address::getId, addressDraft -> addressDraft, (addressDraftA, addressDraftB) -> {
                    throw new DuplicateKeyException(format("Address IDs drafts have duplicated names. "
                            + "Duplicated attribute definition name: '%s'. "
                            + "Attribute definitions names are expected to be unique inside their product type.",
                        addressDraftA.getId()));
                    }
                    ));

        return oldAddresses
            .stream()
            .map(oldAddress -> {
                final String oldAddressId = oldAddress.getId();
                final Address matchingNewAddress =
                    newAddressesIdMap.get(oldAddressId);
                return ofNullable(matchingNewAddress)
                    .map(addressDraft ->
                        new ArrayList<UpdateAction<Customer>>())
                    .orElseGet(() -> singletonList(RemoveAddress.of(oldAddressId)));

            })
            .flatMap(Collection::stream)
            .collect(toList());

    }

    @Nonnull
    private static Optional<UpdateAction<Customer>> buildChangeAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                                   @Nonnull final List<Address> newAddresses){
        //or does it need to be keys, because the Draft of an address only has a key, no ID?
        final List<String> newIds = newAddresses
            .stream()
            .map(Address::getId)
            .collect(toList());

        final List<String> existingIds = oldAddresses
            .stream()
            .map(Address::getId)
            .filter(newIds::contains)
            .collect(toList());

        final List<String> notExistingIds = newIds
            .stream()
            .filter(newId -> !existingIds.contains(newId))
            .collect(toList());

        final List<String> newAddressOrder = newAddresses
            .stream()
            .map(Address::getId)
            .collect(toList());

        final List<String> allIds = Stream
            .concat(existingIds.stream(), notExistingIds.stream())
            .collect(toList());

        return buildUpdateAction(
            allIds,
            newIds,
            () -> ChangeAddress.of(newAddressOrder)
        );
    }

    @Nonnull
    private static List<UpdateAction<Customer>> buildAddAddressUpdateActions(@Nonnull final List<Address> oldAddresses,
                                                                             @Nonnull final List<Address> newAddresses){
        final Map<String, Address> oldAddressKeyMap = oldAddresses
            .stream()
            .collect(toMap(Address::getKey, key -> key));

        return newAddresses
            .stream()
            .filter(keyDraft -> oldAddressKeyMap.containsKey(keyDraft.getKey())
            )
            .map(AddAddress::of)
            .collect(toList());
    }
}










