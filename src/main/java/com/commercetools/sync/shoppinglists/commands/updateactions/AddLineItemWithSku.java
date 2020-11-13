package com.commercetools.sync.shoppinglists.commands.updateactions;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * TODO (JVM-SDK): https://github.com/commercetools/commercetools-jvm-sdk/issues/2079
 * ShoppingList#AddLineItem action does not support product variant selection by SKU,
 * so we needed to add this custom action as a workaround.
 */
public final class AddLineItemWithSku extends UpdateActionImpl<ShoppingList> {

    private final String sku;
    private final Long quantity;
    private final ZonedDateTime addedAt;
    private final CustomFieldsDraft custom;

    private AddLineItemWithSku(
        @Nullable final String sku,
        @Nullable final Long quantity,
        @Nullable final ZonedDateTime addedAt,
        @Nullable final CustomFieldsDraft custom) {

        super("addLineItem");

        this.sku = sku;
        this.quantity = quantity;
        this.addedAt = addedAt;
        this.custom = custom;
    }

    /**
     * Creates an update action "addLineItem" which adds a line item to a shopping list.
     *
     * @param lineItemDraft Line item draft template to map update action's fields.
     * @return an update action "addLineItem" which adds a line item to a shopping list.
     */
    @Nonnull
    public static UpdateAction<ShoppingList> of(@Nonnull final LineItemDraft lineItemDraft) {

        return new AddLineItemWithSku(lineItemDraft.getSku(),
            lineItemDraft.getQuantity(),
            lineItemDraft.getAddedAt(),
            lineItemDraft.getCustom());
    }

    public String getSku() {
        return sku;
    }
}

