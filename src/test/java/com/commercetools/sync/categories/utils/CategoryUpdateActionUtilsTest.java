package com.commercetools.sync.categories.utils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.*;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryUpdateActionUtilsTest {
    private Category mockOldCategory;
    private CategorySyncOptions categorySyncOptions;
    private final CtpClient ctpClient = mock(CtpClient.class);
    private final Locale locale = Locale.GERMAN;
    private final String mockCategoryReferenceType = "type";
    private final String mockOldCategoryParentId = "1";
    private final String mockOldCategoryName = "categoryName";
    private final String mockOldCategorySlug = "categorySlug";
    private final String mockOldCategoryExternalId = "externalId";
    private final String mockOldCategoryDescription = "categoryDesc";
    private final String mockOldCategoryMetaDescription = "categoryMetaDesc";
    private final String mockOldCategoryMetaTitle = "categoryMetaTitle";
    private final String mockOldCategoryMetaKeywords = "categoryKeywords";
    private final String mockOldCategoryOrderhint = "123";

    @Before
    public void setup() {
        final SphereClientConfig clientConfig = SphereClientConfig.of("testPK", "testCI", "testCS");
        when(ctpClient.getClientConfig()).thenReturn(clientConfig);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
                .build();

        mockOldCategory = getMockCategory(locale,
                mockOldCategoryName,
                mockOldCategorySlug,
                mockOldCategoryExternalId,
                mockOldCategoryDescription,
                mockOldCategoryMetaDescription,
                mockOldCategoryMetaTitle,
                mockOldCategoryMetaKeywords,
                mockOldCategoryOrderhint,
                mockOldCategoryParentId);
    }

    @Test
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, "newName"));

        final UpdateAction<Category> changeNameUpdateAction =
                buildChangeNameUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(LocalizedString.of(locale, "newName"));
    }

    @Test
    public void buildChangeNameUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getName()).thenReturn(null);

        final UpdateAction<Category> changeNameUpdateAction =
                buildChangeNameUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isNull();
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameName.getName())
                .thenReturn(LocalizedString.of(locale, mockOldCategoryName));

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
                buildChangeNameUpdateAction(mockOldCategory, newCategoryDraftWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, "newSlug"));

        final UpdateAction<Category> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(locale, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(null);

        final UpdateAction<Category> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isNull();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
                .thenReturn(LocalizedString.of(locale, mockOldCategorySlug));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
                buildChangeSlugUpdateAction(mockOldCategory, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(locale, "newDescription"));

        final UpdateAction<Category> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(mockOldCategory, newCategoryDraft,
                        categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
                .isEqualTo(LocalizedString.of(locale, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
                .thenReturn(LocalizedString.of(locale, mockOldCategoryDescription));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(mockOldCategory, newCategoryDraftWithSameDescription,
                        categorySyncOptions);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(mockOldCategory.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getDescription()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
                buildSetDescriptionUpdateAction(mockOldCategory, newCategory,
                        categorySyncOptions);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'description' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Reference.of("Category", "2"));

        final UpdateAction<Category> changeParentUpdateAction =
                buildChangeParentUpdateAction(mockOldCategory, newCategoryDraft, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) changeParentUpdateAction).getParent())
                .isEqualTo(Reference.of("Category", "2"));
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
                .thenReturn(Reference.of(mockCategoryReferenceType, mockOldCategoryParentId));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(mockOldCategory, newCategoryDraftWithSameParent, categorySyncOptions);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentnUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(mockOldCategory.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getParent()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
                buildChangeParentUpdateAction(mockOldCategory, newCategory,
                        categorySyncOptions);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'parent' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final UpdateAction<Category> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(mockOldCategory, newCategoryDraft, categorySyncOptions).orElse(null);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) changeOrderHintUpdateAction).getOrderHint())
                .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(mockOldCategoryOrderhint);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(mockOldCategory, newCategoryDraftWithSameOrderHint,
                        categorySyncOptions);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(mockOldCategory.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getOrderHint()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setWarningCallBack(updateActionWarningCallBack)
                .build();

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
                buildChangeOrderHintUpdateAction(mockOldCategory, newCategory,
                        categorySyncOptions);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'orderHint' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(locale, "newMetaTitle"));

        final UpdateAction<Category> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
                .isEqualTo(LocalizedString.of(locale, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(null);

        final UpdateAction<Category> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
                .isEqualTo(null);
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
                .thenReturn(LocalizedString.of(locale, mockOldCategoryMetaTitle));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
                buildSetMetaTitleUpdateAction(mockOldCategory, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(locale, "newMetaKW"));

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords())
                .isEqualTo(LocalizedString.of(locale, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(null);

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isNull();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
                .thenReturn(LocalizedString.of(locale, mockOldCategoryMetaKeywords));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
                buildSetMetaKeywordsUpdateAction(mockOldCategory, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(locale, "newMetaDesc"));

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
                .isEqualTo(LocalizedString.of(locale, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(null);

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(mockOldCategory, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isNull();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
                .thenReturn(LocalizedString.of(locale, mockOldCategoryMetaDescription));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
                buildSetMetaDescriptionUpdateAction(mockOldCategory, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }
}
