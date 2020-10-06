package com.commercetools.sync.customers.utils;

import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.expansion.CustomerExpansionModel;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;
import static org.apache.http.util.TextUtils.isBlank;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class CustomerReferenceResolutionUtils {

    /**
     * Returns a {@link List}&lt;{@link CustomerDraft}&gt; consisting of the results of applying the
     * mapping from {@link Customer} to {@link CustomerDraft} with considering reference resolution.
     *
     * <table summary="Mapping of Reference fields for the reference resolution">
     *   <thead>
     *     <tr>
     *       <th>Reference field</th>
     *       <th>from</th>
     *       <th>to</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *        <td>customerGroup</td>
     *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link CustomerGroup}&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>stores</td>
     *        <td>{@link Set}&lt;{@link KeyReference}&lt;{@link Store}&gt;&gt;</td>
     *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Store}&gt;&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>custom.type</td>
     *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * <p><b>Note:</b> The {@link CustomerGroup} and {@link Type} references should be expanded with a key.
     * Any reference that is not expanded will have its id in place and not replaced by the key will be
     * considered as existing resources on the target commercetools project and
     * the library will issues an update/create API request without reference resolution.
     *
     * @param customers the customers with expanded references.
     * @return a {@link List} of {@link CustomerDraft} built from the supplied {@link List} of {@link Customer}.
     */
    @Nonnull
    public static List<CustomerDraft> mapToCustomerDrafts(
        @Nonnull final List<Customer> customers) {
        return customers
            .stream()
            .map(CustomerReferenceResolutionUtils::mapToCustomerDraft)
            .collect(toList());
    }

    @Nonnull
    private static CustomerDraft mapToCustomerDraft(@Nonnull final Customer customer) {
        return CustomerDraftBuilder
            .of(customer.getEmail(), customer.getPassword())
            .customerNumber(customer.getCustomerNumber())
            .key(customer.getKey())
            .firstName(customer.getFirstName())
            .lastName(customer.getLastName())
            .middleName(customer.getMiddleName())
            .title(customer.getTitle())
            .externalId(customer.getExternalId())
            .companyName(customer.getCompanyName())
            .customerGroup(getResourceIdentifierWithKey(customer.getCustomerGroup()))
            .dateOfBirth(customer.getDateOfBirth())
            .isEmailVerified(customer.isEmailVerified())
            .vatId(customer.getVatId())
            .addresses(customer.getAddresses())
            .defaultBillingAddress(getAddressIndex(customer.getAddresses(), customer.getDefaultBillingAddressId()))
            .billingAddresses(getAddressIndexList(customer.getAddresses(), customer.getBillingAddressIds()))
            .defaultShippingAddress(getAddressIndex(customer.getAddresses(), customer.getDefaultShippingAddressId()))
            .shippingAddresses(getAddressIndexList(customer.getAddresses(), customer.getShippingAddressIds()))
            .custom(mapToCustomFieldsDraft(customer))
            .locale(customer.getLocale())
            .salutation(customer.getSalutation())
            .stores(mapToStores(customer))
            .build();
    }


    @Nullable
    private static Integer getAddressIndex(
        @Nullable final List<Address> allAddresses,
        @Nullable final String addressId) {

        if (allAddresses == null) {
            return null;
        }
        if (isBlank(addressId)) {
            return null;
        }
        for (int i = 0; i < allAddresses.size(); i++) {
            String id = allAddresses.get(i).getId();
            if (id != null && id.equals(addressId)) {
                return i;
            }
        }
        return null;
    }

    @Nullable
    private static List<Integer> getAddressIndexList(
        @Nullable final List<Address> allAddresses,
        @Nullable final List<String> addressIds) {
        if (allAddresses == null || addressIds == null) {
            return null;
        }
        final List<Integer> indexes = new ArrayList<>();
        for (String addressId : addressIds) {
            indexes.add(getAddressIndex(allAddresses, addressId));
        }
        return indexes;
    }

    @Nullable
    private static List<ResourceIdentifier<Store>> mapToStores(@Nonnull final Customer customer) {
        final List<KeyReference<Store>> storeReferences = customer.getStores();
        if (storeReferences != null) {
            return storeReferences
                .stream()
                .map(storeKeyReference -> ResourceIdentifier.<Store>ofKey(storeKeyReference.getKey()))
                .collect(toList());
        }
        return null;
    }

    /**
     * Builds a {@link CustomerQuery} for fetching customers from a source CTP project with all the needed
     * references expanded for the sync:
     * <ul>
     *     <li>Custom Type</li>
     *     <li>CustomerGroup</li>
     * </ul>
     *
     * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
     * a source commercetools project. Otherwise, it is more efficient to build the query without expansions, if they
     * are not needed, to avoid unnecessarily bigger payloads fetched from the source project.
     *
     * @return the query for fetching customers from the source CTP project with all the aforementioned references
     *         expanded.
     */
    public static CustomerQuery buildCustomerQuery() {
        return CustomerQuery.of()
                            .withExpansionPaths(CustomerExpansionModel::customerGroup)
                            .plusExpansionPaths(ExpansionPath.of("custom.type"));
    }

    private CustomerReferenceResolutionUtils() {
    }
}
