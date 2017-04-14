package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.*;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;

public class CategorySyncUtils {

    /**
     * TODO: JAVADOC.
     * TODO: UNIT TEST.
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     *
     * @param oldCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final Category newCategory,
                                                            @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(oldCategory, newCategory, syncOptions);
        final List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(oldCategory, newCategory, syncOptions);
        return Stream.concat(updateActions.stream(),
                assetUpdateActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildCoreActions(Category, Category, CategorySyncOptions)}.
     *
     * @param oldCategory
     * @param newCategory
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                @Nonnull final Category newCategory,
                                                                @Nonnull final CategorySyncOptions syncOptions) {
        return new ArrayList<>();
    }

    /**
     * TODO: SEE GITHUB ISSUE#3 & #9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildAssetActions(Category, Category, CategorySyncOptions)}.
     *
     * @param oldCategory
     * @param newCategory
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category oldCategory,
                                                                 @Nonnull final Category newCategory,
                                                                 @Nonnull final CategorySyncOptions syncOptions) {
        return new ArrayList<>();
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     *
     * @param oldCategory
     * @param newCategory
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final CategoryDraft newCategory,
                                                            @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(oldCategory, newCategory, syncOptions);
        final List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(oldCategory, newCategory, syncOptions);
        return Stream.concat(updateActions.stream(),
                assetUpdateActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     *
     * @param oldCategory
     * @param newCategory
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                @Nonnull final CategoryDraft newCategory,
                                                                @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
        buildChangeNameUpdateAction(oldCategory, newCategory)
                .map(updateActions::add);

        buildChangeSlugUpdateAction(oldCategory, newCategory)
                .map(updateActions::add);

        buildSetDescriptionUpdateAction(oldCategory, newCategory, syncOptions)
                .map(updateActions::add);

        buildChangeParentUpdateAction(oldCategory, newCategory, syncOptions)
                .map(updateActions::add);

        buildChangeOrderHintUpdateAction(oldCategory, newCategory, syncOptions)
                .map(updateActions::add);

        buildSetMetaTitleUpdateAction(oldCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaDescriptionUpdateAction(oldCategory, newCategory)
                .map(updateActions::add);

        buildSetMetaKeywordsUpdateAction(oldCategory, newCategory)
                .map(updateActions::add);

        final List<UpdateAction<Category>> categoryTypeUpdateActions =
                buildCustomUpdateActions(oldCategory, newCategory, syncOptions);
        return Stream.concat(updateActions.stream(),
                categoryTypeUpdateActions.stream())
                .collect(Collectors.toList());
    }


    /**
     * TODO: SEE GITHUB ISSUE#3
     *
     * @param oldCategory
     * @param newCategory
     * @param syncOptions
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category oldCategory,
                                                                 @Nonnull final CategoryDraft newCategory,
                                                                 @Nonnull final CategorySyncOptions syncOptions) {
        return new ArrayList<>();
    }
}
