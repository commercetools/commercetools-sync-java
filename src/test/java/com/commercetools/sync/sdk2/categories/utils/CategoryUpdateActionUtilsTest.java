package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.sync.sdk2.categories.CategorySyncMockUtils.getMockCategory;
import static com.commercetools.sync.sdk2.categories.utils.CategoryUpdateActionUtils.*;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CategoryUpdateActionUtilsTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private static final CategorySyncOptions CATEGORY_SYNC_OPTIONS =
      CategorySyncOptionsBuilder.of(CTP_CLIENT).build();
  private static final Locale LOCALE = Locale.GERMAN;
  private static final String MOCK_OLD_CATEGORY_PARENT_ID = "1";
  private static final String MOCK_OLD_CATEGORY_NAME = "categoryName";
  private static final String MOCK_OLD_CATEGORY_SLUG = "categorySlug";
  private static final String MOCK_OLD_CATEGORY_KEY = "categoryKey";
  private static final String MOCK_OLD_CATEGORY_EXTERNAL_ID = "externalId";
  private static final String MOCK_OLD_CATEGORY_DESCRIPTION = "categoryDesc";
  private static final String MOCK_OLD_CATEGORY_META_DESCRIPTION = "categoryMetaDesc";
  private static final String MOCK_OLD_CATEGORY_META_TITLE = "categoryMetaTitle";
  private static final String MOCK_OLD_CATEGORY_META_KEYWORDS = "categoryKeywords";
  private static final String MOCK_OLD_CATEGORY_ORDERHINT = "123";
  private static final Category MOCK_OLD_CATEGORY =
      getMockCategory(
          LOCALE,
          MOCK_OLD_CATEGORY_NAME,
          MOCK_OLD_CATEGORY_SLUG,
          MOCK_OLD_CATEGORY_KEY,
          MOCK_OLD_CATEGORY_EXTERNAL_ID,
          MOCK_OLD_CATEGORY_DESCRIPTION,
          MOCK_OLD_CATEGORY_META_DESCRIPTION,
          MOCK_OLD_CATEGORY_META_TITLE,
          MOCK_OLD_CATEGORY_META_KEYWORDS,
          MOCK_OLD_CATEGORY_ORDERHINT,
          MOCK_OLD_CATEGORY_PARENT_ID);

  @Test
  void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

    final CategoryChangeNameAction changeNameUpdateAction =
        (CategoryChangeNameAction)
            buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
    assertThat(changeNameUpdateAction.getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
  }

  @Test
  void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameName = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameName.getName())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

    final Optional<CategoryUpdateAction> changeNameUpdateAction =
        buildChangeNameUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameName);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

    final CategoryChangeSlugAction changeSlugUpdateAction =
        (CategoryChangeSlugAction)
            buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
    assertThat(changeSlugUpdateAction.getSlug()).isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
  }

  @Test
  void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameSlug = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameSlug.getSlug())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_SLUG));

    final Optional<CategoryUpdateAction> changeSlugUpdateAction =
        buildChangeSlugUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameSlug);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction).isNotPresent();
  }

  @Test
  void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getDescription())
        .thenReturn(LocalizedString.of(LOCALE, "newDescription"));

    final CategorySetDescriptionAction setDescriptionUpdateAction =
        (CategorySetDescriptionAction)
            buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(setDescriptionUpdateAction.getDescription())
        .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
  }

  @Test
  void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameDescription = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameDescription.getDescription())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_DESCRIPTION));

    final Optional<CategoryUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameDescription);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction).isNotPresent();
  }

  @Test
  void buildSetDescriptionUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
    when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getDescription()).thenReturn(null);

    final ArrayList<Object> callBackResponse = new ArrayList<>();

    final CategorySetDescriptionAction setDescriptionUpdateAction =
        (CategorySetDescriptionAction)
            buildSetDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategory).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(setDescriptionUpdateAction.getDescription()).isEqualTo(null);
    assertThat(callBackResponse).isEmpty();
  }

  @Test
  void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getParent())
        .thenReturn(CategoryResourceIdentifierBuilder.of().id("2").build());

    final CategoryChangeParentAction changeParentUpdateAction = // parent id is 1.
        (CategoryChangeParentAction)
            buildChangeParentUpdateAction(
                    MOCK_OLD_CATEGORY, newCategoryDraft, CATEGORY_SYNC_OPTIONS)
                .orElse(null);

    assertThat(changeParentUpdateAction).isNotNull();
    assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
    assertThat(changeParentUpdateAction.getParent())
        .isEqualTo(CategoryResourceIdentifierBuilder.of().id("2").build());
  }

  @Test
  void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getParent())
        .thenReturn(
            CategoryResourceIdentifierBuilder.of().id(oldCategory.getParent().getId()).build());

    final Optional<CategoryUpdateAction> changeParentUpdateAction =
        buildChangeParentUpdateAction(oldCategory, newCategory, CATEGORY_SYNC_OPTIONS);

    assertThat(changeParentUpdateAction).isNotNull();
    assertThat(changeParentUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeParentUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
    when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getParent()).thenReturn(null);

    final ArrayList<Object> callBackResponse = new ArrayList<>();
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        updateActionWarningCallBack =
            (exception, newResource, oldResource) -> callBackResponse.add(exception.getMessage());

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .warningCallback(updateActionWarningCallBack)
            .build();

    final Optional<CategoryUpdateAction> changeParentUpdateAction =
        buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategory, categorySyncOptions);

    assertThat(changeParentUpdateAction).isNotNull();
    assertThat(changeParentUpdateAction).isNotPresent();
    assertThat(callBackResponse).hasSize(1);
    assertThat(callBackResponse.get(0))
        .isEqualTo("Cannot unset 'parent' field of category with id 'oldCatId'.");
  }

  @Test
  void
      buildChangeParentUpdateAction_WithBothNullValues_ShouldNotBuildUpdateActionAndNotCallCallback() {
    when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
    when(MOCK_OLD_CATEGORY.getParent()).thenReturn(null);
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getParent()).thenReturn(null);

    final ArrayList<Object> callBackResponse = new ArrayList<>();
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        updateActionWarningCallBack =
            (exception, newResource, oldResource) -> callBackResponse.add(exception.getMessage());

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .warningCallback(updateActionWarningCallBack)
            .build();

    final Optional<CategoryUpdateAction> changeParentUpdateAction =
        buildChangeParentUpdateAction(MOCK_OLD_CATEGORY, newCategory, categorySyncOptions);

    assertThat(changeParentUpdateAction).isNotNull();
    assertThat(changeParentUpdateAction).isNotPresent();
    assertThat(callBackResponse).isEmpty();
  }

  @Test
  void buildChangeOrderHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getOrderHint()).thenReturn("099");

    final CategoryChangeOrderHintAction changeOrderHintUpdateAction =
        (CategoryChangeOrderHintAction)
            buildChangeOrderHintUpdateAction(
                    MOCK_OLD_CATEGORY, newCategoryDraft, CATEGORY_SYNC_OPTIONS)
                .orElse(null);

    assertThat(changeOrderHintUpdateAction).isNotNull();
    assertThat(changeOrderHintUpdateAction.getAction()).isEqualTo("changeOrderHint");
    assertThat(changeOrderHintUpdateAction.getOrderHint()).isEqualTo("099");
  }

  @Test
  void buildChangeOrderHintUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameOrderHint = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameOrderHint.getOrderHint()).thenReturn(MOCK_OLD_CATEGORY_ORDERHINT);

    final Optional<CategoryUpdateAction> changeOrderHintUpdateAction =
        buildChangeOrderHintUpdateAction(
            MOCK_OLD_CATEGORY, newCategoryDraftWithSameOrderHint, CATEGORY_SYNC_OPTIONS);

    assertThat(changeOrderHintUpdateAction).isNotNull();
    assertThat(changeOrderHintUpdateAction).isNotPresent();
  }

  @Test
  void buildChangeOrderHintUpdateAction_WithNullValues_ShouldNotBuildUpdateActionAndCallCallback() {
    when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getOrderHint()).thenReturn(null);

    final ArrayList<Object> callBackResponse = new ArrayList<>();
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        updateActionWarningCallBack =
            (exception, newResource, oldResource) -> callBackResponse.add(exception.getMessage());

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .warningCallback(updateActionWarningCallBack)
            .build();

    final Optional<CategoryUpdateAction> changeOrderHintUpdateAction =
        buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategory, categorySyncOptions);

    assertThat(changeOrderHintUpdateAction).isNotNull();
    assertThat(changeOrderHintUpdateAction).isNotPresent();
    assertThat(callBackResponse).hasSize(1);
    assertThat(callBackResponse.get(0))
        .isEqualTo("Cannot unset 'orderHint' field of category with id 'oldCatId'.");
  }

  @Test
  void
      buildChangeOrderHintUpdateAction_WithBothNullValues_ShouldNotBuildUpdateActionAndNotCallCallback() {
    when(MOCK_OLD_CATEGORY.getId()).thenReturn("oldCatId");
    when(MOCK_OLD_CATEGORY.getOrderHint()).thenReturn(null);
    final CategoryDraft newCategory = mock(CategoryDraft.class);
    when(newCategory.getOrderHint()).thenReturn(null);

    final ArrayList<Object> callBackResponse = new ArrayList<>();
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        updateActionWarningCallBack =
            (exception, newResource, oldResource) -> callBackResponse.add(exception.getMessage());

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_CLIENT)
            .warningCallback(updateActionWarningCallBack)
            .build();

    final Optional<CategoryUpdateAction> changeOrderHintUpdateAction =
        buildChangeOrderHintUpdateAction(MOCK_OLD_CATEGORY, newCategory, categorySyncOptions);

    assertThat(changeOrderHintUpdateAction).isNotNull();
    assertThat(changeOrderHintUpdateAction).isNotPresent();
    assertThat(callBackResponse).isEmpty();
  }

  @Test
  void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(LOCALE, "newMetaTitle"));

    final CategorySetMetaTitleAction setMetaTitleUpdateAction =
        (CategorySetMetaTitleAction)
            buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
    assertThat(setMetaTitleUpdateAction.getMetaTitle())
        .isEqualTo(LocalizedString.of(LOCALE, "newMetaTitle"));
  }

  @Test
  void buildSetMetaTitleUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaTitle()).thenReturn(null);

    final CategorySetMetaTitleAction setMetaTitleUpdateAction =
        (CategorySetMetaTitleAction)
            buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
    assertThat(setMetaTitleUpdateAction.getMetaTitle()).isEqualTo(null);
  }

  @Test
  void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameTitle = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameTitle.getMetaTitle())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_TITLE));

    final Optional<CategoryUpdateAction> setMetaTitleUpdateAction =
        buildSetMetaTitleUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameTitle);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction).isNotPresent();
  }

  @Test
  void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(LOCALE, "newMetaKW"));

    final CategorySetMetaKeywordsAction setMetaKeywordsUpdateAction =
        (CategorySetMetaKeywordsAction)
            buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
    assertThat(setMetaKeywordsUpdateAction.getMetaKeywords())
        .isEqualTo(LocalizedString.of(LOCALE, "newMetaKW"));
  }

  @Test
  void buildSetMetaKeywordsUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaKeywords()).thenReturn(null);

    final CategorySetMetaKeywordsAction setMetaKeywordsUpdateAction =
        (CategorySetMetaKeywordsAction)
            buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
    assertThat(setMetaKeywordsUpdateAction.getMetaKeywords()).isNull();
  }

  @Test
  void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameMetaKeywords = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameMetaKeywords.getMetaKeywords())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_KEYWORDS));

    final Optional<CategoryUpdateAction> setMetaKeywordsUpdateAction =
        buildSetMetaKeywordsUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaKeywords);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction).isNotPresent();
  }

  @Test
  void buildSetMetaDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaDescription())
        .thenReturn(LocalizedString.of(LOCALE, "newMetaDesc"));

    final CategorySetMetaDescriptionAction setMetaDescriptionUpdateAction =
        (CategorySetMetaDescriptionAction)
            buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
    assertThat(setMetaDescriptionUpdateAction.getMetaDescription())
        .isEqualTo(LocalizedString.of(LOCALE, "newMetaDesc"));
  }

  @Test
  void buildSetMetaDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getMetaDescription()).thenReturn(null);

    final CategorySetMetaDescriptionAction setMetaDescriptionUpdateAction =
        (CategorySetMetaDescriptionAction)
            buildSetMetaDescriptionUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
    assertThat(setMetaDescriptionUpdateAction.getMetaDescription()).isNull();
  }

  @Test
  void buildSetMetaDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryDraft newCategoryDraftWithSameMetaDescription = mock(CategoryDraft.class);
    when(newCategoryDraftWithSameMetaDescription.getMetaDescription())
        .thenReturn(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_META_DESCRIPTION));

    final Optional<CategoryUpdateAction> setMetaDescriptionUpdateAction =
        buildSetMetaDescriptionUpdateAction(
            MOCK_OLD_CATEGORY, newCategoryDraftWithSameMetaDescription);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction).isNotPresent();
  }

  @Test
  void buildSetExternalIdUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getExternalId()).thenReturn("newExternalId");

    final CategorySetExternalIdAction setExternalIdUpdateAction =
        (CategorySetExternalIdAction)
            buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setExternalIdUpdateAction).isNotNull();
    assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
    assertThat(setExternalIdUpdateAction.getExternalId()).isEqualTo("newExternalId");
  }

  @Test
  void buildSetExternalIdUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getExternalId()).thenReturn(null);

    final CategorySetExternalIdAction setExternalIdUpdateAction =
        (CategorySetExternalIdAction)
            buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft).orElse(null);

    assertThat(setExternalIdUpdateAction).isNotNull();
    assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
    assertThat(setExternalIdUpdateAction.getExternalId()).isNull();
  }

  @Test
  void buildSetExternalIdUpdateAction_WithSameValues_ShouldNotBuildUpdateActions() {
    final CategoryDraft newCategoryDraft = mock(CategoryDraft.class);
    when(newCategoryDraft.getExternalId()).thenReturn(MOCK_OLD_CATEGORY_EXTERNAL_ID);

    final Optional<CategoryUpdateAction> setExternalIdUpdateAction =
        buildSetExternalIdUpdateAction(MOCK_OLD_CATEGORY, newCategoryDraft);

    assertThat(setExternalIdUpdateAction).isNotNull();
    assertThat(setExternalIdUpdateAction).isNotPresent();
  }
}
