package com.commercetools.sync.inventories;

final class InventorySyncMessages {

    public static final String CTP_INVENTORY_FETCH_FAILED = "Failed to fetch existing inventory entries of SKUs %s.";
    public static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED = "Failed to update inventory entry of sku '%s' and "
        + "supply channel id '%s'.";
    public static final String INVENTORY_DRAFT_HAS_NO_SKU = "Failed to process inventory entry without sku.";
    public static final String INVENTORY_DRAFT_IS_NULL = "Failed to process null inventory draft.";
    public static final String CTP_INVENTORY_ENTRY_CREATE_FAILED = "Failed to create inventory entry of sku '%s' "
        + "and supply channel id '%s'.";
    public static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "InventoryEntryDraft with sku:'%s'. Reason: %s";
    public static final String FAILED_TO_RESOLVE_SUPPLY_CHANNEL = "Failed to resolve supply channel reference on "
        + "InventoryEntryDraft with sku:'%s'. Reason: %s";

    private InventorySyncMessages() { }
}
