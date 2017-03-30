package com.commercetools.sync.commons;


import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.channels.Channel;
import java.util.Map;

import static com.commercetools.sync.commons.constants.UpdateActions.*;

/**
 * TODO: UNIT TESTS
 * TODO: JAVADOC
 */
public class GenericUpdateActionBuilder {

    @Nullable
    static <T extends Custom> UpdateAction<T> buildTypedSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                            @Nullable final Map<String, JsonNode> customFieldsJsonMap,
                                                                            @Nonnull final T resource) {
        return buildTypedUpdateAction(customTypeKey, customFieldsJsonMap, resource, SET_CUSTOM_TYPE);
    }

    @Nullable
    static <T extends Custom> UpdateAction<T> buildTypedRemoveCustomTypeAction(@Nonnull final T resource) {
        return buildTypedUpdateAction(resource, SET_CUSTOM_TYPE_REMOVE);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T extends Custom> UpdateAction<T> buildTypedSetCustomFieldAction(@Nonnull final String customFieldName,
                                                                             @Nullable final JsonNode customFieldValue,
                                                                             @Nonnull final T resource) {
        return buildTypedUpdateAction(customFieldName, customFieldValue, resource, SET_CUSTOM_FIELD);
    }

    @Nullable
    static <T extends Custom> UpdateAction<T> buildTypedUpdateAction(@Nullable final String customTypeKey,
                                                                     @Nullable final Map<String, JsonNode> customFieldsJsonMap,
                                                                     @Nonnull final T resource,
                                                                     @Nonnull final String updateAction) {
        return buildTypedUpdateAction(customTypeKey, customFieldsJsonMap, null, null,
                resource, updateAction);
    }

    @Nullable
    static <T extends Custom> UpdateAction<T> buildTypedUpdateAction(@Nonnull final T resource,
                                                                     @Nonnull final String updateAction) {
        return buildTypedUpdateAction(null, null, null, null,
                resource, updateAction);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T extends Custom> UpdateAction<T> buildTypedUpdateAction(@Nullable final String customFieldName,
                                                                     @Nullable final JsonNode customFieldValue,
                                                                     @Nonnull final T resource,
                                                                     @Nonnull final String updateAction) {
        return buildTypedUpdateAction(null, null, customFieldName, customFieldValue,
                resource, updateAction);

    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T extends Custom> UpdateAction<T> buildTypedUpdateAction(@Nullable final String customTypeKey,
                                                                     @Nullable final Map<String, JsonNode> customFieldsJsonMap,
                                                                     @Nullable final String customFieldName,
                                                                     @Nullable final JsonNode customFieldValue,
                                                                     @Nonnull final T resource,
                                                                     @Nonnull final String updateAction) {
        if (resource instanceof Category) {
            switch (updateAction) {
                case SET_CUSTOM_TYPE_REMOVE:
                    return (UpdateAction<T>)
                            io.sphere.sdk.categories.commands.updateactions.SetCustomType.ofRemoveType();
                case SET_CUSTOM_TYPE:
                    return (UpdateAction<T>)
                            io.sphere.sdk.categories.commands.updateactions.
                                    SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap);
                case SET_CUSTOM_FIELD:
                    return (UpdateAction<T>) io.sphere.sdk.categories.commands.updateactions
                            .SetCustomField.ofJson(customFieldName, customFieldValue);
                default:
                    return null;
            }
        }
        if (resource instanceof Channel) {
            switch (updateAction) {
                case SET_CUSTOM_TYPE_REMOVE:
                    return (UpdateAction<T>) io.sphere.sdk.channels.commands.updateactions
                            .SetCustomType.ofRemoveType();
                case SET_CUSTOM_TYPE:
                    return (UpdateAction<T>) io.sphere.sdk.channels.commands.updateactions
                            .SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap);
                case SET_CUSTOM_FIELD:
                    return (UpdateAction<T>) io.sphere.sdk.categories.commands.updateactions
                            .SetCustomField.ofJson(customFieldName, customFieldValue);
                default:
                    return null;
            }
        }
        return null;
    }
}
