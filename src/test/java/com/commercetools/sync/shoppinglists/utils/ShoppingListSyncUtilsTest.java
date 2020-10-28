package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListSyncUtils.buildActions;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShoppingListSyncUtilsTest {
    private static final Locale LOCALE = Locale.GERMAN;

    @Test
    void buildActions_WithDifferentValues_ShouldReturnActions() {
        final String customerId = "1";
        final Reference<Customer> oldCustomerReference = Reference.of(Customer.referenceTypeId(), customerId);
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));
        when(oldShoppingList.getCustomer()).thenReturn(oldCustomerReference);
        when(oldShoppingList.getAnonymousId()).thenReturn("123");
        when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50);

        final String newCustomerId = "2";
        final Reference<Customer> newCustomerReference = Reference.of(Customer.referenceTypeId(), newCustomerId);
        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));
        when(newShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));
        when(newShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));
        when(newShoppingList.getCustomer()).thenReturn(newCustomerReference);
        when(newShoppingList.getAnonymousId()).thenReturn("567");
        when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(70);

        final List<UpdateAction<ShoppingList>> updateActions = buildActions(oldShoppingList, newShoppingList);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).contains(SetSlug.of(newShoppingList.getSlug()));
        assertThat(updateActions).contains(ChangeName.of(newShoppingList.getName()));
        assertThat(updateActions).contains(SetDescription.of(newShoppingList.getDescription()));
        //TODO: Fix assertion for SetCustomer action
        //assertThat(updateActions).contains(SetCustomer.of(newCustomerReference));
        assertThat(updateActions).contains(SetAnonymousId.of(newShoppingList.getAnonymousId()));
        assertThat(updateActions).contains(
            SetDeleteDaysAfterLastModification.of(newShoppingList.getDeleteDaysAfterLastModification()));
    }
}
