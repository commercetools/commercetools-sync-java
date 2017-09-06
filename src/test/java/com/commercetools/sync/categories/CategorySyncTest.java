package com.commercetools.sync.categories;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
    private CategorySync categorySync;
    private CategorySyncOptions categorySyncOptions;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .setErrorCallBack((errorMessage, exception) -> {
                                                            errorCallBackMessages.add(errorMessage);
                                                            errorCallBackExceptions.add(exception);
                                                        })
                                                        .build();
        categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), getMockCategoryService());
    }

    @Test
    public void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(new ArrayList<>())
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(0);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 0 categories were processed in "
            + "total (0 created, 0 updated, 0 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    public void sync_WithANullDraft_ShouldBeCountedAsFailed() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(null);

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft is null.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithADraftWithNoSetKey_ShouldFailSync() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "noKeyDraft", "no-key-id-draft",null));


        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("CategoryDraft with name: "
            + "LocalizedString(en -> noKeyDraft) doesn't have a key.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithNoExistingCategory_ShouldCreateCategory() {
        final CategoryService mockCategoryService = getMockCategoryService();
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

        final CategorySync mockCategorySync =
            new CategorySync(categorySyncOptions, getMockTypeService(), mockCategoryService);

        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "newKey", "parentKey",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = mockCategorySync.sync(categoryDrafts)
                                                                      .toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(1);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (1 created, 0 updated, 0 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithExistingCategory_ShouldUpdateCategory() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts)
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 1 updated, 0 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH,
            "name",
            "slug",
            "key",
            "externalId",
            "description",
            "metaDescription",
            "metaTitle",
            "metaKeywords",
            "orderHint",
            "parentId");
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(categoryDraft);

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 0 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithExistingCategoryButWithNotAllowedUuidReferenceResolution_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String parentUuid = String.valueOf(UUID.randomUUID());
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", parentUuid,
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Found a UUID in the id field. Expecting a key without a UUID"
            + " value. If you want to allow UUID values for reference keys, please use the setAllowUuidKeys(true)"
            + " option in the sync options.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", null,
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Key is blank (null/empty) on both expanded reference object"
            + " and reference id field.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithNoCustomType_ShouldSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "name"));
        when(categoryDraft.getKey()).thenReturn("key");
        when(categoryDraft.getCustom()).thenReturn(null);

        categoryDrafts.add(categoryDraft);

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 1 updated, 0 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "",
            "customTypeId", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 0 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve parent reference on CategoryDraft"
            + " with key:'key'. Reason: Key is blank (null/empty) on both expanded reference object"
            + " and reference id field.", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            "", new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve custom type reference on "
            + "CategoryDraft with key:'key'. Reason: Reference 'id' field value is blank (null/"
            + "empty).", ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithNotAllowedUuidCustomTypeKey_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            uuidCustomTypeKey, new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated()).isEqualTo(0);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 0 updated, 1 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
            + " key:'key'. Reason: %s: Failed to resolve custom type reference on "
            + "CategoryDraft with key:'key'. Reason: Found a UUID in the id field. Expecting a key"
            + " without a UUID value. If you want to allow UUID values for reference keys, please use the"
            + " setAllowUuidKeys(true) option in the sync options.",
            ReferenceResolutionException.class.getCanonicalName()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithAllowedUuidCustomTypeKey_ShouldSync() {
        categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                        .setAllowUuidKeys(true)
                                                        .build();
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key", "parentKey",
            uuidCustomTypeKey, new HashMap<>()));

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getReportMessage()).isEqualTo("Summary: 1 categories were processed in "
            + "total (0 created, 1 updated, 0 failed to sync and 1 categories with a missing parent).");
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void requiresChangeParentUpdateAction_WithTwoDifferentParents_ShouldReturnTrue() {
        final String parentId = "parentId";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(Category.referenceOfId("differentParent"))
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isTrue();
    }

    @Test
    public void requiresChangeParentUpdateAction_WithTwoIdenticalParents_ShouldReturnFalse() {
        final String parentId = "parentId";
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(Category.referenceOfId(parentId));

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .parent(Category.referenceOfId(parentId))
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }

    @Test
    public void requiresChangeParentUpdateAction_WithNonExistingParents_ShouldReturnFalse() {
        final Category category = mock(Category.class);
        when(category.getParent()).thenReturn(null);

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "name"), LocalizedString.of(Locale.ENGLISH, "slug"))
            .build();
        final boolean doesRequire = CategorySync.requiresChangeParentUpdateAction(category, categoryDraft);
        assertThat(doesRequire).isFalse();
    }
}
