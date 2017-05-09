package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.commands.updateactions.SetDescription;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.categories.commands.updateactions.ChangeOrderHint;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.categories.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncUtilsTest {
    private Category mockOldCategory;
    private CategorySyncOptions categorySyncOptions;
    private final CtpClient ctpClient = mock(CtpClient.class);
    private final Locale locale = Locale.GERMAN;
    private final String categoryParentId = "1";
    private final String categoryName = "categoryName";
    private final String categorySlug = "categorySlug";
    private final String categoryExternalId = "externalId";
    private final String categoryDesc = "categoryDesc";
    private final String categoryMetaDesc = "categoryMetaDesc";
    private final String categoryMetaTitle = "categoryMetaTitle";
    private final String categoryKeywords = "categoryKeywords";
    private final String categoryOrderHint = "123";

    /**
     * Initializes an instance of {@link CategorySyncOptions} and {@link Category}. It also sets a mock
     * {@code clientConfig} for an instance of {@link CtpClient} to be used across all the unit tests.
     */
    @Before
    public void setup() {
        final SphereClientConfig clientConfig = SphereClientConfig.of("testPK", "testCI", "testCS");
        when(ctpClient.getClientConfig()).thenReturn(clientConfig);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .build();

        mockOldCategory = getMockCategory(locale,
            categoryName,
            categorySlug,
            categoryExternalId,
            categoryDesc,
            categoryMetaDesc,
            categoryMetaTitle,
            categoryKeywords,
            categoryOrderHint,
            categoryParentId);
    }

    @Test
    public void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            "differentName",
            categorySlug,
            categoryExternalId,
            categoryDesc,
            categoryMetaDesc,
            categoryMetaTitle,
            categoryKeywords,
            categoryOrderHint,
            categoryParentId);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);

        final UpdateAction<Category> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(locale, "differentName"));
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            "differentName",
            "differentSlug",
            categoryExternalId,
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(8);

        final UpdateAction<Category> nameUpdateAction = updateActions.get(0);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(locale, "differentName"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(1);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(locale, "differentSlug"));

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(2);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(locale, "differentDescription"));

        final UpdateAction<Category> parentUpdateAction = updateActions.get(3);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(4);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(5);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(locale, "differentMetaTitle"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(6);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(locale, "differentMetaDescription"));

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(7);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(locale, "differentMetaKeywords"));
    }

    @Test
    public void buildActions_FromDraftsWithMultipleDifferentValuesWithFilterFunction_ShouldBuildFilteredActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            "differentName",
            "differentSlug",
            categoryExternalId,
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>>
            reverseOrderFilter = (unfilteredList) -> {
                Collections.reverse(unfilteredList);
                return unfilteredList;
            };

        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                                                        .setUpdateActionsFilter(reverseOrderFilter)
                                                        .build();

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(8);

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(0);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(locale, "differentMetaKeywords"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(1);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(locale, "differentMetaDescription"));

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(2);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(locale, "differentMetaTitle"));

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(3);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> parentUpdateAction = updateActions.get(4);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(5);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(locale, "differentDescription"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(6);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(locale, "differentSlug"));

        final UpdateAction<Category> nameUpdateAction = updateActions.get(7);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(locale, "differentName"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            "differentName",
            categorySlug,
            categoryExternalId,
            categoryDesc,
            categoryMetaDesc,
            categoryMetaTitle,
            categoryKeywords,
            categoryOrderHint,
            categoryParentId);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);

        final UpdateAction<Category> updateAction = updateActions.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(locale, "differentName"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithMultipleDifferentValues_ShouldBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            "differentName",
            "differentSlug",
            categoryExternalId,
            "differentDescription",
            "differentMetaDescription",
            "differentMetaTitle",
            "differentMetaKeywords",
            "differentOrderHint",
            "differentParentId");

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(8);

        final UpdateAction<Category> nameUpdateAction = updateActions.get(0);
        assertThat(nameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) nameUpdateAction).getName()).isEqualTo(LocalizedString.of(locale, "differentName"));

        final UpdateAction<Category> slugUpdateAction = updateActions.get(1);
        assertThat(slugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) slugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(locale, "differentSlug"));

        final UpdateAction<Category> descriptionUpdateAction = updateActions.get(2);
        assertThat(descriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) descriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(locale, "differentDescription"));

        final UpdateAction<Category> parentUpdateAction = updateActions.get(3);
        assertThat(parentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) parentUpdateAction).getParent().getId()).isEqualTo("differentParentId");

        final UpdateAction<Category> orderHintUpdateAction = updateActions.get(4);
        assertThat(orderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) orderHintUpdateAction).getOrderHint()).isEqualTo("differentOrderHint");

        final UpdateAction<Category> metaTitleUpdateAction = updateActions.get(5);
        assertThat(metaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) metaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(locale, "differentMetaTitle"));

        final UpdateAction<Category> metaDescriptionUpdateAction = updateActions.get(6);
        assertThat(metaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) metaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(locale, "differentMetaDescription"));

        final UpdateAction<Category> metaKeywordsUpdateAction = updateActions.get(7);
        assertThat(metaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) metaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(locale, "differentMetaKeywords"));
    }

    @Test
    public void buildCoreActions_FromDraftsWithSameValues_ShouldNotBuildUpdateActions() {
        final CategoryDraft newCategoryDraft = getMockCategoryDraft(locale,
            categoryName,
            categorySlug,
            categoryExternalId,
            categoryDesc,
            categoryMetaDesc,
            categoryMetaTitle,
            categoryKeywords,
            categoryOrderHint,
            categoryParentId);

        final List<UpdateAction<Category>> updateActions =
            CategorySyncUtils.buildCoreActions(mockOldCategory, newCategoryDraft, categorySyncOptions,
                mock(TypeService.class));
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(0);
    }
}
