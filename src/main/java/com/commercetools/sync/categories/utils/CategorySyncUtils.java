package com.commercetools.sync.categories.utils;

import com.commercetools.sync.commons.helpers.SyncResult;
import com.commercetools.sync.services.TypeService;
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
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final Category newCategory,
                                                            @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(oldCategory, newCategory, typeService);
        final List<UpdateAction<Category>> assetUpdateActions = buildAssetActions(oldCategory, newCategory);
        return Stream.concat(updateActions.stream(),
                assetUpdateActions.stream())
                .collect(Collectors.toList());
    }

    /**
     * TODO: NEEDS TO BE IMPLEMENTED. GITHUB ISSUE#9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildCoreActions(Category, CategoryDraft, TypeService)}.
     *
     * @param oldCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                @Nonnull final Category newCategory,
                                                                @Nonnull final TypeService typeService) {
        return new ArrayList<>();
    }

    /**
     * TODO: SEE GITHUB ISSUE#3 & #9
     * SHOULD TRANSFORM Category TO CategoryDraft
     * then call {@link CategorySyncUtils#buildAssetActions(Category, CategoryDraft)}.
     *
     * @param oldCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category oldCategory,
                                                                 @Nonnull final Category newCategory) {
        return new ArrayList<>();
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     *
     * @param oldCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static SyncResult<Category> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final CategoryDraft newCategory,
                                                            @Nonnull final TypeService typeService) {
        final SyncResult<Category> syncResults = buildCoreActions(oldCategory, newCategory, typeService);
        syncResults.merge(buildAssetActions(oldCategory, newCategory));
        return syncResults;
    }

    /**
     * TODO: JAVADOC
     * TODO: UNIT TEST
     *
     * @param oldCategory
     * @param newCategory
     * @param typeService
     * @return
     */
    @Nonnull
    public static SyncResult<Category> buildCoreActions(@Nonnull final Category oldCategory,
                                                        @Nonnull final CategoryDraft newCategory,
                                                        @Nonnull final TypeService typeService) {
        final SyncResult<Category> syncResults = buildChangeNameUpdateAction(oldCategory, newCategory);
        syncResults.merge(buildChangeSlugUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildSetDescriptionUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildChangeParentUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildChangeOrderHintUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildSetMetaTitleUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildSetMetaDescriptionUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildSetMetaKeywordsUpdateAction(oldCategory, newCategory));
        syncResults.merge(buildCustomUpdateActions(oldCategory, newCategory, typeService));
        return syncResults;
    }


    /**
     * TODO: SEE GITHUB ISSUE#3
     *
     * @param oldCategory
     * @param newCategory
     * @return
     */
    @Nonnull
    public static SyncResult<Category> buildAssetActions(@Nonnull final Category oldCategory,
                                                                 @Nonnull final CategoryDraft newCategory) {
        return SyncResult.emptyResult();
    }
}
