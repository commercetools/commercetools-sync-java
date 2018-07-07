package com.commercetools.sync.commons;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.jupiter.params.provider.Arguments;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ArgumentUtils {
    private static final Locale LOCALE = Locale.GERMAN;

    public static Stream buildChangeParentTestCases() {
        return Stream.of(
                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        null,
                        null,
                        null),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        null,
                        Category.referenceOfId("bar"),
                        ChangeParent.of(Category.referenceOfId("bar"))),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        Category.referenceOfId("foo"),
                        null,
                        null,
                        category -> format("Cannot unset 'parent' field of category with id '%s'.", category.getId())),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        Category.referenceOfId("foo"),
                        Category.referenceOfId("bar"),
                        ChangeParent.of(Category.referenceOfId("bar"))),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        Category.referenceOfId(""),
                        Category.referenceOfId("bar"),
                        ChangeParent.of(Category.referenceOfId("bar"))),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        Category.referenceOfId("foo"),
                        Category.referenceOfId(""),
                        ChangeParent.of(Category.referenceOfId(""))),

                buildCategoryFieldTestCaseArgs(Category::getParent, CategoryDraft::getParent,
                        Category.referenceOfId("foo"),
                        Category.referenceOfId("foo"),
                        null));
    }

    public static Stream buildTestCasesForStringsDiff(
            @Nonnull final Function<Category, String> oldStringMapper,
            @Nonnull final Function<CategoryDraft, String> newStringMapper,
            @Nonnull final Function<String, UpdateAction<Category>> actionBuilder,
            final boolean isUnSettable,
            @Nullable final Function<Category, String> warningMapper) {
        return Stream.of(
                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        null, null, null),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        null, "foo", actionBuilder.apply("foo")),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "foo", null,
                        isUnSettable ? actionBuilder.apply(null) : null, warningMapper),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "", "", null),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "", "bar", actionBuilder.apply("bar")),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "foo", "", actionBuilder.apply("")),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "foo", "bar", actionBuilder.apply("bar")),

                buildCategoryFieldTestCaseArgs(oldStringMapper, newStringMapper,
                        "foo", "foo", null));
    }

    public static Stream buildTestCasesForLocalizedStringsDiff(
            @Nonnull final Function<Category, LocalizedString> oldLocalizedStringMapper,
            @Nonnull final Function<CategoryDraft, LocalizedString> newLocalizedStringMapper,
            @Nonnull final Function<LocalizedString, UpdateAction<Category>> actionBuilder) {

        return Stream.of(
                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        null,
                        null,
                        null),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, "foo"),
                        null,
                        actionBuilder.apply(null)),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        null,
                        LocalizedString.of(LOCALE, "foo"),
                        actionBuilder.apply(LocalizedString.of(LOCALE, "foo"))),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, "foo"),
                        LocalizedString.empty(),
                        actionBuilder.apply(LocalizedString.empty())),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.empty(),
                        LocalizedString.of(LOCALE, "foo"),
                        actionBuilder.apply(LocalizedString.of(LOCALE, "foo"))),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.empty(),
                        LocalizedString.empty(),
                        null),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, "foo"),
                        LocalizedString.of(LOCALE, "bar"),
                        actionBuilder.apply(LocalizedString.of(LOCALE, "bar"))),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, ""),
                        LocalizedString.of(LOCALE, "bar"),
                        actionBuilder.apply(LocalizedString.of(LOCALE, "bar"))),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, "foo"),
                        LocalizedString.of(LOCALE, ""),
                        actionBuilder.apply(LocalizedString.of(LOCALE, ""))),

                buildCategoryFieldTestCaseArgs(oldLocalizedStringMapper, newLocalizedStringMapper,
                        LocalizedString.of(LOCALE, "foo"),
                        LocalizedString.of(LOCALE, "foo"),
                        null));
    }

    private static <T> Arguments buildCategoryFieldTestCaseArgs(
            @Nonnull final Function<Category, T> oldFieldMapper,
            @Nonnull final Function<CategoryDraft, T> newFieldMapper,
            @Nullable final T oldValue,
            @Nullable final T newValue,
            @Nullable final UpdateAction<Category> updateAction) {
        return buildCategoryFieldTestCaseArgs(oldFieldMapper, newFieldMapper, oldValue, newValue, updateAction, null);
    }

    private static <T> Arguments buildCategoryFieldTestCaseArgs(
            @Nonnull final Function<Category, T> oldFieldMapper,
            @Nonnull final Function<CategoryDraft, T> newFieldMapper,
            @Nullable final T oldValue,
            @Nullable final T newValue,
            @Nullable final UpdateAction<Category> updateAction,
            @Nullable final Function<Category, String> warningMapper) {

        final Category oldCategory = mock(Category.class);
        when(oldFieldMapper.apply(oldCategory)).thenReturn(oldValue);
        when(oldCategory.toString()).thenReturn(ofNullable(newValue).map(Object::toString).orElse(null));

        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newFieldMapper.apply(newCategory)).thenReturn(newValue);
        when(newCategory.toString()).thenReturn(ofNullable(oldValue).map(Object::toString).orElse(null));

        return Arguments.of(oldCategory, newCategory, updateAction,
                ofNullable(warningMapper)
                        .map(mapper -> mapper.apply(oldCategory))
                        .map(Collections::singletonList)
                        .orElse(emptyList()));
    }

    private ArgumentUtils() {
    }
}
