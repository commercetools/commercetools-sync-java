package com.commercetools.sync.commons.constants;


import static java.lang.String.format;

/**
 * Used as a container or all {@link String} entries that are used
 * as error/warning messages across the sync update action build modules.
 */
public class SyncMessages {
    private static final String REQUIRED_FIELD_MESSAGE_FORMAT = "Cannot unset %s field of %s" +
            " with id '%s'.";

    public static final String CATEGORY_SET_DESCRIPTION_EMPTY_DESCRIPTION = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "description", "category", "%s");
    public static final String CATEGORY_CHANGE_PARENT_EMPTY_PARENT = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "parent", "category", "%s");
    public static final String CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT = format(REQUIRED_FIELD_MESSAGE_FORMAT,
            "orderHint", "category", "%s");

    private SyncMessages() {
    }
}
