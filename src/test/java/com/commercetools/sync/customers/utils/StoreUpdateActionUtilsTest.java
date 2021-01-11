package com.commercetools.sync.customers.utils;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildStoreUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.AddStore;
import io.sphere.sdk.customers.commands.updateactions.RemoveStore;
import io.sphere.sdk.customers.commands.updateactions.SetStores;
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class StoreUpdateActionUtilsTest {

  private Customer oldCustomer;

  @BeforeEach
  void setup() {
    oldCustomer = mock(Customer.class);
  }

  @Test
  void buildStoreUpdateActions_WithSameStores_ShouldNotReturnAction() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass")
            .stores(singletonList(ResourceIdentifier.ofKey("store-key")))
            .build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNullOldStores_ShouldReturnOnlySetStoreAction() {

    when(oldCustomer.getStores()).thenReturn(null);

    final List<ResourceIdentifier<Store>> newStores =
        singletonList(ResourceIdentifier.ofKey("store-key"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isNotEmpty().containsExactly(SetStores.of(newStores));
  }

  @Test
  void buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<ResourceIdentifier<Store>> newStores =
        singletonList(ResourceIdentifier.ofKey("store-key"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isNotEmpty().containsExactly(SetStores.of(newStores));
  }

  @Test
  void
      buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreWithoutNullReferencesInIt() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<ResourceIdentifier<Store>> newStores =
        asList(ResourceIdentifier.ofKey("store-key"), null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(SetStores.of(singletonList(ResourceIdentifier.ofKey("store-key"))));
  }

  @Test
  void buildStoreUpdateActions_WithOnlyNullNewStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final List<ResourceIdentifier<Store>> newStores = asList(null, null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithBothNullStoreReferences_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(asList(null, null));

    final List<ResourceIdentifier<Store>> newStores = asList(null, null);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNullNewStores_ShouldReturnSetStoreActionWithUnset() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer = CustomerDraftBuilder.of("email", "pass").stores(null).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isNotEmpty().containsExactly(SetStores.of(emptyList()));
  }

  @Test
  void buildStoreUpdateActions_WithEmptyNewStores_ShouldReturnSetStoreActionWithUnset() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(emptyList()).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isNotEmpty().containsExactly(SetStores.of(emptyList()));
  }

  @Test
  void buildStoreUpdateActions_WithBothNullStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(null);

    final CustomerDraft newCustomer = CustomerDraftBuilder.of("email", "pass").stores(null).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithBothEmptyStores_ShouldNotReturnAction() {

    when(oldCustomer.getStores()).thenReturn(emptyList());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(emptyList()).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildStoreUpdateActions_WithNewStores_ShouldReturnAddStoreActions() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final List<ResourceIdentifier<Store>> newStores =
        asList(
            ResourceIdentifier.ofKey("store-key1"),
            ResourceIdentifier.ofKey("store-key2"),
            ResourceIdentifier.ofKey("store-key3"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(AddStore.of(newStores.get(1)), AddStore.of(newStores.get(2)));
  }

  @Test
  void buildStoreUpdateActions_WithLessStores_ShouldReturnRemoveStoreActions() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    final KeyReference<Store> keyReference2 = mock(KeyReference.class);
    when(keyReference2.getKey()).thenReturn("store-key2");
    final KeyReference<Store> keyReference3 = mock(KeyReference.class);
    when(keyReference3.getKey()).thenReturn("store-key3");

    final List<KeyReference<Store>> keyReferences =
        asList(keyReference1, keyReference2, keyReference3);
    when(oldCustomer.getStores()).thenReturn(keyReferences);

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass")
            .stores(singletonList(ResourceIdentifier.ofKey("store-key1")))
            .build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            RemoveStore.of(ResourceIdentifier.ofKey("store-key2")),
            RemoveStore.of(ResourceIdentifier.ofKey("store-key3")));
  }

  @Test
  void buildStoreUpdateActions_WithMixedStores_ShouldReturnSetStoresAction() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    final KeyReference<Store> keyReference3 = mock(KeyReference.class);
    when(keyReference3.getKey()).thenReturn("store-key3");
    final KeyReference<Store> keyReference4 = mock(KeyReference.class);
    when(keyReference4.getKey()).thenReturn("store-key4");

    final List<KeyReference<Store>> keyReferences =
        asList(keyReference1, keyReference3, keyReference4);
    when(oldCustomer.getStores()).thenReturn(keyReferences);

    final List<ResourceIdentifier<Store>> newStores =
        asList(
            ResourceIdentifier.ofKey("store-key1"),
            ResourceIdentifier.ofKey("store-key2"),
            ResourceIdentifier.ofKey("store-key3"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(
            SetStores.of(
                asList(
                    ResourceIdentifier.ofKey("store-key1"),
                    ResourceIdentifier.ofKey("store-key2"),
                    ResourceIdentifier.ofKey("store-key3"))));
  }

  @Test
  void buildStoreUpdateActions_WithNewStoresWithOnlyIdReference_ShouldReturnAddStoreActions() {

    final KeyReference<Store> keyReference1 = mock(KeyReference.class);
    when(keyReference1.getKey()).thenReturn("store-key1");
    when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

    final List<ResourceIdentifier<Store>> newStores =
        asList(
            ResourceIdentifier.ofKey("store-key1"),
            ResourceIdentifier.ofId("store-id2"),
            ResourceIdentifier.ofId("store-id3"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of("email", "pass").stores(newStores).build();

    final List<UpdateAction<Customer>> updateActions =
        buildStoreUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .isNotEmpty()
        .containsExactly(AddStore.of(newStores.get(1)), AddStore.of(newStores.get(2)));
  }
}
