package com.commercetools.sync.products.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.mockito.Mockito.mock;

public class ProductUpdateActionUtilsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
/*
    @Test
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final UpdateAction<Category> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    public void buildChangeSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getSlug()).thenReturn(null);

        final UpdateAction<Category> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isNull();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameSlug.getSlug())
            .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_SLUG));

        final Optional<UpdateAction<Category>> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameSlug);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameDescription.getDescription())
            .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));

        final Optional<UpdateAction<Category>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameDescription);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getDescription()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();

        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategory).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(null);
        assertThat(callBackResponse).isEmpty();
    }

    @Test
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getParent()).thenReturn(Category.referenceOfId("2"));

        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft, CATEGORY_SYNC_OPTIONS).orElse(null);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) changeParentUpdateAction).getParent())
            .isEqualTo(Category.referenceOfId("2"));
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameParent = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameParent.getParent())
            .thenReturn(Category.referenceOfId(MOCK_OLD_CATEGORY_PARENT_ID));

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
            buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameParent, CATEGORY_SYNC_OPTIONS);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeParentUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getParent()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .setWarningCallBack(
                                                                                      updateActionWarningCallBack)
                                                                                  .build();

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
            buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                categorySyncOptions);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'parent' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeParentUpdateAction_WithBothNullValues_ShouldNotBuildUpdateActionAndNotCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        when(MOCK_OLD_CATEGORY.getParent()).thenReturn(null);
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getParent()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .setWarningCallBack(
                                                                                      updateActionWarningCallBack)
                                                                                  .build();

        final Optional<UpdateAction<Category>> changeParentUpdateAction =
            buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                categorySyncOptions);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction).isNotPresent();
        assertThat(callBackResponse).isEmpty();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getOrderHint()).thenReturn("099");

        final UpdateAction<Category> changeOrderHintUpdateAction =
            buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft, CATEGORY_SYNC_OPTIONS).orElse(null);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
        assertThat(((ChangeOrderHint) changeOrderHintUpdateAction).getOrderHint())
            .isEqualTo("099");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
            buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameOrderHint,
                CATEGORY_SYNC_OPTIONS);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getOrderHint()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .setWarningCallBack(
                                                                                      updateActionWarningCallBack)
                                                                                  .build();

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
            buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                categorySyncOptions);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
        assertThat(callBackResponse).hasSize(1);
        assertThat(callBackResponse.get(0)).isEqualTo("Cannot unset 'orderHint' field of category with id 'oldCatId'.");
    }

    @Test
    public void buildChangeOrderHintUpdateAction_WithBothNullValues_ShouldNotBuildUpdateActionAndNotCallCallback() {
        when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
        when(MOCK_OLD_CATEGORY.getOrderHint()).thenReturn(null);
        final CategoryDraft newCategory = mock(CategoryDraft.class);
        when(newCategory.getOrderHint()).thenReturn(null);

        final ArrayList<Object> callBackResponse = new ArrayList<>();
        final Consumer<String> updateActionWarningCallBack = callBackResponse::add;

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .setWarningCallBack(
                                                                                      updateActionWarningCallBack)
                                                                                  .build();

        final Optional<UpdateAction<Category>> changeOrderHintUpdateAction =
            buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategory,
                categorySyncOptions);

        assertThat(changeOrderHintUpdateAction).isNotNull();
        assertThat(changeOrderHintUpdateAction).isNotPresent();
        assertThat(callBackResponse).isEmpty();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
            .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaTitle()).thenReturn(null);

        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
            .isEqualTo(null);
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameTitle.getMetaTitle())
            .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_TITLE));

        final Optional<UpdateAction<Category>> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameTitle);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
            buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords())
            .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaKeywords()).thenReturn(null);

        final UpdateAction<Category> setMetaKeywordsUpdateAction =
            buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isNull();
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
            .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_KEYWORDS));

        final Optional<UpdateAction<Category>> setMetaKeywordsUpdateAction =
            buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaKeywords);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getMetaDescription()).thenReturn(null);

        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isNull();
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
        when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
            .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_DESCRIPTION));

        final Optional<UpdateAction<Category>> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaDescription);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getExternalId()).thenReturn("newExternalId");

        final UpdateAction<Category> setExternalIdUpdateAction =
            buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setExternalIdUpdateAction).isNotNull();
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId())
            .isEqualTo("newExternalId");
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getExternalId()).thenReturn(null);

        final UpdateAction<Category> setExternalIdUpdateAction =
            buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

        assertThat(setExternalIdUpdateAction).isNotNull();
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId())
            .isNull();
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithSameValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
        when(newCategoryDraft.getExternalId()).thenReturn(MOCK_OLD_CATEGORY_EXTERNAL_ID);

        final Optional<UpdateAction<Category>> setExternalIdUpdateAction =
            buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

        assertThat(setExternalIdUpdateAction).isNotNull();
        assertThat(setExternalIdUpdateAction).isNotPresent();
    }*/

}
