package com.commercetools.sync.commons.constants;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.types.Custom;

import java.util.Map;

/**
 * Used as a container of constants that could be used as flags to trigger the needed update action for
 * {@link com.commercetools.sync.commons.utils.GenericUpdateActionUtils#buildTypedUpdateAction(String, Map, String, JsonNode, Custom, String)}
 */
public final class UpdateActions {
    public static final String SET_CUSTOM_TYPE_REMOVE = "SET_CUSTOM_TYPE_REMOVE";
    public static final String SET_CUSTOM_TYPE = "SET_CUSTOM_TYPE";
    public static final String SET_CUSTOM_FIELD = "SET_CUSTOM_FIELD";

    private UpdateActions() {
    }
}
