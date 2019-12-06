package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.categories.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetAssetSources;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSource;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildActions;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildChangeAssetNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetTagsUpdateAction;
import static io.sphere.sdk.models.LocalizedString.empty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryAssetUpdateActionUtilsTest {
    private static final CategorySyncOptions SYNC_OPTIONS = CategorySyncOptionsBuilder
        .of(mock(SphereClient.class)).build();

    @Test
    void buildActions_WithDifferentValues_ShouldBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");
        final Set<String> newTags = new HashSet<>();
        oldTags.add("newTag");

        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());
        final List<AssetSource> newAssetSources = singletonList(AssetSourceBuilder.ofUri("newUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);
        when(oldAsset.getTags()).thenReturn(oldTags);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(newAssetSources, newName)
                                                          .tags(newTags)
                                                          .custom(newCustomFieldsDraft)
                                                          .build();


        final List<UpdateAction<Category>> updateActions = buildActions(oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).hasSize(5);
        assertThat(updateActions).containsExactlyInAnyOrder(
            ChangeAssetName.ofKey(null, newName),
            SetAssetTags.ofKey(null, newTags),
            SetAssetSources.ofKey(null, newAssetSources),
            SetAssetCustomField
                .ofJsonValueWithKey(null, "invisibleInShop", newCustomFieldsMap.get("invisibleInShop")),
            SetAssetCustomField
                .ofJsonValueWithKey(null, "backgroundColor", newCustomFieldsMap.get("backgroundColor"))
        );
    }

    @Test
    void buildActions_WithIdenticalValues_ShouldBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");

        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);
        when(oldAsset.getTags()).thenReturn(oldTags);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(oldAsset).build();


        final List<UpdateAction<Category>> updateActions = buildActions(oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAssetNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), newName).build();

        final UpdateAction<Category> changeNameUpdateAction = buildChangeAssetNameUpdateAction(oldAsset, newAssetDraft)
            .orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isInstanceOf(ChangeAssetName.class);
        assertThat(((ChangeAssetName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    void buildChangeAssetNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), oldName).build();

        final Optional<UpdateAction<Category>> changeNameUpdateAction =
            buildChangeAssetNameUpdateAction(oldAsset, newAssetDraft);

        assertThat(changeNameUpdateAction).isEmpty();
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");
        final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(oldDesc);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(newDesc).build();

        final UpdateAction<Category> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft).orElse(null);

        assertThat(setAssetDescription).isNotNull();
        assertThat(setAssetDescription).isInstanceOf(SetAssetDescription.class);
        assertThat(((SetAssetDescription) setAssetDescription).getDescription()).isEqualTo(newDesc);
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(oldDesc);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(oldDesc).build();

        final Optional<UpdateAction<Category>> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft);

        assertThat(setAssetDescription).isEmpty();
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithNullOldValue_ShouldBuildUpdateAction() {
        final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(null);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(newDesc).build();

        final UpdateAction<Category> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(oldAsset, newAssetDraft).orElse(null);

        assertThat(setAssetDescription).isNotNull();
        assertThat(setAssetDescription).isInstanceOf(SetAssetDescription.class);
        assertThat(((SetAssetDescription) setAssetDescription).getDescription()).isEqualTo(newDesc);
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");
        final Set<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(oldTags);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(newTags).build();

        final UpdateAction<Category> productUpdateAction =
            buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetTags.class);
        assertThat(((SetAssetTags) productUpdateAction).getTags()).isEqualTo(newTags);
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(oldTags);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(oldTags).build();


        final Optional<UpdateAction<Category>> productUpdateAction =
            buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft);

        assertThat(productUpdateAction).isEmpty();
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithNullOldValues_ShouldBuildUpdateAction() {
        final Set<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(null);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(newTags).build();

        final UpdateAction<Category> productUpdateAction =
            buildSetAssetTagsUpdateAction(oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetTags.class);
        assertThat(((SetAssetTags) productUpdateAction).getTags()).isEqualTo(newTags);
    }

    @Test
    void buildSetAssetSourcesUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());
        final List<AssetSource> newAssetSources = singletonList(AssetSourceBuilder.ofUri("newUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .sources(newAssetSources).build();

        final UpdateAction<Category> productUpdateAction =
            buildSetAssetSourcesUpdateAction(oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetSources.class);
        assertThat(((SetAssetSources) productUpdateAction).getSources()).isEqualTo(newAssetSources);
    }

    @Test
    void buildSetAssetSourcesUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .sources(oldAssetSources).build();

        final Optional<UpdateAction<Category>> productUpdateAction =
            buildSetAssetSourcesUpdateAction(oldAsset, newAssetDraft);

        assertThat(productUpdateAction).isEmpty();
    }

    @Test
    void buildCustomUpdateActions_WithSameValues_ShouldNotBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", oldCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .custom(newCustomFieldsDraft)
                                                          .build();

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildCustomUpdateActions_WithDifferentValues_ShouldBuildUpdateAction() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId("1"));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .custom(newCustomFieldsDraft)
                                                          .build();

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).hasSize(2);
    }

    @Test
    void buildCustomUpdateActions_WithNullOldValues_ShouldBuildUpdateAction() {

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("1", newCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(null);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .custom(newCustomFieldsDraft)
                                                          .build();

        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            SetAssetCustomType.ofKey(newAssetDraft.getKey(), newCustomFieldsDraft)
        );
    }

    @Test
    void buildCustomUpdateActions_WithBadCustomFieldData_ShouldNotBuildUpdateActionAndTriggerErrorCallback() {
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        newCustomFieldsMap.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("es", "rojo"));


        final CustomFields oldCustomFields = mock(CustomFields.class);
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(""));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
            CustomFieldsDraft.ofTypeIdAndJson("", newCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .custom(newCustomFieldsDraft)
                                                          .build();

        final List<String> errors = new ArrayList<>();

        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                          .errorCallback((errorMessage, throwable) ->
                                                                              errors.add(errorMessage))
                                                                          .build();


        final List<UpdateAction<Category>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, syncOptions);

        assertThat(updateActions).isEmpty();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0))
            .isEqualTo(format("Failed to build custom fields update actions on the asset with id '%s'."
            + " Reason: Custom type ids are not set for both the old and new asset.", oldAsset.getId()));
    }
}
