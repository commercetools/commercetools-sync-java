package com.commercetools.sync.customers.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.SetStores;
import io.sphere.sdk.models.KeyReference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildStoreUpdateActions;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// todo (ahmetoz) cover remove and add actions.
public class StoreUpdateActionUtilsTest {

    @Test
    void buildStoreUpdateActions_WithSameStores_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        final KeyReference<Store> keyReference1 = mock(KeyReference.class);
        when(keyReference1.getKey()).thenReturn("store-key");
        when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(singletonList(ResourceIdentifier.ofKey("store-key")))
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction).isEmpty();
    }

    @Test
    void buildStoreUpdateActions_WithNullOldStores_ShouldReturnOnlySetStoreAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(null);

        final List<ResourceIdentifier<Store>> newStores = singletonList(ResourceIdentifier.ofKey("store-key"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(newStores)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction)
            .isNotEmpty()
            .containsExactly(SetStores.of(newStores));
    }

    @Test
    void buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(emptyList());

        final List<ResourceIdentifier<Store>> newStores = singletonList(ResourceIdentifier.ofKey("store-key"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(newStores)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction)
            .isNotEmpty()
            .containsExactly(SetStores.of(newStores));
    }

    @Test
    void buildStoreUpdateActions_WithEmptyOldStores_ShouldReturnOnlySetStoreWithoutNullReferencesInIt() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(emptyList());

        final List<ResourceIdentifier<Store>> newStores =
            Arrays.asList(ResourceIdentifier.ofKey("store-key"), null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(newStores)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction)
            .isNotEmpty()
            .containsExactly(SetStores.of(singletonList(ResourceIdentifier.ofKey("store-key"))));
    }

    @Test
    void buildStoreUpdateActions_WithOnlyNullNewStores_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(emptyList());

        final List<ResourceIdentifier<Store>> newStores =
            Arrays.asList(null, null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(newStores)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction).isEmpty();
    }

    @Test
    void buildStoreUpdateActions_WithBothNullStoreReferences_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(Arrays.asList(null, null));

        final List<ResourceIdentifier<Store>> newStores =
            Arrays.asList(null, null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(newStores)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction).isEmpty();
    }

    @Test
    void buildStoreUpdateActions_WithNullNewStores_ShouldReturnSetStoreActionWithUnset() {
        final Customer oldCustomer = mock(Customer.class);
        final KeyReference<Store> keyReference1 = mock(KeyReference.class);
        when(keyReference1.getKey()).thenReturn("store-key");
        when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(null)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction)
            .isNotEmpty()
            .containsExactly(SetStores.of(null));
    }

    @Test
    void buildStoreUpdateActions_WithEmptyNewStores_ShouldReturnSetStoreActionWithUnset() {
        final Customer oldCustomer = mock(Customer.class);
        final KeyReference<Store> keyReference1 = mock(KeyReference.class);
        when(keyReference1.getKey()).thenReturn("store-key");
        when(oldCustomer.getStores()).thenReturn(singletonList(keyReference1));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(emptyList())
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction)
            .isNotEmpty()
            .containsExactly(SetStores.of(null));
    }

    @Test
    void buildStoreUpdateActions_WithBothNullStores_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(null)
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction).isEmpty();
    }

    @Test
    void buildStoreUpdateActions_WithBothEmptyStores_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getStores()).thenReturn(emptyList());

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .stores(emptyList())
                                .build();

        final List<UpdateAction<Customer>> setStoreUpdateAction =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        assertThat(setStoreUpdateAction).isEmpty();
    }
}
