package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeOrderHint;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.commands.updateactions.SetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetExternalId;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.categories.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategorySyncUtilsTest {
    private Category mockOldCategory;
    private CategorySyncOptions categorySyncOptions;
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
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
    private static final List<AssetDraft> ASSET_DRAFTS = asList(
        AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("name")).key("1")
                         .build(),
        AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("name")).key("2")
                         .build());

    /**
     * Initializes an instance of {@link CategorySyncOptions} and {@link Category}.
     */
    @Before
    public void setup() {
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                        .build();

        mockOldCategory = getMockCategory(LOCALE,
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
    public void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(LOCALE, "differentName"), LocalizedString.of(LOCALE, CATEGORY_SLUG))
            .key(CATEGORY_KEY)
            .externalId(CATEGORY_EXTERNAL_ID)
            .description(LocalizedString.of(LOCALE, CATEGORY_DESC))
            .metaDescription(LocalizedString.of(LOCALE, CATEGORY_META_DESC))
            .metaTitle(LocalizedString.of(LOCALE, CATEGORY_META_TITLE))
            .metaKeywords(LocalizedString.of(LOCALE, CATEGORY_KEYWORDS))
            .orderHint(CATEGORY_ORDER_HINT)
            .parent(Category.referenceOfId(CATEGORY_PARENT_ID).toResourceIdentifier())
            .build();

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).containsExactly(
            ChangeName.of(LocalizedString.of(LOCALE, "differentName")));

    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {

        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(LOCALE, "differentName"), LocalizedString.of(LOCALE, "differentSlug"))
            .key(CATEGORY_KEY)
            .externalId("differentExternalId")
            .description(LocalizedString.of(LOCALE, "differentDescription"))
            .metaDescription(LocalizedString.of(LOCALE, "differentMetaDescription"))
            .metaTitle(LocalizedString.of(LOCALE, "differentMetaTitle"))
            .metaKeywords(LocalizedString.of(LOCALE, "differentMetaKeywords"))
            .orderHint("differentOrderHint")
            .parent(Category.referenceOfId("differentParentId").toResourceIdentifier())
            .assets(ASSET_DRAFTS)
            .build();


        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();

        assertThat(updateActions).containsExactly(
            ChangeName.of(LocalizedString.of(LOCALE, "differentName")),
            ChangeSlug.of(LocalizedString.of(LOCALE, "differentSlug")),
            SetExternalId.of("differentExternalId"),
            SetDescription.of(LocalizedString.of(LOCALE, "differentDescription")),
            ChangeParent.of(ResourceIdentifier.ofId("differentParentId")),
            ChangeOrderHint.of("differentOrderHint"),
            SetMetaTitle.of(LocalizedString.of(LOCALE, "differentMetaTitle")),
            SetMetaDescription.of(LocalizedString.of(LOCALE, "differentMetaDescription")),
            SetMetaKeywords.of(LocalizedString.of(LOCALE, "differentMetaKeywords")),
            AddAsset.of(ASSET_DRAFTS.get(0), 0),
            AddAsset.of(ASSET_DRAFTS.get(1), 1));
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValuesWithFilterFunction_ShouldBuildFilteredActions() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(LOCALE, "differentName"), LocalizedString.of(LOCALE, "differentSlug"))
            .key(CATEGORY_KEY)
            .externalId("differentExternalId")
            .description(LocalizedString.of(LOCALE, "differentDescription"))
            .metaDescription(LocalizedString.of(LOCALE, "differentMetaDescription"))
            .metaTitle(LocalizedString.of(LOCALE, "differentMetaTitle"))
            .metaKeywords(LocalizedString.of(LOCALE, "differentMetaKeywords"))
            .orderHint("differentOrderHint")
            .parent(Category.referenceOfId("differentParentId").toResourceIdentifier())
            .assets(ASSET_DRAFTS)
            .build();

        final TriFunction<List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>>
            reverseOrderFilter = (updateActions, newCategory, oldCategory) -> {
                if (updateActions != null) {
                    Collections.reverse(updateActions);
                }
                return updateActions;
            };

        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                        .beforeUpdateCallback(reverseOrderFilter)
                                                        .build();

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions);
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).containsExactly(
            AddAsset.of(ASSET_DRAFTS.get(1), 1),
            AddAsset.of(ASSET_DRAFTS.get(0), 0),
            SetMetaKeywords.of(LocalizedString.of(LOCALE, "differentMetaKeywords")),
            SetMetaDescription.of(LocalizedString.of(LOCALE, "differentMetaDescription")),
            SetMetaTitle.of(LocalizedString.of(LOCALE, "differentMetaTitle")),
            ChangeOrderHint.of("differentOrderHint"),
            ChangeParent.of(ResourceIdentifier.ofId("differentParentId")),
            SetDescription.of(LocalizedString.of(LOCALE, "differentDescription")),
            SetExternalId.of("differentExternalId"),
            ChangeSlug.of(LocalizedString.of(LOCALE, "differentSlug")),
            ChangeName.of(LocalizedString.of(LOCALE, "differentName")));
    }
}
