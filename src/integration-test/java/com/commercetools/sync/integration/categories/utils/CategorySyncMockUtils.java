package com.commercetools.sync.integration.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQueryBuilder;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;

public class CategorySyncMockUtils {
    public static List<CategoryDraft> getMockCategoryDrafts() {
        return Arrays.asList(getMockCategoryDraft(Locale.GERMAN, "draft", "slug", "1"),
            getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "2"),
            getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "3"),
            getMockCategoryDraft(Locale.GERMAN, "draft3", "slug3", "4"),
            getMockCategoryDraft(Locale.GERMAN, "draft4", "slug4", "5"),
            getMockCategoryDraft(Locale.GERMAN, "draft5", "slug5", "6"),
            getMockCategoryDraft(Locale.GERMAN, "draft6", "slug6", "7"),
            getMockCategoryDraft(Locale.GERMAN, "draft7", "slug7", "8"),
            getMockCategoryDraft(Locale.ENGLISH, "draft8", "slug8", "9"),
            getMockCategoryDraft(Locale.GERMAN, "draft9", "slug9", "10"),
            getMockCategoryDraft(Locale.GERMAN, "draft10", "slug10", "11"),
            getMockCategoryDraft(Locale.GERMAN, "draft11", "slug11", "12"),
            getMockCategoryDraft(Locale.GERMAN, "draft12", "slug12", "13"),
            getMockCategoryDraft(Locale.GERMAN, "draft13", "slug13", "14"),
            getMockCategoryDraft(Locale.GERMAN, "draft14", "slug14", "15"),
            getMockCategoryDraft(Locale.GERMAN, "draft15", "slug15", "16"),
            getMockCategoryDraft(Locale.GERMAN, "draft16", "slug16", "17"),
            getMockCategoryDraft(Locale.GERMAN, "draft17", "slug17", "18"));
    }

    /**
     * Given a {@code locale}, {@code name}, {@code slug} and {@code externalId}; this method creates a mock of
     * {@link CategoryDraft} with all those supplied fields. All the supplied arguments are given as {@link String} and
     * the method internally converts them to their required types. For example, for all the fields that require a
     * {@link LocalizedString} as a value type; the method creates an instance of a {@link LocalizedString} with
     * the given {@link String} and {@link Locale}.
     *
     * @param locale     the locale to create with all the {@link LocalizedString} instances.
     * @param name       the name of the category.
     * @param slug       the slug of the category.
     * @param externalId the external id of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nullable final String externalId) {

        return CategoryDraftBuilder.of(LocalizedString.of(locale, name), LocalizedString.of(locale, slug))
                                   .externalId(externalId)
                                   .custom(getMockCustomFieldsDraft())
                                   .build();
    }

    public static void removeAllCategories(@Nonnull final BlockingSphereClient sphereClient) {
        sphereClient.executeBlocking(CategoryQueryBuilder.of().limit(500).build())
                    .getResults()
                    .forEach(category -> sphereClient.executeBlocking(CategoryDeleteCommand.of(category)));
    }

    public static void createCategories(@Nonnull final BlockingSphereClient sphereClient,
                                        @Nonnull final List<CategoryDraft> categoryDrafts) {
        final List<CompletableFuture<Category>> futures = new ArrayList<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
            final CompletableFuture<Category> categoryCompletableFuture =
                sphereClient.execute(categoryCreateCommand).toCompletableFuture();
            futures.add(categoryCompletableFuture);

        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                         .toCompletableFuture().join();
    }
}
