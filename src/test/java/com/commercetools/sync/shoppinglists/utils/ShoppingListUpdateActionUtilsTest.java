package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetAnonymousIdUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetCustomerUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDeleteDaysAfterLastModificationUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetSlugUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListUpdateActionUtilsTest {
    private static final Locale LOCALE = Locale.GERMAN;

    @Test
    void buildSetSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final UpdateAction<ShoppingList> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
        assertThat(((SetSlug) setSlugUpdateAction).getSlug())
            .isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    void buildSetSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(null);

        final UpdateAction<ShoppingList> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
        assertThat(((SetSlug) setSlugUpdateAction).getSlug()).isNull();
    }

    @Test
    void buildSetSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final Optional<UpdateAction<ShoppingList>> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final UpdateAction<ShoppingList> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    void buildChangeNameUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getName()).thenReturn(null);

        final UpdateAction<ShoppingList> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isNull();
    }

    @Test
    void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingListWithSameName = mock(ShoppingListDraft.class);
        when(newShoppingListWithSameName.getName())
            .thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final Optional<UpdateAction<ShoppingList>> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingListWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final UpdateAction<ShoppingList> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    void buildSetDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription()).thenReturn(null);

        final UpdateAction<ShoppingList> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription()).isEqualTo(null);
    }

    @Test
    void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription())
            .thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final Optional<UpdateAction<ShoppingList>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    void buildSetCustomerUpdateAction_WithDifferentReference_ShouldBuildUpdateAction() {
        final String customerId = UUID.randomUUID().toString();
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerId);
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getCustomer()).thenReturn(customerReference);

        final String resolvedCustomerId = UUID.randomUUID().toString();
        final Reference<Customer> newCustomerReference = Reference.of(Customer.referenceTypeId(), resolvedCustomerId);
        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getCustomer()).thenReturn(newCustomerReference);

        final Optional<UpdateAction<ShoppingList>> setCustomerUpdateAction =
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setCustomerUpdateAction).isNotEmpty();
        assertThat(setCustomerUpdateAction).containsInstanceOf(SetCustomer.class);
        assertThat(((SetCustomer)setCustomerUpdateAction.get()).getCustomer())
            .isEqualTo(Reference.of(Customer.referenceTypeId(), resolvedCustomerId));
    }

    @Test
    void buildSetCustomerUpdateAction_WithSameReference_ShouldNotBuildUpdateAction() {
        final String customerId = UUID.randomUUID().toString();
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerId);
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getCustomer()).thenReturn(customerReference);

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getCustomer()).thenReturn(ResourceIdentifier.ofId(customerId));

        final Optional<UpdateAction<ShoppingList>> setCustomerUpdateAction =
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setCustomerUpdateAction).isEmpty();
    }

    @Test
    void buildSetCustomerUpdateAction_WithOnlyNewReference_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);

        final String newCustomerId = UUID.randomUUID().toString();
        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getCustomer()).thenReturn(ResourceIdentifier.ofId(newCustomerId));

        final Optional<UpdateAction<ShoppingList>> setCustomerUpdateAction =
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setCustomerUpdateAction).isNotEmpty();
        assertThat(setCustomerUpdateAction).containsInstanceOf(SetCustomer.class);
        assertThat(((SetCustomer)setCustomerUpdateAction.get()).getCustomer())
            .isEqualTo(Reference.of(Customer.referenceTypeId(), newCustomerId));
    }

    @Test
    void buildSetCustomerUpdateAction_WithoutNewReference_ShouldReturnUnsetAction() {
        final String customerId = UUID.randomUUID().toString();
        final Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customerId);
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getCustomer()).thenReturn(customerReference);

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getCustomer()).thenReturn(null);

        final Optional<UpdateAction<ShoppingList>> setCustomerUpdateAction =
            buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setCustomerUpdateAction).isNotEmpty();
        assertThat(setCustomerUpdateAction).containsInstanceOf(SetCustomer.class);
        //Note: If the old value is set, but the new one is empty - the command will unset the customer.
        assertThat(((SetCustomer) setCustomerUpdateAction.get()).getCustomer()).isNull();
    }

    @Test
    void buildSetAnonymousId_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getAnonymousId()).thenReturn("123");

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getAnonymousId()).thenReturn("567");

        final Optional<UpdateAction<ShoppingList>> setAnonymousIdUpdateAction =
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setAnonymousIdUpdateAction).isNotNull();
        assertThat(setAnonymousIdUpdateAction).containsInstanceOf(SetAnonymousId.class);
    }

    @Test
    void buildSetAnonymousId_WithNullValues_ShouldBuildUpdateActions() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getAnonymousId()).thenReturn("123");

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getAnonymousId()).thenReturn(null);

        final Optional<UpdateAction<ShoppingList>> setAnonymousIdUpdateAction =
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setAnonymousIdUpdateAction).isNotNull();
        assertThat(setAnonymousIdUpdateAction).containsInstanceOf(SetAnonymousId.class);
    }

    @Test
    void buildSetAnonymousId_WithSameValues_ShouldNotBuildUpdateActions() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getAnonymousId()).thenReturn("123");

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getAnonymousId()).thenReturn("123");

        final Optional<UpdateAction<ShoppingList>> setAnonymousIdUpdateAction =
            buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setAnonymousIdUpdateAction).isEmpty();
    }

    @Test
    void buildSetDeleteDaysAfterLastModificationUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50);

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(70);

        final Optional<UpdateAction<ShoppingList>> setDeleteDaysUpdateAction =
            buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setDeleteDaysUpdateAction).isNotNull();
        assertThat(setDeleteDaysUpdateAction).containsInstanceOf(SetDeleteDaysAfterLastModification.class);
    }

    @Test
    void buildSetDeleteDaysAfterLastModificationUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50);

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(null);

        final Optional<UpdateAction<ShoppingList>> setDeleteDaysUpdateAction =
            buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setDeleteDaysUpdateAction).isNotNull();
        assertThat(setDeleteDaysUpdateAction).containsInstanceOf(SetDeleteDaysAfterLastModification.class);
    }
}
