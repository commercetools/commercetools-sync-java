package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.categories.CategorySyncMockUtils.getMockCategory;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorySyncUtilsTest {
  private Category mockOldCategory;
  private CategorySyncOptions categorySyncOptions;
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private static final Locale LOCALE = Locale.GERMAN;
  private static final String CATEGORY_PARENT_ID = "1";
  private static final String CATEGORY_NAME = "categoryName";
  private static final String CATEGORY_SLUG = "categorySlug";
  private static final String CATEGORY_KEY = "categoryKey";
  private static final String CATEGORY_EXTERNAL_ID = "externalId";
  private static final String CATEGORY_DESC = "categoryDesc";
  private static final String CATEGORY_META_DESC = "categoryMetaDesc";
  private static final String CATEGORY_META_TITLE = "categoryMetaTitle";
  private static final String CATEGORY_KEYWORDS = "categoryKeywords";
  private static final String CATEGORY_ORDER_HINT = "123";
  private static final List<AssetDraft> ASSET_DRAFTS =
      asList(
          AssetDraftBuilder.of()
              .name(ofEnglish("name"))
              .sources(AssetSourceBuilder.of().uri("uri").build())
              .key("1")
              .build(),
          AssetDraftBuilder.of()
              .name(ofEnglish("name"))
              .sources(AssetSourceBuilder.of().uri("uri").build())
              .key("2")
              .build());

  /** Initializes an instance of {@link CategorySyncOptions} and {@link Category}. */
  @BeforeEach
  void setup() {
    categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT).build();

    mockOldCategory =
        getMockCategory(
            LOCALE,
            CATEGORY_NAME,
            CATEGORY_SLUG,
            CATEGORY_KEY,
            CATEGORY_EXTERNAL_ID,
            CATEGORY_DESC,
            CATEGORY_META_DESC,
            CATEGORY_META_TITLE,
            CATEGORY_KEYWORDS,
            CATEGORY_ORDER_HINT,
            CATEGORY_PARENT_ID);
  }

  @Test
  void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
    final CategoryDraft newCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(LOCALE, "differentName"))
            .slug(LocalizedString.of(LOCALE, CATEGORY_SLUG))
            .key(CATEGORY_KEY)
            .externalId(CATEGORY_EXTERNAL_ID)
            .description(LocalizedString.of(LOCALE, CATEGORY_DESC))
            .metaDescription(LocalizedString.of(LOCALE, CATEGORY_META_DESC))
            .metaTitle(LocalizedString.of(LOCALE, CATEGORY_META_TITLE))
            .metaKeywords(LocalizedString.of(LOCALE, CATEGORY_KEYWORDS))
            .orderHint(CATEGORY_ORDER_HINT)
            .parent(CategoryResourceIdentifierBuilder.of().id(CATEGORY_PARENT_ID).build())
            .build();

    final List<CategoryUpdateAction> updateActions =
        CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
    assertThat(updateActions).isNotNull();
    assertThat(updateActions)
        .containsExactly(
            CategoryChangeNameActionBuilder.of()
                .name(LocalizedString.of(LOCALE, "differentName"))
                .build());
  }

  @Test
  void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {

    final CategoryDraft newCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(LOCALE, "differentName"))
            .slug(LocalizedString.of(LOCALE, "differentSlug"))
            .key(CATEGORY_KEY)
            .externalId("differentExternalId")
            .description(LocalizedString.of(LOCALE, "differentDescription"))
            .metaDescription(LocalizedString.of(LOCALE, "differentMetaDescription"))
            .metaTitle(LocalizedString.of(LOCALE, "differentMetaTitle"))
            .metaKeywords(LocalizedString.of(LOCALE, "differentMetaKeywords"))
            .orderHint("differentOrderHint")
            .parent(CategoryResourceIdentifierBuilder.of().id("differentParentId").build())
            .assets(ASSET_DRAFTS)
            .build();

    final List<CategoryUpdateAction> updateActions =
        CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
    assertThat(updateActions).isNotNull();

    assertThat(updateActions)
        .containsExactly(
            CategoryChangeNameActionBuilder.of()
                .name(LocalizedString.of(LOCALE, "differentName"))
                .build(),
            CategoryChangeSlugActionBuilder.of()
                .slug(LocalizedString.of(LOCALE, "differentSlug"))
                .build(),
            CategorySetExternalIdActionBuilder.of().externalId("differentExternalId").build(),
            CategorySetDescriptionActionBuilder.of()
                .description(LocalizedString.of(LOCALE, "differentDescription"))
                .build(),
            CategoryChangeParentActionBuilder.of()
                .parent(CategoryResourceIdentifierBuilder.of().id("differentParentId").build())
                .build(),
            CategoryChangeOrderHintActionBuilder.of().orderHint("differentOrderHint").build(),
            CategorySetMetaTitleActionBuilder.of()
                .metaTitle(LocalizedString.of(LOCALE, "differentMetaTitle"))
                .build(),
            CategorySetMetaDescriptionActionBuilder.of()
                .metaDescription(LocalizedString.of(LOCALE, "differentMetaDescription"))
                .build(),
            CategorySetMetaKeywordsActionBuilder.of()
                .metaKeywords(LocalizedString.of(LOCALE, "differentMetaKeywords"))
                .build(),
            CategoryAddAssetActionBuilder.of().asset(ASSET_DRAFTS.get(0)).position(0).build(),
            CategoryAddAssetActionBuilder.of().asset(ASSET_DRAFTS.get(1)).position(1).build());
  }
}
