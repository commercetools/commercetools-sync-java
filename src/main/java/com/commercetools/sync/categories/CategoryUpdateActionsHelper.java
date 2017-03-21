package com.commercetools.sync.categories;


import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CategoryUpdateActionsHelper {

    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeNameUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getName(),
                newCategory.getName(),
                ChangeName.of(newCategory.getName()));
    }

    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeSlugUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getSlug(),
                newCategory.getSlug(),
                ChangeSlug.of(newCategory.getSlug()));
    }

    @Nonnull
    static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE PARENT REMOVAL
        // TODO: TEMP WORKAROUND:
        if (newCategory.getDescription() == null) {
            return Optional.empty();
        }
        return buildUpdateActionForLocalizedStrings(existingCategory.getDescription(),
                newCategory.getDescription(),
                SetDescription.of(newCategory.getDescription()));
    }

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

    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaTitle(),
                newCategory.getMetaTitle(),
                SetMetaTitle.of(newCategory.getMetaTitle()));
    }

    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaKeywords(),
                newCategory.getMetaKeywords(),
                SetMetaKeywords.of(newCategory.getMetaKeywords()));
    }

    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaDescription(),
                newCategory.getMetaDescription(),
                SetMetaDescription.of(newCategory.getMetaDescription()));
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(
            @Nullable final LocalizedString localizedString1,
            @Nullable final LocalizedString localizedString2,
            UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(localizedString1, localizedString2, updateAction);
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForReferences(
            @Nullable final Reference<Category> categoryReference1,
            @Nullable final Reference<Category> categoryReference2,
            UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(categoryReference1, categoryReference2, updateAction);
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForStrings(@Nullable final String string1,
                                                                                @Nullable final String string2,
                                                                                UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(string1, string2, updateAction);
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForObjects(@Nullable final Object Object1,
                                                                                @Nullable final Object Object2,
                                                                                UpdateAction<Category> updateAction) {
        if (!Objects.equals(Object1, Object2)) {
            return Optional.of(updateAction);
        }
        return Optional.empty();
    }

    /**
     * TODO: REFACTOR AND RENAME!!!
     *
     * @param newCategory
     * @param existingCategory
     * @return
     */
    @Nonnull
    static List<UpdateAction<Category>> buildSetCustomTypeUpdateActions(@Nonnull final Category existingCategory,
                                                                        @Nonnull final CategoryDraft newCategory) {
        CustomFieldsDraft newCategoryCustom = newCategory.getCustom();
        CustomFields existingCategoryCustom = existingCategory.getCustom();
        if (existingCategoryCustom != null && newCategoryCustom != null) {
            ResourceIdentifier<Type> newCategoryCustomType = newCategoryCustom.getType();
            Map<String, JsonNode> newCategoryCustomFields = newCategoryCustom.getFields();
            ResourceIdentifier<Type> existingCategoryCustomType = existingCategoryCustom.getType().toResourceIdentifier();
            Map<String, JsonNode> existingCategoryCustomFields = existingCategoryCustom.getFieldsJsonMap();
            // compare custom types
            if (existingCategoryCustomType.getKey().equals(newCategoryCustomType.getKey())) {
                // CASE#1 Both categories have same key of category custom type.
                // Start value comparison..
                return buildSetCustomFieldsUpdateActions(existingCategoryCustomFields, newCategoryCustomFields);
            } else {
                // CASE#2 Categories have different keys of custom type. Then set the existing category to the new custom type
                return Collections.singletonList(
                        SetCustomType.ofTypeKeyAndJson(
                                newCategoryCustom.getType().getKey(), newCategoryCustomFields)
                );
            }
        } else {
            if (newCategoryCustom != null) {
                // Existing Category has no custom type but new one has custom type set.
                // CASE#2
                Map<String, JsonNode> newCategoryCustomFields = newCategoryCustom.getFields();
                return Collections.singletonList(
                        SetCustomType.ofTypeKeyAndJson(
                                newCategoryCustom.getType().getKey(), newCategoryCustomFields)
                );
            } else {
                if (existingCategoryCustom != null) {
                    // CASE#3
                    // New Category has no custom type set and existing one has custom type set.
                    return Collections.singletonList(SetCustomType.ofRemoveType());
                }
            }
        }
        // CASE#4 DO NOTHING
        return Collections.emptyList();
    }

    /**
     * TODO: REFACTOR
     *
     * @param existingCustomFields
     * @param newCustomFields
     * @return
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildSetCustomFieldsUpdateActions(
            @Nonnull final Map<String, JsonNode> existingCustomFields,
            @Nonnull final Map<String, JsonNode> newCustomFields) {
        List<UpdateAction<Category>> updateActions = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> newCustomFieldsIterator = newCustomFields.entrySet().iterator();
        while (newCustomFieldsIterator.hasNext()) {
            Map.Entry newCustomFieldsEntry = newCustomFieldsIterator.next();
            String newCustomFieldEntryKey = (String) newCustomFieldsEntry.getKey();
            JsonNode newCustomFieldValue = newCustomFields.get(newCustomFieldEntryKey);
            JsonNode existingCustomFieldValue = existingCustomFields.get(newCustomFieldEntryKey);
            if (!newCustomFieldValue.equals(existingCustomFieldValue)) {
                updateActions.add(SetCustomField.ofJson(newCustomFieldEntryKey, newCustomFieldValue));
            }
        }
        return updateActions;
    }
}
