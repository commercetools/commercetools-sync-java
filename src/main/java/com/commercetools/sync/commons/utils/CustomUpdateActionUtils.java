package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.helpers.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomDraft;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.*;
import static java.lang.String.format;

public final class CustomUpdateActionUtils {

    /**
     * Compares the {@link CustomFields} of an old resource {@link T} (for example {@link Category},
     * {@link io.sphere.sdk.products.Product}, etc..), to the {@link CustomFieldsDraft}, of a new
     * resource draft {@link S} (for example {@link CategoryDraft}, {@link io.sphere.sdk.products.ProductVariantDraft},
     * etc..), and returns a {@link List<UpdateAction>} as a result. If no update action is needed,
     * for example in the case where both the {@link CustomFields} and the {@link CustomFieldsDraft} are null, an empty
     * {@link List<UpdateAction>} is returned. A {@link BaseSyncOptions} instance is injected into the
     * method which is responsible for supplying the sync options to the sync utility method. For example, the callbacks for errors,
     * services like the {@link TypeService} instance which is responsible for fetching the key of the old resource type
     * from it's cache (see {@link CustomUpdateActionUtils#buildNonNullCustomFieldsUpdateActions(CustomFields,
     * CustomFieldsDraft, Custom, BaseSyncOptions)}).
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>If the new resources's custom type is set, but old resources's custom type is not. A "setCustomType" update
     * actions is added, which sets the custom type (and all it's fields to the old resource).</li>
     * <li>If the new resource's custom type is not set, but the old resource's custom type is set. A
     * "setCustomType" update action is added, which removes the type set on the old resource.</li>
     * <li>If both the resources custom types are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see
     * {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom, BaseSyncOptions)})</li>
     * <li>If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.</li>
     * <li>If both resources custom type keys are identical but the custom fields of the new resource's custom type is
     * not set.</li>
     * </ol>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources' custom types are not set.</li>
     * <li>If both the resources' custom type keys are not set.</li>
     * <li>Custom fields are both empty.</li>
     * <li>Custom field JSON values have different ordering.</li>
     * <li>Custom field values are identical.</li>
     * </ol>
     *
     * @param oldResource the resource which should be updated.
     * @param newResource the resource draft where we get the new custom fields.
     * @param syncOptions responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    public static <T extends Custom & Resource<T>, S extends CustomDraft> List<UpdateAction<T>> buildCustomUpdateActions(
            @Nonnull final T oldResource,
            @Nonnull final S newResource,
            @Nonnull final BaseSyncOptions syncOptions) {
        final CustomFields oldResourceCustomFields = oldResource.getCustom();
        final CustomFieldsDraft newResourceCustomFields = newResource.getCustom();
        if (oldResourceCustomFields != null && newResourceCustomFields != null) {
            try {
                return buildNonNullCustomFieldsUpdateActions(oldResourceCustomFields, newResourceCustomFields,
                        oldResource, syncOptions);
            } catch (BuildUpdateActionException e) {
                syncOptions.callUpdateActionErrorCallBack(format("Failed to build custom fields update actions on the " +
                                "%s with id '%s'. Reason: %s", oldResource.toReference().getTypeId(), oldResource.getId(),
                        e.getMessage()), e);
            }
        } else {
            if (oldResourceCustomFields == null) {
                if (newResourceCustomFields != null) {
                    // New resource's custom fields are set, but old resources's custom fields are not set. So we
                    // should set the custom type and fields of the new resource to the old one.
                    final String newCustomFieldsTypeKey = newResourceCustomFields.getType().getKey();
                    final Map<String, JsonNode> newCustomFieldsJsonMap = newResourceCustomFields.getFields();
                    final UpdateAction<T> updateAction = buildTypedSetCustomTypeUpdateAction(
                            newCustomFieldsTypeKey, newCustomFieldsJsonMap, oldResource, syncOptions).orElse(null);
                    return updateAction != null ? Collections.singletonList(updateAction) : Collections.emptyList();
                }
            } else {
                // New resource's custom fields are not set, but old resource's custom fields are set. So we
                // should remove the custom type from the old resource.
                final UpdateAction<T> updateAction = buildTypedRemoveCustomTypeUpdateAction(oldResource, syncOptions)
                        .orElse(null);
                return updateAction != null ? Collections.singletonList(updateAction) : Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Compares a non null {@link CustomFields} to a non null {@link CustomFieldsDraft} and returns a
     * {@link List<UpdateAction>} as a result. The keys are used to compare the custom types. The key of the old
     * resource custom type is fetched from the caching mechanism of the {@link TypeService} instance in the injected
     * {@link BaseSyncOptions} instance. The key of the new resource custom type is expected to be set on the type.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources custom type keys are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see
     * {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom, BaseSyncOptions)})</li>
     * <li>If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.</li>
     * <li>If both resources custom type keys are identical but the custom fields
     * of the new resource's custom type is not set.</li>
     * </ol>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources' custom type keys are not set.</li>
     * </ol>
     *
     * @param oldCustomFields the old resource's custom fields.
     * @param newCustomFields the new resource draft's custom fields.
     * @param resource        the resource that the custom fields are on. It is used to identify the type of the resource, to
     *                        call the corresponding update actions.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> List<UpdateAction<T>>
    buildNonNullCustomFieldsUpdateActions(
            @Nonnull final CustomFields oldCustomFields,
            @Nonnull final CustomFieldsDraft newCustomFields,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) throws BuildUpdateActionException {
        final String oldCustomFieldsTypeKey = syncOptions.getTypeService().getCachedTypeKeyById(oldCustomFields.getType().getId());
        final Map<String, JsonNode> oldCustomFieldsJsonMap = oldCustomFields.getFieldsJsonMap();
        final String newCustomFieldsTypeKey = newCustomFields.getType().getKey();
        final Map<String, JsonNode> newCustomFieldsJsonMap = newCustomFields.getFields();

        if (Objects.equals(oldCustomFieldsTypeKey, newCustomFieldsTypeKey)) {
            if (oldCustomFieldsTypeKey == null && newCustomFieldsTypeKey == null) {
                throw new BuildUpdateActionException(format("Custom type keys are not set for both the old and new %s.",
                        resource.toReference().getTypeId()));
            }
            if (newCustomFieldsJsonMap == null) {
                // New resource's custom fields are null/not set. So we should unset old custom fields.
                final UpdateAction<T> updateAction = buildTypedSetCustomTypeUpdateAction(
                        newCustomFieldsTypeKey, null, resource, syncOptions).orElse(null);
                return updateAction != null ? Collections.singletonList(updateAction) : Collections.emptyList();
            }
            // old and new resource's custom fields are set. So we should calculate update actions for the
            // the fields of both.
            return buildSetCustomFieldsUpdateActions(oldCustomFieldsJsonMap, newCustomFieldsJsonMap, resource, syncOptions);
        } else {
            final UpdateAction<T> updateAction =
                    buildTypedSetCustomTypeUpdateAction(
                            newCustomFieldsTypeKey, newCustomFieldsJsonMap, resource, syncOptions)
                            .orElse(null);
            return updateAction != null ? Collections.singletonList(updateAction) : Collections.emptyList();
        }
    }

    /**
     * Compares two {@link Map} objects representing a map of the custom field name to the JSON representation
     * of the value of the corresponding custom field. It returns a {@link List<UpdateAction>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>A custom field value is changed.</li>
     * <li>A custom field value is removed.</li>
     * <li>A new custom field value is added.</li>
     * </ol>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * <ol>
     * <li>Custom fields are both empty.</li>
     * <li>Custom field JSON values have different ordering.</li>
     * <li>Custom field values are identical.</li>
     *
     * @param oldCustomFields the old resource's custom fields map of JSON values.
     * @param newCustomFields the new resource's custom fields map of JSON values.
     * @param resource        the resource that the custom fields are on. It is used to identify the type of the resource, to
     *                        call the corresponding update actions.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> List<UpdateAction<T>> buildSetCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) {
        final List<UpdateAction<T>> newOrModifiedCustomFieldsActions =
                buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource, syncOptions);
        final List<UpdateAction<T>> removedCustomFieldsActions =
                buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource, syncOptions);
        return Stream.concat(newOrModifiedCustomFieldsActions.stream(),
                removedCustomFieldsActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * Traverses the new resource's custom fields map of JSON values {@link Map} to create
     * "setCustomField" update actions that represent either the modification of an existent custom field's
     * value or the addition of a new one. It returns a {@link List<UpdateAction>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     *
     * @param oldCustomFields the old resource's custom fields map of JSON values.
     * @param newCustomFields the new resource's custom fields map of JSON values.
     * @param resource        the resource that the custom fields are on. It is used to identify the type of the resource, to
     *                        call the corresponding update actions.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> List<UpdateAction<T>> buildNewOrModifiedCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) {
        return newCustomFields.keySet().stream()
                .filter(newCustomFieldName -> !Objects.equals(
                        newCustomFields.get(newCustomFieldName), oldCustomFields.get(newCustomFieldName)))
                .map(newCustomFieldName -> buildTypedSetCustomFieldUpdateAction(
                        newCustomFieldName, newCustomFields.get(newCustomFieldName), resource, syncOptions).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Traverses the old resource's custom fields map of JSON values {@link Map} to create update
     * "setCustomField" update actions that represent removal of an existent custom field from the new resource's custom
     * fields. It returns a {@link List<UpdateAction>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     *
     * @param oldCustomFields the old resource's custom fields map of JSON values.
     * @param newCustomFields the new resources's custom fields map of JSON values.
     * @param resource        the resource that the custom fields are on. It is used to identify the type of the resource, to
     *                        call the corresponding update actions.
     * @param syncOptions     responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static <T extends Custom & Resource<T>> List<UpdateAction<T>> buildRemovedCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource,
            @Nonnull final BaseSyncOptions syncOptions) {
        return oldCustomFields.keySet().stream()
                .filter(oldCustomFieldsName -> Objects.isNull(newCustomFields.get(oldCustomFieldsName)))
                .map(oldCustomFieldsName -> buildTypedSetCustomFieldUpdateAction(
                        oldCustomFieldsName, null, resource, syncOptions).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

