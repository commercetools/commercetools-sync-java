package com.commercetools.sync.categories.utils;


import com.commercetools.sync.commons.constants.SyncMessages;
import com.commercetools.sync.commons.helpers.SyncResult;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

public class CategoryUpdateActionUtils {

    /**
     * Compares the {@link LocalizedString} names of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeName"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same name, the sync result would have an empty list of update actions.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new name.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the names are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildChangeNameUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryName = newCategory.getName();
        return buildUpdateAction(oldCategory.getName(),
                newCategoryName,
                ChangeName.of(newCategoryName));
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeSlug"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same slugs, the sync result would have an empty list of update actions.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new slug.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the slugs are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildChangeSlugUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategorySlug = newCategory.getSlug();
        return buildUpdateAction(oldCategory.getSlug(),
                newCategorySlug,
                ChangeSlug.of(newCategorySlug));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "setDescription"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same description values, the sync result would have an empty list of
     * update actions.
     * <p>
     * Note: If the description of the new {@link CategoryDraft} is null, a message is added in the statistics
     * {@link java.util.Map} of the {@link SyncResult<Category>}.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new description.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the description values are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildSetDescriptionUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryDescription = newCategory.getDescription();
        if (newCategoryDescription != null) {
            return buildUpdateAction(oldCategory.getDescription(), newCategoryDescription,
                    SetDescription.of(newCategoryDescription));
        } else {
            return SyncResult.ofStatistic(SyncMessages.CATEGORY_SET_DESCRIPTION_EMPTY_DESCRIPTION);
        }
    }

    /**
     * Compares the parents {@link Reference<Category>} of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeParent"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same parents, the sync result would have an empty list of
     * update actions.
     * <p>
     * Note: If the parent {@link Reference<Category>} of the new {@link CategoryDraft} is null,
     * a message is added in the statistics {@link java.util.Map} of the {@link SyncResult<Category>}.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new parent.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the parents are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildChangeParentUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final Reference<Category> newCategoryParentReference = newCategory.getParent();
        if (newCategoryParentReference != null) {
            return buildUpdateAction(oldCategory.getParent(),
                    newCategoryParentReference,
                    ChangeParent.of(newCategoryParentReference));
        } else {
            return SyncResult.ofStatistic(SyncMessages.CATEGORY_CHANGE_PARENT_EMPTY_PARENT);
        }
    }

    /**
     * Compares tthe orderHint values of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeOrderHint"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same orderHint values, the sync result would have an empty list of
     * update actions.
     * <p>
     * Note: If the orderHint of the new {@link CategoryDraft} is null, a message is added in the
     * statistics {@link java.util.Map} of the {@link SyncResult<Category>}.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new orderHint.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the orderHint values are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildChangeOrderHintUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final String newCategoryOrderHint = newCategory.getOrderHint();
        if (newCategoryOrderHint != null) {
            return buildUpdateAction(oldCategory.getOrderHint(),
                    newCategoryOrderHint,
                    ChangeOrderHint.of(newCategoryOrderHint));
        } else {
            return SyncResult.ofStatistic(SyncMessages.CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT);
        }
    }

    /**
     * Compares the {@link LocalizedString} meta title of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeSlug"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same meta title values, the sync result would have an
     * empty list of update actions.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta title.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the meta title values are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildSetMetaTitleUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaTitle = newCategory.getMetaTitle();
        return buildUpdateAction(oldCategory.getMetaTitle(),
                newCategoryMetaTitle,
                SetMetaTitle.of(newCategoryMetaTitle));
    }

    /**
     * Compares the {@link LocalizedString} meta keywords of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeSlug"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same meta keywords values, the sync result would have an
     * empty list of update actions.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta keywords.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the meta keywords values are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaKeywords = newCategory.getMetaKeywords();
        return buildUpdateAction(oldCategory.getMetaKeywords(),
                newCategoryMetaKeywords,
                SetMetaKeywords.of(newCategoryMetaKeywords));
    }

    /**
     * Compares the {@link LocalizedString} meta description of a {@link Category} and a {@link CategoryDraft} and returns a
     * {@link SyncResult<Category>} containing a list of update actions, which would contain the "changeSlug"
     * {@link UpdateAction<Category>}. If no update action is needed, for example in case where both the {@link Category}
     * and the {@link CategoryDraft} have the same meta description values, the sync result would have an
     * empty list of update actions.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta description.
     * @return A sync result with a list containing the update action or a sync result containing an empty list
     * of update actions if the meta description values are identical.
     */
    @Nonnull
    public static SyncResult<Category> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategory) {
        final LocalizedString newCategoryMetaDescription = newCategory.getMetaDescription();
        return buildUpdateAction(oldCategory.getMetaDescription(),
                newCategoryMetaDescription,
                SetMetaDescription.of(newCategoryMetaDescription));
    }
}
