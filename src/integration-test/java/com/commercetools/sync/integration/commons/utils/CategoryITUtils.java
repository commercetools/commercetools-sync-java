package com.commercetools.sync.integration.commons.utils;

import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CategoryITUtils {
  private static final Logger logger = LoggerFactory.getLogger(CategoryITUtils.class);

  public static final String OLD_CATEGORY_CUSTOM_TYPE_KEY = "oldCategoryCustomTypeKey";
  public static final String OLD_CATEGORY_CUSTOM_TYPE_NAME = "old_type_name";

  public static final String CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedReferencesService.categoryDrafts";

  /**
   * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects that
   * can be used for integration tests to mimic existing categories in a target CTP project for
   * example. All the newly created category drafts will have {@code parentCategory} as a parent.
   *
   * @param parentCategory the parent of the drafts.
   * @param numberOfCategories the number of category drafts to create.
   * @param withCustom
   * @return a list of CategoryDrafts.
   */
  public static List<CategoryDraft> getCategoryDrafts(
      @Nullable final Category parentCategory, final int numberOfCategories, boolean withCustom) {
    List<CategoryDraft> categoryDrafts = new ArrayList<>();
    for (int i = 0; i < numberOfCategories; i++) {
      final LocalizedString name = LocalizedString.of(Locale.ENGLISH, format("draft%s", i + 1));
      final LocalizedString slug = LocalizedString.of(Locale.ENGLISH, format("slug%s", i + 1));
      final LocalizedString description =
          LocalizedString.of(Locale.ENGLISH, format("desc%s", i + 1));
      final String key = format("key%s", i + 1);
      final String orderHint = format("0.%s", i + 1);
      final CategoryDraft categoryDraft =
          CategoryDraftBuilder.of()
              .name(name)
              .slug(slug)
              .parent(
                  parentCategory != null
                      ? CategoryResourceIdentifierBuilder.of().key(parentCategory.getKey()).build()
                      : null)
              .description(description)
              .key(key)
              .orderHint(orderHint)
              .custom(withCustom ? getCustomFieldsDraft() : null)
              .build();
      categoryDrafts.add(categoryDraft);
    }
    return categoryDrafts;
  }

  /**
   * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects
   * (with a customized prefix string for the name, slug and description) that can be used for
   * integration tests to mimic existing categories in a target CTP project for example. All the
   * newly created category drafts will have {@code parentCategory} as a parent.
   *
   * @param numberOfCategories the number of category drafts to create.
   * @param parentCategory the parent of the drafts.
   * @return a list of CategoryDrafts.
   */
  public static List<CategoryDraft> getCategoryDraftsWithPrefix(
      @Nonnull final Locale locale,
      @Nonnull final String prefix,
      @Nullable final Category parentCategory,
      final int numberOfCategories) {
    final List<CategoryDraft> categoryDraftsWithPrefix = new ArrayList<>();
    final List<CategoryDraft> categoryDrafts =
        getCategoryDrafts(parentCategory, numberOfCategories, true);
    for (CategoryDraft categoryDraft : categoryDrafts) {
      final LocalizedString newCategoryName =
          LocalizedString.of(locale, format("%s%s", prefix, categoryDraft.getName().get(locale)));
      final LocalizedString newCategorySlug =
          LocalizedString.of(locale, format("%s%s", prefix, categoryDraft.getSlug().get(locale)));
      final LocalizedString newCategoryDescription =
          LocalizedString.of(
              locale, format("%s%s", prefix, categoryDraft.getDescription().get(locale)));
      final CategoryDraftBuilder categoryDraftBuilder =
          CategoryDraftBuilder.of(categoryDraft)
              .name(newCategoryName)
              .slug(newCategorySlug)
              .description(newCategoryDescription);
      categoryDraftsWithPrefix.add(categoryDraftBuilder.build());
    }
    return categoryDraftsWithPrefix;
  }

  /**
   * This method creates {@code numberOfChildren} categories as children to the supplied {@code
   * parent} category in the supplied {@link ProjectApiRoot} project in a blocking fashion. It
   * assigns them a key, and an {@code Locale.ENGLISH} name and slug of the value of the supplied
   * {@code prefix} appended to the (index of the child + 1). For example, if the prefix supplied is
   * {@code "cat"}, the key and the english locales of the name and the slug would be {@code "cat1"}
   * for the first child.
   *
   * @param numberOfChildren the number of children categories to create.
   * @param parent the parent category to assign these children to.
   * @param prefix a prefix to string to prepend to index of the children, to assign it as a key,
   *     name and slug to each of the created categories.
   * @param ctpClient the ctpClient that defines the CTP project to create the categories on.
   * @return the list of Categories created.
   */
  public static List<Category> createChildren(
      final int numberOfChildren,
      @Nullable final Category parent,
      @Nonnull final String prefix,
      @Nonnull final ProjectApiRoot ctpClient) {
    final List<Category> children = new ArrayList<>();
    final List<CompletableFuture<Category>> futures = new ArrayList<>();
    for (int i = 0; i < numberOfChildren; i++) {
      final String categoryName = prefix + (i + 1);
      CategoryDraftBuilder childBuilder =
          CategoryDraftBuilder.of()
              .name(LocalizedString.of(Locale.ENGLISH, categoryName))
              .slug(LocalizedString.of(Locale.ENGLISH, categoryName))
              .key(categoryName)
              .custom(getCustomFieldsDraft())
              .orderHint("sameOrderHint");
      if (parent != null) {
        childBuilder =
            childBuilder.parent(
                CategoryResourceIdentifierBuilder.of().key(parent.getKey()).build());
      }
      CategoryDraft child = childBuilder.build();
      final CompletableFuture<Category> future =
          ctpClient
              .categories()
              .create(child)
              .execute()
              .thenApply(ApiHttpResponse::getBody)
              .toCompletableFuture();
      futures.add(future);
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
        .thenAccept(
            voidResult -> {
              for (CompletableFuture<Category> creationFuture : futures) {
                children.add(creationFuture.join());
              }
            })
        .join();
    return children;
  }

  /**
   * Creates a dummy instance of {@link CustomFieldsDraft} with the key defined by {@code
   * OLD_CATEGORY_CUSTOM_TYPE_KEY} and two custom fields 'invisibleInShop' & 'backgroundColor'.
   *
   * <p>The 'invisibleInShop' field is of type {@code boolean} and has value {@code false}. The
   * 'backgroundColor' field is of type {@code localisedString} and has the values {"de": "rot",
   * "en": "red"}.
   *
   * @return a dummy instance of {@link CustomFieldsDraft} with some hardcoded custom fields and
   *     key.
   */
  public static CustomFieldsDraft getCustomFieldsDraft() {
    return CustomFieldsDraftBuilder.of()
        .type(TypeResourceIdentifierBuilder.of().key(OLD_CATEGORY_CUSTOM_TYPE_KEY).build())
        .fields(ITUtils.createCustomFieldsJsonMap())
        .build();
  }

  /**
   * This method blocks to create the supplied {@code categoryDrafts} in the CTP project defined by
   * the supplied {@code ctpClient},
   *
   * <p>Note: the method creates the given categories in parallel. So it expects them all to be in
   * the same hierarchy level.
   *
   * @param ctpClient defines the CTP project to create the categories on.
   * @param categoryDrafts the drafts to build the categories from.
   */
  public static List<Category> ensureCategories(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final List<CategoryDraft> categoryDrafts) {
    final List<CompletableFuture<Category>> futures = new ArrayList<>();
    for (CategoryDraft categoryDraft : categoryDrafts) {
      final CompletableFuture<Category> categoryCompletableFuture =
          ctpClient
              .categories()
              .get()
              .withWhere("key=:key")
              .withPredicateVar("key", categoryDraft.getKey())
              .execute()
              .thenApply(ApiHttpResponse::getBody)
              .thenApply(CategoryPagedQueryResponse::getResults)
              .thenApply(
                  categories -> {
                    if (categories.isEmpty()) {
                      return ctpClient
                          .categories()
                          .create(categoryDraft)
                          .execute()
                          .thenApply(ApiHttpResponse::getBody);
                    } else {
                      return CompletableFuture.completedFuture(categories.get(0));
                    }
                  })
              .thenCompose(Function.identity());

      futures.add(categoryCompletableFuture);
    }

    return listOfFuturesToFutureOfList(futures).join();
  }

  /**
   * This method blocks to create a category custom Type on the CTP project defined by the supplied
   * {@code ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type ensureCategoriesCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {
    return ITUtils.createTypeIfNotAlreadyExisting(
        typeKey, locale, name, singletonList(ResourceTypeId.CATEGORY), ctpClient);
  }

  /**
   * Deletes all categories and types from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the categories and types from.
   */
  public static void deleteCategorySyncTestData(@Nonnull final ProjectApiRoot ctpClient) {
    deleteAllCategories(ctpClient);
    ITUtils.deleteTypes(ctpClient);
    CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects(
        ctpClient, CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY);
  }

  /**
   * Deletes all categories from CTP projects defined by the {@code ctpClient}. Only issues delete
   * request action to a category in the case that none of its ancestors was already deleted or not
   * to avoid trying to delete a category which would have been already deleted, due to deletion of
   * an ancestor of it. As a performance improvement, this method sorts categories by least
   * ancestors for faster deletion (due to deletion of ancestors always first, which in turn deletes
   * all the children and grand children.
   *
   * @param ctpClient defines the CTP project to delete the categories from.
   */
  public static void deleteAllCategories(@Nonnull final ProjectApiRoot ctpClient) {
    final Set<String> deletedIds = new HashSet<>();
    final List<Category> categories =
        QueryUtils.queryAll(
                ctpClient.categories().get().addExpand("ancestors[*]"), categories1 -> categories1)
            .thenApply(
                fetchedCategories ->
                    fetchedCategories.stream().flatMap(List::stream).collect(Collectors.toList()))
            .thenApply(CategoryITUtils::sortCategoriesByLeastAncestors)
            .toCompletableFuture()
            .join();
    categories.forEach(
        category -> {
          final String categoryId = category.getId();
          if (!hasADeletedAncestor(category, deletedIds)) {
            try {
              ctpClient.categories().delete(category).execute().toCompletableFuture().join();
              deletedIds.add(categoryId);
            } catch (NotFoundException e) {
              // Already deleted, mark as deleted
              deletedIds.add(categoryId);
            } catch (RuntimeException e) {
              // May have been deleted by parent cascade - this is expected behavior
              logger.debug(
                  "Could not delete category ID: {}, key: {}. Reason: {}",
                  categoryId,
                  category.getKey(),
                  e.getMessage());
            }
          }
        });
  }

  /**
   * Deletes categories from CTP projects defined by the {@code ctpClient} that match any of the
   * supplied slugs in the specified locale. This method is useful for cleaning up specific
   * categories by slug, especially those without keys.
   *
   * <p>This method handles errors gracefully and will not throw exceptions if categories don't
   * exist or have already been deleted.
   *
   * @param ctpClient defines the CTP project to delete the categories from.
   * @param locale the locale to use when matching slugs.
   * @param slugs the list of slugs to match for deletion.
   */
  public static void deleteCategoriesBySlug(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final Locale locale,
      @Nonnull final List<String> slugs) {
    if (slugs == null || slugs.isEmpty()) {
      return;
    }

    for (String slug : slugs) {
      try {
        ctpClient
            .categories()
            .get()
            .addWhere("slug(" + locale.getLanguage() + "=:slug)")
            .addPredicateVar("slug", slug)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults()
            .forEach(
                cat -> {
                  try {
                    ctpClient
                        .categories()
                        .withId(cat.getId())
                        .delete()
                        .withVersion(cat.getVersion())
                        .execute()
                        .toCompletableFuture()
                        .join();
                  } catch (NotFoundException e) {
                    // Already deleted
                    logger.debug(
                      "Category with slug '{}' already deleted", slug);

                  } catch (RuntimeException e) {
                    // May have been deleted by parent cascade or version conflict
                    logger.debug(
                        "Could not delete category with slug '{}', ID: {}", slug, cat.getId());
                  }
                });
      } catch (RuntimeException e) {
        // Slug doesn't exist or query failed - this is expected
        logger.debug(
            "Category with slug '{}' does not exist", slug);
      }
    }
  }

  private static List<Category> sortCategoriesByLeastAncestors(
      @Nonnull final List<Category> categories) {
    categories.sort(Comparator.comparingInt(category -> category.getAncestors().size()));
    return categories;
  }

  private static boolean hasADeletedAncestor(
      @Nonnull final Category category, @Nonnull final Set<String> idsOfDeletedAncestors) {
    final List<CategoryReference> categoryAncestors = category.getAncestors();
    return categoryAncestors.stream()
        .filter(Objects::nonNull)
        .anyMatch(
            ancestor ->
                ancestor.getObj() != null
                    && idsOfDeletedAncestors.contains(ancestor.getObj().getId()));
  }

  /**
   * Builds a {@link java.util.Set} of {@link CategoryResourceIdentifier} with keys in place of ids
   * from the supplied {@link java.util.List} of {@link Category}.
   *
   * @param categories a {@link java.util.List} of {@link Category} from which the {@link
   *     java.util.List} of {@link CategoryResourceIdentifier} will be built.
   * @return a {@link java.util.List} of {@link CategoryResourceIdentifier} with keys in place of
   *     ids from the supplied {@link java.util.List} of {@link Category}.
   */
  @Nonnull
  public static List<CategoryResourceIdentifier> getResourceIdentifiersWithKeys(
      @Nonnull final List<Category> categories) {
    return categories.stream()
        .map(category -> CategoryResourceIdentifierBuilder.of().key(category.getKey()).build())
        .collect(Collectors.toList());
  }

  /**
   * Builds a {@link java.util.List} of {@link
   * com.commercetools.api.models.category.CategoryReference} built from the supplied {@link
   * java.util.List} of {@link Category}.
   *
   * @param categories a {@link java.util.List} of {@link Category} from which the {@link
   *     java.util.List} of {@link CategoryReference} will be built.
   * @return a {@link java.util.List} of {@link CategoryReference} built from the supplied {@link
   *     java.util.List} of {@link Category}.
   */
  @Nonnull
  public static List<CategoryReference> getReferencesWithIds(
      @Nonnull final List<Category> categories) {
    return categories.stream()
        .map(category -> CategoryReferenceBuilder.of().id(category.getId()).build())
        .collect(Collectors.toList());
  }

  public static List<CategoryResourceIdentifier> getResourceIdentifiersWithIds(
      @Nonnull final List<Category> categories) {
    return categories.stream()
        .map(category -> CategoryResourceIdentifierBuilder.of().id(category.getId()).build())
        .collect(Collectors.toList());
  }

  /**
   * Given a {@link CategoryOrderHints} instance and a {@link java.util.List} of {@link Category},
   * this method replaces all the categoryOrderHint ids with the {@link Category} keys.
   *
   * @param categoryOrderHints the categoryOrderHints that should have its keys replaced with ids.
   * @param categories the categories that the keys would be taken from to replace on the newly
   *     created {@link CategoryOrderHints}.
   * @return a new {@link CategoryOrderHints} instance with keys replacing the category ids.
   */
  @Nonnull
  public static CategoryOrderHints replaceCategoryOrderHintCategoryIdsWithKeys(
      @Nonnull final CategoryOrderHints categoryOrderHints,
      @Nonnull final List<Category> categories) {
    final Map<String, String> categoryOrderHintKeyMap = new HashMap<>();
    categoryOrderHints
        .values()
        .forEach(
            (categoryId, categoryOrderHintValue) ->
                categories.stream()
                    .filter(category -> Objects.equals(category.getId(), categoryId))
                    .findFirst()
                    .ifPresent(
                        category ->
                            categoryOrderHintKeyMap.put(
                                category.getKey(), categoryOrderHintValue)));
    return CategoryOrderHintsBuilder.of().values(categoryOrderHintKeyMap).build();
  }

  private CategoryITUtils() {}
}
