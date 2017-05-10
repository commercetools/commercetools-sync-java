package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilderFactory;
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

import static com.commercetools.sync.commons.enums.Error.*;
import static java.lang.String.format;

@SuppressWarnings("unchecked")
final class GenericUpdateActionUtils {

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
            return Optional.of(GenericCustomActionBuilderFactory
                    .of(resource)
                    .buildSetCustomTypeAction(customTypeKey, customFieldsJsonMap));
        } catch (BuildUpdateActionException | IllegalAccessException | InstantiationException e) {
            syncOptions.applyErrorCallback(format(SET_CUSTOM_TYPE_BUILD_FAILED.getDescription()
                    , resource.toReference().getTypeId(), resource.getId(), e.getMessage()), e);
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
            return Optional.of(GenericCustomActionBuilderFactory
                    .of(resource)
                    .buildRemoveCustomTypeAction());
        } catch (BuildUpdateActionException | IllegalAccessException | InstantiationException e) {
            syncOptions.applyErrorCallback(format(REMOVE_CUSTOM_TYPE_BUILD_FAILED.getDescription(),
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
            return Optional.of(GenericCustomActionBuilderFactory
                    .of(resource)
                    .buildSetCustomFieldAction(customFieldName, customFieldValue));
        } catch (BuildUpdateActionException | IllegalAccessException | InstantiationException e) {
            syncOptions.applyErrorCallback(format(SET_CUSTOM_FIELD_BUILD_FAILED.getDescription(), customFieldName,
                    resource.toReference().getTypeId(), resource.getId(), e.getMessage()), e);
            return Optional.empty();
        }
    }
}
