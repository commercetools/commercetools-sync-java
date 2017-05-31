package com.commercetools.sync.categories;

import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;

import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
    private CategorySync categorySync;
    private CategorySyncOptions categorySyncOptions;
    private String errorMessage;

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        final SphereClient ctpClient = mock(SphereClient.class);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .setErrorCallBack((err, ex) -> errorMessage = err)
                                                        .build();
        categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), getMockCategoryService());
    }

    @Test
    public void sync_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        categorySync.sync(new ArrayList<>());

        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 0 categories were processed in total "
                + "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void sync_WithANullDraft_ShouldBeCountedAsFailed() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(null);

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("CategoryDraft is null.");
    }

    @Test
    public void sync_WithADraftWithNoSetExternalID_ShouldFailSync() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "noExternalIdDraft", "no-external-id-draft", null));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("CategoryDraft with name: LocalizedString(en -> noExternalIdDraft)"
            + " doesn't have an externalId.");
    }

    @Test
    public void sync_WithNoExistingCategory_ShouldCreateCategory() {
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
                                                           categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "parentExternalId",
            "customTypeId", new HashMap<>()));


        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(1 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void sync_WithExistingCategory_ShouldUpdateCategory() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 1 updated and 0 categories failed to sync).");
    }

    @Test
    public void sync_WithIdenticalExistingCategory_ShouldNotUpdateCategory() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH,
            "name",
            "slug",
            "externalId",
            "description",
            "metaDescription",
            "metaTitle",
            "metaKeywords",
            "orderHint",
            "parentId");
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(categoryDraft);


        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void sync_WithExistingCategoryButExceptionOnFetch_ShouldFailSync() {
        CompletableFuture<Optional<Category>> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException();
        });
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString())).thenReturn(futureThrowingSphereException);

        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).contains("Failed to fetch category with externalId:'externalId'");
    }

    @Test
    public void sync_WithNoExistingCategoryButExceptionOnCreate_ShouldFailSync() {
        final CompletableFuture<Category> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException();
        });
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(categoryService.createCategory(any())).thenReturn(futureThrowingSphereException);

        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).contains("Failed to create category with externalId:'externalId'");
    }

    @Test
    public void sync_WithExistingCategoryButExceptionOnUpdate_ShouldFailSync() {
        final CompletableFuture<Category> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException();
        });
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.updateCategory(any(), any())).thenReturn(futureThrowingSphereException);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(), categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).contains("Failed to update category with externalId:'externalId'");
    }

    @Test
    public void sync_WithExistingCategoryButWithNotAllowedUuidReferenceResolution_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String parentUuid = String.valueOf(UUID.randomUUID());
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", parentUuid,
            "customTypeId", new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).contains("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve parent reference. Reason: Found a UUID in the id field. Expecting a key "
            + "without a UUID value. If you want to allow UUID values for reference keys, please use the "
            + "setAllowUuid(true) option in the sync options.");
    }

    @Test
    public void sync_WithExistingCategoryButWithNullParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", null,
            "customTypeId", new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve parent reference. Reason: Key is blank (null/empty) on both expanded"
            + " reference object and reference id field.");
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyParentReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "",
            "customTypeId", new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve parent reference. Reason: Key is blank (null/empty) on both expanded"
            + " reference object and reference id field.");
    }

    @Test
    public void sync_WithExistingCategoryButWithNullCustomTypeReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "parentExternalId",
            null, new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).contains("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
            + "(null/empty).");
    }

    @Test
    public void sync_WithExistingCategoryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "parentExternalId",
            "", new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
            + "(null/empty).");
    }

    @Test
    public void sync_WithNotAllowedUuidCustomTypeKey_ShouldFailSync() {
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "parentExternalId",
            uuidCustomTypeKey, new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
        assertThat(errorMessage).isEqualTo("Failed to resolve reference on CategoryDraft with externalId:'externalId'."
            + " Reason: Failed to resolve custom type reference. Reason: Found a UUID in the id field. Expecting a key"
            + " without a UUID value. If you want to allow UUID values for reference keys, please use the"
            + " setAllowUuid(true) option in the sync options.");
    }

    @Test
    public void sync_WithAllowedUuidCustomTypeKey_ShouldSync() {
        categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                        .setErrorCallBack((err, ex) -> errorMessage = err)
                                                        .setAllowUuid(true)
                                                        .build();
        final CategorySync categorySync = new CategorySync(categorySyncOptions, getMockTypeService(),
            getMockCategoryService());
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "externalId", "parentExternalId",
            uuidCustomTypeKey, new HashMap<>()));

        categorySync.sync(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 1 updated and 0 categories failed to sync).");
    }

}
