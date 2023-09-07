package com.commercetools.sync.products.utils;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSource;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductChangeAssetNameAction;
import com.commercetools.api.models.product.ProductChangeAssetNameActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldAction;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetCustomTypeActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetDescriptionAction;
import com.commercetools.api.models.product.ProductSetAssetSourcesAction;
import com.commercetools.api.models.product.ProductSetAssetSourcesActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetTagsAction;
import com.commercetools.api.models.product.ProductSetAssetTagsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductVariantAssetUpdateActionUtilsTest {
  private static final ProductSyncOptions SYNC_OPTIONS =
      ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
  final ProductDraft productDraft = mock(ProductDraft.class);

  @Test
  void buildActions_WithDifferentValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
    final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", true);
    oldCustomFieldsMap.put("backgroundColor", Map.of("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", false);
    newCustomFieldsMap.put("backgroundColor", Map.of("es", "rojo"));

    final FieldContainer oldFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id("1").build().toResourceIdentifier())
            .fields(newFieldContainer)
            .build();

    final List<String> oldTags = new ArrayList<>();
    oldTags.add("oldTag");
    final List<String> newTags = new ArrayList<>();
    oldTags.add("newTag");

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

    ProductDraft productDraft = mock(ProductDraft.class);
    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildActions(
            productDraft, 1L, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(5);
    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductChangeAssetNameActionBuilder.of()
                .assetKey(null)
                .variantId(1L)
                .name(newName)
                .staged(true)
                .build(),
            ProductSetAssetTagsActionBuilder.of()
                .variantId(1L)
                .assetKey(null)
                .tags(newTags)
                .staged(true)
                .build(),
            ProductSetAssetSourcesActionBuilder.of()
                .variantId(1L)
                .assetKey(null)
                .sources(newAssetSources)
                .staged(true)
                .build(),
            ProductSetAssetCustomFieldActionBuilder.of()
                .variantId(1L)
                .assetKey(null)
                .name("invisibleInShop")
                .value(newCustomFieldsMap.get("invisibleInShop"))
                .staged(true)
                .build(),
            ProductSetAssetCustomFieldActionBuilder.of()
                .variantId(1L)
                .assetKey(null)
                .name("backgroundColor")
                .value(newCustomFieldsMap.get("backgroundColor"))
                .staged(true)
                .build());
  }

  @Test
  void buildActions_WithIdenticalValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
    final FieldContainer oldFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReference.builder().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldFieldContainer);

    final List<String> oldTags = new ArrayList<>();
    oldTags.add("oldTag");

    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);
    when(oldAsset.getTags()).thenReturn(oldTags);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Optional<CustomFields> oldAssetCustomOptional = Optional.ofNullable(oldAsset.getCustom());
    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .key(oldAsset.getKey())
            .name(oldAsset.getName())
            .description(oldAsset.getDescription())
            .sources(oldAsset.getSources())
            .tags(oldAsset.getTags())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        oldAssetCustomOptional
                            .map(customFields -> customFields.getType().toResourceIdentifier())
                            .orElse(null))
                    .fields(
                        oldAssetCustomOptional
                            .map(customFields -> customFields.getFields())
                            .orElse(null))
                    .build())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildActions(
            productDraft, 1L, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildChangeAssetNameUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
    final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(newName).build();

    final ProductUpdateAction changeNameUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildChangeAssetNameUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction).isInstanceOf(ProductChangeAssetNameAction.class);
    assertThat(((ProductChangeAssetNameAction) changeNameUpdateAction).getName())
        .isEqualTo(newName);
  }

  @Test
  void buildChangeAssetNameUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getName()).thenReturn(oldName);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(oldName).build();

    final Optional<ProductUpdateAction> changeNameUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildChangeAssetNameUpdateAction(
            1L, oldAsset, newAssetDraft);

    assertThat(changeNameUpdateAction).isEmpty();
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");
    final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(oldDesc);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .description(newDesc)
            .build();

    final ProductUpdateAction setAssetDescription =
        ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(setAssetDescription).isNotNull();
    assertThat(setAssetDescription).isInstanceOf(ProductSetAssetDescriptionAction.class);
    assertThat(((ProductSetAssetDescriptionAction) setAssetDescription).getDescription())
        .isEqualTo(newDesc);
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithNullOldStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .description(newDesc)
            .build();

    final ProductUpdateAction setAssetDescription =
        ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(setAssetDescription).isNotNull();
    assertThat(setAssetDescription).isInstanceOf(ProductSetAssetDescriptionAction.class);
    assertThat(((ProductSetAssetDescriptionAction) setAssetDescription).getDescription())
        .isEqualTo(newDesc);
  }

  @Test
  void buildSetAssetDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getDescription()).thenReturn(oldDesc);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .description(oldDesc)
            .build();

    final Optional<ProductUpdateAction> setAssetDescription =
        ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction(
            1L, oldAsset, newAssetDraft);

    assertThat(setAssetDescription).isEmpty();
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final List<String> oldTags = new ArrayList<>();
    oldTags.add("oldTag");
    final List<String> newTags = new ArrayList<>();
    newTags.add("newTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(oldTags);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .tags(newTags)
            .build();

    final ProductUpdateAction productUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(ProductSetAssetTagsAction.class);
    assertThat(((ProductSetAssetTagsAction) productUpdateAction).getTags()).isEqualTo(newTags);
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithNullOldStagedValues_ShouldBuildUpdateAction() {
    final List<String> newTags = new ArrayList<>();
    newTags.add("newTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .tags(newTags)
            .build();

    final ProductUpdateAction productUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(ProductSetAssetTagsAction.class);
    assertThat(((ProductSetAssetTagsAction) productUpdateAction).getTags()).isEqualTo(newTags);
  }

  @Test
  void buildSetAssetTagsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final List<String> oldTags = new ArrayList<>();
    oldTags.add("oldTag");

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getTags()).thenReturn(oldTags);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .tags(oldTags)
            .build();

    final Optional<ProductUpdateAction> productUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction(
            1L, oldAsset, newAssetDraft);

    assertThat(productUpdateAction).isEmpty();
  }

  @Test
  void buildSetAssetSourcesUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());
    final List<AssetSource> newAssetSources =
        singletonList(AssetSourceBuilder.of().uri("newUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().name(LocalizedString.of()).sources(newAssetSources).build();

    final ProductUpdateAction productUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction(
                1L, oldAsset, newAssetDraft)
            .orElse(null);

    assertThat(productUpdateAction).isNotNull();
    assertThat(productUpdateAction).isInstanceOf(ProductSetAssetSourcesAction.class);
    assertThat(((ProductSetAssetSourcesAction) productUpdateAction).getSources())
        .isEqualTo(newAssetSources);
  }

  @Test
  void buildSetAssetSourcesUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final List<AssetSource> oldAssetSources =
        singletonList(AssetSourceBuilder.of().uri("oldUri").build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getSources()).thenReturn(oldAssetSources);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().name(LocalizedString.of()).sources(oldAssetSources).build();

    final Optional<ProductUpdateAction> productUpdateAction =
        ProductVariantAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction(
            1L, oldAsset, newAssetDraft);

    assertThat(productUpdateAction).isEmpty();
  }

  @Test
  void buildCustomUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id("1").build().toResourceIdentifier())
            .fields(oldCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildCustomUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("1").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id("1").build().toResourceIdentifier())
            .fields(newCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions).hasSize(2);
  }

  @Test
  void buildCustomUpdateActions_WithNullOldStagedValues_ShouldBuildUpdateAction() {
    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();
    final TypeResourceIdentifier typeResourceIdentifier =
        TypeReferenceBuilder.of().id("1").build().toResourceIdentifier();
    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifier)
            .fields(newCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomTypeActionBuilder.of()
                .type(typeResourceIdentifier)
                .variantId(1L)
                .assetKey(newAssetDraft.getKey())
                .fields(newCustomFieldContainer)
                .staged(true)
                .build());
  }

  @Test
  void
      buildCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    newCustomFieldsMap.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id("").build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id("").build().toResourceIdentifier())
            .fields(newCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the asset with id '%s'."
                    + " Reason: Custom type ids are not set for both the old and new asset.",
                oldAsset.getId()));
  }

  @Test
  void buildCustomUpdateActions_WithNullValue_ShouldCorrectlyBuildAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    oldCustomFieldsMap.put(
        "setOfBooleans",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
    newCustomFieldsMap.put("setOfBooleans", null);

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id(typeId).build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id(typeId).build().toResourceIdentifier())
            .fields(newCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.add(exception.getMessage()))
            .build();
    // test
    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, syncOptions);

    // assertion
    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldAction.builder()
                .variantId(1L)
                .assetKey(oldAsset.getKey())
                .name("setOfBooleans")
                .staged(true)
                .build());
  }

  @Test
  void buildCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put(
        "setOfBooleans",
        JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.booleanNode(false)));

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", JsonNodeFactory.instance.nullNode());

    final FieldContainer oldCustomFieldContainer =
        FieldContainerBuilder.of().values(oldCustomFieldsMap).build();
    final FieldContainer newCustomFieldContainer =
        FieldContainerBuilder.of().values(newCustomFieldsMap).build();

    final CustomFields oldCustomFields = mock(CustomFields.class);
    final String typeId = UUID.randomUUID().toString();
    when(oldCustomFields.getType()).thenReturn(TypeReferenceBuilder.of().id(typeId).build());
    when(oldCustomFields.getFields()).thenReturn(oldCustomFieldContainer);

    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeReferenceBuilder.of().id(typeId).build().toResourceIdentifier())
            .fields(newCustomFieldContainer)
            .build();

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(LocalizedString.of())
            .custom(newCustomFieldsDraft)
            .build();

    final List<String> errors = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    errors.add(exception.getMessage()))
            .build();
    // test
    final List<ProductUpdateAction> updateActions =
        ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions(
            productDraft, 1L, oldAsset, newAssetDraft, syncOptions);

    // assertion
    assertThat(errors).isEmpty();
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .variantId(1L)
                .assetKey(oldAsset.getKey())
                .name("setOfBooleans")
                .staged(true)
                .build());
  }
}
