package com.commercetools.sync.shoppinglists.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

// TODO (JVM-SDK), open an issue for this.
public final class AddLineItemWithSKU extends UpdateActionImpl<ShoppingList> {
    private final String sku;
    private final Long quantity;
    private final ZonedDateTime addedAt;
    private final CustomFieldsDraft custom;

    public AddLineItemWithSKU(
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
}

