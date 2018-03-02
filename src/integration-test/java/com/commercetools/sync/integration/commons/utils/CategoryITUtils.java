package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static io.sphere.sdk.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.lang.String.format;

public final class CategoryITUtils {
    public static final String OLD_CATEGORY_CUSTOM_TYPE_KEY = "oldCategoryCustomTypeKey";
    public static final String OLD_CATEGORY_CUSTOM_TYPE_NAME = "old_type_name";

    /**
     * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects that can be used for
     * integration tests to mimic existing categories in a target CTP project for example. All the newly created
     * category drafts will have {@code parentCategory} as a parent.
     *
     * @param numberOfCategories the number of category drafts to create.
     * @param parentCategory     the parent of the drafts.
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getCategoryDrafts(@Nullable final Category parentCategory,
                                                        final int numberOfCategories) {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        for (int i = 0; i < numberOfCategories; i++) {
            final LocalizedString name = LocalizedString.of(Locale.ENGLISH, format("draft%s", i + 1));
            final LocalizedString slug = LocalizedString.of(Locale.ENGLISH, format("slug%s", i + 1));
            final LocalizedString description = LocalizedString.of(Locale.ENGLISH, format("desc%s", i + 1));
            final String key = format("key%s", i + 1);
            final String orderHint = format("0.%s", i + 1);
            final CategoryDraft categoryDraft = CategoryDraftBuilder.of(name, slug)
                                                                    .parent(parentCategory)
                                                                    .description(description)
                                                                    .key(key)
                                                                    .orderHint(orderHint)
                                                                    .custom(getCustomFieldsDraft())
                                                                    .build();
            categoryDrafts.add(categoryDraft);
        }
        return categoryDrafts;
    }

    /**
     * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects (with a customized
     * prefix string for the name, slug and description) that can be used for integration tests to mimic existing
     * categories in a target CTP project for example. All the newly created category drafts will have
     * {@code parentCategory} as a parent.
     *
     * @param numberOfCategories the number of category drafts to create.
     * @param parentCategory     the parent of the drafts.
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getCategoryDraftsWithPrefix(@Nonnull final Locale locale,
                                                                  @Nonnull final String prefix,
                                                                  @Nullable final Category parentCategory,
                                                                  final int numberOfCategories) {
        final List<CategoryDraft> categoryDraftsWithPrefix = new ArrayList<>();
        final List<CategoryDraft> categoryDrafts = getCategoryDrafts(parentCategory, numberOfCategories);
        for (CategoryDraft categoryDraft : categoryDrafts) {
            final LocalizedString newCategoryName = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getName().get(locale)));
            final LocalizedString newCategorySlug = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getSlug().get(locale)));
            final LocalizedString newCategoryDescription = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getDescription().get(locale)));
            final CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft)
                                                                                  .name(newCategoryName)
                                                                                  .slug(newCategorySlug)
                                                                                  .description(newCategoryDescription);
            categoryDraftsWithPrefix.add(categoryDraftBuilder.build());
        }
        return categoryDraftsWithPrefix;
    }

    /**
     * This method creates {@code numberOfChildren} categories as children to the supplied {@code parent} category in
     * the supplied {@link SphereClient} project in a blocking fashion. It assigns them a key, and an
     * {@code Locale.ENGLISH} name and slug of the value of the supplied {@code prefix} appended to the
     * (index of the child + 1). For example, if the prefix supplied is {@code "cat"}, the key and the english locales
     * of the name and the slug would be {@code "cat1"} for the first child.
     *
     * @param numberOfChildren the number of children categories to create.
     * @param parent           the parent category to assign these children to.
     * @param prefix           a prefix to string to prepend to index of the children, to assign it as a key, name and
     *                         slug to each of the created categories.
     * @param ctpClient        the ctpClient that defines the CTP project to create the categories on.
     * @return the list of Categories created.
     */
    public static List<Category> createChildren(final int numberOfChildren,
                                                @Nullable final Category parent,
                                                @Nonnull final String prefix,
                                                @Nonnull final SphereClient ctpClient) {
        final List<Category> children = new ArrayList<>();
        final List<CompletableFuture<Category>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfChildren; i++) {
            final String categoryName = prefix + (i + 1);
            CategoryDraft child = CategoryDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, categoryName),
                    LocalizedString.of(Locale.ENGLISH, categoryName))
                .key(categoryName)
                .parent(parent)
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
                .orderHint("sameOrderHint")
                .build();
            final CompletableFuture<Category> future = ctpClient
                .execute(CategoryCreateCommand.of(child)).toCompletableFuture();
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                         .thenAccept(voidResult -> {
                             for (CompletableFuture<Category> creationFuture : futures) {
                                 children.add(creationFuture.join());
                             }
                         }).join();
        return children;
    }

    /**
     * Creates a dummy instance of {@link CustomFieldsDraft} with the key defined by
     * {@code OLD_CATEGORY_CUSTOM_TYPE_KEY} and two custom fields 'invisibleInShop' & 'backgroundColor'.
     *
     * <p>The 'invisibleInShop' field is of type {@code boolean} and has value {@code false}. The 'backgroundColor'
     * field is of type {@code localisedString} and has the values {"de": "rot", "en": "red"}.
     *
     * @return a dummy instance of {@link CustomFieldsDraft} with some hardcoded custom fields and key.
     */
    public static CustomFieldsDraft getCustomFieldsDraft() {
        return CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap());
    }

    /**
     * This method blocks to create the supplied {@code categoryDrafts} in the CTP project defined by the supplied
     * {@code ctpClient},
     *
     * <p>Note: the method creates the given categories in parallel. So it expects them all to be in the same hierarchy
     * level.
     *
     * @param ctpClient      defines the CTP project to create the categories on.
     * @param categoryDrafts the drafts to build the categories from.
     */
    public static List<Category> createCategories(@Nonnull final SphereClient ctpClient,
                                                  @Nonnull final List<CategoryDraft> categoryDrafts) {
        final List<CompletableFuture<Category>> futures = new ArrayList<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
            final CompletableFuture<Category> categoryCompletableFuture =
                ctpClient.execute(categoryCreateCommand).toCompletableFuture();
            futures.add(categoryCompletableFuture);

        }

        return listOfFuturesToFutureOfList(futures).join();
    }

    /**
     * This method blocks to create a category custom Type on the CTP project defined by the supplied
     * {@code ctpClient}, with the supplied data.
     *  @param typeKey   the type key
     * @param locale    the locale to be used for specifying the type name and field definitions names.
     * @param name      the name of the custom type.
     * @param ctpClient defines the CTP project to create the type on.
     */
    public static Type createCategoriesCustomType(@Nonnull final String typeKey,
                                                  @Nonnull final Locale locale,
                                                  @Nonnull final String name,
                                                  @Nonnull final SphereClient ctpClient) {

        return createTypeIfNotAlreadyExisting(typeKey, locale, name, ResourceTypeIdsSetBuilder.of().addCategories(),
            ctpClient);
    }


    /**
     * Deletes all categories from CTP projects defined by the {@code ctpClient}. Only issues delete request action to a
     * category in the case that none of its ancestors was already deleted or not to avoid trying to delete a category
     * which would have been already deleted, due to deletion of an ancestor of it. As a performance improvement, this
     * method sorts categories by least ancestors for faster deletion (due to deletion of ancestors always first, which
     * in turn deletes all the children and grand children.
     *
     * @param ctpClient defines the CTP project to delete the categories from.
     */
    public static void deleteAllCategories(@Nonnull final SphereClient ctpClient) {
        final Set<String> keys = new HashSet<>();
        final List<Category> categories = QueryExecutionUtils.queryAll(ctpClient,
            CategoryQuery.of().withExpansionPaths(CategoryExpansionModel::ancestors))
                                                             .thenApply(CategoryITUtils::sortCategoriesByLeastAncestors)
                                                             .toCompletableFuture().join();
        categories.forEach(category -> {
            final String categoryKey = category.getKey();
            if (!hasADeletedAncestor(category, keys)) {
                ctpClient.execute(CategoryDeleteCommand.of(category))
                         .thenAccept(deletedCategory -> keys.add(categoryKey))
                         .toCompletableFuture().join();
            }
        });
    }

    private static List<Category> sortCategoriesByLeastAncestors(@Nonnull final List<Category> categories) {
        categories.sort(Comparator.comparingInt(category -> category.getAncestors().size()));
        return categories;
    }

    private static boolean hasADeletedAncestor(@Nonnull final Category category,
                                               @Nonnull final Set<String> keysOfDeletedAncestors) {
        final List<Reference<Category>> categoryAncestors = category.getAncestors();
        return categoryAncestors.stream().anyMatch(ancestor ->
            keysOfDeletedAncestors.contains(ancestor.getObj().getKey()));
    }


    /**
     * Given a list of {@link CategoryDraft} batches represented by a
     * {@link List}&lt;{@link List}&lt;{@link CategoryDraft}&gt;&gt; and an instance of {@link CategorySync}, this
     * method recursively calls sync by the instance of {@link CategorySync} on each batch, then removes it, until
     * there are no more batches, in other words, all batches have been synced.
     *
     * @param categorySync the categorySync instance to sync with each batch of {@link CategoryDraft}
     * @param batches      the batches of {@link CategoryDraft} to sync.
     * @param result       in the first call of this recursive method, this result is normally a completed future, it
     *                     used from within the method to recursively sync each batch once the previous batch has
     *                     finished syncing.
     * @return an instance of {@link CompletionStage} which contains as a result an instance of
     *          {@link CategorySyncStatistics} representing the {@code statistics} of the sync process executed on the
     *          given list of batches.
     */
    public static CompletionStage<CategorySyncStatistics> syncBatches(@Nonnull final CategorySync categorySync,
                                                                      @Nonnull final List<List<CategoryDraft>> batches,
                                                                      @Nonnull final
                                                                      CompletionStage<CategorySyncStatistics> result) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<CategoryDraft> firstBatch = batches.remove(0);
        return syncBatches(categorySync, batches, result
            .thenCompose(subResult -> categorySync.sync(firstBatch)));
    }

    /**
     * Builds a {@link List} of {@link Reference} with keys in place of ids from the supplied {@link List} of
     * {@link Category}.
     *
     * @param categories a {@link List} of {@link Category} from which the {@link List} of {@link Reference} will be
     *                   built.
     * @return a {@link List} of {@link Reference} with keys in place of ids from the supplied {@link List} of
     *         {@link Category}.
     */
    @Nonnull
    public static List<Reference<Category>> getReferencesWithKeys(@Nonnull final List<Category> categories) {
        return categories.stream()
                         .map(category -> Category.referenceOfId(category.getKey()))
                         .collect(Collectors.toList());
    }

    /**
     * Builds a {@link List} of {@link Reference} built from the supplied {@link List} of {@link Category}.
     *
     * @param categories a {@link List} of {@link Category} from which the {@link List} of {@link Reference} will be
     *                   built.
     * @return a {@link List} of {@link Reference} built from the supplied {@link List} of {@link Category}.
     */
    @Nonnull
    public static List<Reference<Category>> getReferencesWithIds(@Nonnull final List<Category> categories) {
        return categories.stream()
                         .map(category -> Category.referenceOfId(category.getId()))
                         .collect(Collectors.toList());
    }

    /**
     * Given a {@link CategoryOrderHints} instance and a {@link List} of {@link Category}, this method replaces all the
     * categoryOrderHint ids with the {@link Category} keys.
     *
     * @param categoryOrderHints the categoryOrderHints that should have its keys replaced with ids.
     * @param categories the categories that the keys would be taken from to replace on the newly created
     *                  {@link CategoryOrderHints}.
     * @return a new {@link CategoryOrderHints} instance with keys replacing the category ids.
     */
    @Nonnull
    public static CategoryOrderHints replaceCategoryOrderHintCategoryIdsWithKeys(
        @Nonnull final CategoryOrderHints categoryOrderHints,
        @Nonnull final List<Category> categories) {
        final Map<String, String> categoryOrderHintKeyMap = new HashMap<>();
        categoryOrderHints.getAsMap()
                          .forEach((categoryId, categoryOrderHintValue) ->
                              categories.stream()
                                        .filter(category -> Objects.equals(category.getId(), categoryId))
                                        .findFirst()
                                        .ifPresent(category ->
                                            categoryOrderHintKeyMap.put(category.getKey(), categoryOrderHintValue)));
        return CategoryOrderHints.of(categoryOrderHintKeyMap);
    }
}
