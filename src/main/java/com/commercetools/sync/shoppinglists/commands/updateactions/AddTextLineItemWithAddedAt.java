package com.commercetools.sync.shoppinglists.commands.updateactions;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * TODO (JVM-SDK): https://github.com/commercetools/commercetools-jvm-sdk/issues/2079
 * ShoppingList#AddTextLineItem action does not support `addedAt` value,
 * so we needed to add this custom action as a workaround.
 */
public final class AddTextLineItemWithAddedAt extends UpdateActionImpl<ShoppingList> {

    private final LocalizedString name;
    private final LocalizedString description;
    private final Long quantity;
    private final ZonedDateTime addedAt;
    private final CustomFieldsDraft custom;

    private AddTextLineItemWithAddedAt(
        @Nonnull final LocalizedString name,
        @Nullable final LocalizedString description,
        @Nullable final Long quantity,
        @Nullable final ZonedDateTime addedAt,
        @Nullable final CustomFieldsDraft custom) {

        super("addTextLineItem");
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.addedAt = addedAt;
        this.custom = custom;
    }

    /**
     * Creates an update action "addTextLineItem" which adds a text line item to a shopping list.
     *
     * @param textLineItemDraft text line item draft template to map update action's fields.
     * @return an update action "addTextLineItem" which adds a text line item to a shopping list.
     */
    @Nonnull
    public static UpdateAction<ShoppingList> of(@Nonnull final TextLineItemDraft textLineItemDraft) {

        return new AddTextLineItemWithAddedAt(
            textLineItemDraft.getName(),
            textLineItemDraft.getDescription(),
            textLineItemDraft.getQuantity(),
            textLineItemDraft.getAddedAt(),
            textLineItemDraft.getCustom());
    }
}
