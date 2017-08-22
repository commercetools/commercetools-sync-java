package com.commercetools.sync.integration.externalsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsJsons;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ITUtils.getStatisticsAsJSONString;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncIT.class);
    private CategorySync categorySync;
    private final String oldCategoryKey = "oldCategoryKey";

    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Categories and Types from target CTP project, then it populates it with category test data.
     */
    @Before
    public void setupTest() {
        deleteAllCategories(CTP_TARGET_CLIENT);

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .setErrorCallBack(LOGGER::error)
                                                                                  .setErrorCallBack(LOGGER::warn)
                                                                                  .build();
        categorySync = new CategorySync(categorySyncOptions);

        // Create a mock in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                         .toCompletableFuture()
                         .join();
    }

    /**
     * Cleans up the target test data that were built in each test.
     */
    @After
    public void tearDownTest() {
        deleteAllCategories(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the entire target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void syncDrafts_WithANewCategoryWithNewSlug_ShouldCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 1, 0, 0, 0));
    }

    @Test
    public void syncDrafts_WithANewCategoryWithDuplicateSlug_ShouldNotCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithMultipleBatchSyncing_ShouldSync() {
        // Existing array of [1, 2, 3, oldCategoryKey]
        final CategoryDraft oldCategoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"), LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft1))
                         .toCompletableFuture()
                         .join();
        final CategoryDraft oldCategoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"), LocalizedString.of(Locale.ENGLISH, "furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft2))
                         .toCompletableFuture()
                         .join();
        final CategoryDraft oldCategoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"), LocalizedString.of(Locale.ENGLISH, "furniture3"))
            .key("cat3")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft3))
                         .toCompletableFuture()
                         .join();


        //_-----_-----_-----_-----_-----_PREPARE BATCHES FROM EXTERNAL SOURCE-----_-----_-----_-----_-----_-----
        //_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "oldCategoryKey"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .parent(Category.referenceOfId("cat7"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch1 = new ArrayList<>();
        batch1.add(categoryDraft1);

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat7"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat7")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch2 = new ArrayList<>();
        batch2.add(categoryDraft2);

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat6"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat6")
            .parent(Category.referenceOfId("cat5"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch3 = new ArrayList<>();
        batch3.add(categoryDraft3);

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat5")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch4 = new ArrayList<>();
        batch4.add(categoryDraft4);

        final CategorySyncStatistics syncStatistics = categorySync.sync(batch1)
                                                        .thenCompose(result -> categorySync.sync(batch2))
                                                        .thenCompose(result -> categorySync.sync(batch3))
                                                        .thenCompose(result -> categorySync.sync(batch4))
                                                        .toCompletableFuture()
                                                        .join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 4, 3, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithOneBatchSyncing_ShouldSync() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();


        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 5, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithSameSlugDraft_ShouldNotSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        // Same slug draft
        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 5, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithDraftWithInvalidParentKey_ShouldNotSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        // With invalid parent key
        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId(UUID.randomUUID().toString()))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 4, 1, 1, 0));
    }

    @Test
    public void syncDrafts_WithValidAndInvalidCustomTypeKeys_ShouldSyncCorrectly() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();
        final String newCustomTypeKey = "newKey";
        createCategoriesCustomType(newCustomTypeKey, Locale.ENGLISH, "newCustomTypeName", CTP_TARGET_CLIENT);

        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson("nonExistingKey", getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture-2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture-2"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(newCustomTypeKey, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 2, 1, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithValidCustomFieldsChange_ShouldSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons
            .put(LOCALISED_STRING_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.objectNode()
                                                                             .put("de", "rot")
                                                                             .put("en", "red")
                                                                             .put("it", "rosso"));

        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, customFieldsJsons))
            .build();

        newCategoryDrafts.add(categoryDraft1);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithDraftWithAMissingParentKey_ShouldNotSyncIt() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final String nonExistingParentKey = "nonExistingParent";
        final CategoryDraft categoryDraftWithMissingParent = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture1"))
            .key("cat1")
            .parent(Category.referenceOfId(nonExistingParentKey))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(categoryDraft);
        categoryDrafts.add(categoryDraftWithMissingParent);

        final long startTime = System.currentTimeMillis();
        LOGGER.info("Starting to sync categories:");
        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts)
                                                                  .toCompletableFuture().join();
        LOGGER.info(syncStatistics.getReportMessage());
        try {
            LOGGER.info(getStatisticsAsJSONString(syncStatistics));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to build JSON String of summary.", exception);
        }
        final long syncTimeTaken = System.currentTimeMillis() - startTime;
        LOGGER.info("Syncing categories took: " + syncTimeTaken + "ms");

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 2, 2, 0, 0, 1));


        assertThat(syncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        final ArrayList<String> missingParentsChildren = syncStatistics.getCategoryKeysWithMissingParents()
                                                                       .get(nonExistingParentKey);
        assertThat(missingParentsChildren).hasSize(1);
        final String childrenKeys = missingParentsChildren.get(0);
        assertThat(childrenKeys).isEqualTo(categoryDraftWithMissingParent.getKey());


    }
}
