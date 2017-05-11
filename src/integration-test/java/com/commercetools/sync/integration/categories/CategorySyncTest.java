package com.commercetools.sync.integration.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClientConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.integration.categories.utils.CategorySyncMockUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncTest.class);
    private static final String CTP_PROJECT_KEY = "xxxxxxxxx";
    private static final String CTP_CLIENT_ID = "xxxxxxxxx";
    private static final String CTP_CLIENT_SECRET = "xxxxxxxxx";
    private static final List<CategoryDraft> OLD_CATEGORIES = getMockCategoryDrafts();

    private static CategorySync categorySync;
    private static CategorySyncOptions categorySyncOptions;
    private static CtpClient ctpClient;

    @BeforeClass
    public static void setupTestSuite() {
        final SphereClientConfig CLIENT_CONFIG = SphereClientConfig
            .of(CTP_PROJECT_KEY, CTP_CLIENT_ID, CTP_CLIENT_SECRET);
        ctpClient = new CtpClient(CLIENT_CONFIG);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .setErrorCallBack(LOGGER::error)
                                                        .setErrorCallBack(LOGGER::warn)
                                                        .build();
    }

    @Before
    public void setupTest() {
        categorySync = new CategorySync(categorySyncOptions);
        removeAllCategories(ctpClient.getClient());
        createCategories(ctpClient.getClient(), OLD_CATEGORIES);
    }

    @AfterClass
    public static void tearDown() {
        removeAllCategories(ctpClient.getClient()); //Clean project
    }

    @Test
    public void syncDrafts_WithANewCategory_ShouldCreateCategory() {
        final List<CategoryDraft> newCategories = new ArrayList<>();
        final CategoryDraft newCategory =
            getMockCategoryDraft(Locale.ENGLISH, "newCategory", "slug", "newExternalId");
        newCategories.add(newCategory);
        newCategories.addAll(OLD_CATEGORIES);

        categorySync.syncDrafts(newCategories);
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (1 created, 0 updated and 0 categories "
                + "failed to sync).", newCategories.size()));
    }

    @Test
    public void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
        final List<CategoryDraft> newCategories = new ArrayList<>();
        final CategoryDraft updatedCategory =
            getMockCategoryDraft(Locale.ENGLISH, "newCategory", "newSlug", "1");

        newCategories.addAll(OLD_CATEGORIES);
        newCategories.remove(0);
        newCategories.add(updatedCategory);

        categorySync.syncDrafts(newCategories);
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (0 created, 1 updated and 0 categories "
                + "failed to sync).", newCategories.size()));
    }
}
