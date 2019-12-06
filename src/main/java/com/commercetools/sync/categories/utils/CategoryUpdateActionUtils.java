package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.CategoryAssetActionFactory;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.utils.AssetsUpdateActionUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeOrderHint;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.commands.updateactions.SetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetExternalId;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.categories.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

public final class CategoryUpdateActionUtils {
    private static final String CATEGORY_CHANGE_PARENT_EMPTY_PARENT = "Cannot unset 'parent' field of category with id"
        + " '%s'.";
    private static final String CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT = "Cannot unset 'orderHint' field of "
        + "category with id '%s'.";

    /**
     * Compares the {@link LocalizedString} names of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Category} and
     * the {@link CategoryDraft} have the same name, then no update action is needed and hence an empty {@link Optional}
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
        return buildUpdateAction(oldCategory.getName(),
            newCategory.getName(), () -> ChangeName.of(newCategory.getName()));
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Category} and
     * the {@link CategoryDraft} have the same slug, then no update action is needed and hence an empty {@link Optional}
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
        return buildUpdateAction(oldCategory.getSlug(),
            newCategory.getSlug(), () -> ChangeSlug.of(newCategory.getSlug()));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of a {@link Category} and a {@link CategoryDraft} and
     * returns an {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the
     * {@link Category} and the {@link CategoryDraft} have the same description, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetDescriptionUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory) {
        return buildUpdateAction(oldCategory.getDescription(),
            newCategory.getDescription(), () -> SetDescription.of(newCategory.getDescription()));
    }

    /**
     * Compares the parents {@link Reference}&lt;{@link Category}&gt; of a {@link Category} and a {@link CategoryDraft}
     * and returns an {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the
     * {@link Category} and the {@link CategoryDraft} have the same parents, then no update action is needed and hence
     * an empty {@link Optional} is returned.
     *
     * <p>Note: If the parent {@link Reference}&lt;{@link Category}&gt; of the new {@link CategoryDraft} is null, an
     * empty {@link Optional} is returned with no update actions and a custom callback function, if set on the
     * supplied {@link CategorySyncOptions}, is called.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new parent.
     * @param syncOptions the sync syncOptions with which a custom callback function is called in case the parent
     *                    is null.
     * @return A filled optional with the update action or an empty optional if the parent references are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeParentUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory,
        @Nonnull final CategorySyncOptions syncOptions) {

        final Reference<Category> oldParent = oldCategory.getParent();
        final ResourceIdentifier<Category> newParent = newCategory.getParent();
        if (newParent == null && oldParent != null) {
            syncOptions.applyWarningCallback(format(CATEGORY_CHANGE_PARENT_EMPTY_PARENT, oldCategory.getId()));
            return Optional.empty();
        } else {
            // The newParent.getId() call below can not cause an NPE in this case, since if both newParent and oldParent
            // are null, then the supplier will not be called at all. The remaining cases all involve the newParent
            // being not null.
            return buildUpdateActionForReferences(oldParent, newParent,
                () -> ChangeParent.of(ResourceIdentifier.ofId(newParent.getId())));
        }
    }

    /**
     * Compares the orderHint values of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Category} and
     * the {@link CategoryDraft} have the same orderHint, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * <p>Note: If the orderHint of the new {@link CategoryDraft} is null, an empty {@link Optional} is returned with
     * no update actions and a custom callback function, if set on the supplied {@link CategorySyncOptions}, is called.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new orderHint.
     * @param syncOptions the sync syncOptions with which a custom callback function is called in case the orderHint
     *                    is null.
     * @return A filled optional with the update action or an empty optional if the orderHint values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeOrderHintUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory,
        @Nonnull final CategorySyncOptions syncOptions) {
        if (newCategory.getOrderHint() == null && oldCategory.getOrderHint() != null) {
            syncOptions.applyWarningCallback(
                format(CATEGORY_CHANGE_ORDER_HINT_EMPTY_ORDERHINT, oldCategory.getId()));
            return Optional.empty();
        }
        return buildUpdateAction(oldCategory.getOrderHint(),
            newCategory.getOrderHint(), () -> ChangeOrderHint.of(newCategory.getOrderHint()));
    }

    /**
     * Compares the {@link LocalizedString} meta title of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Category} and
     * the {@link CategoryDraft} have the same meta title, then no update action is needed and hence an empty
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
        return buildUpdateAction(oldCategory.getMetaTitle(),
            newCategory.getMetaTitle(), () -> SetMetaTitle.of(newCategory.getMetaTitle()));
    }

    /**
     * Compares the {@link LocalizedString} meta keywords of a {@link Category} and a {@link CategoryDraft} and
     * returns an {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the
     * {@link Category} and the {@link CategoryDraft} have the same meta keywords, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta keywords.
     * @return A filled optional with the update action or an empty optional if the meta keywords values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetMetaKeywordsUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory) {
        return buildUpdateAction(oldCategory.getMetaKeywords(),
            newCategory.getMetaKeywords(), () -> SetMetaKeywords.of(newCategory.getMetaKeywords()));
    }

    /**
     * Compares the {@link LocalizedString} meta description of a {@link Category} and a {@link CategoryDraft} and
     * returns an {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the
     * {@link Category} and the {@link CategoryDraft} have the same meta description, then no update action is needed
     * and hence an empty {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new meta description.
     * @return A filled optional with the update action or an empty optional if the meta description values are
     *      identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetMetaDescriptionUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory) {
        return buildUpdateAction(oldCategory.getMetaDescription(),
            newCategory.getMetaDescription(), () -> SetMetaDescription.of(newCategory.getMetaDescription()));
    }

    /**
     * Compares the externalId values of a {@link Category} and a {@link CategoryDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Category} and
     * the {@link CategoryDraft} have the same externalId, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new externalId.
     * @return A filled optional with the update action or an empty optional if the externalId values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetExternalIdUpdateAction(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory) {
        return buildUpdateAction(oldCategory.getExternalId(),
            newCategory.getExternalId(), () -> SetExternalId.of(newCategory.getExternalId()));
    }

    /**
     * Compares the assets of a {@link Category} and a {@link CategoryDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If both the {@link Category} and
     * the {@link CategoryDraft} have the identical assets, then no update action is needed and hence an empty
     * {@link List} is returned. In case, the new category draft has a list of assets in which a duplicate key exists,
     * the error callback is triggered and an empty list is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new externalId.
     * @param syncOptions the sync options with which a custom callback function is called in case errors exists
     *                    while building assets custom field/type actions.
     * @return A list with the update actions or an empty list if the assets are identical.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetsUpdateActions(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory,
        @Nonnull final CategorySyncOptions syncOptions) {

        try {
            return AssetsUpdateActionUtils.buildAssetsUpdateActions(
                oldCategory.getAssets(),
                newCategory.getAssets(),
                new CategoryAssetActionFactory(syncOptions));
        } catch (final BuildUpdateActionException exception) {
            syncOptions.applyErrorCallback(format("Failed to build update actions for the assets "
                + "of the category with the key '%s'. Reason: %s", oldCategory.getKey(), exception), exception);
            return emptyList();
        }
    }

    private CategoryUpdateActionUtils() {
    }
}
