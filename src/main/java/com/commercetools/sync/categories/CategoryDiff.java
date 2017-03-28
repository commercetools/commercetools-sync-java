package com.commercetools.sync.categories;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CategoryDiff {

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
        final LocalizedString newCategoryDescription = newCategory.getDescription();
        if (newCategoryDescription == null) {
            return Optional.empty();
        }
        return buildUpdateActionForLocalizedStrings(existingCategory.getDescription(),
                newCategoryDescription,
                SetDescription.of(newCategoryDescription));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE PARENT REMOVAL
        // TODO: TEMP WORKAROUND:
        final Reference<Category> parentCategoryReference = newCategory.getParent();
        if (parentCategoryReference == null) {
            return Optional.empty();
        }
        return buildUpdateActionForReferences(existingCategory.getParent(),
                parentCategoryReference,
                ChangeParent.of(parentCategoryReference));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE ORDERHINT REMOVAL
        final String orderHint = newCategory.getOrderHint();
        if (orderHint == null) {
            return Optional.empty();
        }
        return buildUpdateActionForStrings(existingCategory.getOrderHint(),
                orderHint,
                ChangeOrderHint.of(orderHint));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaTitle = newCategory.getMetaTitle();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaTitle(),
                newCategoryMetaTitle,
                SetMetaTitle.of(newCategoryMetaTitle));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaKeywords = newCategory.getMetaKeywords();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaKeywords(),
                newCategoryMetaKeywords,
                SetMetaKeywords.of(newCategoryMetaKeywords));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaDescription = newCategory.getMetaDescription();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaDescription(),
                newCategoryMetaDescription,
                SetMetaDescription.of(newCategoryMetaDescription));
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(
            @Nullable final LocalizedString existingLocalisedString,
            @Nullable final LocalizedString newLocalisedString,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingLocalisedString, newLocalisedString, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForReferences(
            @Nullable final Reference<Category> existingCategoryReference,
            @Nullable final Reference<Category> newCategoryReference,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingCategoryReference, newCategoryReference, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForStrings(@Nullable final String existingString,
                                                                        @Nullable final String newString,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingString, newString, updateAction);
    }

    //TODO: JAVADOC
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForObjects(@Nullable final Object existingObject,
                                                                        @Nullable final Object newObject,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        if (!Objects.equals(existingObject, newObject)) {
            return Optional.of(updateAction);
        }
        return Optional.empty();
    }
}
