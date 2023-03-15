package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static io.sphere.sdk.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
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
import io.sphere.sdk.models.ResourceIdentifier;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CategoryITUtils {
  public static final String OLD_CATEGORY_CUSTOM_TYPE_KEY = "oldCategoryCustomTypeKey";
  public static final String OLD_CATEGORY_CUSTOM_TYPE_NAME = "old_type_name";

  /**
   * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects that
   * can be used for integration tests to mimic existing categories in a target CTP project for
   * example. All the newly created category drafts will have {@code parentCategory} as a parent.
   *
   * @param numberOfCategories the number of category drafts to create.
   * @param parentCategory the parent of the drafts.
   * @return a list of CategoryDrafts.
   */
  public static List<CategoryDraft> getCategoryDrafts(
      @Nullable final Category parentCategory, final int numberOfCategories) {
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
              .custom(getCustomFieldsDraft())
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
        getCategoryDrafts(parentCategory, numberOfCategories);
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
  public static List<Category> createCategories(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final List<CategoryDraft> categoryDrafts) {
    final List<CompletableFuture<Category>> futures = new ArrayList<>();
    for (CategoryDraft categoryDraft : categoryDrafts) {
      final CompletableFuture<Category> categoryCompletableFuture =
          ctpClient
              .categories()
              .create(categoryDraft)
              .execute()
              .thenApply(ApiHttpResponse::getBody)
              .toCompletableFuture();
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
  public static Type createCategoriesCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {

    return createTypeIfNotAlreadyExisting(
        typeKey, locale, name, singletonList(ResourceTypeId.CATEGORY), ctpClient);
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
    final Set<String> keys = new HashSet<>();
    final List<Category> categories =
        QueryUtils.queryAll(ctpClient.categories().get(), categories1 -> categories1)
            .thenApply(
                fetchedCategories ->
                    fetchedCategories.stream().flatMap(List::stream).collect(Collectors.toList()))
            .thenApply(CategoryITUtils::sortCategoriesByLeastAncestors)
            .toCompletableFuture()
            .join();
    categories.forEach(
        category -> {
          final String categoryKey = category.getKey();
          if (!hasADeletedAncestor(category, keys)) {
            ctpClient
                .categories()
                .delete(category)
                .execute()
                .thenAccept(deletedCategory -> keys.add(categoryKey))
                .toCompletableFuture()
                .join();
          }
        });
  }

  private static List<Category> sortCategoriesByLeastAncestors(
      @Nonnull final List<Category> categories) {
    categories.sort(Comparator.comparingInt(category -> category.getAncestors().size()));
    return categories;
  }

  private static boolean hasADeletedAncestor(
      @Nonnull final Category category, @Nonnull final Set<String> keysOfDeletedAncestors) {
    final List<CategoryReference> categoryAncestors = category.getAncestors();
    return categoryAncestors.stream()
        .anyMatch(ancestor -> keysOfDeletedAncestors.contains(ancestor.getObj().getKey()));
  }

  /**
   * Builds a {@link java.util.Set} of {@link CategoryResourceIdentifier} with keys in place of ids
   * from the supplied {@link java.util.List} of {@link Category}.
   *
   * @param categories a {@link java.util.List} of {@link Category} from which the {@link
   *     java.util.Set} of {@link ResourceIdentifier} will be built.
   * @return a {@link java.util.Set} of {@link io.sphere.sdk.models.ResourceIdentifier} with keys in
   *     place of ids from the supplied {@link java.util.List} of {@link Category}.
   */
  @Nonnull
  public static Set<CategoryResourceIdentifier> getResourceIdentifiersWithKeys(
      @Nonnull final List<Category> categories) {
    return categories.stream()
        .map(category -> CategoryResourceIdentifierBuilder.of().key(category.getKey()).build())
        .collect(Collectors.toSet());
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
