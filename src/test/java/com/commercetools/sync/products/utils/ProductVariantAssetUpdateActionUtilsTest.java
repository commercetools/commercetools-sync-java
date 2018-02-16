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
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildChangeAssetNameUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction;
import static io.sphere.sdk.models.LocalizedString.empty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductVariantAssetUpdateActionUtilsTest {

    @Test
    public void buildChangeAssetNameUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
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
    public void buildChangeAssetNameUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldName = LocalizedString.of(Locale.GERMAN, "oldName");

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getName()).thenReturn(oldName);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), oldName).build();

        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            buildChangeAssetNameUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(changeNameUpdateAction).isEmpty();
    }

    @Test
    public void buildSetAssetDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
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
    public void buildSetAssetDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
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
    public void buildSetAssetTagsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final Set<String> oldTags = new HashSet<>();
        oldTags.add("oldTag");
        final Set<String> newTags = new HashSet<>();
        oldTags.add("newTag");

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
    public void buildSetAssetTagsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
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
    public void buildSetAssetSourcesUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
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
    public void buildSetAssetSourcesUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final List<AssetSource> oldAssetSources = singletonList(AssetSourceBuilder.ofUri("oldUri").build());

        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getSources()).thenReturn(oldAssetSources);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), empty())
                                                          .sources(oldAssetSources).build();

        final Optional<UpdateAction<Product>> productUpdateAction =
            buildSetAssetSourcesUpdateAction(1, oldAsset, newAssetDraft);

        assertThat(productUpdateAction).isEmpty();
    }

    //TODO: ADD CUSTOM FIELD TESTS

    @Test
    public void buildCustomUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
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

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();


        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldAsset, newAssetDraft, syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildCustomUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
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

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();


        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(1, oldAsset, newAssetDraft, syncOptions);

        assertThat(updateActions).hasSize(2);
    }
}
