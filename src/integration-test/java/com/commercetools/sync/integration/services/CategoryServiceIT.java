package com.commercetools.sync.integration.services;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class CategoryServiceIT {
    private CategoryService categoryService;
    private Category targetProjectRootCategory;
    private Category oldCategory;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .setErrorCallBack(
                                                                                      (errorMessage, exception) -> {
                                                                                          errorCallBackMessages
                                                                                              .add(errorMessage);
                                                                                          errorCallBackExceptions
                                                                                              .add(exception);
                                                                                      })
                                                                                  .build();
        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        // Create a mock new category in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("oldCategoryKey")
            .parent(targetProjectRootCategory)
            .custom(getMockCustomFieldsDraft())
            .build();
        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();

        categoryService = new CategoryServiceImpl(categorySyncOptions);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    public void cacheKeysToIds_ShouldCacheCategoryKeysOnlyFirstTime() {
        Map<String, String> cache = categoryService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(2);

        // Create new category without caching
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .parent(targetProjectRootCategory)
            .build();

        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        cache = categoryService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(2);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(Collections.emptySet())
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithAllExistingSetOfKeys_ShouldReturnSetOfCategories() {
        final Set<String> keys =  new HashSet<>();
        keys.add(targetProjectRootCategory.getKey());
        keys.add("oldCategoryKey");
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(keys)
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).hasSize(2);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithSomeExistingSetOfKeys_ShouldReturnSetOfCategories() {
        final Set<String> keys =  new HashSet<>();
        keys.add(targetProjectRootCategory.getKey());
        keys.add("oldCategoryKey");
        keys.add("new-key");
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(keys)
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).hasSize(2);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithNonExistingCategory_ShouldNotFetchACategory() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId("non-existing-category-key")
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void createCategories_WithAllValidCategories_ShouldCreateCategories() {
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture1"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture1", Locale.GERMAN, "klassische-moebel1"))
            .key("key1")
            .parent(targetProjectRootCategory)
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture2"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture2", Locale.GERMAN, "klassische-moebel2"))
            .key("key2")
            .parent(targetProjectRootCategory)
            .build();

        final Set<CategoryDraft> categoryDrafts = new HashSet<>();
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);

        final Set<Category> createdCategories = categoryService.createCategories(categoryDrafts)
                                                               .toCompletableFuture().join();

        assertThat(createdCategories).hasSize(2);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void createCategories_WithSomeValidCategories_ShouldCreateCategoriesAndTriggerCallBack() {
        // Draft with invalid key
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture1"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture1", Locale.GERMAN, "klassische-moebel1"))
            .key("1")
            .parent(targetProjectRootCategory)
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture2"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture2", Locale.GERMAN, "klassische-moebel2"))
            .key("key2")
            .parent(targetProjectRootCategory)
            .build();

        final Set<CategoryDraft> categoryDrafts = new HashSet<>();
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);

        final Set<Category> createdCategories = categoryService.createCategories(categoryDrafts)
                                                               .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Invalid category key '1'. Category keys may only contain "
            + "alphanumeric characters, underscores and hyphens and must have a maximum length of 256 characters.");
        assertThat(createdCategories).hasSize(1);
    }

    @Test
    public void createCategories_WithNoneValidCategories_ShouldTriggerCallBack() {
        // Draft with invalid key
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture1"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture1", Locale.GERMAN, "klassische-moebel1"))
            .key("1")
            .parent(targetProjectRootCategory)
            .build();

        // Draft with duplicate slug
        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture2"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("key2")
            .parent(targetProjectRootCategory)
            .build();

        final Set<CategoryDraft> categoryDrafts = new HashSet<>();
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);

        final Set<Category> createdCategories = categoryService.createCategories(categoryDrafts)
                                                               .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).hasSize(2);
        assertThat(errorCallBackMessages).hasSize(2);
        // Since the order of creation is not ensured by allOf, so we and assert in list of error messages together:
        assertThat(errorCallBackMessages.toString()).contains("Invalid category key '1'. Category keys may only contain"
            + " alphanumeric characters, underscores and hyphens and must have a maximum length of 256 characters.");
        assertThat(errorCallBackMessages.toString()).contains(" A duplicate value '\"furniture\"' exists for field "
            + "'slug.en'");
        assertThat(createdCategories).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithExistingCategory_ShouldFetchCategoryAndCache() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId(oldCategory.getKey())
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }


    @Test
    public void fetchCachedCategoryId_ShouldCacheCategoryKeysOnlyFirstTime() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyKey").toCompletableFuture().join();

        // Create new category
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .parent(targetProjectRootCategory)
            .build();

        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryKey).toCompletableFuture().join();

        assertThat(newCategoryId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void createCategory_WithValidCategory_ShouldCreateCategory() {
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getMockCustomFieldsDraft())
            .parent(targetProjectRootCategory)
            .build();

        final Optional<Category> createdCategoryOptional = categoryService.createCategory(categoryDraft)
                                                           .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(createdCategoryOptional).isNotEmpty();
        final Category createdCategory = createdCategoryOptional.get();


        //assert CTP state
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of()
                                  .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategoryKey)))
            .toCompletableFuture().join().head();

        assertThat(categoryOptional).isNotEmpty();
        final Category fetchedCategory = categoryOptional.get();
        assertThat(fetchedCategory.getName()).isEqualTo(createdCategory.getName());
        assertThat(fetchedCategory.getSlug()).isEqualTo(createdCategory.getSlug());
        assertThat(fetchedCategory.getParent()).isEqualTo(Category.reference(targetProjectRootCategory.getId()));
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getKey()).isEqualTo(newCategoryKey);
    }

    @Test
    public void createCategory_WithInvalidCategory_ShouldNotCreateCategory() {
        final String newCategoryKey = "1";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getMockCustomFieldsDraft())
            .parent(targetProjectRootCategory)
            .build();

        final Optional<Category> createdCategoryOptional = categoryService.createCategory(categoryDraft)
                                                                          .toCompletableFuture().join();
        assertThat(createdCategoryOptional).isEmpty();
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Invalid category key '1'. Category keys may only contain "
            + "alphanumeric characters, underscores and hyphens and must have a maximum length of 256 characters.");

        //assert CTP state
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of()
                                  .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategoryKey)))
            .toCompletableFuture().join().head();
        assertThat(categoryOptional).isEmpty();
    }

    @Test
    public void updateCategory_WithValidChanges_ShouldUpdateCategoryCorrectly() {
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        final String newCategoryName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newCategoryName));

        final Optional<Category> updatedCategoryOptional = categoryService
            .updateCategory(categoryOptional.get(), Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture().join();
        assertThat(updatedCategoryOptional).isNotEmpty();

        final Category updatedCategory = updatedCategoryOptional.get();

        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getName()).isEqualTo(updatedCategory.getName());
        assertThat(fetchedCategory.getSlug()).isEqualTo(updatedCategory.getSlug());
        assertThat(fetchedCategory.getParent()).isEqualTo(updatedCategory.getParent());
        assertThat(fetchedCategory.getCustom()).isEqualTo(updatedCategory.getCustom());
        assertThat(fetchedCategory.getKey()).isEqualTo(updatedCategory.getKey());
    }

    @Test
    public void updateCategory_WithInvalidChanges_ShouldNotUpdateCategory() {
        final LocalizedString newSlug = LocalizedString.of(Locale.ENGLISH, "furniture");
        final ChangeSlug changeSlugUpdateAction = ChangeSlug.of(newSlug);

        final Optional<Category> updatedCategoryOptional = categoryService
            .updateCategory(targetProjectRootCategory, Collections.singletonList(changeSlugUpdateAction))
            .toCompletableFuture().join();

        assertThat(updatedCategoryOptional).isEmpty();
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("A duplicate value '\"furniture\"' exists for field"
            + " 'slug.en'");

        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(targetProjectRootCategory.getKey())))
            .toCompletableFuture().join().head();

        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getSlug()).isNotEqualTo(newSlug);
    }

}
