package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSource;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
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
import java.util.UUID;

import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildActions;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildChangeAssetNameUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction;
import static io.sphere.sdk.models.LocalizedString.empty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductVariantAssetUpdateActionUtilsTest {
    private static final ProductSyncOptions SYNC_OPTIONS = ProductSyncOptionsBuilder
        .of(mock(SphereClient.class)).build();
    final Product product = mock(Product.class);
    final ProductDraft productDraft = mock(ProductDraft.class);

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

        Product product = mock(Product.class);
        ProductDraft productDraft = mock(ProductDraft.class);
        final List<UpdateAction<Product>> updateActions = buildActions(product,productDraft,1,
            oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).hasSize(5);
        assertThat(updateActions).containsExactlyInAnyOrder(
            ChangeAssetName.ofAssetKeyAndVariantId(1, null, newName, true),
            SetAssetTags.ofVariantIdAndAssetKey(1, null, newTags, true),
            SetAssetSources.ofVariantIdAndAssetKey(1, null, newAssetSources, true),
            SetAssetCustomField
                .ofVariantIdUsingJsonAndAssetKey(1, null,
                    "invisibleInShop", newCustomFieldsMap.get("invisibleInShop"), true),
            SetAssetCustomField
                .ofVariantIdUsingJsonAndAssetKey(1, null, "backgroundColor",
                    newCustomFieldsMap.get("backgroundColor"), true)
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


        final List<UpdateAction<Product>> updateActions = buildActions(product, productDraft,1, oldAsset,
            newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAssetNameUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), newName).build();

        final UpdateAction<Product> changeNameUpdateAction =
            buildChangeAssetNameUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isInstanceOf(ChangeAssetName.class);
        assertThat(((ChangeAssetName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    void buildChangeAssetNameUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), oldName).build();

        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            buildChangeAssetNameUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(changeNameUpdateAction).isEmpty();
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");
        final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(oldDesc);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(newDesc).build();

        final UpdateAction<Product> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(setAssetDescription).isNotNull();
        assertThat(setAssetDescription).isInstanceOf(SetAssetDescription.class);
        assertThat(((SetAssetDescription) setAssetDescription).getDescription()).isEqualTo(newDesc);
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithNullOldStagedValues_ShouldBuildUpdateAction() {
        final LocalizedString newDesc = LocalizedString.of(Locale.GERMAN, "newDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(null);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(newDesc).build();

        final UpdateAction<Product> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(setAssetDescription).isNotNull();
        assertThat(setAssetDescription).isInstanceOf(SetAssetDescription.class);
        assertThat(((SetAssetDescription) setAssetDescription).getDescription()).isEqualTo(newDesc);
    }

    @Test
    void buildSetAssetDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldDesc = LocalizedString.of(Locale.GERMAN, "oldDesc");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getDescription()).thenReturn(oldDesc);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .description(oldDesc).build();

        final Optional<UpdateAction<Product>> setAssetDescription =
            buildSetAssetDescriptionUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(setAssetDescription).isEmpty();
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");
        final Set<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(oldTags);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(newTags).build();

        final UpdateAction<Product> productUpdateAction =
            buildSetAssetTagsUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetTags.class);
        assertThat(((SetAssetTags) productUpdateAction).getTags()).isEqualTo(newTags);
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithNullOldStagedValues_ShouldBuildUpdateAction() {
        final Set<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(null);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(newTags).build();

        final UpdateAction<Product> productUpdateAction =
            buildSetAssetTagsUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetTags.class);
        assertThat(((SetAssetTags) productUpdateAction).getTags()).isEqualTo(newTags);
    }

    @Test
    void buildSetAssetTagsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getTags()).thenReturn(oldTags);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .tags(oldTags).build();


        final Optional<UpdateAction<Product>> productUpdateAction =
            buildSetAssetTagsUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(productUpdateAction).isEmpty();
    }

    @Test
    void buildSetAssetSourcesUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());
        final List<AssetSource> newAssetSources = singletonList(AssetSourceBuilder.ofUri("newUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .sources(newAssetSources).build();

        final UpdateAction<Product> productUpdateAction =
            buildSetAssetSourcesUpdateAction(1, oldAsset, newAssetDraft).orElse(null);

        assertThat(productUpdateAction).isNotNull();
        assertThat(productUpdateAction).isInstanceOf(SetAssetSources.class);
        assertThat(((SetAssetSources) productUpdateAction).getSources()).isEqualTo(newAssetSources);
    }

    @Test
    void buildSetAssetSourcesUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .sources(oldAssetSources).build();

        final Optional<UpdateAction<Product>> productUpdateAction =
            buildSetAssetSourcesUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(productUpdateAction).isEmpty();
    }

    @Test
    void buildCustomUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
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

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildCustomUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
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

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).hasSize(2);
    }

    @Test
    void buildCustomUpdateActions_WithNullOldStagedValues_ShouldBuildUpdateAction() {
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

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            SetAssetCustomType.ofVariantIdAndAssetKey(1, newAssetDraft.getKey(), newCustomFieldsDraft, true)
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

        final ProductSyncOptions syncOptions =
            ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) ->
                    errors.add(exception.getMessage()))
                .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, syncOptions);

        assertThat(updateActions).isEmpty();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0))
            .isEqualTo(format("Failed to build custom fields update actions on the asset with id '%s'."
                + " Reason: Custom type ids are not set for both the old and new asset.", oldAsset.getId()));
    }

    @Test
    void buildCustomUpdateActions_WithNullValue_ShouldCorrectlyBuildAction() {
        // preparation
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFieldsMap.put("setOfBooleans", JsonNodeFactory
                .instance
                .arrayNode()
                .add(JsonNodeFactory.instance.booleanNode(false)));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFieldsMap.put("setOfBooleans", null);


        final CustomFields oldCustomFields = mock(CustomFields.class);
        final String typeId = UUID.randomUUID().toString();
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
                CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                .custom(newCustomFieldsDraft)
                .build();

        final List<String> errors = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, actions) ->
                        errors.add(exception.getMessage()))
                .build();
        // test
        final List<UpdateAction<Product>> updateActions =
                buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, syncOptions);

        // assertion
        assertThat(errors).isEmpty();
        assertThat(updateActions)
                .containsExactly(SetAssetCustomField
                        .ofVariantIdUsingJsonAndAssetKey(1, oldAsset.getKey(), "setOfBooleans", null, true));
    }

    @Test
    void buildCustomUpdateActions_WithNullJsonNodeValue_ShouldCorrectlyBuildAction() {
        // preparation
        final Map<String, JsonNode> oldCustomFieldsMap = new HashMap<>();
        oldCustomFieldsMap.put("setOfBooleans", JsonNodeFactory
                .instance
                .arrayNode()
                .add(JsonNodeFactory.instance.booleanNode(false)));

        final Map<String, JsonNode> newCustomFieldsMap = new HashMap<>();
        newCustomFieldsMap.put("setOfBooleans", JsonNodeFactory.instance.nullNode());


        final CustomFields oldCustomFields = mock(CustomFields.class);
        final String typeId = UUID.randomUUID().toString();
        when(oldCustomFields.getType()).thenReturn(Type.referenceOfId(typeId));
        when(oldCustomFields.getFieldsJsonMap()).thenReturn(oldCustomFieldsMap);

        final CustomFieldsDraft newCustomFieldsDraft =
                CustomFieldsDraft.ofTypeIdAndJson(typeId, newCustomFieldsMap);

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(oldCustomFields);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                .custom(newCustomFieldsDraft)
                .build();

        final List<String> errors = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, actions) ->
                        errors.add(exception.getMessage()))
                .build();
        // test
        final List<UpdateAction<Product>> updateActions =
                buildCustomUpdateActions(product, productDraft,1, oldAsset, newAssetDraft, syncOptions);

        // assertion
        assertThat(errors).isEmpty();
        assertThat(updateActions)
                .containsExactly(SetAssetCustomField
                        .ofVariantIdUsingJsonAndAssetKey(1, oldAsset.getKey(), "setOfBooleans",
                                null, true));
    }
}
