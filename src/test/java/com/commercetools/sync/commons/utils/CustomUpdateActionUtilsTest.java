package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNewOrModifiedCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildNonNullCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildRemovedCustomFieldsUpdateActions;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildSetCustomFieldsUpdateActions;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.types.CustomFieldsDraft.ofTypeKeyAndJson;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomUpdateActionUtilsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final CategorySyncOptions CATEGORY_SYNC_OPTIONS = CategorySyncOptionsBuilder.of(CTP_CLIENT).build();

    @Test
    public void buildCustomUpdateActions_WithNonNullCustomFieldsWithDifferentTypes_ShouldBuildUpdateActions() {
        final Asset oldAsset = mock(Asset.class);
        final CustomFields oldAssetCustomFields = mock(CustomFields.class);
        final Reference<Type> oldAssetCustomFieldsDraftTypeReference = Type.referenceOfId("2");
        when(oldAssetCustomFields.getType()).thenReturn(oldAssetCustomFieldsDraftTypeReference);
        when(oldAsset.getCustom()).thenReturn(oldAssetCustomFields);

        final AssetDraft newAssetDraft = mock(AssetDraft.class);
        final CustomFieldsDraft newAssetCustomFieldsDraft = mock(CustomFieldsDraft.class);

        final ResourceIdentifier<Type> typeResourceIdentifier = Type.referenceOfId("1");
        when(newAssetCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);
        when(newAssetDraft.getCustom()).thenReturn(newAssetCustomFieldsDraft);


        final List<UpdateAction<Product>> updateActions = buildCustomUpdateActions(oldAsset, newAssetDraft,
            Product.class, 10, Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        // Should set custom type of old asset.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(null);

        final CustomFieldsDraft newAssetCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> typeResourceIdentifier = Type.referenceOfId("1");
        when(newAssetCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                          .custom(newAssetCustomFieldsDraft)
                                                          .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, Product.class, 10,
                Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey,
                ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        // Should add custom type to old asset.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildCustomUpdateActions_WithNullOldCustomFieldsAndBlankNewTypeId_ShouldCallErrorCallBack() {
        final Asset oldAsset = mock(Asset.class);
        final String oldAssetId = "oldAssetId";
        when(oldAsset.getId()).thenReturn(oldAssetId);
        when(oldAsset.getCustom()).thenReturn(null);


        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                          .custom(ofTypeKeyAndJson("key", new HashMap<>()))
                                                          .build();

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .errorCallback(updateActionErrorCallBack)
                                                                               .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, Product.class, 10,
                Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey, productSyncOptions);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(0);
        assertThat(callBackResponses.get(0)).isEqualTo(format("Failed to build custom fields update actions on the "
            + "asset with id '%s'. Reason: New resource's custom type id is blank (empty/null).", oldAssetId));
        assertThat(callBackResponses.get(1)).isNull();
    }

    @Test
    public void buildCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
        final Asset oldAsset = mock(Asset.class);
        final String oldAssetId = "oldAssetId";
        when(oldAsset.getId()).thenReturn(oldAssetId);
        when(oldAsset.getCustom()).thenReturn(mock(CustomFields.class));

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                          .custom(null)
                                                          .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, Product.class, 10,
                Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey,
                ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        // Should remove custom type from old asset.
        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildCustomUpdateActions_WithNullIds_ShouldCallSyncOptionsCallBack() {
        final Reference<Type> assetCustomTypeReference = Type.referenceOfId(null);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(assetCustomTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(assetCustomTypeReference);

        // Mock old Asset
        final Asset oldAsset = mock(Asset.class);
        final String oldAssetId = "oldAssetId";
        when(oldAsset.getId()).thenReturn(oldAssetId);
        when(oldAsset.getCustom()).thenReturn(oldCustomFieldsMock);

        final AssetDraft newAssetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                          .custom(newCustomFieldsMock)
                                                          .build();




        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .errorCallback(updateActionErrorCallBack)
                                                                               .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, Product.class, 10,
                Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey, productSyncOptions);

        assertThat(callBackResponses).hasSize(2);
        assertThat(callBackResponses.get(0)).isEqualTo("Failed to build custom fields update actions on the asset"
            + " with id 'oldAssetId'. Reason: Custom type ids are not set for both the old and new asset.");
        assertThat((Exception) callBackResponses.get(1)).isInstanceOf(BuildUpdateActionException.class);
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
        final Asset oldAsset = mock(Asset.class);
        when(oldAsset.getCustom()).thenReturn(null);

        final AssetDraft newAssetDraft = mock(AssetDraft.class);
        when(newAssetDraft.getCustom()).thenReturn(null);

        // Mock custom options error callback
        final ArrayList<Object> callBackResponses = new ArrayList<>();
        final BiConsumer<String, Throwable> updateActionErrorCallBack = (errorMessage, exception) -> {
            callBackResponses.add(errorMessage);
            callBackResponses.add(exception);
        };

        // Mock sync options
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .errorCallback(updateActionErrorCallBack)
                                                                               .build();

        final List<UpdateAction<Product>> updateActions =
            buildCustomUpdateActions(oldAsset, newAssetDraft, Product.class, 10,
                Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey, productSyncOptions);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void
        buildNonNullCustomFieldsUpdateActions_WithSameCategoryTypesButDifferentFieldValues_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        final Map<String, JsonNode> oldCustomFieldsJsonMapMock = new HashMap<>();
        oldCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryAssetCustomTypeId"));
        when(oldCustomFieldsMock.getFieldsJsonMap()).thenReturn(oldCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        final Map<String, JsonNode> newCustomFieldsJsonMapMock = new HashMap<>();
        newCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("categoryAssetCustomTypeId"));
        when(newCustomFieldsMock.getFields()).thenReturn(newCustomFieldsJsonMapMock);


        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Asset.class), Category.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getKey,
            CATEGORY_SYNC_OPTIONS);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(
            io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeIds_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("assetCustomTypeId"));

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(ResourceIdentifier.ofId("newAssetCustomTypeId"));

        final List<UpdateAction<Category>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Asset.class), Category.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getKey,
            CATEGORY_SYNC_OPTIONS);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(
            io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullOldCategoryTypeId_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId(null));

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("priceCustomTypeId"));

        final List<UpdateAction<Product>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullNewCategoryTypeId_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(Type.referenceOfId("1"));

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(Type.referenceOfId(null));

        final List<UpdateAction<Product>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithSameIdsButNullNewCustomFields_ShouldBuildUpdateActions()
        throws BuildUpdateActionException {
        final Reference<Type> productPriceTypeReference = Type.referenceOfId("productPriceCustomTypeId");

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        final Map<String, JsonNode> oldCustomFieldsJsonMapMock = new HashMap<>();
        oldCustomFieldsJsonMapMock.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        when(oldCustomFieldsMock.getType()).thenReturn(productPriceTypeReference);
        when(oldCustomFieldsMock.getFieldsJsonMap()).thenReturn(oldCustomFieldsJsonMapMock);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(productPriceTypeReference);
        when(newCustomFieldsMock.getFields()).thenReturn(null);

        final List<UpdateAction<Product>> updateActions = buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
            newCustomFieldsMock, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildNonNullCustomFieldsUpdateActions_WithNullIds_ShouldThrowBuildUpdateActionException() {
        final Reference<Type> productPriceTypeReference = Type.referenceOfId(null);

        // Mock old CustomFields
        final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
        when(oldCustomFieldsMock.getType()).thenReturn(productPriceTypeReference);

        // Mock new CustomFieldsDraft
        final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
        when(newCustomFieldsMock.getType()).thenReturn(productPriceTypeReference);

        assertThatThrownBy(() ->
            buildNonNullCustomFieldsUpdateActions(oldCustomFieldsMock,
                newCustomFieldsMock, mock(Price.class), Product.class, 1, Price::getId,
                priceResource -> Price.resourceTypeId(), Price::getId,
                ProductSyncOptionsBuilder.of(CTP_CLIENT).build()))
            .isInstanceOf(BuildUpdateActionException.class)
            .hasMessageMatching("Custom type ids are not set for both the old and new product-price.");
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Product>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(2);
        final UpdateAction<Product> categoryUpdateAction = updateActions.get(0);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction).isInstanceOf(SetProductPriceCustomField.class);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithNoNewCustomFieldsInOldCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));
        newCustomFields.put("url", JsonNodeFactory.instance.objectNode().put("domain", "domain.com"));
        newCustomFields.put("size", JsonNodeFactory.instance.objectNode().put("cm", 34));

        final List<UpdateAction<Product>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(4);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithOldCustomFieldNotInNewFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Product>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomField.class);
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot"));

        final List<UpdateAction<Category>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Asset.class), Category.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getId, CATEGORY_SYNC_OPTIONS);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor",
            JsonNodeFactory.instance.objectNode().put("de", "rot").put("es", "rojo"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor",
            JsonNodeFactory.instance.objectNode().put("es", "rojo").put("de", "rot"));

        final List<UpdateAction<Category>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Asset.class), Category.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getId, CATEGORY_SYNC_OPTIONS);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode());

        final List<UpdateAction<Product>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Asset.class), Product.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();

        final Map<String, JsonNode> newCustomFields = new HashMap<>();

        final List<UpdateAction<Product>> updateActions = buildSetCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Asset.class), Product.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(), Asset::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNewOrModifiedCustomFields_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Product>> updateActions = buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomField.class);
    }

    @Test
    public void buildNewOrModifiedCustomFieldsUpdateActions_WithNoNewOrModifiedCustomFields_ShouldNotBuildActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Product>> updateActions = buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithRemovedCustomField_ShouldBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        oldCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final List<UpdateAction<Product>> updateActions = buildRemovedCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(SetProductPriceCustomField.class);
    }

    @Test
    public void buildRemovedCustomFieldsUpdateActions_WithNoRemovedCustomField_ShouldNotBuildUpdateActions() {
        final Map<String, JsonNode> oldCustomFields = new HashMap<>();
        oldCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));

        final Map<String, JsonNode> newCustomFields = new HashMap<>();
        newCustomFields.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(true));
        newCustomFields.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));

        final List<UpdateAction<Product>> updateActions = buildRemovedCustomFieldsUpdateActions(oldCustomFields,
            newCustomFields, mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(), Price::getId, ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).isEmpty();
    }
}
