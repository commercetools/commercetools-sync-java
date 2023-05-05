package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.api.models.common.LocalizedString.empty;
import static com.commercetools.sync.sdk2.categories.utils.CategoryAssetUpdateActionUtils.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryChangeAssetNameAction;
import com.commercetools.api.models.category.CategoryChangeAssetNameActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategorySetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetCustomTypeActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetDescriptionAction;
import com.commercetools.api.models.category.CategorySetAssetSourcesAction;
import com.commercetools.api.models.category.CategorySetAssetSourcesActionBuilder;
import com.commercetools.api.models.category.CategorySetAssetTagsAction;
import com.commercetools.api.models.category.CategorySetAssetTagsActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSource;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CategoryAssetUpdateActionUtilsTest {
  private static final CategorySyncOptions SYNC_OPTIONS =
      CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  CategoryDraft mainCategoryDraft = mock(CategoryDraft.class);

  @Test
  void buildActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
    final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", false);
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainer.builder().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(FieldContainer.builder().values(newCustomFieldsMap).build())
            .build();

    final List<String> oldTags = Collections.singletonList("oldTag");
    final List<String> newTags = Collections.singletonList("newTag");

    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());
    final List<AssetSource> newAssetSources =
        singletonList(AssetSourceBuilder.of().uri("newUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);
    when(oldAsset.getTags()).thenReturn(oldTags);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(newAssetSources)
            .name(newName)
            .tags(newTags)
            .custom(newCustomFieldsDraft)
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildActions(mainCategoryDraft, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(5);
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryChangeAssetNameActionBuilder.of().assetKey(null).name(newName).build(),
            CategorySetAssetTagsActionBuilder.of().assetKey(null).tags(newTags).build(),
            CategorySetAssetSourcesActionBuilder.of()
                .assetKey(null)
                .sources(newAssetSources)
                .build(),
            CategorySetAssetCustomFieldActionBuilder.of()
                .assetKey(null)
                .name("invisibleInShop")
                .value(newCustomFieldsMap.get("invisibleInShop"))
                .build(),
            CategorySetAssetCustomFieldActionBuilder.of()
                .assetKey(null)
                .name("backgroundColor")
                .value(newCustomFieldsMap.get("backgroundColor"))
                .build());
  }

  @Test
  void buildActions_WithIdenticalValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final CustomFields oldCustomFields =
        CustomFieldsBuilder.of()
            .type(TypeReferenceBuilder.of().id("1").build())
            .fields(FieldContainer.builder().values(oldCustomFieldsMap).build())
            .build();

    final List<String> oldTags = Collections.singletonList("oldTag");

    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);
    when(oldAsset.getTags()).thenReturn(oldTags);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(oldAssetSources)
            .name(oldName)
            .tags(oldTags)
            .custom(oldCustomFields.toDraft())
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildActions(mainCategoryDraft, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildChangeAssetNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
    final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(newName).build();

    final CategoryUpdateAction changeNameUpdateAction =
        buildChangeAssetNameUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction).isInstanceOf(CategoryChangeAssetNameAction.class);
    assertThat(((CategoryChangeAssetNameAction) changeNameUpdateAction).getName())
        .isEqualTo(newName);
  }

  @Test
  void buildChangeAssetNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(oldName).build();

    final Optional<CategoryUpdateAction> changeNameUpdateAction =
        buildChangeAssetNameUpdateAction(oldAsset, newAssetDraft);

    assertThat(changeNameUpdateAction).isEmpty();
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");
    final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(oldDesc);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).description(newDesc).build();

    final CategoryUpdateAction setAssetDescription =
        buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(setAssetDescription).isNotNull();
    assertThat(setAssetDescription).isInstanceOf(CategorySetAssetDescriptionAction.class);
    assertThat(((CategorySetAssetDescriptionAction) setAssetDescription).getDescription())
        .isEqualTo(newDesc);
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(oldDesc);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).description(oldDesc).build();

    final Optional<CategoryUpdateAction> setAssetDescription =
        buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft);

    assertThat(setAssetDescription).isEmpty();
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithNullOldValue_ShouldBuildUpdateAction() {
    final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).description(newDesc).build();

    final CategoryUpdateAction setAssetDescription =
        buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(setAssetDescription).isNotNull();
    assertThat(setAssetDescription).isInstanceOf(CategorySetAssetDescriptionAction.class);
    assertThat(((CategorySetAssetDescriptionAction) setAssetDescription).getDescription())
        .isEqualTo(newDesc);
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final List<String> oldTags = Collections.singletonList("oldTag");
    final List<String> newTags = Collections.singletonList("newTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(oldTags);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).tags(newTags).build();

    final CategoryUpdateAction productUpdateAction =
        buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(CategorySetAssetTagsAction.class);
    assertThat(((CategorySetAssetTagsAction) productUpdateAction).getTags()).isEqualTo(newTags);
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final List<String> oldTags = Collections.singletonList("newTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(oldTags);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).tags(oldTags).build();

    final Optional<CategoryUpdateAction> productUpdateAction =
        buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft);

    assertThat(productUpdateAction).isEmpty();
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithNullOldValues_ShouldBuildUpdateAction() {
    final List<String> newTags = Collections.singletonList("newTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).tags(newTags).build();

    final CategoryUpdateAction productUpdateAction =
        buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(CategorySetAssetTagsAction.class);
    assertThat(((CategorySetAssetTagsAction) productUpdateAction).getTags()).isEqualTo(newTags);
  }

  @Test
  void buildSetAssetSourcesUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());
    final List<AssetSource> newAssetSources =
        singletonList(AssetSourceBuilder.of().uri("newUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).sources(newAssetSources).build();

    final CategoryUpdateAction productUpdateAction =
        buildSetAssetSourcesUpdateAction(oldAsset, newAssetDraft).orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(CategorySetAssetSourcesAction.class);
    assertThat(((CategorySetAssetSourcesAction) productUpdateAction).getSources())
        .isEqualTo(newAssetSources);
  }

  @Test
  void buildSetAssetSourcesUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(empty()).sources(oldAssetSources).build();

    final Optional<CategoryUpdateAction> productUpdateAction =
        buildSetAssetSourcesUpdateAction(oldAsset, newAssetDraft);

    assertThat(productUpdateAction).isEmpty();
  }

  @Test
  void buildCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainer.builder().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(FieldContainer.builder().values(oldCustomFieldsMap).build())
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(empty())
            .custom(newCustomFieldsDraft)
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildCustomUpdateActions(mainCategoryDraft, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", false);
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainer.builder().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(FieldContainer.builder().values(newCustomFieldsMap).build())
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(empty())
            .custom(newCustomFieldsDraft)
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildCustomUpdateActions(mainCategoryDraft, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(2);
  }

  @Test
  void buildCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {
    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", false);
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
            .fields(FieldContainer.builder().values(newCustomFieldsMap).build())
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(empty())
            .custom(newCustomFieldsDraft)
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildCustomUpdateActions(mainCategoryDraft, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            CategorySetAssetCustomTypeActionBuilder.of()
                .assetKey(newAssetDraft.getKey())
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("1"))
                .fields(FieldContainer.builder().values(newCustomFieldsMap).build())
                .build());
  }

  @Test
  void
      buildCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", false);
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("").build());
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainer.builder().values(oldCustomFieldsMap).build());

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(""))
            .fields(FieldContainer.builder().values(newCustomFieldsMap).build())
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(empty())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildCustomUpdateActions(mainCategoryDraft, oldAsset, newAssetDraft, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the asset with id '%s'."
                    + " Reason: Custom type ids are not set for both the old and new asset.",
                oldAsset.getId()));
  }
}
