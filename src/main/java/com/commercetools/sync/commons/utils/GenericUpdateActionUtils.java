package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.helpers.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.sync.commons.constants.UpdateActions.*;
import static java.lang.String.format;

class GenericUpdateActionUtils {

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@link T} (which currently could either
     * be a {@link Category} or a {@link Channel}).
     *
     * @param customTypeKey       the key of the new custom type.
     * @param customFieldsJsonMap the custom fields map of JSON values.
     * @param resource            the resource to do the update action on.
     * @param <T>                 the type of the resource to do the update action on.
     * @param syncOptions         responsible for supplying the sync options to the sync utility method.
     * @return a setCustomType update action of the type of the resource it's requested on.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedSetCustomTypeUpdateAction(
            @Nullable final String customTypeKey,
            @Nullable final Map<String, JsonNode> customFieldsJsonMap,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) {
        try {
            return buildTypedUpdateAction(customTypeKey, customFieldsJsonMap, resource, SET_CUSTOM_TYPE);
        } catch (BuildUpdateActionException e) {
            syncOptions.callUpdateActionErrorCallBack(format("Failed to build 'setCustomType' update action on " +
                    "the %s with id '%s'. Reason: %s", resource.toReference().getTypeId(), resource.getId(), e.getMessage()), e);
            return Optional.empty();
        }
    }

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@link T} that removes the custom type set on
     * the given resource {@link T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @param resource    the resource to do the update action on.
     * @param <T>         the type of the resource to do the update action on.
     * @param syncOptions responsible for supplying the sync options to the sync utility method.
     * @return a setCustomType update action that removes the custom type from the resource it's requested on.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedRemoveCustomTypeUpdateAction(
            @Nonnull final T resource, @Nonnull final BaseSyncOptions syncOptions) {
        try {
            return buildTypedUpdateAction(resource, SET_CUSTOM_TYPE_REMOVE);
        } catch (BuildUpdateActionException e) {
            syncOptions.callUpdateActionErrorCallBack(format("Failed to build 'setCustomType' update action to" +
                            " remove the custom type on the %s with id '%s'. Reason: %s",
                    resource.toReference().getTypeId(), resource.getId(), e.getMessage()), e);
            return Optional.empty();
        }
    }

    /**
     * Creates a CTP "setCustomField" update action on the given resource {@link T} that updates a custom field with
     * {@code customFieldName} and a {@code customFieldValue} on the given
     * resource {@link T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @param customFieldName  the name of the custom field to update.
     * @param customFieldValue the new JSON value of the custom field.
     * @param resource         the resource to do the update action on.
     * @param <T>              the type of the resource to do the update action on.
     * @param syncOptions      responsible for supplying the sync options to the sync utility method.
     * @return a setCustomField update action on the provided field name, with the provided value
     * on the resource it's requested on.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedSetCustomFieldUpdateAction(
            @Nonnull final String customFieldName,
            @Nullable final JsonNode customFieldValue,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) {
        try {
            return buildTypedUpdateAction(customFieldName, customFieldValue, resource, SET_CUSTOM_FIELD);
        } catch (BuildUpdateActionException e) {
            syncOptions.callUpdateActionErrorCallBack(format("Failed to build 'setCustomField' update action on " +
                            "the custom field with the name '%s' on the %s with id '%s'. Reason: %s", customFieldName,
                    resource.toReference().getTypeId(), resource.getId(), e.getMessage()), e);
            return Optional.empty();
        }
    }

    @Nonnull
    private static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedUpdateAction(
            @Nullable final String customTypeKey,
            @Nullable final Map<String, JsonNode> customFieldsJsonMap,
            @Nonnull final T resource,
            @Nonnull final String updateActionName) throws BuildUpdateActionException {
        return buildTypedUpdateAction(customTypeKey, customFieldsJsonMap, null, null,
                resource, updateActionName);
    }

    // This method is not private since it is used by one of the unit tests.
    @Nonnull
    static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedUpdateAction(
            @Nonnull final T resource,
            @Nonnull final String updateActionName) throws BuildUpdateActionException {
        return buildTypedUpdateAction(null, null, null, null,
                resource, updateActionName);
    }

    @Nonnull
    private static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedUpdateAction(
            @Nullable final String customFieldName,
            @Nullable final JsonNode customFieldValue,
            @Nonnull final T resource,
            @Nonnull final String updateActionName) throws BuildUpdateActionException {
        return buildTypedUpdateAction(null, null, customFieldName, customFieldValue,
                resource, updateActionName);

    }

    /**
     * Creates a CTP update action on the given resource {@link T} (which currently could either be a {@link Category}
     * or a {@link Channel}) according to the {@code updateActionName} flag. According to this flag value, the required
     * update action is built:
     * <ol>
     * <li>SET_CUSTOM_TYPE_REMOVE -> creates a "setCustomType" update action that removes a custom type from the resource.</li>
     * <li>SET_CUSTOM_TYPE -> creates a "setCustomType" update action that changes the custom type set on the resource.</li>
     * <li>SET_CUSTOM_FIELD -> creates a "setCustomField" update action that changes the value of a custom field.</li>
     * </ol>
     *
     * @param customTypeKey       the key of the new custom type, only if the flag is SET_CUSTOM_TYPE.
     * @param customFieldsJsonMap the custom fields map of JSON values, only if the flag is SET_CUSTOM_TYPE.
     * @param customFieldName     the name of the custom field to update, only if the flag is SET_CUSTOM_FIELD.
     * @param customFieldValue    the new JSON value of the custom field, only if the flag is SET_CUSTOM_FIELD.
     * @param resource            the resource to do the update action on.
     * @param updateActionName    the flag value that decided which update action to do.
     * @param <T>                 the type of the resource to do the update action on.
     * @return an update action that depends on the provided flag on the resource it's requested on.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private static <T extends Custom & Resource<T>> Optional<UpdateAction<T>> buildTypedUpdateAction(
            @Nullable final String customTypeKey,
            @Nullable final Map<String, JsonNode> customFieldsJsonMap,
            @Nullable final String customFieldName,
            @Nullable final JsonNode customFieldValue,
            @Nonnull final T resource,
            @Nonnull final String updateActionName) throws BuildUpdateActionException {
        if (resource instanceof Category) {
            switch (updateActionName) {
                case SET_CUSTOM_TYPE_REMOVE:
                    return Optional.of((UpdateAction<T>)
                            io.sphere.sdk.categories.commands.updateactions.SetCustomType.ofRemoveType());
                case SET_CUSTOM_TYPE:
                    return Optional.of((UpdateAction<T>)
                            io.sphere.sdk.categories.commands.updateactions.
                                    SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap));
                case SET_CUSTOM_FIELD:
                    return Optional.of((UpdateAction<T>) io.sphere.sdk.categories.commands.updateactions
                            .SetCustomField.ofJson(customFieldName, customFieldValue));
                default:
                    throw new BuildUpdateActionException(format("Update action '%s' for Categories is not implemented.",
                            updateActionName));
            }
        }
        if (resource instanceof Channel) {
            switch (updateActionName) {
                case SET_CUSTOM_TYPE_REMOVE:
                    return Optional.of((UpdateAction<T>) io.sphere.sdk.channels.commands.updateactions
                            .SetCustomType.ofRemoveType());
                case SET_CUSTOM_TYPE:
                    return Optional.of((UpdateAction<T>) io.sphere.sdk.channels.commands.updateactions
                            .SetCustomType.ofTypeKeyAndJson(customTypeKey, customFieldsJsonMap));
                case SET_CUSTOM_FIELD:
                    return Optional.of((UpdateAction<T>) io.sphere.sdk.channels.commands.updateactions
                            .SetCustomField.ofJson(customFieldName, customFieldValue));
                default:
                    throw new BuildUpdateActionException(format("Update action '%s' for Channels is not implemented.",
                            updateActionName));
            }
        }
        throw new BuildUpdateActionException(format("Update actions for resource: '%s' is not implemented.",
                resource.toReference().getTypeId()));
    }
}
