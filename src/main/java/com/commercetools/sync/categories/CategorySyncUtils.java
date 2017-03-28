package com.commercetools.sync.categories;

import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.categories.CategoryDiff.*;
import static com.commercetools.sync.categories.CategoryTypeDiff.buildTypeActions;

public class CategorySyncUtils {

    /**
     * TODO: JAVADOC.
     * TODO: UNIT TEST.
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     *
     * @param existingCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final Category newCategory,
                                                            @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory, typeService);
        final List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        return Stream.concat(updateActions.stream(),
                assetUpdateActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildCoreActions(Category, CategoryDraft, TypeService)}.
     *
     * @param existingCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final Category newCategory,
                                                                @Nonnull final TypeService typeService) {
        return new ArrayList<>();
    }

    /**
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildAssetActions(Category, CategoryDraft)}.
     *
     * @param existingCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category existingCategory,
                                                                 @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     * @param existingCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category existingCategory,
                                                            @Nonnull final CategoryDraft newCategory,
                                                            @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(existingCategory, newCategory, typeService);
        final List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(existingCategory, newCategory);
        return Stream.concat(updateActions.stream(),
                assetUpdateActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     * @param existingCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category existingCategory,
                                                                @Nonnull final CategoryDraft newCategory,
                                                                @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions = new ArrayList<>();
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

        final List<UpdateAction<Category>> categoryTypeUpdateActions =
                buildTypeActions(existingCategory, newCategory, typeService);
        return Stream.concat(updateActions.stream(),
                categoryTypeUpdateActions.stream())
                .collect(Collectors.toList());
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
}
