package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.ArgumentUtils;
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
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeOrderHintUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.commons.ArgumentUtils.buildTestCasesForLocalizedStringsDiff;
import static com.commercetools.sync.commons.ArgumentUtils.buildTestCasesForStringsDiff;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CategoryUpdateActionUtilsTest {
    @ParameterizedTest(name = "Comparing names of old category:[{0}] to categoryDraft:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildChangeNameTestCases")
    void buildChangeNameUpdateActionTest(@Nonnull final Category oldCategory,
                                         @Nonnull final CategoryDraft newCategoryDraft,
                                         @Nullable final ChangeName expectedAction) {

        final Optional<UpdateAction<Category>> actionOptional =
                buildChangeNameUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), actionOptional);
    }

    private static Stream buildChangeNameTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getName, CategoryDraft::getName, ChangeName::of);
    }


    @ParameterizedTest(name = "comparing old slug:[{0}] to new slug:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildChangeSlugTestCases")
    void buildChangeSlugUpdateActionTest(@Nonnull final Category oldCategory,
                                         @Nonnull final CategoryDraft newCategoryDraft,
                                         @Nullable final ChangeSlug expectedAction) {

        final Optional<UpdateAction<Category>> actionOptional =
                buildChangeSlugUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), actionOptional);
    }

    private static Stream buildChangeSlugTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getSlug, CategoryDraft::getSlug, ChangeSlug::of);
    }

    @ParameterizedTest(name = "comparing old description:[{0}] to new description:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildSetDescriptionTestCases")
    void buildSetDescriptionUpdateActionTest(@Nonnull final Category oldCategory,
                                             @Nonnull final CategoryDraft newCategoryDraft,
                                             @Nullable final SetDescription expectedAction) {

        final Optional<UpdateAction<Category>> actionOptional =
                buildSetDescriptionUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), actionOptional);
    }

    private static Stream buildSetDescriptionTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getDescription, CategoryDraft::getDescription, SetDescription::of);
    }

    @ParameterizedTest(name = "comparing old parent:[{0}] to new parent:[{1}] Should result in update action:[{2}] and warnings {3}")
    @MethodSource("buildChangeParentTestCases")
    void buildChangeParentUpdateActionTest(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final ChangeParent expectedAction,
            @Nonnull final List<String> expectedWarnings) {

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                .warningCallback(
                        updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> actionOptional =
                buildChangeParentUpdateAction(oldCategory, newCategoryDraft, categorySyncOptions);

        assertEquals(ofNullable(expectedAction), actionOptional);
        assertEquals(callBackResponse, expectedWarnings);
    }

    private static Stream buildChangeParentTestCases() {
        return ArgumentUtils.buildChangeParentTestCases();
    }

    @ParameterizedTest(name = "comparing old orderHint:[{0}] to new orderHint:[{1}] Should result in update action:[{2}] and warnings: {3}")
    @MethodSource("buildChangeOrderHintTestCases")
    void buildChangeOrderHintUpdateActionTest(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final ChangeOrderHint expectedAction,
            @Nonnull final List<String> expectedWarnings) {

        final ArrayList<Object> warningCallbackResults = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = warningCallbackResults::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                .warningCallback(
                        updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> optionalAction =
                buildChangeOrderHintUpdateAction(oldCategory, newCategoryDraft, categorySyncOptions);

        assertEquals(ofNullable(expectedAction), optionalAction);
        assertEquals(expectedWarnings, warningCallbackResults);
    }

    private static Stream buildChangeOrderHintTestCases() {
        return buildTestCasesForStringsDiff(Category::getOrderHint, CategoryDraft::getOrderHint, ChangeOrderHint::of,
                false,
                category -> format("Cannot unset 'orderHint' field of category with id '%s'.", category.getId()));
    }

    @ParameterizedTest(name = "comparing old meta title:[{0}] to new meta title:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildSetMetaTitleTestCases")
    void buildSetMetaTitleUpdateActionTest(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final SetMetaTitle expectedAction) {

        final Optional<UpdateAction<Category>> optionalAction =
                buildSetMetaTitleUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), optionalAction);
    }

    private static Stream buildSetMetaTitleTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getMetaTitle, CategoryDraft::getMetaTitle, SetMetaTitle::of);
    }

    @ParameterizedTest(name = "comparing old meta keywords:[{0}] to new meta keywords:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildSetMetaKeywordsTestCases")
    void buildSetMetaKeywordsUpdateActionTest(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final SetMetaKeywords expectedAction) {
        final Optional<UpdateAction<Category>> optionalAction =
                buildSetMetaKeywordsUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), optionalAction);
    }

    private static Stream buildSetMetaKeywordsTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getMetaKeywords, CategoryDraft::getMetaKeywords,
                SetMetaKeywords::of);
    }

    @ParameterizedTest(name = "comparing old meta description:[{0}] to new meta description:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildSetMetaDescriptionTestCases")
    void buildSetMetaDescriptionUpdateActionTest(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final SetMetaDescription expectedAction) {

        final Optional<UpdateAction<Category>> optionalAction =
                buildSetMetaDescriptionUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), optionalAction);
    }

    private static Stream buildSetMetaDescriptionTestCases() {
        return buildTestCasesForLocalizedStringsDiff(Category::getMetaDescription, CategoryDraft::getMetaDescription,
                SetMetaDescription::of);
    }

    @ParameterizedTest(name = "comparing old external id:[{0}] to new external id:[{1}] Should result in update action:[{2}]")
    @MethodSource("buildSetExternalIdTestCases")
    void buildSetExternalIdUpdateActionTest2(
            @Nonnull final Category oldCategory,
            @Nonnull final CategoryDraft newCategoryDraft,
            @Nullable final SetExternalId expectedAction) {

        final Optional<UpdateAction<Category>> optionalAction =
                buildSetExternalIdUpdateAction(oldCategory, newCategoryDraft);

        assertEquals(ofNullable(expectedAction), optionalAction);
    }

    private static Stream buildSetExternalIdTestCases() {
        return buildTestCasesForStringsDiff(Category::getExternalId, CategoryDraft::getExternalId, SetExternalId::of,
                true, null);
    }
}
