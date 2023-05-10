package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategorySetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetBuilder;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldAction;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetCustomTypeAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.AssetCustomTypeAdapter;
import com.commercetools.sync.sdk2.commons.models.AssetDraftCustomTypeAdapter;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.sdk2.products.helpers.PriceCustomActionBuilder;
import com.commercetools.sync.sdk2.products.models.PriceCustomTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomUpdateActionUtilsTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private static final CategorySyncOptions CATEGORY_SYNC_OPTIONS =
      CategorySyncOptionsBuilder.of(CTP_CLIENT).build();
  final ProductDraft productDraftMock = mock(ProductDraft.class);

  @Test
  void
      buildCustomUpdateActions_WithNonNullCustomFieldsWithDifferentTypes_ShouldBuildUpdateActions() {
    final Asset oldAsset = mock(Asset.class);
    final TypeReference oldAssetCustomFieldsDraftTypeReference =
        TypeReferenceBuilder.of().id("2").build();
    final CustomFields oldAssetCustomFields =
        CustomFieldsBuilder.of()
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .type(oldAssetCustomFieldsDraftTypeReference)
            .build();
    when(oldAsset.getCustom()).thenReturn(oldAssetCustomFields);

    final AssetDraft newAssetDraft = mock(AssetDraft.class);
    final CustomFieldsDraft newAssetCustomFieldsDraft = mock(CustomFieldsDraft.class);

    final TypeResourceIdentifier typeResourceIdentifier =
        TypeResourceIdentifierBuilder.of().id("1").build();
    when(newAssetCustomFieldsDraft.getType()).thenReturn(typeResourceIdentifier);
    when(newAssetDraft.getCustom()).thenReturn(newAssetCustomFieldsDraft);

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(
            productDraftMock,
            AssetCustomTypeAdapter.of(oldAsset),
            AssetDraftCustomTypeAdapter.of(newAssetDraft),
            new AssetCustomActionBuilder(),
            10L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

    // Should set custom type of old asset.
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetAssetCustomTypeAction.class);
  }

  @Test
  void buildCustomUpdateActions_WithNullOldCustomFields_ShouldBuildUpdateActions() {
    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(null);

    final TypeResourceIdentifier typeResourceIdentifier =
        TypeResourceIdentifierBuilder.of().id("1").build();
    final CustomFieldsDraft newAssetCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifier)
            .fields(fieldContainerBuilder -> fieldContainerBuilder)
            .build();

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(ofEnglish("assetName"))
            .custom(newAssetCustomFieldsDraft)
            .build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(
            productDraftMock,
            AssetCustomTypeAdapter.of(oldAsset),
            AssetDraftCustomTypeAdapter.of(newAssetDraft),
            new AssetCustomActionBuilder(),
            10L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

    // Should add custom type to old asset.
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetAssetCustomTypeAction.class);
  }

  @Test
  void buildCustomUpdateActions_WithNullOldCustomFieldsAndBlankNewTypeId_ShouldCallErrorCallBack() {
    final Asset oldAsset = mock(Asset.class);

    final String oldAssetId = "oldAssetId";
    when(oldAsset.getId()).thenReturn(oldAssetId);
    when(oldAsset.getCustom()).thenReturn(null);

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(ofEnglish("assetName"))
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key("key"))
                        .fields(fieldContainerBuilder -> fieldContainerBuilder))
            .build();

    // Mock custom options error callback
    final ArrayList<Object> callBackResponses = new ArrayList<>();
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              callBackResponses.add(exception.getMessage());
              callBackResponses.add(exception.getCause());
            };

    // Mock sync options
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).errorCallback(errorCallback).build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(
            productDraftMock,
            AssetCustomTypeAdapter.of(oldAsset),
            AssetDraftCustomTypeAdapter.of(newAssetDraft),
            new AssetCustomActionBuilder(),
            10L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            productSyncOptions);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(0);
    assertThat(callBackResponses.get(0))
        .isEqualTo(
            format(
                "Failed to build custom fields update actions on the "
                    + "asset with id '%s'. Reason: New resource's custom type id is blank (empty/null).",
                oldAssetId));
    assertThat(callBackResponses.get(1)).isNull();
  }

  @Test
  void buildCustomUpdateActions_WithNullNewCustomFields_ShouldBuildUpdateActions() {
    final Asset oldAsset = mock(Asset.class);
    final String oldAssetId = "oldAssetId";
    when(oldAsset.getId()).thenReturn(oldAssetId);
    when(oldAsset.getCustom()).thenReturn(mock(CustomFields.class));

    final AssetDraft newAssetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(ofEnglish("assetName")).build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(
            productDraftMock,
            AssetCustomTypeAdapter.of(oldAsset),
            AssetDraftCustomTypeAdapter.of(newAssetDraft),
            new AssetCustomActionBuilder(),
            10L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

    // Should remove custom type from old asset.
    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetAssetCustomTypeAction.class);
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithBothNullCustomFields_ShouldNotBuildUpdateActions() {
    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(null);

    final AssetDraft newAssetDraft = mock(AssetDraft.class);
    when(newAssetDraft.getCustom()).thenReturn(null);

    // Mock custom options error callback
    final ArrayList<Object> callBackResponses = new ArrayList<>();
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallback =
            (exception, newResource, oldResource, updateActions) -> {
              callBackResponses.add(exception.getMessage());
              callBackResponses.add(exception.getCause());
            };

    // Mock sync options
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).errorCallback(errorCallback).build();

    final List<ProductUpdateAction> updateActions =
        buildCustomUpdateActions(
            productDraftMock,
            AssetCustomTypeAdapter.of(oldAsset),
            AssetDraftCustomTypeAdapter.of(newAssetDraft),
            new AssetCustomActionBuilder(),
            10L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            productSyncOptions);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
    assertThat(callBackResponses).isEmpty();
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithSameCategoryTypesButDifferentFieldValues_ShouldBuildUpdateActions()
          throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    final Map<String, Object> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", true);

    when(oldCustomFieldsMock.getType())
        .thenReturn(TypeReferenceBuilder.of().id("categoryAssetCustomTypeId").build());
    when(oldCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsJsonMapMock).build());

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    final Map<String, Object> newCustomFieldsJsonMapMock = new HashMap<>();
    newCustomFieldsJsonMapMock.put("invisibleInShop", false);
    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("categoryAssetCustomTypeId").build());
    when(newCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(newCustomFieldsJsonMapMock).build());

    when(newCustomFieldsMock.getType())
        .thenReturn(
            TypeReferenceBuilder.of()
                .id("categoryAssetCustomTypeId")
                .build()
                .toResourceIdentifier());
    when(newCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(newCustomFieldsJsonMapMock).build());

    final List<ProductUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            CATEGORY_SYNC_OPTIONS);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetAssetCustomFieldAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithDifferentCategoryTypeIds_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .type(TypeReferenceBuilder.of().id("assetCustomTypeId").build())
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);

    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("newAssetCustomTypeId").build());

    final List<ProductUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId,
            asset -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            CATEGORY_SYNC_OPTIONS);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetAssetCustomTypeAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithEmptyOldCategoryTypeId_ShouldBuildUpdateActions()
      throws BuildUpdateActionException {
    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .type(TypeReferenceBuilder.of().id("").build())
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(TypeResourceIdentifierBuilder.of().id("priceCustomTypeId").build());

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            PriceCustomTypeAdapter.of(price),
            new PriceCustomActionBuilder(),
            1L,
            PriceCustomTypeAdapter::getId,
            asset -> ResourceTypeId.PRODUCT_PRICE.getJsonName(),
            PriceCustomTypeAdapter::getId,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetProductPriceCustomTypeAction.class);
  }

  @Test
  void
      buildNonNullCustomFieldsUpdateActions_WithSameIdsButNullNewCustomFields_ShouldBuildUpdateActions()
          throws BuildUpdateActionException {
    final TypeReference productPriceTypeReference =
        TypeReferenceBuilder.of().id("productPriceCustomTypeId").build();

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock = mock(CustomFields.class);
    final Map<String, Object> oldCustomFieldsJsonMapMock = new HashMap<>();
    oldCustomFieldsJsonMapMock.put("invisibleInShop", true);
    when(oldCustomFieldsMock.getType()).thenReturn(productPriceTypeReference);
    when(oldCustomFieldsMock.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsJsonMapMock).build());

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(productPriceTypeReference.toResourceIdentifier());
    when(newCustomFieldsMock.getFields()).thenReturn(null);

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildNonNullCustomFieldsUpdateActions(
            oldCustomFieldsMock,
            newCustomFieldsMock,
            PriceCustomTypeAdapter.of(price),
            new PriceCustomActionBuilder(),
            1L,
            PriceCustomTypeAdapter::getId,
            asset -> ResourceTypeId.PRODUCT_PRICE.getJsonName(),
            PriceCustomTypeAdapter::getId,
            ProductSyncOptionsBuilder.of(CTP_CLIENT).build());

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isInstanceOf(ProductSetProductPriceCustomFieldAction.class);
  }

  @Test
  void buildNonNullCustomFieldsUpdateActions_WithNullIds_ShouldThrowBuildUpdateActionException() {
    final TypeReference productPriceTypeReference = TypeReferenceBuilder.of().id("").build();

    // Mock old CustomFields
    final CustomFields oldCustomFieldsMock =
        CustomFieldsBuilder.of()
            .type(productPriceTypeReference)
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(Collections.emptyMap()))
            .build();

    // Mock new CustomFieldsDraft
    final CustomFieldsDraft newCustomFieldsMock = mock(CustomFieldsDraft.class);
    when(newCustomFieldsMock.getType())
        .thenReturn(productPriceTypeReference.toResourceIdentifier());

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    assertThatThrownBy(
            () ->
                buildNonNullCustomFieldsUpdateActions(
                    oldCustomFieldsMock,
                    newCustomFieldsMock,
                    PriceCustomTypeAdapter.of(price),
                    new PriceCustomActionBuilder(),
                    1L,
                    PriceCustomTypeAdapter::getId,
                    asset -> ResourceTypeId.PRODUCT_PRICE.getJsonName(),
                    PriceCustomTypeAdapter::getId,
                    ProductSyncOptionsBuilder.of(CTP_CLIENT).build()))
        .isInstanceOf(BuildUpdateActionException.class)
        .hasMessageMatching("Custom type ids are not set for both the old and new product-price.");
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithDifferentCustomFieldValues_ShouldBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", true);
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "en", "red"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);
    newCustomFields.put("backgroundColor", Map.of("de", "rot"));

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            PriceCustomTypeAdapter.of(price),
            new PriceCustomActionBuilder(),
            1L,
            PriceCustomTypeAdapter::getId);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isNotEmpty();
    assertThat(updateActions).hasSize(1);
    final ProductUpdateAction categoryUpdateAction = updateActions.get(0);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction).isInstanceOf(ProductSetProductPriceCustomFieldAction.class);
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNewCustomFields_ShouldBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);
    newCustomFields.put("backgroundColor", Map.of("de", "rot"));
    newCustomFields.put("url", Map.of("domain", "domain.com"));
    newCustomFields.put("size", Map.of("cm", 34));

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            PriceCustomTypeAdapter.of(price),
            new PriceCustomActionBuilder(),
            1L,
            PriceCustomTypeAdapter::getId);

    assertThat(updateActions).hasSize(4);
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithRemovedCustomFields_ShouldBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", true);
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "en", "red"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);

    // Mock new price
    final Price price =
        PriceBuilder.of()
            .id("1")
            .value(
                builder ->
                    builder
                        .centPrecisionBuilder()
                        .centAmount(10L)
                        .currencyCode("EUR")
                        .fractionDigits(0))
            .build();

    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            PriceCustomTypeAdapter.of(price),
            new PriceCustomActionBuilder(),
            1L,
            PriceCustomTypeAdapter::getId);

    assertThat(updateActions)
        .hasSize(1)
        .singleElement()
        .matches(
            action -> {
              assertThat(action).isInstanceOf(ProductSetProductPriceCustomFieldAction.class);
              final ProductSetProductPriceCustomFieldAction setProductPriceCustomFieldAction =
                  (ProductSetProductPriceCustomFieldAction) action;

              assertThat(setProductPriceCustomFieldAction.getName()).isEqualTo("backgroundColor");
              assertThat(
                      ((FieldContainer) setProductPriceCustomFieldAction.getValue())
                          .values()
                          .get("backgroundColor"))
                  .isEqualTo(null);
              return true;
            });
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithSameCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("invisibleInShop", true);
    oldCustomFields.put("backgroundColor", Map.of("de", "rot"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("invisibleInShop", true);
    newCustomFields.put("backgroundColor", Map.of("de", "rot"));

    final Asset newAssetDraft =
        AssetBuilder.of().id("test").sources(emptyList()).name(ofEnglish("assetName")).build();

    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            AssetCustomTypeAdapter.of(newAssetDraft),
            new CategoryCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildSetCustomFieldsUpdateActions_WithDifferentOrderOfCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("backgroundColor", Map.of("de", "rot", "es", "rojo"));

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("backgroundColor", Map.of("es", "rojo", "de", "rot"));

    final Asset newAssetDraft =
        AssetBuilder.of().id("test").sources(emptyList()).name(ofEnglish("assetName")).build();

    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            AssetCustomTypeAdapter.of(newAssetDraft),
            new com.commercetools.sync.sdk2.categories.helpers.AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFieldValues_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();
    oldCustomFields.put("backgroundColor", "");

    final Map<String, Object> newCustomFields = new HashMap<>();
    newCustomFields.put("backgroundColor", "");

    final Asset newAssetDraft =
        AssetBuilder.of().id("test").sources(emptyList()).name(ofEnglish("assetName")).build();

    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            AssetCustomTypeAdapter.of(newAssetDraft),
            new com.commercetools.sync.sdk2.categories.helpers.AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyCustomFields_ShouldNotBuildUpdateActions() {
    final Map<String, Object> oldCustomFields = new HashMap<>();

    final Map<String, Object> newCustomFields = new HashMap<>();

    final Asset newAssetDraft =
        AssetBuilder.of().id("test").sources(emptyList()).name(ofEnglish("assetName")).build();

    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            AssetCustomTypeAdapter.of(newAssetDraft),
            new com.commercetools.sync.sdk2.categories.helpers.AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", List.of(true));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<CategoryUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new com.commercetools.sync.sdk2.categories.helpers.AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            CategorySetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .name("setOfBooleans")
                .value(null)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyArrayAsDiffValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", Collections.singletonList(true));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of().values(Map.of("setOfBooleans", emptyList())).build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyArrayAndNullOldValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", null);

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of().values(Map.of("setOfBooleans", emptyList())).build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyArrayAndNullNodeOldValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", null);

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of().values(Map.of("setOfBooleans", emptyList())).build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyArrayAsNewValue_ShouldBuildSetAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(new HashMap<>()).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of().values(Map.of("setOfBooleans", emptyList())).build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithEmptyArrayAsSameValue_ShouldBuildSetAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", Collections.emptyList());

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of().values(Map.of("setOfBooleans", emptyList())).build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullJsonNodeNewValue_ShouldBuildAction() {
    // preparation
    final Map<String, Object> oldCustomFieldsMap = new HashMap<>();
    oldCustomFieldsMap.put("setOfBooleans", Collections.singletonList(false));

    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(oldCustomFieldsMap).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            oldCustomFieldsMap,
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetCustomFieldActionBuilder.of()
                .assetKey(oldAsset.getKey())
                .variantId(1L)
                .name("setOfBooleans")
                .value(
                    FieldContainerBuilder.of()
                        .values(Collections.singletonMap("setOfBooleans", null))
                        .build())
                .staged(true)
                .build());
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullNewValueOfNewField_ShouldNotBuildAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(new HashMap<>()).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildSetCustomFieldsUpdateActions_WithNullJsonNodeNewValueOfNewField_ShouldNotBuildAction() {
    // preparation
    final CustomFields oldCustomFields = mock(CustomFields.class);
    when(oldCustomFields.getFields())
        .thenReturn(FieldContainerBuilder.of().values(new HashMap<>()).build());

    final Asset oldAsset = mock(Asset.class);
    when(oldAsset.getCustom()).thenReturn(oldCustomFields);

    final Map<String, Object> newCustomFieldsMap = new HashMap<>();
    newCustomFieldsMap.put("setOfBooleans", null);

    // test
    final List<ProductUpdateAction> updateActions =
        buildSetCustomFieldsUpdateActions(
            new HashMap<>(),
            newCustomFieldsMap,
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId);

    // assertion
    assertThat(updateActions).isEmpty();
  }
}
