package com.commercetools.sync.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CategorySyncUtils {

    @Nonnull
    public static List<UpdateAction<Category>> buildActionsUsingGenerator(@Nonnull final Category existingCategory,
                                                                          @Nonnull final CategoryDraft newCategory) {
        CategoryUpdateActionsGenerator updateActionsGenerator = new CategoryUpdateActionsGenerator(existingCategory, newCategory);
        return updateActionsGenerator.generateCoreActions();
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final CategoryDraft newCategory) {
        List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory);
        List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        updateActions.addAll(assetUpdateActions);
        return updateActions;
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final Category newCategory) {
        List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory);
        List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        updateActions.addAll(assetUpdateActions);
        return updateActions;
    }

    /**
     * Update actions concerned with Categories:
     * <p>
     * - changeName
     * - changeSlug
     * - setDescription
     * - changeParent
     * - changeOrderHint
     * - setExternalId
     * - setMetaTitle
     * - setMetaDescription
     * - setMetaKeywords
     * - setCustomType
     * - setCustomField
     * <p>
     * <p>
     * - delete?? Should I delete a category if it doesn't exist in new feed!? TODO
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final CategoryDraft newCategory) {
        List<UpdateAction<Category>> updateActions = new ArrayList<>();
        buildChangeNameUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeSlugUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetDescriptionUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeParentUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildChangeOrderHintUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaTitleUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaDescriptionUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaKeywordsUpdateAction(existingCategory, newCategory)
                .map(updateActions::add);

        // Compare custom fields
        updateActions.addAll(buildSetCustomTypeUpdateActions(existingCategory, newCategory));
        return updateActions;
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildChangeNameUpdateAction(@Nonnull final Category existingCategory,
                                                                                @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getName(),
                newCategory.getName(),
                ChangeName.of(existingCategory.getName()));
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(@Nullable final LocalizedString localizedString1,
                                                                                         @Nullable final LocalizedString localizedString2,
                                                                                         UpdateAction<Category> updateAction) {
        if (!Objects.equals(localizedString1, localizedString2)) {
            return Optional.of(updateAction);
        }
        return Optional.empty();

    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildUpdateActionForReferences(@Nullable final Reference<Category> categoryReference1,
                                                                                         @Nullable final Reference<Category> categoryReference2,
                                                                                         UpdateAction<Category> updateAction) {
        if (!Objects.equals(categoryReference1, categoryReference2)) {
            return Optional.of(updateAction);
        }
        return Optional.empty();

    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildChangeSlugUpdateAction(@Nonnull final Category existingCategory,
                                                                                @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getSlug(),
                newCategory.getSlug(),
                ChangeSlug.of(existingCategory.getSlug()));
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(@Nonnull final Category existingCategory,
                                                                                    @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getDescription(),
                newCategory.getDescription(),
                SetDescription.of(newCategory.getDescription()));
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(@Nonnull final Category existingCategory,
                                                                                  @Nonnull final CategoryDraft newCategory) {
        Reference<Category> newCategoryParentReference = newCategory.getParent();
        if (newCategoryParentReference == null) {
            // TODO: NEED TO HANDLE PARENT REMOVAL
            newCategoryParentReference = Reference.of()
        } else {

        }
        return buildUpdateActionForReferences(existingCategory.getParent(),
                newCategory.getParent(),
                ChangeParent.of());
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(@Nonnull final Category existingCategory,
                                                                                     @Nonnull final CategoryDraft newCategory) {
        String existingCategoryOrderHint = existingCategory.getOrderHint();
        String newCategoryOrderHint = newCategory.getOrderHint();
        if (existingCategoryOrderHint != null && !existingCategoryOrderHint.equals(newCategoryOrderHint)) {
            return Optional.of(ChangeOrderHint.of(newCategoryOrderHint));
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(@Nonnull final Category existingCategory,
                                                                                  @Nonnull final CategoryDraft newCategory) {
        LocalizedString existingCategoryMetaTitle = existingCategory.getMetaTitle();
        LocalizedString newCategoryMetaTitle = newCategory.getMetaTitle();
        if (existingCategoryMetaTitle != null && !existingCategoryMetaTitle.equals(newCategoryMetaTitle)) {
            return Optional.of(SetMetaTitle.of(newCategoryMetaTitle));
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(@Nonnull final Category existingCategory,
                                                                                     @Nonnull final CategoryDraft newCategory) {
        LocalizedString existingCategoryMetaKeywords = existingCategory.getMetaKeywords();
        LocalizedString newCategoryMetaKeywords = newCategory.getMetaKeywords();
        if (existingCategoryMetaKeywords != null && !existingCategoryMetaKeywords.equals(newCategoryMetaKeywords)) {
            return Optional.of(SetMetaTitle.of(newCategoryMetaKeywords));
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(@Nonnull final Category existingCategory,
                                                                                        @Nonnull final CategoryDraft newCategory) {
        LocalizedString existingCategoryMetaDescription = existingCategory.getMetaDescription();
        LocalizedString newCategoryMetaDescription = newCategory.getMetaDescription();
        if (existingCategoryMetaDescription != null && !existingCategoryMetaDescription.equals(newCategoryMetaDescription)) {
            return Optional.of(SetMetaTitle.of(newCategoryMetaDescription));
        }
        return Optional.empty();
    }

    /**
     * TODO: REFACTOR!!!
     *
     * @param newCategory
     * @param existingCategory
     * @return
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildSetCustomTypeUpdateActions(@Nonnull final Category existingCategory,
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
                // CASE#1
                // Start value comparison..
                return buildSetCustomFieldsUpdateActions(existingCategoryCustomFields, newCategoryCustomFields);
            } else {
                // CASE#2
                return Collections.singletonList(
                        SetCustomType.ofTypeKeyAndJson(
                                newCategoryCustom.getType().getKey(), newCategoryCustomFields)
                );
            }
        } else {
            if (newCategoryCustom != null) {
                // Existing Category has no custom type
                // CASE#2
                Map<String, JsonNode> newCategoryCustomFields = newCategoryCustom.getFields();
                return Collections.singletonList(
                        SetCustomType.ofTypeKeyAndJson(
                                newCategoryCustom.getType().getKey(), newCategoryCustomFields)
                );
            } else {
                // CASE#3
                // New Category has no custom type set
                return Collections.singletonList(SetCustomType.ofRemoveType());
            }

        }
    }

    /**
     * TODO: REFACTOR
     *
     * @param existingCustomFields
     * @param newCustomFields
     * @return
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildSetCustomFieldsUpdateActions(@Nonnull final Map<String, JsonNode> existingCustomFields,
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

    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }

    /**
     * - addAsset
     * - removeAsset
     * - changeAssetOrder
     * - changeAssetName
     * - setAssetDescription
     * - setAssetTags
     * - setAssetSources
     * - setAssetCustomType
     * - setAssetCustomField
     *
     * @param existingCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category existingCategory,
                                                                 @Nonnull final CategoryDraft newCategory) {
        return new ArrayList<>();
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category existingCategory,
                                                                 @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }
}
