package com.commercetools.sync.integration.externalsource.categories.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.integration.commons.utils.CategoryITUtils;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CategoryReferenceResolutionUtilsIT {

  /**
   * Delete all categories and types from source and target project. Then create custom types for
   * source and target CTP project categories.
   */
  @BeforeAll
  static void setup() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    ITUtils.deleteTypesFromTargetAndSource();
  }

  /** Cleans up the target and source test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    ITUtils.deleteTypesFromTargetAndSource();
  }

  @Test
  void buildCategoryQuery_Always_ShouldFetchProductWithoutExpansionOnReferences() {
    final CategoryDraft parentDraft =
        CategoryDraftBuilder.of().name(ofEnglish("parent")).slug(ofEnglish("parent")).build();
    final Category parentCategory =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .create(parentDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    CategoryITUtils.ensureCategoriesCustomType(CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY, ENGLISH, "anyName", TestClientUtils.CTP_TARGET_CLIENT);

    final Type assetsCustomType =
        ITUtils.ensureAssetsCustomType(
            "assetsCustomTypeKey", ENGLISH, "assetsCustomTypeName", TestClientUtils.CTP_TARGET_CLIENT);
    final List<AssetDraft> assetDrafts =
        singletonList(ITUtils.createAssetDraft("1", ofEnglish("1"), assetsCustomType.getId()));

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("name"))
            .slug(ofEnglish("slug"))
            .parent(parentCategory.toResourceIdentifier())
            .custom(CategoryITUtils.getCustomFieldsDraft())
            .assets(assetDrafts)
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.categories().create(categoryDraft).executeBlocking();

    final List<Category> categories =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .addWhere("slug(en=:slug)")
            .addPredicateVar("slug", "slug")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CategoryPagedQueryResponse::getResults)
            .join();

    assertThat(categories).hasSize(1);
    final Category fetchedCategory = categories.get(0);

    // Assert category parent references are not expanded.
    assertThat(fetchedCategory.getParent()).isNotNull();
    assertThat(fetchedCategory.getParent().getObj()).isNull();

    // Assert category custom type references are not expanded.
    assertThat(fetchedCategory.getCustom()).isNotNull();
    assertThat(fetchedCategory.getCustom().getType().getObj()).isNull();

    // Assert category assets custom type references are not expanded.
    assertThat(fetchedCategory.getAssets()).hasSize(1);
    final Asset masterVariantAsset = fetchedCategory.getAssets().get(0);
    assertThat(masterVariantAsset.getCustom()).isNotNull();
    assertThat(masterVariantAsset.getCustom().getType().getObj()).isNull();
  }
}
