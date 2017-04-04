package com.commercetools.sync.commons.constants;


import static java.lang.String.format;

// TODO: DOCUMENT
public class SyncMessages {
    private static final String REQUIRED_FIELD_MESSAGE_FORMAT = "There were (x) %s with null/not set %s fields." +
            " The old %s' %s fields weren't updated.";

    public static final String CATEGORY_SET_DESCRIPTION_EMPTY_DESCRIPTION = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "categories", "description", "categories", "description");
    public static final String CATEGORY_CHANGE_PARENT_EMPTY_PARENT = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "categories", "parent", "categories", "parent");

    public static final String CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "categories", "orderHint", "categories", "orderHint");

    private SyncMessages() {
    }
}
