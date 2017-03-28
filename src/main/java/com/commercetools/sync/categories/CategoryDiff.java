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

    /**
     * Compares the {@link LocalizedString} names of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same name, an empty {@link Optional}
     * is returned.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeNameUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getName(),
                newCategory.getName(),
                ChangeName.of(newCategory.getName()));
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same slugs, an empty {@link Optional}
     * is returned.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new slug.
     * @return A filled optional with the update action or an empty optional if the slugs are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeSlugUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        return buildUpdateActionForLocalizedStrings(existingCategory.getSlug(),
                newCategory.getSlug(),
                ChangeSlug.of(newCategory.getSlug()));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same description values, an empty
     * {@link Optional} is returned.
     * <p>
     * Note: If the description of the new {@link CategoryDraft} is null, an empty {@link Optional} is returned with
     * no update actions.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE DESCRIPTION REMOVAL.
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final LocalizedString newCategoryDescription = newCategory.getDescription();
        if (newCategoryDescription == null) {
            return Optional.empty();
        }
        return buildUpdateActionForLocalizedStrings(existingCategory.getDescription(),
                newCategoryDescription,
                SetDescription.of(newCategoryDescription));
    }

    /**
     * Compares the parents {@link Reference<Category>} of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link Optional} is returned.
     * <p>
     * Note: If the parent {@link Reference<Category>} of the new {@link CategoryDraft} is null, an empty
     * {@link Optional} is returned with no update actions.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new parent.
     * @return A filled optional with the update action or an empty optional if the parent references are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE PARENT REMOVAL.
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final Reference<Category> parentCategoryReference = newCategory.getParent();
        if (parentCategoryReference == null) {
            return Optional.empty();
        }
        return buildUpdateActionForReferences(existingCategory.getParent(),
                parentCategoryReference,
                ChangeParent.of(parentCategoryReference));
    }

    /**
     * Compares the orderHint values of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same orderHint values, an empty
     * {@link Optional} is returned.
     * <p>
     * Note: If the orderHint of the new {@link CategoryDraft} is null, an empty {@link Optional} is returned with
     * no update actions.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new orderHint.
     * @return A filled optional with the update action or an empty optional if the orderHint values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE ORDERHINT REMOVAL
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final String orderHint = newCategory.getOrderHint();
        if (orderHint == null) {
            return Optional.empty();
        }
        return buildUpdateActionForStrings(existingCategory.getOrderHint(),
                orderHint,
                ChangeOrderHint.of(orderHint));
    }

    /**
     * Compares the {@link LocalizedString} meta title of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same meta title values, an empty
     * {@link Optional} is returned.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new meta title.
     * @return A filled optional with the update action or an empty optional if the meta titles values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaTitle = newCategory.getMetaTitle();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaTitle(),
                newCategoryMetaTitle,
                SetMetaTitle.of(newCategoryMetaTitle));
    }

    /**
     * Compares the {@link LocalizedString} meta keywords of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same meta keywords values, an empty
     * {@link Optional} is returned.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new meta keywords.
     * @return A filled optional with the update action or an empty optional if the meta keywords values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaKeywords = newCategory.getMetaKeywords();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaKeywords(),
                newCategoryMetaKeywords,
                SetMetaKeywords.of(newCategoryMetaKeywords));
    }

    /**
     * Compares the {@link LocalizedString} meta description of a {@link Category} and a {@link CategoryDraft} and
     * returns an {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed,
     * for example in case where both the {@link Category} and the {@link CategoryDraft} have the same meta description
     * values, an empty {@link Optional} is returned.
     *
     * @param existingCategory the category which should be updated.
     * @param newCategory      the category draft where we get the new meta description.
     * @return A filled optional with the update action or an empty optional if the meta description values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category existingCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaDescription = newCategory.getMetaDescription();
        return buildUpdateActionForLocalizedStrings(existingCategory.getMetaDescription(),
                newCategoryMetaDescription,
                SetMetaDescription.of(newCategoryMetaDescription));
    }

    /**
     * Compares two {@link LocalizedString} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link LocalizedString}
     * have the same fields and values, an empty {@link Optional} is returned.
     *
     * @param existingLocalisedString the localised string which should be updated.
     * @param newLocalisedString      the localised string with the new information.
     * @param updateAction            the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the localised string values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForLocalizedStrings(
            @Nullable final LocalizedString existingLocalisedString,
            @Nullable final LocalizedString newLocalisedString,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingLocalisedString, newLocalisedString, updateAction);
    }

    /**
     * Compares two {@link Reference<Category>} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link Reference<Category>}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingCategoryReference the category reference which should be updated.
     * @param newCategoryReference      the category reference with the new information.
     * @param updateAction              the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the category reference
     * values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForReferences(
            @Nullable final Reference<Category> existingCategoryReference,
            @Nullable final Reference<Category> newCategoryReference,
            @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingCategoryReference, newCategoryReference, updateAction);
    }

    /**
     * Compares two {@link String} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link String}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingString the string which should be updated.
     * @param newString      the string with the new information.
     * @param updateAction   the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the string values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForStrings(@Nullable final String existingString,
                                                                        @Nullable final String newString,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        return buildUpdateActionForObjects(existingString, newString, updateAction);
    }

    /**
     * Compares two {@link Object} and returns a supplied {@link UpdateAction<Category>} as a result in an
     * {@link Optional}. If no update action is needed, for example in case where both the {@link Object}
     * have the same values, an empty {@link Optional} is returned.
     *
     * @param existingObject the object which should be updated.
     * @param newObject      the object with the new information.
     * @param updateAction   the update action to return in the optional.
     * @return A filled optional with the update action or an empty optional if the object values are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Category>> buildUpdateActionForObjects(@Nullable final Object existingObject,
                                                                        @Nullable final Object newObject,
                                                                        @Nonnull final UpdateAction<Category> updateAction) {
        return !Objects.equals(existingObject, newObject) ? Optional.of(updateAction) : Optional.empty();
    }
}
