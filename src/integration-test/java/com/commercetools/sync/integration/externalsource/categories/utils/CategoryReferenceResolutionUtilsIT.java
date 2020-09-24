package com.commercetools.sync.integration.externalsource.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryReferenceResolutionUtils.buildCategoryQuery;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createAssetsCustomType;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

class CategoryReferenceResolutionUtilsIT {

    /**
     * Delete all categories and types from source and target project. Then create custom types for source and target
     * CTP project categories.
     */
    @BeforeAll
    static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypesFromTargetAndSource();
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypesFromTargetAndSource();
    }

    @Test
    void buildCategoryQuery_Always_ShouldFetchProductWithAllExpandedReferences() {
        final CategoryDraft parentDraft = CategoryDraftBuilder.of(ofEnglish("parent"), ofEnglish("parent"))
                                                                .build();
        final Category parentCategory =
            executeBlocking(CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(parentDraft)));

        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, ENGLISH, "anyName", CTP_TARGET_CLIENT);

        final Type assetsCustomType = createAssetsCustomType("assetsCustomTypeKey", ENGLISH,
            "assetsCustomTypeName", CTP_TARGET_CLIENT);
        final List<AssetDraft> assetDrafts = singletonList(
            createAssetDraft("1", ofEnglish("1"), assetsCustomType.getId()));

        final CategoryDraft categoryDraft = CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug"))
                                                                .parent(parentCategory.toResourceIdentifier())
                                                                .custom(getCustomFieldsDraft())
                                                                .assets(assetDrafts)
                                                                .build();

        executeBlocking(CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)));

        final List<Category> categories =
            executeBlocking(CTP_TARGET_CLIENT.execute(buildCategoryQuery().bySlug(ENGLISH, "slug"))).getResults();

        assertThat(categories).hasSize(1);
        final Category fetchedCategory = categories.get(0);

        // Assert category parent references are expanded.
        assertThat(fetchedCategory.getParent()).isNotNull();
        assertThat(fetchedCategory.getParent().getObj()).isNotNull();

        // Assert category custom type references are expanded.
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getCustom().getType().getObj()).isNotNull();

        // Assert category assets custom type references are expanded.
        assertThat(fetchedCategory.getAssets()).hasSize(1);
        final Asset masterVariantAsset = fetchedCategory.getAssets().get(0);
        assertThat(masterVariantAsset.getCustom()).isNotNull();
        assertThat(masterVariantAsset.getCustom().getType().getObj()).isNotNull();

    }
}
