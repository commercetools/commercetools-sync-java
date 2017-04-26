package com.commercetools.sync.commons.constants;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.types.Custom;

import java.util.Map;

/**
 * Used as a container of constants that could be used as flags to trigger the needed update action for
 * {@link com.commercetools.sync.commons.utils.GenericUpdateActionUtils#buildTypedUpdateAction(String, Map, String,
 * JsonNode, Custom, UpdateAction)}
 */
public final class UpdateActions {

    /**
     * <ol>
     * <li>SET_CUSTOM_TYPE_REMOVE -> signal for a "setCustomType" update action that removes a custom type from the
     * resource.</li>
     * <li>SET_CUSTOM_TYPE -> signal for a "setCustomType" update action that changes the custom type set on the
     * resource.</li>
     * <li>SET_CUSTOM_FIELD -> signal for a "setCustomField" update action that changes the value of a custom
     * field.</li>
     * </ol>
     */
    public enum UpdateAction {
        SET_CUSTOM_TYPE_REMOVE, SET_CUSTOM_TYPE, SET_CUSTOM_FIELD, NON_IMPLEMENTED_ACTION
    }

    private UpdateActions() {
    }
}
