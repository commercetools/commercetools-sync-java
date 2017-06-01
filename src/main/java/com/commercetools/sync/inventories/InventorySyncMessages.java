package com.commercetools.sync.inventories;

class InventorySyncMessages {

    public static final String CTP_INVENTORY_FETCH_FAILED = "Failed to fetch existing inventory entries of SKUs %s.";
    public static final String CTP_CHANNEL_FETCH_FAILED = "Failed to fetch supply channels.";
    public static final String CTP_INVENTORY_ENTRY_UPDATE_FAILED = "Failed to update inventory entry of sku '%s' and "
        + "supply channel key '%s'.";
    public static final String INVENTORY_DRAFT_HAS_NO_SKU = "Failed to process inventory entry without sku.";
    public static final String INVENTORY_DRAFT_IS_NULL = "Failed to process null inventory draft.";
    public static final String CTP_CHANNEL_CREATE_FAILED = "Failed to create new supply channel of key '%s'.";
    public static final String CTP_INVENTORY_ENTRY_CREATE_FAILED = "Failed to create inventory entry of sku '%s' "
        + "and supply channel key '%s'.";
    public static final String CHANNEL_KEY_MAPPING_DOESNT_EXIST = "Failed to find supply channel of key '%s'.";

    private InventorySyncMessages() { }
}
