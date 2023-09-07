package com.commercetools.sync.customers.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupReferenceBuilder;
import com.commercetools.api.models.store.StoreKeyReference;
import com.commercetools.api.models.store.StoreKeyReferenceBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.neovisionaries.i18n.CountryCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CustomerReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void
      mapToCustomerDrafts_WithNonExpandedReferencesAndIdsCached_ShouldReturnResourceIdentifiersWithKeys() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String customerGroupId = UUID.randomUUID().toString();
    final String customerGroupKey = "customerGroupKey";
    final String storeKey1 = "storeKey1";
    final String storeKey2 = "storeKey2";

    // Cache key values with ids.
    referenceIdToKeyCache.add(customTypeId, customTypeKey);
    referenceIdToKeyCache.add(customerGroupId, customerGroupKey);

    final List<Customer> mockCustomers = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      final Customer mockCustomer = mock(Customer.class);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);

      final CustomerGroupReference customerGroupReference =
          CustomerGroupReferenceBuilder.of().id(customerGroupId).build();
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

      List<StoreKeyReference> keyReferences =
          asList(
              StoreKeyReferenceBuilder.of().key(storeKey1).build(),
              StoreKeyReferenceBuilder.of().key(storeKey2).build());

      when(mockCustomer.getStores()).thenReturn(keyReferences);
      when(mockCustomer.getAddresses()).thenReturn(emptyList());
      when(mockCustomer.getEmail()).thenReturn("email");
      when(mockCustomer.getPassword()).thenReturn("password");

      mockCustomers.add(mockCustomer);
    }

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(mockCustomers, referenceIdToKeyCache);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
          assertThat(draft.getCustomerGroup().getKey()).isEqualTo(customerGroupKey);
          assertThat(draft.getStores().get(0).getKey()).isEqualTo(storeKey1);
          assertThat(draft.getStores().get(1).getKey()).isEqualTo(storeKey2);
        });
  }

  @Test
  void
      mapToCustomerDrafts_WithNonExpandedReferencesAndIdsNotCached_ShouldReturnResourceIdentifiersWithoutKeys() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customerGroupId = UUID.randomUUID().toString();

    final List<Customer> mockCustomers = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      final Customer mockCustomer = mock(Customer.class);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);

      final CustomerGroupReference customerGroupReference =
          CustomerGroupReferenceBuilder.of().id(customerGroupId).build();
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

      when(mockCustomer.getStores()).thenReturn(null);
      when(mockCustomer.getAddresses()).thenReturn(emptyList());
      when(mockCustomer.getEmail()).thenReturn("email");
      when(mockCustomer.getPassword()).thenReturn("password");

      mockCustomers.add(mockCustomer);
    }

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(mockCustomers, referenceIdToKeyCache);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isEqualTo(customTypeId);
          assertThat(draft.getCustom().getType().getKey()).isNull();
          assertThat(draft.getCustomerGroup().getId()).isEqualTo(customerGroupId);
          assertThat(draft.getCustomerGroup().getKey()).isNull();
        });
  }

  @Test
  void mapToCustomerDrafts_WithAddresses_ShouldReturnResourceIdentifiersWithCorrectIndexes() {
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getEmail()).thenReturn("email");
    when(mockCustomer.getPassword()).thenReturn("password");
    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id1").build(),
                AddressBuilder.of().country(CountryCode.FR.toString()).id("address-id2").build(),
                AddressBuilder.of().country(CountryCode.US.toString()).id("address-id3").build()));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("address-id1");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn("address-id2");
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "address-id3"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(asList("address-id2", "address-id3"));

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getDefaultBillingAddress()).isEqualTo(0);
    assertThat(customerDraft.getDefaultShippingAddress()).isEqualTo(1);
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, 2));
    assertThat(customerDraft.getShippingAddresses()).isEqualTo(asList(1, 2));
  }

  @Test
  void
      mapToCustomerDrafts_WithMissingAddresses_ShouldReturnResourceIdentifiersWithCorrectIndexes() {
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getEmail()).thenReturn("email");
    when(mockCustomer.getPassword()).thenReturn("password");
    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id1").build(),
                AddressBuilder.of().country(CountryCode.FR.toString()).id("address-id2").build(),
                AddressBuilder.of().country(CountryCode.US.toString()).id("address-id3").build()));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("non-existing-id");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn(null);
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "non-existing-id"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(asList(" ", "address-id3", null));

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getDefaultBillingAddress()).isNull();
    assertThat(customerDraft.getDefaultShippingAddress()).isNull();
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, null));
    assertThat(customerDraft.getShippingAddresses()).isEqualTo(asList(null, 2, null));
  }

  @Test
  void
      mapToCustomerDrafts_WithNullIdOnAddresses_ShouldReturnResourceIdentifiersWithCorrectIndexes() {
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getEmail()).thenReturn("email");
    when(mockCustomer.getPassword()).thenReturn("password");
    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id1").build(),
                AddressBuilder.of().country(CountryCode.US.toString()).id(null).build(),
                AddressBuilder.of().country(CountryCode.US.toString()).id("address-id3").build()));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("address-id1");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn("address-id2");
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "address-id3"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(null);

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getDefaultBillingAddress()).isEqualTo(0);
    assertThat(customerDraft.getDefaultShippingAddress()).isNull();
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, 2));
    assertThat(customerDraft.getShippingAddresses()).isEmpty();
  }
}
