package com.commercetools.sync.integration.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.common.LocalizedString;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link CategoryITUtils} utility methods that require actual CTP API
 * interactions.
 */
class CategoryITUtilsIT {

  /** Delete all categories and types from target project before running tests. */
  @BeforeAll
  static void setup() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    ITUtils.deleteTypes(TestClientUtils.CTP_TARGET_CLIENT);
  }

  /** Clean up before each test to ensure a fresh state. */
  @BeforeEach
  void setupTest() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    ITUtils.deleteTypes(TestClientUtils.CTP_TARGET_CLIENT);
  }

  @Test
  void deleteCategoriesBySlug_WithExistingCategories_ShouldDeleteOnlyMatchingSlugs() {
    // preparation - create 4 categories with different slugs
    final CategoryDraft category1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "test-slug-1"))
            .key("key1")
            .build();

    final CategoryDraft category2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "test-slug-2"))
            .key("key2")
            .build();

    final CategoryDraft category3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 3"))
            .slug(LocalizedString.of(Locale.ENGLISH, "test-slug-3"))
            .key("key3")
            .build();

    final CategoryDraft category4 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 4"))
            .slug(LocalizedString.of(Locale.ENGLISH, "other-slug"))
            .key("key4")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category1).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category2).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category3).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category4).executeBlocking();

    // test - delete categories with slugs test-slug-1 and test-slug-2
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT, Locale.ENGLISH, List.of("test-slug-1", "test-slug-2"));

    // assertion - verify only 2 categories remain (test-slug-3 and other-slug)
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).hasSize(2);
    assertThat(remainingCategories)
        .extracting(category -> category.getSlug().get(Locale.ENGLISH))
        .containsExactlyInAnyOrder("test-slug-3", "other-slug");
    assertThat(remainingCategories)
        .extracting(Category::getKey)
        .containsExactlyInAnyOrder("key3", "key4");
  }

  @Test
  void deleteCategoriesBySlug_WithCategoriesWithoutKeys_ShouldDeleteSuccessfully() {
    // preparation - create categories without keys
    final CategoryDraft categoryWithoutKey1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category Without Key 1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "no-key-slug-1"))
            .build();

    final CategoryDraft categoryWithoutKey2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category Without Key 2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "no-key-slug-2"))
            .build();

    final CategoryDraft categoryWithKey =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category With Key"))
            .slug(LocalizedString.of(Locale.ENGLISH, "with-key-slug"))
            .key("with-key")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryWithoutKey1).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryWithoutKey2).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryWithKey).executeBlocking();

    // test - delete categories without keys by their slugs
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT,
        Locale.ENGLISH,
        List.of("no-key-slug-1", "no-key-slug-2"));

    // assertion - verify only the category with key remains
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).hasSize(1);
    assertThat(remainingCategories.get(0).getSlug().get(Locale.ENGLISH)).isEqualTo("with-key-slug");
    assertThat(remainingCategories.get(0).getKey()).isEqualTo("with-key");
  }

  @Test
  void deleteCategoriesBySlug_WithNonExistingSlugs_ShouldNotThrowException() {
    // preparation - create one category
    final CategoryDraft category =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category"))
            .slug(LocalizedString.of(Locale.ENGLISH, "existing-slug"))
            .key("existing-key")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category).executeBlocking();

    // test - try to delete categories with non-existing slugs
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT,
        Locale.ENGLISH,
        List.of("non-existing-slug-1", "non-existing-slug-2"));

    // assertion - verify the existing category was not affected
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).hasSize(1);
    assertThat(remainingCategories.get(0).getSlug().get(Locale.ENGLISH)).isEqualTo("existing-slug");
  }

  @Test
  void deleteCategoriesBySlug_WithEmptySlugList_ShouldNotDeleteAnything() {
    // preparation - create categories
    final CategoryDraft category1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug-1"))
            .key("key1")
            .build();

    final CategoryDraft category2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category 2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug-2"))
            .key("key2")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category1).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category2).executeBlocking();

    // test - call with empty list
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT, Locale.ENGLISH, List.of());

    // assertion - verify both categories still exist
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).hasSize(2);
    assertThat(remainingCategories)
        .extracting(category -> category.getSlug().get(Locale.ENGLISH))
        .containsExactlyInAnyOrder("slug-1", "slug-2");
  }

  @Test
  void deleteCategoriesBySlug_WithDifferentLocale_ShouldDeleteMatchingCategories() {
    // preparation - create categories with German slugs
    final CategoryDraft categoryDe1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.GERMAN, "Kategorie 1"))
            .slug(LocalizedString.of(Locale.GERMAN, "deutsche-slug-1"))
            .key("de-key1")
            .build();

    final CategoryDraft categoryDe2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.GERMAN, "Kategorie 2"))
            .slug(LocalizedString.of(Locale.GERMAN, "deutsche-slug-2"))
            .key("de-key2")
            .build();

    final CategoryDraft categoryDe3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.GERMAN, "Kategorie 3"))
            .slug(LocalizedString.of(Locale.GERMAN, "andere-slug"))
            .key("de-key3")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryDe1).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryDe2).executeBlocking();
    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryDe3).executeBlocking();

    // test - delete categories with German slugs
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT, Locale.GERMAN, List.of("deutsche-slug-1"));

    // assertion - verify only 2 categories remain
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).hasSize(2);
    assertThat(remainingCategories)
        .extracting(category -> category.getSlug().get(Locale.GERMAN))
        .containsExactlyInAnyOrder("deutsche-slug-2", "andere-slug");
  }

  @Test
  void deleteCategoriesBySlug_WithDuplicateSlugsInList_ShouldHandleGracefully() {
    // preparation - create category
    final CategoryDraft category =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Category"))
            .slug(LocalizedString.of(Locale.ENGLISH, "duplicate-slug"))
            .key("dup-key")
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(category).executeBlocking();

    // test - try to delete with duplicate slugs in the list
    CategoryITUtils.deleteCategoriesBySlug(
        TestClientUtils.CTP_TARGET_CLIENT,
        Locale.ENGLISH,
        List.of("duplicate-slug", "duplicate-slug", "duplicate-slug"));

    // assertion - verify category was deleted (no error thrown)
    final List<Category> remainingCategories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    assertThat(remainingCategories).isEmpty();
  }
}
