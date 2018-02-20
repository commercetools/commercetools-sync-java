package com.commercetools.sync.commons.utils;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

final class GenericUpdateActionUtils {

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@code T} according to the type of the
     * {@link GenericCustomActionBuilder} passed. If the {@code customTypeId} passed is blank (null/empty), the error
     * callback is triggered with an error message that the setCustomType update action cannot be built with a blank id,
     * and an empty optional is returned.
     *
     * @param <T>                    the type of the resource which has the custom fields.
     * @param <U>                    the type of the resource to do the update action on.
     * @param customTypeId           the id of the new custom type.
     * @param customFieldsJsonMap    the custom fields map of JSON values.
     * @param resource               the resource which has the custom fields.
     * @param customActionBuilder    the builder instance responsible for building the custom update actions.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param resourceIdGetter       a function used to get the id of the resource being updated.
     * @param resourceTypeIdGetter   a function used to get the Type id of the resource being updated.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @param syncOptions            responsible for supplying the sync options to the sync utility method.
     * @return a setCustomType update action of the type of the resource it's requested on, or an empty optional
     *         if the {@code customTypeId} is blank.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> Optional<UpdateAction<U>> buildTypedSetCustomTypeUpdateAction(
        @Nullable final String customTypeId,
        @Nullable final Map<String, JsonNode> customFieldsJsonMap,
        @Nonnull final T resource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> resourceIdGetter,
        @Nonnull final Function<T, String> resourceTypeIdGetter,
        @Nonnull final Function<T, String> updateIdGetter,
        @Nonnull final BaseSyncOptions syncOptions) {

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@code T} that removes the custom type set on
     * the given resource {@code T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @param <T>                    the type of the resource which has the custom fields.
     * @param <U>                    the type of the resource to do the update action on.
     * @param resource               the resource which has the custom fields.
     * @param containerResourceClass the class of the container resource which will be updated.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param resourceIdGetter       a function used to get the id of the resource being updated.
     * @param resourceTypeIdGetter   a function used to get the Type id of the resource being updated.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @param syncOptions            responsible for supplying the sync options to the sync utility method.
     * @return a setCustomType update action that removes the custom type from the resource it's requested on.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> Optional<UpdateAction<U>> buildTypedRemoveCustomTypeUpdateAction(
        @Nonnull final T resource,
        @Nullable final Class<U> containerResourceClass,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> resourceIdGetter,
        @Nonnull final Function<T, String> resourceTypeIdGetter,
        @Nonnull final Function<T, String> updateIdGetter,
        @Nonnull final BaseSyncOptions syncOptions) {
        try {
            return Optional.of(
                GenericCustomActionBuilderFactory.createBuilder(resource, containerResourceClass)
                                                 .buildRemoveCustomTypeAction(variantId,
                                                     updateIdGetter.apply(resource)));
        } catch (BuildUpdateActionException | IllegalAccessException | InstantiationException exception) {
            syncOptions.applyErrorCallback(format(REMOVE_CUSTOM_TYPE_BUILD_FAILED, resourceTypeIdGetter.apply(resource),
                resourceIdGetter.apply(resource), exception.getMessage()), exception);
            return Optional.empty();
        }
    }

    /**
     * Creates a CTP "setCustomField" update action on the given resource {@code T} that updates a custom field with
     * {@code customFieldName} and a {@code customFieldValue} on the given
     * resource {@code T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @param customFieldName        the name of the custom field to update.
     * @param customFieldValue       the new JSON value of the custom field.
     * @param <T>                    the type of the resource which has the custom fields.
     * @param <U>                    the type of the resource to do the update action on.
     * @param resource               the resource which has the custom fields.
     * @param containerResourceClass the class of the container resource which will be updated.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param resourceIdGetter       a function used to get the id of the resource being updated.
     * @param resourceTypeIdGetter   a function used to get the Type id of the resource being updated.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @param syncOptions            responsible for supplying the sync options to the sync utility method.
     * @return a setCustomField update action on the provided field name, with the provided value
     *         on the resource it's requested on.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> Optional<UpdateAction<U>> buildTypedSetCustomFieldUpdateAction(
        @Nonnull final String customFieldName,
        @Nullable final JsonNode customFieldValue,
        @Nonnull final T resource,
        @Nullable final Class<U> containerResourceClass,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> resourceIdGetter,
        @Nonnull final Function<T, String> resourceTypeIdGetter,
        @Nonnull final Function<T, String> updateIdGetter,
        @Nonnull final BaseSyncOptions syncOptions) {
        try {
        if (!isBlank(customTypeId)) {
            return Optional.of(
                GenericCustomActionBuilderFactory.createBuilder(resource, containerResourceClass)
                                                 .buildSetCustomFieldAction(variantId,
                                                     updateIdGetter.apply(resource), customFieldName,
                                                     customFieldValue));
        } catch (BuildUpdateActionException | IllegalAccessException | InstantiationException exception) {
            syncOptions.applyErrorCallback(format(SET_CUSTOM_FIELD_BUILD_FAILED, customFieldName,
                resourceTypeIdGetter.apply(resource), resourceIdGetter.apply(resource), exception.getMessage()),
                exception);
                customActionBuilder.buildSetCustomTypeAction(variantId, updateIdGetter.apply(resource),
                    customTypeId, customFieldsJsonMap));
        } else {
            final String errorMessage = format("Failed to build 'setCustomType' update action on the "
                    + "%s with id '%s'. Reason: New Custom Type id is blank (null/empty).",
                resourceTypeIdGetter.apply(resource), resourceIdGetter.apply(resource));
            syncOptions.applyErrorCallback(errorMessage);
            return Optional.empty();
        }
    }
}
