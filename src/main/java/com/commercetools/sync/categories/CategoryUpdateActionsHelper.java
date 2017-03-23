package com.commercetools.sync.categories;


import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CategoryUpdateActionsHelper {

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeNameUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getName(),
                newCategory.getName(),
                ChangeName.of(newCategory.getName()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeSlugUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getSlug(),
                newCategory.getSlug(),
                ChangeSlug.of(newCategory.getSlug()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE DESCRIPTION REMOVAL
        // TODO: TEMP WORKAROUND:
        if (newCategory.getDescription() == null) {
            return Optional.empty();
        }
        return buildUpdateActionForLocalizedStrings(existingCategory.getDescription(),
                newCategory.getDescription(),
                SetDescription.of(newCategory.getDescription()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE PARENT REMOVAL
        // TODO: TEMP WORKAROUND:
        if (newCategory.getParent() == null) {
            return Optional.empty();
        }
        assert newCategory.getParent() != null;
        return buildUpdateActionForReferences(existingCategory.getParent(),
                newCategory.getParent(),
                ChangeParent.of(newCategory.getParent()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE ORDERHINT REMOVAL
        if (newCategory.getOrderHint() == null) {
            return Optional.empty();
        }
        return buildUpdateActionForStrings(existingCategory.getOrderHint(),
                newCategory.getOrderHint(),
                ChangeOrderHint.of(newCategory.getOrderHint()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaTitle(),
                newCategory.getMetaTitle(),
                SetMetaTitle.of(newCategory.getMetaTitle()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaKeywords(),
                newCategory.getMetaKeywords(),
                SetMetaKeywords.of(newCategory.getMetaKeywords()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaDescription(),
                newCategory.getMetaDescription(),
                SetMetaDescription.of(newCategory.getMetaDescription()));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(
            @Nullable final LocalizedString existingLocalisedString,
            @Nullable final LocalizedString newLocalisedString,
            UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingLocalisedString, newLocalisedString, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForReferences(
            @Nullable final Reference<Category> existingCategoryReference,
            @Nullable final Reference<Category> newCategoryReference,
            UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingCategoryReference, newCategoryReference, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForStrings(@Nullable final String existingString,
                                                                        @Nullable final String newString,
                                                                        UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingString, newString, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForObjects(@Nullable final Object existingObject,
                                                                        @Nullable final Object newObject,
                                                                        UpdateAction<Category> updateAction) {
        if (!Objects.equals(existingObject, newObject)) {
            return Optional.of(updateAction);
        }
        return Optional.empty();
    }

    //TODO: UNIT TEST
    //TODO: JAVADOC
    @Nonnull
    static List<UpdateAction<Category>> buildCustomTypeActions(@Nonnull final Category existingCategory,
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
                    String newCustomFieldsDraftTypeKey = newCustomFieldsDraft.getType().getKey();
                    Map<String, JsonNode> newCustomFieldsDraftJsonMap = newCustomFieldsDraft.getFields();
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

    // TODO: UNIT TEST
    // TODO: JAVADOC
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
                // TODO: wrap all procesesing results in a ProcessResult wrapper class.
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
     * TODO: JAVADOC
     *
     * @param existingCustomFields
     * @param newCustomFields
     * @return
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
     * TODO: JAVADOC
     * Calculate update actions for new or changed custom fields.
     *
     * @param existingCustomFields
     * @param newCustomFields
     * @return
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
     * TODO: JAVADOC
     * Calculate update actions for custom fields that don't exist anymore.
     *
     * @param existingCustomFields
     * @param newCustomFields
     * @return
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
