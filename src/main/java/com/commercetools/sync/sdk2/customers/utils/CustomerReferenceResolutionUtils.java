package com.commercetools.sync.sdk2.customers.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.common.AddressDraftBuilder;
import com.commercetools.api.models.common.BaseAddress;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.store.StoreKeyReference;
import com.commercetools.api.models.store.StoreResourceIdentifier;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class CustomerReferenceResolutionUtils {

  /**
   * Returns a {@link List}&lt;{@link CustomerDraft}&gt; consisting of the results of applying the
   * mapping from {@link Customer} to {@link CustomerDraft} with considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
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
   *        <td>{@link CustomerGroupReference}</td>
   *        <td>{@link CustomerGroupResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>stores</td>
   *        <td>{@link List}&lt;{@link StoreKeyReference}&gt;</td>
   *        <td>{@link List}&lt;{@link StoreResourceIdentifier}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link TypeReference}</td>
   *        <td>{@link TypeResourceIdentifier}</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link CustomerGroup} and {@link Type} references should contain Id in the
   * map(cache) with a key value. Any reference, which have its id in place and not replaced by the
   * key, it would not be found in the map. In this case, this reference will be considered as
   * existing resources on the target commercetools project and the library will issues an
   * update/create API request without reference resolution.
   *
   * @param customers the customers without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link CustomerDraft} built from the supplied {@link List} of {@link
   *     Customer}.
   */
  @Nonnull
  public static List<CustomerDraft> mapToCustomerDrafts(
      @Nonnull final List<Customer> customers,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return customers.stream()
        .map(customer -> mapToCustomerDraft(customer, referenceIdToKeyCache))
        .collect(toList());
  }

  @Nonnull
  private static CustomerDraft mapToCustomerDraft(
      @Nonnull final Customer customer,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return CustomerDraftBuilder.of()
        .email(customer.getEmail())
        .password(customer.getPassword())
        .customerNumber(customer.getCustomerNumber())
        .key(customer.getKey())
        .firstName(customer.getFirstName())
        .lastName(customer.getLastName())
        .middleName(customer.getMiddleName())
        .title(customer.getTitle())
        .externalId(customer.getExternalId())
        .companyName(customer.getCompanyName())
        .customerGroup(
            mapToCustomerGroupResourceIdentifier(
                customer.getCustomerGroup(), referenceIdToKeyCache))
        .dateOfBirth(customer.getDateOfBirth())
        .isEmailVerified(customer.getIsEmailVerified())
        .vatId(customer.getVatId())
        .addresses(mapToAddressesDraft(customer.getAddresses()))
        .defaultBillingAddress(
            getAddressIndex(customer.getAddresses(), customer.getDefaultBillingAddressId()))
        .billingAddresses(
            getAddressIndexList(customer.getAddresses(), customer.getBillingAddressIds()))
        .defaultShippingAddress(
            getAddressIndex(customer.getAddresses(), customer.getDefaultShippingAddressId()))
        .shippingAddresses(
            getAddressIndexList(customer.getAddresses(), customer.getShippingAddressIds()))
        .custom(mapToCustomFieldsDraft(customer.getCustom(), referenceIdToKeyCache))
        .locale(customer.getLocale())
        .salutation(customer.getSalutation())
        .stores(mapToStores(customer))
        .build();
  }

  private static CustomerGroupResourceIdentifier mapToCustomerGroupResourceIdentifier(
      @Nullable CustomerGroupReference reference,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (reference != null) {
      CustomerGroupResourceIdentifierBuilder builder = new CustomerGroupResourceIdentifierBuilder();
      final String id = reference.getId();
      if (referenceIdToKeyCache.containsKey(id)) {
        builder.key(referenceIdToKeyCache.get(id));
      } else {
        builder.id(id);
      }
      return builder.build();
    }
    return null;
  }

  private static List<BaseAddress> mapToAddressesDraft(@Nonnull List<Address> addresses) {
    if (addresses.isEmpty()) {
      return emptyList();
    }

    return addresses.stream()
        .map(
            address -> {
              final AddressDraftBuilder builder =
                  AddressDraftBuilder.of()
                      .id(address.getId())
                      .key(address.getKey())
                      .title(address.getTitle())
                      .salutation(address.getSalutation())
                      .firstName(address.getFirstName())
                      .lastName(address.getLastName())
                      .streetName(address.getStreetName())
                      .streetNumber(address.getStreetNumber())
                      .additionalAddressInfo(address.getAdditionalAddressInfo())
                      .postalCode(address.getPostalCode())
                      .city(address.getCity())
                      .region(address.getRegion())
                      .country(address.getCountry())
                      .company(address.getCompany())
                      .department(address.getDepartment())
                      .building(address.getBuilding())
                      .apartment(address.getApartment())
                      .pOBox(address.getPOBox())
                      .phone(address.getPhone())
                      .mobile(address.getMobile())
                      .email(address.getEmail())
                      .fax(address.getFax())
                      .externalId(address.getExternalId());

              if (address.getCustom() != null) {
                builder.custom(
                    CustomFieldsDraftBuilder.of().fields(address.getCustom().getFields()).build());
              }
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  @Nullable
  private static Integer getAddressIndex(
      @Nullable final List<Address> allAddresses, @Nullable final String addressId) {

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
  
  private static List<Integer> getAddressIndexList(
      @Nullable final List<Address> allAddresses, @Nullable final List<String> addressIds) {
    if (allAddresses == null || addressIds == null) {
      return emptyList();
    }
    final List<Integer> indexes = new ArrayList<>();
    for (String addressId : addressIds) {
      indexes.add(getAddressIndex(allAddresses, addressId));
    }
    return indexes;
  }

  @Nullable
  private static CustomFieldsDraft mapToCustomFieldsDraft(
      @Nullable final CustomFields customFields,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (customFields != null) {
      final String typeId = customFields.getType().getId();
      CustomFieldsDraftBuilder customFieldsDraftBuilder = CustomFieldsDraftBuilder.of();
      if (referenceIdToKeyCache.containsKey(typeId)) {
        customFieldsDraftBuilder.type(
            TypeResourceIdentifierBuilder.of().key(referenceIdToKeyCache.get(typeId)).build());

      } else {
        customFieldsDraftBuilder.type(TypeResourceIdentifierBuilder.of().id(typeId).build());
      }
      customFieldsDraftBuilder.fields(customFields.getFields());
      return customFieldsDraftBuilder.build();
    }
    return null;
  }

  private static List<StoreResourceIdentifier> mapToStores(@Nonnull final Customer customer) {
    final List<StoreKeyReference> storeReferences = customer.getStores();
    if (storeReferences != null) {
      return storeReferences.stream()
          .map(
              storeKeyReference ->
                  StoreResourceIdentifierBuilder.of().key(storeKeyReference.getKey()).build())
          .collect(toList());
    }
    return emptyList();
  }

  private CustomerReferenceResolutionUtils() {}
}
