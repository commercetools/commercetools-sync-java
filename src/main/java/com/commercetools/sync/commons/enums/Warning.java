package com.commercetools.sync.commons.enums;


import javax.annotation.Nonnull;

import static java.lang.String.format;

/**
 * Represents all warning messages across the sync update action build modules.
 */
public enum Warning {
    CATEGORY_SET_DESCRIPTION_EMPTY_DESCRIPTION("Cannot unset 'description' field of category with id '%s'."),
    CATEGORY_CHANGE_PARENT_EMPTY_PARENT("Cannot unset 'parent' field of category with id '%s'."),
    CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT("Cannot unset 'orderHint' field of category with id '%s'.");

    private final String description;

    Warning(@Nonnull final String description) {
        this.description = description;
    }

    /**
     * Substitutes the given value of the {@code resourceId} in the string description of the warning and returns the
     * result.
     *
     * @param resourceId the id of the resource to be substituted in the warning message.
     * @return the description of the warning message.
     */
    public String getDescription(final String resourceId) {
        return format(this.description, resourceId);
    }
}