package com.commercetools.sync.categories.utils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public class CategoryUpdateActionUtils {

    /**
     * Compares the {@link LocalizedString} names of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same name, an empty {@link Optional}
     * is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeNameUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryName = newCategory.getName();
        return buildUpdateAction(oldCategory.getName(),
                newCategoryName,
                ChangeName.of(newCategoryName));
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same slugs, an empty {@link Optional}
     * is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new slug.
     * @return A filled optional with the update action or an empty optional if the slugs are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeSlugUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategorySlug = newCategory.getSlug();
        return buildUpdateAction(oldCategory.getSlug(),
                newCategorySlug,
                ChangeSlug.of(newCategorySlug));
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
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE DESCRIPTION REMOVAL.
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final LocalizedString newCategoryDescription = newCategory.getDescription();
        return newCategoryDescription != null ? buildUpdateAction(oldCategory.getDescription(),
                newCategoryDescription,
                SetDescription.of(newCategoryDescription)) : Optional.empty();
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
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new parent.
     * @return A filled optional with the update action or an empty optional if the parent references are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE PARENT REMOVAL.
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final Reference<Category> newCategoryParentReference = newCategory.getParent();
        return newCategoryParentReference != null ? buildUpdateAction(oldCategory.getParent(),
                newCategoryParentReference,
                ChangeParent.of(newCategoryParentReference)) : Optional.empty();
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
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new orderHint.
     * @return A filled optional with the update action or an empty optional if the orderHint values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        // TODO: NEED TO HANDLE ORDERHINT REMOVAL
        // TODO: TEMP WORKAROUND UNTIL GITHUB ISSUE#8 IS RESOLVED.
        final String newCategoryOrderHint = newCategory.getOrderHint();
        return newCategoryOrderHint != null ? buildUpdateAction(oldCategory.getOrderHint(),
                newCategoryOrderHint,
                ChangeOrderHint.of(newCategoryOrderHint)) : Optional.empty();
    }

    /**
     * Compares the {@link LocalizedString} meta title of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same meta title values, an empty
     * {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta title.
     * @return A filled optional with the update action or an empty optional if the meta titles values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetMetaTitleUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaTitle = newCategory.getMetaTitle();
        return buildUpdateAction(oldCategory.getMetaTitle(),
                newCategoryMetaTitle,
                SetMetaTitle.of(newCategoryMetaTitle));
    }

    /**
     * Compares the {@link LocalizedString} meta keywords of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same meta keywords values, an empty
     * {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta keywords.
     * @return A filled optional with the update action or an empty optional if the meta keywords values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaKeywords = newCategory.getMetaKeywords();
        return buildUpdateAction(oldCategory.getMetaKeywords(),
                newCategoryMetaKeywords,
                SetMetaKeywords.of(newCategoryMetaKeywords));
    }

    /**
     * Compares the {@link LocalizedString} meta description of a {@link Category} and a {@link CategoryDraft} and
     * returns an {@link UpdateAction<Category>} as a result in an {@link Optional}. If no update action is needed,
     * for example in case where both the {@link Category} and the {@link CategoryDraft} have the same meta description
     * values, an empty {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta description.
     * @return A filled optional with the update action or an empty optional if the meta description values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaDescription = newCategory.getMetaDescription();
        return buildUpdateAction(oldCategory.getMetaDescription(),
                newCategoryMetaDescription,
                SetMetaDescription.of(newCategoryMetaDescription));
    }
}
