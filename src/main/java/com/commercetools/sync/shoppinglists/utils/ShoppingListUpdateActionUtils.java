package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public final class ShoppingListUpdateActionUtils {
    private ShoppingListUpdateActionUtils() { }

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
}
