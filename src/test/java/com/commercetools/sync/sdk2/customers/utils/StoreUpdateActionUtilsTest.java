package com.commercetools.sync.sdk2.customers.utils;

import static com.commercetools.sync.sdk2.customers.utils.CustomerUpdateActionUtils.buildStoreUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerAddStoreActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerRemoveStoreActionBuilder;
import com.commercetools.api.models.customer.CustomerSetStoresActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.store.StoreKeyReference;
import com.commercetools.api.models.store.StoreResourceIdentifier;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoreUpdateActionUtilsTest {

  private Customer oldCustomer;

  @BeforeEach
  void setup() {
    oldCustomer = mock(Customer.class);
  }

  @Test
  void buildStoreUpdateActions_WithSameStores_ShouldNotReturnAction() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .stores(singletonList(StoreResourceIdentifierBuilder.of().key("store-key").build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNullOldStores_ShouldReturnOnlySetStoreAction() {

    when(oldCustomer.getStores()).thenReturn(null);

    final List<StoreResourceIdentifier> newStores =
        singletonList(StoreResourceIdentifierBuilder.of().key("store-key").build());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(CustomerSetStoresActionBuilder.of().stores(newStores).build());
  }

  @Test
  void buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<StoreResourceIdentifier> newStores =
        singletonList(StoreResourceIdentifierBuilder.of().key("store-key").build());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(CustomerSetStoresActionBuilder.of().stores(newStores).build());
  }

  @Test
  void
      buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreWithoutNullReferencesInIt() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<StoreResourceIdentifier> newStores =
        asList(StoreResourceIdentifierBuilder.of().key("store-key").build(), null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            CustomerSetStoresActionBuilder.of()
                .stores(StoreResourceIdentifierBuilder.of().key("store-key").build())
                .build());
  }

  @Test
  void buildStoreUpdateActions_WithOnlyNullNewStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<StoreResourceIdentifier> newStores = asList(null, null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithBothNullStoreReferences_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(asList(null, null));

    final List<StoreResourceIdentifier> newStores = asList(null, null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNullNewStores_ShouldReturnSetStoreActionWithUnset() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(CustomerSetStoresActionBuilder.of().stores(emptyList()).build());
  }

  @Test
  void buildStoreUpdateActions_WithEmptyNewStores_ShouldReturnSetStoreActionWithUnset() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(emptyList()).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(CustomerSetStoresActionBuilder.of().stores(emptyList()).build());
  }

  @Test
  void buildStoreUpdateActions_WithBothNullStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithBothEmptyStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(emptyList()).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNewStores_ShouldReturnAddStoreActions() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final List<StoreResourceIdentifier> newStores =
        asList(
            StoreResourceIdentifierBuilder.of().key("store-key1").build(),
            StoreResourceIdentifierBuilder.of().key("store-key2").build(),
            StoreResourceIdentifierBuilder.of().key("store-key3").build());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            CustomerAddStoreActionBuilder.of().store(newStores.get(1)).build(),
            CustomerAddStoreActionBuilder.of().store(newStores.get(2)).build());
  }

  @Test
  void buildStoreUpdateActions_WithLessStores_ShouldReturnRemoveStoreActions() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    final StoreKeyReference keyReference2 = mock(StoreKeyReference.class);
    when(keyReference2.getKey()).thenReturn("store-key2");
    final StoreKeyReference keyReference3 = mock(StoreKeyReference.class);
    when(keyReference3.getKey()).thenReturn("store-key3");

    final List<StoreKeyReference> keyReferences =
        asList(keyReference1, keyReference2, keyReference3);
    when(oldCustomer.getStores()).thenReturn(keyReferences);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .stores(singletonList(StoreResourceIdentifierBuilder.of().key("store-key1").build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            CustomerRemoveStoreActionBuilder.of()
                .store(StoreResourceIdentifierBuilder.of().key("store-key2").build())
                .build(),
            CustomerRemoveStoreActionBuilder.of()
                .store(StoreResourceIdentifierBuilder.of().key("store-key3").build())
                .build());
  }

  @Test
  void buildStoreUpdateActions_WithMixedStores_ShouldReturnSetStoresAction() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    final StoreKeyReference keyReference3 = mock(StoreKeyReference.class);
    when(keyReference3.getKey()).thenReturn("store-key3");
    final StoreKeyReference keyReference4 = mock(StoreKeyReference.class);
    when(keyReference4.getKey()).thenReturn("store-key4");

    final List<StoreKeyReference> keyReferences =
        asList(keyReference1, keyReference3, keyReference4);
    when(oldCustomer.getStores()).thenReturn(keyReferences);

    final List<StoreResourceIdentifier> newStores =
        asList(
            StoreResourceIdentifierBuilder.of().key("store-key1").build(),
            StoreResourceIdentifierBuilder.of().key("store-key2").build(),
            StoreResourceIdentifierBuilder.of().key("store-key3").build());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            CustomerSetStoresActionBuilder.of()
                .stores(
                    StoreResourceIdentifierBuilder.of().key("store-key1").build(),
                    StoreResourceIdentifierBuilder.of().key("store-key2").build(),
                    StoreResourceIdentifierBuilder.of().key("store-key3").build())
                .build());
  }

  @Test
  void buildStoreUpdateActions_WithNewStoresWithOnlyIdReference_ShouldReturnAddStoreActions() {

    final StoreKeyReference keyReference1 = mock(StoreKeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final List<StoreResourceIdentifier> newStores =
        asList(
            StoreResourceIdentifierBuilder.of().key("store-key1").build(),
            StoreResourceIdentifierBuilder.of().id("store-id2").build(),
            StoreResourceIdentifierBuilder.of().id("store-id3").build());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").stores(newStores).build();

    final List<CustomerUpdateAction> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            CustomerAddStoreActionBuilder.of().store(newStores.get(1)).build(),
            CustomerAddStoreActionBuilder.of().store(newStores.get(2)).build());
  }
}
