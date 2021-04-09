package com.commercetools.sync.customers.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
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
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);

      final Reference<CustomerGroup> customerGroupReference =
          Reference.ofResourceTypeIdAndId(CustomerGroup.referenceTypeId(), customerGroupId);
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

      List<KeyReference<Store>> keyReferences =
          asList(
              KeyReference.of(storeKey1, Store.referenceTypeId()),
              KeyReference.of(storeKey2, Store.referenceTypeId()));

      when(mockCustomer.getStores()).thenReturn(keyReferences);
      when(mockCustomer.getAddresses()).thenReturn(null);

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
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);

      final Reference<CustomerGroup> customerGroupReference =
          Reference.ofResourceTypeIdAndId(CustomerGroup.referenceTypeId(), customerGroupId);
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

      when(mockCustomer.getStores()).thenReturn(null);
      when(mockCustomer.getAddresses()).thenReturn(null);

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

    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                Address.of(CountryCode.DE).withId("address-id1"),
                Address.of(CountryCode.FR).withId("address-id2"),
                Address.of(CountryCode.US).withId("address-id3")));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("address-id1");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn("address-id2");
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "address-id3"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(asList("address-id2", "address-id3"));

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getAddresses()).isEqualTo(mockCustomer.getAddresses());
    assertThat(customerDraft.getDefaultBillingAddress()).isEqualTo(0);
    assertThat(customerDraft.getDefaultShippingAddress()).isEqualTo(1);
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, 2));
    assertThat(customerDraft.getShippingAddresses()).isEqualTo(asList(1, 2));
  }

  @Test
  void
      mapToCustomerDrafts_WithMissingAddresses_ShouldReturnResourceIdentifiersWithCorrectIndexes() {
    final Customer mockCustomer = mock(Customer.class);

    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                Address.of(CountryCode.DE).withId("address-id1"),
                Address.of(CountryCode.FR).withId("address-id2"),
                Address.of(CountryCode.US).withId("address-id3")));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("non-existing-id");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn(null);
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "non-existing-id"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(asList(" ", "address-id3", null));

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getAddresses()).isEqualTo(mockCustomer.getAddresses());
    assertThat(customerDraft.getDefaultBillingAddress()).isNull();
    assertThat(customerDraft.getDefaultShippingAddress()).isNull();
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, null));
    assertThat(customerDraft.getShippingAddresses()).isEqualTo(asList(null, 2, null));
  }

  @Test
  void
      mapToCustomerDrafts_WithNullIdOnAddresses_ShouldReturnResourceIdentifiersWithCorrectIndexes() {
    final Customer mockCustomer = mock(Customer.class);

    when(mockCustomer.getAddresses())
        .thenReturn(
            asList(
                Address.of(CountryCode.DE).withId("address-id1"),
                Address.of(CountryCode.US).withId(null),
                Address.of(CountryCode.US).withId("address-id3")));
    when(mockCustomer.getDefaultBillingAddressId()).thenReturn("address-id1");
    when(mockCustomer.getDefaultShippingAddressId()).thenReturn("address-id2");
    when(mockCustomer.getBillingAddressIds()).thenReturn(asList("address-id1", "address-id3"));
    when(mockCustomer.getShippingAddressIds()).thenReturn(null);

    final List<CustomerDraft> referenceReplacedDrafts =
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(
            singletonList(mockCustomer), referenceIdToKeyCache);

    final CustomerDraft customerDraft = referenceReplacedDrafts.get(0);

    assertThat(customerDraft.getAddresses()).isEqualTo(mockCustomer.getAddresses());
    assertThat(customerDraft.getDefaultBillingAddress()).isEqualTo(0);
    assertThat(customerDraft.getDefaultShippingAddress()).isNull();
    assertThat(customerDraft.getBillingAddresses()).isEqualTo(asList(0, 2));
    assertThat(customerDraft.getShippingAddresses()).isNull();
  }
}
