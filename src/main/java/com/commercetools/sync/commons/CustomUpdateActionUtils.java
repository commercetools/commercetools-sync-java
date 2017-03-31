package com.commercetools.sync.commons;

import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomDraft;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.GenericUpdateActionUtils.*;

public class CustomUpdateActionUtils {

    /**
     * Compares the {@link CustomFields}, of an old resource {@link T} (for example {@link Category},
     * {@link io.sphere.sdk.products.Product}, etc..), to the {@link CustomFieldsDraft}, of a new
     * resource draft {@link S} (for example {@link CategoryDraft}, {@link io.sphere.sdk.products.ProductVariantDraft},
     * etc..), and returns a {@link List<UpdateAction>} as a result. If no update action is needed,
     * for example in the case where both the {@link CustomFields} and the {@link CustomFieldsDraft} are null, an empty
     * {@link List<UpdateAction>} is returned. A {@link TypeService} instance is injected into the
     * method to fetch the key of the old resource type from it's
     * cache (see {@link CustomUpdateActionUtils#buildNonNullCustomFieldsUpdateActions(CustomFields, CustomFieldsDraft, TypeService, Custom)}).
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <p>
     * 1. If the new resources's custom type is set, but old resources's custom type is not. A "setCustomType" update
     * actions is added, which sets the custom type (and all it's fields to the old resource).
     * 2. If the new resource's custom type is not set, but the old resource's custom type is set. A
     * "setCustomType" update action is added, which removes the type set on the old resource.
     * 3. If both the resources custom types are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom)})
     * 4. If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.
     * <p>
     * <p>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * 1. If both the resources' custom types are not set.
     * 2. If both the resources' custom type keys are not set.
     * 3. If both resources custom type keys are identical but the custom fields of the new resource's custom type is not set.
     * 4. Custom fields are both empty.
     * 5. Custom field JSON values have different ordering.
     * 6. Custom field values are identical.
     *
     * @param oldResource the resource which should be updated.
     * @param newResource the resource draft where we get the new custom fields.
     * @param typeService the type service that is used to fetch the cached key of the resource custom type.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    public static <T extends Custom, S extends CustomDraft> List<UpdateAction<T>> buildCustomUpdateActions(
            @Nonnull final T oldResource,
            @Nonnull final S newResource,
            @Nonnull final TypeService typeService) {
        final CustomFields oldResourceCustomFields = oldResource.getCustom();
        final CustomFieldsDraft newResourceCustomFields = newResource.getCustom();
        if (oldResourceCustomFields != null && newResourceCustomFields != null) {
            return buildNonNullCustomFieldsUpdateActions(oldResourceCustomFields, newResourceCustomFields, typeService, oldResource);
        } else {
            if (oldResourceCustomFields == null) {
                if (newResourceCustomFields != null) {
                    // New resource's custom fields are set, but old resources's custom fields are not set. So we
                    // should set the custom type and fields of the new resource to the old one.
                    final String newCustomFieldsTypeKey = newResourceCustomFields.getType().getKey();
                    final Map<String, JsonNode> newCustomFieldsJsonMap = newResourceCustomFields.getFields();
                    return Collections.singletonList(
                            buildTypedSetCustomTypeUpdateAction(
                                    newCustomFieldsTypeKey, newCustomFieldsJsonMap, oldResource));
                }
            } else {
                // New resource's custom fields are not set, but old resource's custom fields are set. So we
                // should remove the custom type from the old resource.
                return Collections.singletonList(buildTypedRemoveCustomTypeUpdateAction(oldResource));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Compares a non null {@link CustomFields} to a non null {@link CustomFieldsDraft} and returns a
     * {@link List<UpdateAction>} as a result. The keys are used to compare the custom types.
     * The key of the old resource custom type is fetched from the caching mechanism of the injected
     * {@link TypeService}. The key of the new resource custom type is expected to be set on the type.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <p>
     * 1. If both the resources custom type keys are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom)})
     * 2. If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.
     * <p>
     * <p>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * 1. If both the resources' custom type keys are not set.
     * 2. If both resources custom type keys are identical but the custom fields of the new resource's custom type is not set.
     *
     * @param oldCustomFields the old resource's custom fields.
     * @param newCustomFields the new resource draft's custom fields.
     * @param typeService     the type service that is used to fetch the cached key of the resource custom type.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    private static <T extends Custom> List<UpdateAction<T>> buildNonNullCustomFieldsUpdateActions(
            @Nonnull final CustomFields oldCustomFields,
            @Nonnull final CustomFieldsDraft newCustomFields,
            @Nonnull final TypeService typeService,
            @Nonnull final T resource) {
        final String oldCustomFieldsTypeKey = typeService.getCachedTypeKeyById(oldCustomFields.getType().getId());
        final Map<String, JsonNode> oldCustomFieldsJsonMap = oldCustomFields.getFieldsJsonMap();
        final String newCustomFieldsTypeKey = newCustomFields.getType().getKey();
        final Map<String, JsonNode> newCustomFieldsJsonMap = newCustomFields.getFields();

        if (Objects.equals(oldCustomFieldsTypeKey, newCustomFieldsTypeKey)) {
            if (oldCustomFieldsTypeKey == null && newCustomFieldsTypeKey == null) {
                // TODO: LOG THIS AS AN ERROR: "CUSTOM TYPE FOR BOTH OLD AND NEW RESOURCE IS NOT SET"
                // TODO: wrap all processing results in a ProcessResult wrapper class.
                return Collections.emptyList();
            }
            if (newCustomFieldsJsonMap == null) {
                // TODO: LOG THIS AS AN ERROR: "CUSTOM TYPE WITH NO CUSTOM FIELDS"
                return Collections.emptyList();
            }
            // old and new resource's custom fields are set. So we should calculate update actions for the
            // the fields of both.
            return buildSetCustomFieldsUpdateActions(oldCustomFieldsJsonMap, newCustomFieldsJsonMap, resource);
        } else {
            return Collections.singletonList(buildTypedSetCustomTypeUpdateAction(
                    newCustomFieldsTypeKey, newCustomFieldsJsonMap, resource));
        }
    }

    /**
     * Compares two {@link Map} objects representing a map of the custom field name to the JSON representation
     * of the value of the corresponding custom field. It returns a {@link List<UpdateAction>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction>} is returned.
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <p>
     * 1. A custom field value is changed.
     * 2. A custom field value is removed.
     * 3. A new custom field value is added.
     * <p>
     * <p>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * 1. Custom fields are both empty.
     * 2. Custom field JSON values have different ordering.
     * 3. Custom field values are identical.
     *
     * @param oldCustomFields the old resource's custom fields map of JSON values.
     * @param newCustomFields the new resource's custom fields map of JSON values.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    private static <T extends Custom> List<UpdateAction<T>> buildSetCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource) {
        final List<UpdateAction<T>> newOrModifiedCustomFieldsActions =
                buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource);
        final List<UpdateAction<T>> removedCustomFieldsActions =
                buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource);
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
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    private static <T extends Custom> List<UpdateAction<T>> buildNewOrModifiedCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource) {
        return newCustomFields.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(newCustomFieldName -> !Objects.equals(newCustomFields.get(newCustomFieldName), oldCustomFields.get(newCustomFieldName)))
                .map(newCustomFieldName -> buildTypedSetCustomFieldUpdateAction(newCustomFieldName, newCustomFields.get(newCustomFieldName), resource))
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
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    private static <T extends Custom> List<UpdateAction<T>> buildRemovedCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> oldCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields,
            @Nonnull final T resource) {
        return oldCustomFields.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(oldCustomFieldsName -> Objects.isNull(newCustomFields.get(oldCustomFieldsName)))
                .map(oldCustomFieldsName -> buildTypedSetCustomFieldUpdateAction(oldCustomFieldsName, null, resource))
                .collect(Collectors.toList());
    }
}

