package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.queries.PagedResult;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.BooleanFieldType;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedStringFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.SetFieldType;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.types.queries.TypeQueryBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is meant only meant for internal use of the library's integration tests. */
public final class ITUtils {
  public static final String LOCALISED_STRING_CUSTOM_FIELD_NAME = "backgroundColor";
  public static final String BOOLEAN_CUSTOM_FIELD_NAME = "invisibleInShop";
  public static final String EMPTY_SET_CUSTOM_FIELD_NAME = "emptySet";
  public static final String NON_EMPTY_SEY_CUSTOM_FIELD_NAME = "nonEmptySet";
  public static final String NULL_NODE_SET_CUSTOM_FIELD_NAME = "nullNode";
  public static final String NULL_SET_CUSTOM_FIELD_NAME = "null";

  /**
   * This method blocks to create an asset custom Type on the CTP project defined by the supplied
   * {@code ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type createAssetsCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final SphereClient ctpClient) {

    return createTypeIfNotAlreadyExisting(
        typeKey, locale, name, ResourceTypeIdsSetBuilder.of().addAssets(), ctpClient);
  }

  /**
   * This method blocks to create a custom Type on the CTP project defined by the supplied {@code
   * ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param resourceTypeIdsSetBuilder builds the resource type ids for the created type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type createTypeIfNotAlreadyExisting(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ResourceTypeIdsSetBuilder resourceTypeIdsSetBuilder,
      @Nonnull final SphereClient ctpClient) {

    return typeExists(typeKey, ctpClient)
        .thenCompose(
            type ->
                type.map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () -> {
                          final TypeDraft typeDraft =
                              TypeDraftBuilder.of(
                                      typeKey,
                                      LocalizedString.of(locale, name),
                                      resourceTypeIdsSetBuilder)
                                  .fieldDefinitions(createCustomTypeFieldDefinitions(locale))
                                  .build();
                          return ctpClient
                              .execute(TypeCreateCommand.of(typeDraft))
                              .toCompletableFuture();
                        }))
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<Optional<Type>> typeExists(
      @Nonnull final String typeKey, @Nonnull final SphereClient ctpClient) {
    return ctpClient
        .execute(
            TypeQueryBuilder.of()
                .predicates(QueryPredicate.of(format("key=\"%s\"", typeKey)))
                .build())
        .thenApply(PagedResult::head);
  }

  /**
   * Builds a list of two field definitions; one for a {@link LocalizedStringFieldType} and one for
   * a {@link BooleanFieldType}. The JSON of the created field definition list looks as follows:
   *
   * <p>"fieldDefinitions": [ { "name": "backgroundColor", "label": { "en": "backgroundColor" },
   * "required": false, "type": { "name": "LocalizedString" }, "inputHint": "SingleLine" }, {
   * "name": "invisibleInShop", "label": { "en": "invisibleInShop" }, "required": false, "type": {
   * "name": "Boolean" }, "inputHint": "SingleLine" } ]
   *
   * @param locale defines the locale for which the field definition names are going to be bound to.
   * @return the list of field definitions.
   */
  private static List<FieldDefinition> createCustomTypeFieldDefinitions(
      @Nonnull final Locale locale) {
    return asList(
        FieldDefinition.of(
            LocalizedStringFieldType.of(),
            LOCALISED_STRING_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, LOCALISED_STRING_CUSTOM_FIELD_NAME),
            false),
        FieldDefinition.of(
            BooleanFieldType.of(),
            BOOLEAN_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, BOOLEAN_CUSTOM_FIELD_NAME),
            false),
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()),
            EMPTY_SET_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, EMPTY_SET_CUSTOM_FIELD_NAME),
            false),
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()),
            NON_EMPTY_SEY_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, NON_EMPTY_SEY_CUSTOM_FIELD_NAME),
            false),
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()),
            NULL_NODE_SET_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, NULL_NODE_SET_CUSTOM_FIELD_NAME),
            false),
        FieldDefinition.of(
            SetFieldType.of(StringFieldType.of()),
            NULL_SET_CUSTOM_FIELD_NAME,
            LocalizedString.of(locale, NULL_SET_CUSTOM_FIELD_NAME),
            false));
  }

  /**
   * Builds a {@link Map} for the custom fields to their {@link JsonNode} values that looks as
   * follows in JSON format:
   *
   * <p>"fields": {"invisibleInShop": false, "backgroundColor": { "en": "red", "de": "rot"}}
   *
   * @return a Map of the custom fields to their JSON values with dummy data.
   */
  public static Map<String, JsonNode> createCustomFieldsJsonMap() {
    final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
    customFieldsJsons.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
    customFieldsJsons.put(
        LOCALISED_STRING_CUSTOM_FIELD_NAME,
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
    return customFieldsJsons;
  }

  /**
   * Deletes all Types from CTP projects defined by the {@code sphereClient}
   *
   * @param ctpClient defines the CTP project to delete the Types from.
   */
  public static void deleteTypes(@Nonnull final SphereClient ctpClient) {
    queryAndExecute(ctpClient, TypeQuery.of(), TypeDeleteCommand::of);
  }

  /**
   * Deletes all Types from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteTypesFromTargetAndSource() {
    deleteTypes(CTP_TARGET_CLIENT);
    deleteTypes(CTP_SOURCE_CLIENT);
  }

  /**
   * Applies the {@code resourceToRequestMapper} function on each page, resulting from the {@code
   * query} executed by the {@code ctpClient}, to map each resource to a {@link SphereRequest} and
   * then executes these requests in parallel within each page.
   *
   * @param ctpClient defines the CTP project to apply the query on.
   * @param query query that should be made on the CTP project.
   * @param resourceToRequestMapper defines a mapper function that should be applied on each
   *     resource, in the fetched page from the query on the specified CTP project, to map it to a
   *     {@link SphereRequest}.
   */
  public static <T extends Resource, C extends QueryDsl<T, C>> void queryAndExecute(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final QueryDsl<T, C> query,
      @Nonnull final Function<T, SphereRequest<T>> resourceToRequestMapper) {

    queryAndCompose(
        ctpClient, query, resource -> ctpClient.execute(resourceToRequestMapper.apply(resource)));
  }

  /**
   * Applies the {@code resourceToStageMapper} function on each page, resulting from the {@code
   * query} executed by the {@code ctpClient}, to map each resource to a {@link CompletionStage} and
   * then executes these stages in parallel within each page.
   *
   * @param ctpClient defines the CTP project to apply the query on.
   * @param query query that should be made on the CTP project.
   * @param resourceToStageMapper defines a mapper function that should be applied on each resource,
   *     in the fetched page from the query on the specified CTP project, to map it to a {@link
   *     CompletionStage} which will be executed (in a blocking fashion) after every page fetch.
   */
  public static <T extends Resource, C extends QueryDsl<T, C>, S> void queryAndCompose(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final QueryDsl<T, C> query,
      @Nonnull final Function<T, CompletionStage<S>> resourceToStageMapper) {

    // TODO: GITHUB ISSUE #248
    final Consumer<List<T>> pageConsumer =
        pageElements ->
            CompletableFuture.allOf(
                    pageElements.stream()
                        .map(resourceToStageMapper)
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new))
                .join();

    CtpQueryUtils.queryAll(ctpClient, query, pageConsumer).toCompletableFuture().join();
  }

  /**
   * Creates an {@link AssetDraft} with the with the given key and name.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @return an {@link AssetDraft} with the with the given key and name.
   */
  public static AssetDraft createAssetDraft(
      @Nonnull final String assetKey, @Nonnull final LocalizedString assetName) {
    return createAssetDraftBuilder(assetKey, assetName).build();
  }

  /**
   * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created
   * will have custom field with the type id supplied ({@code assetCustomTypeId} and the fields
   * built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeId the asset custom type id.
   * @return an {@link AssetDraft} with the with the given key and name. The asset draft created
   *     will have custom field with the type id supplied ({@code assetCustomTypeId} and the fields
   *     built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   */
  public static AssetDraft createAssetDraft(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeId) {
    return createAssetDraft(assetKey, assetName, assetCustomTypeId, createCustomFieldsJsonMap());
  }

  /**
   * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created
   * will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   * fields will be defined by the {@code customFieldsJsonMap} supplied.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeId the asset custom type id.
   * @param customFieldsJsonMap the custom fields of the asset custom type.
   * @return an {@link AssetDraft} with the with the given key and name. The asset draft created
   *     will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   *     fields will be defined by the {@code customFieldsJsonMap} supplied.
   */
  public static AssetDraft createAssetDraft(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeId,
      @Nonnull final Map<String, JsonNode> customFieldsJsonMap) {
    return createAssetDraftBuilder(assetKey, assetName)
        .custom(CustomFieldsDraft.ofTypeIdAndJson(assetCustomTypeId, customFieldsJsonMap))
        .build();
  }

  /**
   * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created
   * will have custom field with the type key supplied ({@code assetCustomTypeKey} and the fields
   * built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeKey the asset custom type key.
   * @return an {@link AssetDraft} with the with the given key and name. The asset draft created
   *     will have custom field with the type key supplied ({@code assetCustomTypeKey} and the
   *     fields built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   */
  public static AssetDraft createAssetDraftWithKey(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeKey) {
    return createAssetDraftWithKey(
        assetKey, assetName, assetCustomTypeKey, createCustomFieldsJsonMap());
  }

  /**
   * Creates an {@link AssetDraft} with the with the given key and name. The asset draft created
   * will have custom field with the type key supplied ({@code assetCustomTypeKey} and the custom
   * fields will be defined by the {@code customFieldsJsonMap} supplied.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeKey the asset custom type key.
   * @param customFieldsJsonMap the custom fields of the asset custom type.
   * @return an {@link AssetDraft} with the with the given key and name. The asset draft created
   *     will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   *     fields will be defined by the {@code customFieldsJsonMap} supplied.
   */
  public static AssetDraft createAssetDraftWithKey(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeKey,
      @Nonnull final Map<String, JsonNode> customFieldsJsonMap) {
    return createAssetDraftBuilder(assetKey, assetName)
        .custom(CustomFieldsDraft.ofTypeKeyAndJson(assetCustomTypeKey, customFieldsJsonMap))
        .build();
  }

  /**
   * Creates an {@link AssetDraftBuilder} with the with the given key and name. The builder created
   * will contain one tag with the same value as the key and will contain one {@link
   * io.sphere.sdk.models.AssetSource} with the uri {@code sourceUri}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @return an {@link AssetDraftBuilder} with the with the given key and name. The builder created
   *     will contain one tag with the same value as the key and will contain one {@link
   *     io.sphere.sdk.models.AssetSource} with the uri {@code sourceUri}.
   */
  private static AssetDraftBuilder createAssetDraftBuilder(
      @Nonnull final String assetKey, @Nonnull final LocalizedString assetName) {
    return AssetDraftBuilder.of(emptyList(), assetName)
        .key(assetKey)
        .tags(singleton(assetKey))
        .sources(singletonList(AssetSourceBuilder.ofUri("sourceUri").build()));
  }

  /**
   * Creates a {@link ProductVariantDraft} draft key and sku of the value supplied {@code
   * variantKeyAndSku} and with the supplied {@code assetDrafts} and {@code priceDrafts}
   *
   * @param variantKeyAndSku the value of the key and sku of the created draft.
   * @param assetDrafts the assets to assign to the created draft.
   * @param priceDrafts the prices to assign to the created draft.
   * @return a {@link ProductVariantDraft} draft key and sku of the value supplied {@code
   *     variantKeyAndSku} and with the supplied {@code assetDrafts} and {@code priceDrafts}.
   */
  public static ProductVariantDraft createVariantDraft(
      @Nonnull final String variantKeyAndSku,
      @Nullable final List<AssetDraft> assetDrafts,
      @Nullable final List<PriceDraft> priceDrafts) {

    return ProductVariantDraftBuilder.of()
        .key(variantKeyAndSku)
        .sku(variantKeyAndSku)
        .assets(assetDrafts)
        .prices(priceDrafts)
        .build();
  }

  /**
   * Asserts that a list of {@link Asset} and a list of {@link AssetDraft} have the same ordering of
   * assets (assets are matched by key). It asserts that the matching assets have the same name,
   * description, custom fields, tags, and asset sources.
   *
   * <p>TODO: This should be refactored into Asset asserts helpers. GITHUB ISSUE#261
   *
   * @param assets the list of assets to compare to the list of asset drafts.
   * @param assetDrafts the list of asset drafts to compare to the list of assets.
   */
  public static void assertAssetsAreEqual(
      @Nonnull final List<Asset> assets, @Nonnull final List<AssetDraft> assetDrafts) {
    IntStream.range(0, assetDrafts.size())
        .forEach(
            index -> {
              final Asset createdAsset = assets.get(index);
              final AssetDraft assetDraft = assetDrafts.get(index);

              assertThat(createdAsset.getName()).isEqualTo(assetDraft.getName());
              assertThat(createdAsset.getDescription()).isEqualTo(assetDraft.getDescription());
              assertThat(createdAsset.getKey()).isEqualTo(assetDraft.getKey());

              ofNullable(assetDraft.getCustom())
                  .ifPresent(
                      customFields -> {
                        assertThat(createdAsset.getCustom()).isNotNull();
                        assertThat(createdAsset.getCustom().getFieldsJsonMap())
                            .isEqualTo(assetDraft.getCustom().getFields());
                      });

              assertThat(createdAsset.getTags()).isEqualTo(assetDraft.getTags());
              assertThat(createdAsset.getSources()).isEqualTo(assetDraft.getSources());
            });
  }

  private ITUtils() {}
}
