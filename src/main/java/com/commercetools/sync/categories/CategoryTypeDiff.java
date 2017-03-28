package com.commercetools.sync.categories;


import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.*;

public class CategoryTypeDiff {

    /**
     * Compares the {@link CustomFields}, of an existing {@link Category}, to the {@link CustomFieldsDraft}, of a new
     * {@link CategoryDraft}, and returns a {@link List<UpdateAction<Category>>} as a result. If no update action is
     * needed, for example in the case where both the {@link CustomFields} and the {@link CustomFieldsDraft} are null,
     * an empty {@link List<UpdateAction<Category>>} is returned. A {@link TypeService} instance is injected into the
     * method to fetch the key of the existing category type from it's
     * cache (see {@link CategoryTypeDiff#buildNonNullCustomFieldsActions(CustomFields, CustomFieldsDraft, TypeService)}).
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <p>
     * 1. If the new category's custom type is set, but existing category's custom type is not. A "setCustomType" update
     * actions is added, which sets the custom type (and all it's fields to the existing category).
     * 2. If the new category's custom type is not set, but the existing category's custom type is set. A
     * "setCustomType" update action is added, which removes the type set on the existing category.
     * 3. If both the categories custom types are the same and the custom fields are both set. The custom
     * field values of both categories are then calculated. (see {@link CategoryTypeDiff#buildSetCustomFieldsActions(Map, Map)})
     * 4. If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * existing category's custom type is set to be as the new one's.
     * <p>
     * <p>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * 1. If both the categories' custom types are not set.
     * 2. If both the categories' custom type keys are not set.
     * 3. If both categories custom type keys are identical but the custom fields of the new category's custom type is not set.
     * 4. Custom fields are both empty.
     * 5. Custom field JSON values have different ordering.
     * 6. Custom field values are identical.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new custom fields.
     * @param typeService      the type service that is used to fetch the cached key of the category custom type.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static List<UpdateAction<Category>> buildTypeActions(@Nonnull final Category existingCategory,
                                                         @Nonnull final CategoryDraft newCategory,
                                                         @Nonnull final TypeService typeService) {
        final CustomFields existingCustomFields = existingCategory.getCustom();
        final CustomFieldsDraft newCustomFieldsDraft = newCategory.getCustom();
        if (existingCustomFields != null && newCustomFieldsDraft != null) {
            return buildNonNullCustomFieldsActions(existingCustomFields, newCustomFieldsDraft, typeService);
        } else {
            if (existingCustomFields == null) {
                if (newCustomFieldsDraft != null) {
                    // New category's custom fields are set, but existing category's custom fields are not set. So we
                    // should set the custom type and fields of the new category to the existing one.
                    final String newCustomFieldsDraftTypeKey = newCustomFieldsDraft.getType().getKey();
                    final Map<String, JsonNode> newCustomFieldsDraftJsonMap = newCustomFieldsDraft.getFields();
                    return Collections.singletonList(
                            SetCustomType.ofTypeKeyAndJson(
                                    newCustomFieldsDraftTypeKey, newCustomFieldsDraftJsonMap)
                    );
                }
            } else {
                // New category's custom fields are not set, but existing category's custom fields are set. So we
                // should remove the custom type from the existing category.
                return Collections.singletonList(SetCustomType.ofRemoveType());
            }
        }
        return Collections.emptyList();
    }

    /**
     * Compares a non null {@link CustomFields} to a non null {@link CustomFieldsDraft} and returns a
     * {@link List<UpdateAction<Category>>} as a result. The keys are used to compare the custom types.
     * The key of the existing category custom type is fetched from the caching mechanism of the injected
     * {@link TypeService}. The key of the new category custom type is expected to be set on the type.
     * If no update action is needed an empty {@link List<UpdateAction<Category>>} is returned.
     * <p>
     * <p>
     * An update action will be added to the result list in the following cases:-
     * <p>
     * 1. If both the categories custom type keys are the same and the custom fields are both set. The custom
     * field values of both categories are then calculated. (see {@link CategoryTypeDiff#buildSetCustomFieldsActions(Map, Map)})
     * 2. If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * existing category's custom type is set to be as the new one's.
     * <p>
     * <p>
     * <p>
     * An update action will <bold>not</bold> be added to the result list in the following cases:-
     * 1. If both the categories' custom type keys are not set.
     * 2. If both categories custom type keys are identical but the custom fields of the new category's custom type is not set.
     *
     * @param existingCustomFields the existing category's custom fields.
     * @param newCustomFieldsDraft the new category draft's custom fields.
     * @param typeService          the type service that is used to fetch the cached key of the category custom type.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static List<UpdateAction<Category>> buildNonNullCustomFieldsActions(@Nonnull final CustomFields existingCustomFields,
                                                                        @Nonnull final CustomFieldsDraft newCustomFieldsDraft,
                                                                        @Nonnull final TypeService typeService) {
        final String existingCustomFieldsTypeKey = typeService.getCachedTypeKeyById(existingCustomFields.getType().getId());
        final Map<String, JsonNode> existingCustomFieldsJsonMap = existingCustomFields.getFieldsJsonMap();
        final String newCustomFieldsDraftTypeKey = newCustomFieldsDraft.getType().getKey();
        final Map<String, JsonNode> newCustomFieldsDraftJsonMap = newCustomFieldsDraft.getFields();

        if (Objects.equals(existingCustomFieldsTypeKey, newCustomFieldsDraftTypeKey)) {
            if (existingCustomFieldsTypeKey == null && newCustomFieldsDraftTypeKey == null) {
                // TODO: LOG THIS AS AN ERROR: "CUSTOM TYPE FOR BOTH EXISTING AND NEW CATEGORY IS NOT SET"
                // TODO: wrap all processing results in a ProcessResult wrapper class.
                return Collections.emptyList();
            }
            if (newCustomFieldsDraftJsonMap == null) {
                // TODO: LOG THIS AS AN ERROR: "CUSTOM TYPE WITH NO CUSTOM FIELDS"
                return Collections.emptyList();
            }
            // New and existing category's custom fields are set. So we should calculate update actions for the
            // the fields of both.
            return buildSetCustomFieldsActions(existingCustomFieldsJsonMap, newCustomFieldsDraftJsonMap);
        } else {
            return Collections.singletonList(
                    SetCustomType.ofTypeKeyAndJson(
                            newCustomFieldsDraftTypeKey, newCustomFieldsDraftJsonMap)
            );
        }
    }

    /**
     * TODO: USE COLLECT
     * Compares two {@link Map<String, JsonNode>} representing a map of the custom field name to the JSON representation
     * of the value of the corresponding custom field. It returns a {@link List<UpdateAction<Category>>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction<Category>>} is returned.
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
     * @param existingCustomFields the existing category's custom fields map of JSON values.
     * @param newCustomFields      the new category's custom fields map of JSON values.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static List<UpdateAction<Category>> buildSetCustomFieldsActions(
            @Nonnull final Map<String, JsonNode> existingCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields) {
        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        updateActions.addAll(buildNewOrModifiedCustomFieldsActions(existingCustomFields, newCustomFields));
        updateActions.addAll(buildRemovedCustomFieldsActions(existingCustomFields, newCustomFields));
        return updateActions;
    }

    /**
     * TODO: USE COLLECT
     * Traverses the new category's custom fields map of JSON values {@link Map<String, JsonNode>} to create
     * "setCustomField" update actions that represent either the modification of an existent custom field's
     * value or the addition of a new one. It returns a {@link List<UpdateAction<Category>>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction<Category>>} is returned.
     *
     * @param existingCustomFields the existing category's custom fields map of JSON values.
     * @param newCustomFields      the new category's custom fields map of JSON values.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static List<UpdateAction<Category>> buildNewOrModifiedCustomFieldsActions(
            @Nonnull final Map<String, JsonNode> existingCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields) {
        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        newCustomFields.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(newCustomFieldName -> !Objects.equals(newCustomFields.get(newCustomFieldName), existingCustomFields.get(newCustomFieldName)))
                .forEach(newCustomFieldName -> updateActions.add(SetCustomField.ofJson(newCustomFieldName, newCustomFields.get(newCustomFieldName))));
        return updateActions;
    }

    /**
     * TODO: USE COLLECT
     * Traverses the existing category's custom fields map of JSON values {@link Map<String, JsonNode>} to create update
     * "setCustomField" update actions that represent removal of an existent custom field from the new category's custom
     * fields. It returns a {@link List<UpdateAction<Category>>} as a result.
     * If no update action is needed an empty {@link List<UpdateAction<Category>>} is returned.
     *
     * @param existingCustomFields the existing category's custom fields map of JSON values.
     * @param newCustomFields      the new category's custom fields map of JSON values.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are needed.
     */
    @Nonnull
    static List<UpdateAction<Category>> buildRemovedCustomFieldsActions(
            @Nonnull final Map<String, JsonNode> existingCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields) {
        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        existingCustomFields.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(existingCustomFieldsName -> Objects.isNull(newCustomFields.get(existingCustomFieldsName)))
                .forEach(existingCustomFieldsName -> updateActions.add(SetCustomField.ofJson(existingCustomFieldsName, null)));
        return updateActions;
    }
}
