package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.ResourceImpl;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.html.Option;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;

public final class ShoppingListUpdateActionUtils {
    private ShoppingListUpdateActionUtils() {
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link ShoppingList} and a {@link ShoppingListDraft} and returns
     * an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same slug, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft where we get the new slug.
     * @return A filled optional with the update action or an empty optional if the slugs are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetSlugUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateAction(oldShoppingList.getSlug(),
            newShoppingList.getSlug(), () -> SetSlug.of(newShoppingList.getSlug()));
    }

    /**
     * Compares the {@link LocalizedString} names of a {@link ShoppingList} and a {@link ShoppingListDraft} and returns
     * an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same name, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildChangeNameUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateAction(oldShoppingList.getName(),
            newShoppingList.getName(), () -> ChangeName.of(newShoppingList.getName()));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of {@link ShoppingList} and a {@link ShoppingListDraft} and
     * returns an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same description, then no update action is needed
     * and hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetDescriptionUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateAction(oldShoppingList.getDescription(),
            newShoppingList.getDescription(), () -> SetDescription.of(newShoppingList.getDescription()));
    }

    /**
     * Compares the customer references of a {@link ShoppingList} and a {@link ShoppingListDraft} and
     * returns an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same customer, then no update action is needed
     * and hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft which holds the new customer.
     * @return A filled optional with the update action or an empty optional if the customers are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetCustomerUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateActionForReferences(oldShoppingList.getCustomer(), newShoppingList.getCustomer(),
            () -> SetCustomer.of(mapResourceIdentifierToReferenceable(newShoppingList.getCustomer())));
    }

    @Nullable
    private static Referenceable<Customer> mapResourceIdentifierToReferenceable(
        @Nullable final ResourceIdentifier<Customer> resourceIdentifier) {

        if (resourceIdentifier == null) {
            return null; // unset
        }

        return new ResourceImpl<Customer>(null, null, null, null) {
            @Override
            public Reference<Customer> toReference() {
                return Reference.of(CustomerGroup.referenceTypeId(), resourceIdentifier.getId());
            }
        };
    }

    /**
     * Compares the AnonymousIds of {@link ShoppingList} and a {@link ShoppingListDraft} and
     * returns an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same AnonymousId, then no update action is needed
     * and hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft which holds the new AnonymousId.
     * @return A filled optional with the update action or an empty optional if the Ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetAnonymousIdUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateAction(oldShoppingList.getAnonymousId(), newShoppingList.getAnonymousId(),
            () -> SetAnonymousId.of(newShoppingList.getAnonymousId()));
    }

    /**
     * Compares the Number for Delete Days of {@link ShoppingList} and a {@link ShoppingListDraft} and
     * returns an {@link UpdateAction}&lt;{@link ShoppingList}&gt; as a result in an {@link Optional}. If both the
     * {@link ShoppingList} and the {@link ShoppingListDraft} have the same number for delete days, then no update
     * action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldShoppingList the shopping list which should be updated.
     * @param newShoppingList the shopping list draft which holds the new number of days after which the ShoppingList
     *                        shall be deleted .
     * @return A filled optional with the update action or an empty optional if the Ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<ShoppingList>> buildSetDeleteDaysAfterLastModificationUpdateAction(
        @Nonnull final ShoppingList oldShoppingList,
        @Nonnull final ShoppingListDraft newShoppingList) {

        return buildUpdateAction(oldShoppingList.getDeleteDaysAfterLastModification(),
            newShoppingList.getDeleteDaysAfterLastModification(), () -> SetDeleteDaysAfterLastModification.of(
                newShoppingList.getDeleteDaysAfterLastModification()));
    }



}
