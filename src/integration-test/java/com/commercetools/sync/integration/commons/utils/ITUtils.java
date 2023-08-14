package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.type.CustomFieldBooleanType;
import com.commercetools.api.models.type.CustomFieldBooleanTypeBuilder;
import com.commercetools.api.models.type.CustomFieldLocalizedStringType;
import com.commercetools.api.models.type.CustomFieldLocalizedStringTypeBuilder;
import com.commercetools.api.models.type.CustomFieldSetTypeBuilder;
import com.commercetools.api.models.type.CustomFieldStringTypeBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
   * This method blocks to create a custom Type on the CTP project defined by the supplied {@code
   * ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param resourceTypeIds the resource type ids for the created type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type createTypeIfNotAlreadyExisting(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final List<ResourceTypeId> resourceTypeIds,
      @Nonnull final ProjectApiRoot ctpClient) {

    return typeExists(typeKey, ctpClient)
        .thenCompose(
            type ->
                type.map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () -> {
                          final TypeDraft typeDraft =
                              TypeDraftBuilder.of()
                                  .key(typeKey)
                                  .name(
                                      LocalizedStringBuilder.of()
                                          .addValue(locale.toLanguageTag(), name)
                                          .build())
                                  .resourceTypeIds(resourceTypeIds)
                                  .fieldDefinitions(createCustomTypeFieldDefinitions(locale))
                                  .build();
                          return ctpClient
                              .types()
                              .post(typeDraft)
                              .execute()
                              .thenApply(ApiHttpResponse::getBody);
                        }))
        .toCompletableFuture()
        .join();
  }

  public static Type ensureTypeByTypeDraft(
      @Nonnull final TypeDraft typeDraft, @Nonnull final ProjectApiRoot ctpClient) {
    return typeExists(typeDraft.getKey(), ctpClient)
        .thenCompose(
            type ->
                type.map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () ->
                            ctpClient
                                .types()
                                .post(typeDraft)
                                .execute()
                                .thenApply(ApiHttpResponse::getBody)))
        .toCompletableFuture()
        .join();
  }

  static CompletionStage<Optional<Type>> typeExists(
      @Nonnull final String typeKey, @Nonnull final ProjectApiRoot ctpClient) {

    return ctpClient
        .types()
        .withKey(typeKey)
        .get()
        .execute()
        .handle(
            (typeApiHttpResponse, throwable) -> {
              if (throwable != null) {
                return Optional.empty();
              }
              return Optional.of(typeApiHttpResponse.getBody().get());
            });
  }

  /**
   * Builds a list of two field definitions; one for a {@link CustomFieldLocalizedStringType} and
   * one for a {@link CustomFieldBooleanType}. The JSON of the created field definition list looks
   * as follows:
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
        FieldDefinitionBuilder.of()
            .type(CustomFieldLocalizedStringTypeBuilder.of().build())
            .name(LOCALISED_STRING_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), LOCALISED_STRING_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build(),
        FieldDefinitionBuilder.of()
            .type(CustomFieldBooleanTypeBuilder.of().build())
            .name(BOOLEAN_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), BOOLEAN_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build(),
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(CustomFieldStringTypeBuilder.of().build())
                    .build())
            .name(EMPTY_SET_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), EMPTY_SET_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build(),
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(CustomFieldStringTypeBuilder.of().build())
                    .build())
            .name(NON_EMPTY_SEY_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), NON_EMPTY_SEY_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build(),
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(CustomFieldStringTypeBuilder.of().build())
                    .build())
            .name(NULL_NODE_SET_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), NULL_NODE_SET_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build(),
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(CustomFieldStringTypeBuilder.of().build())
                    .build())
            .name(NULL_SET_CUSTOM_FIELD_NAME)
            .label(
                LocalizedStringBuilder.of()
                    .addValue(locale.toLanguageTag(), NULL_SET_CUSTOM_FIELD_NAME)
                    .build())
            .required(false)
            .build());
  }

  /**
   * Builds a {@link FieldContainer} for the custom fields to their values that looks as follows in
   * JSON format:
   *
   * <p>"fields": {"invisibleInShop": false, "backgroundColor": { "en": "red", "de": "rot"}}
   *
   * @return FieldContainer includes a Map of the custom fields to their JSON values with dummy
   *     data.
   */
  public static FieldContainer createCustomFieldsJsonMap() {
    final FieldContainerBuilder customFields = FieldContainerBuilder.of();
    customFields.addValue(BOOLEAN_CUSTOM_FIELD_NAME, false);
    customFields.addValue(
        LOCALISED_STRING_CUSTOM_FIELD_NAME,
        LocalizedStringBuilder.of().addValue("de", "rot").addValue("en", "red").build());
    return customFields.build();
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
   * Deletes all Types from CTP projects defined by the {@code ctpClient}
   *
   * @param ctpClient defines the CTP project to delete the Types from.
   */
  public static void deleteTypes(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
        ctpClient.types().get(),
        types -> {
          return CompletableFuture.allOf(
              types.stream()
                  .map(
                      type ->
                          ctpClient
                              .types()
                              .delete(type)
                              .execute()
                              .thenApply(ApiHttpResponse::getBody))
                  .map(CompletionStage::toCompletableFuture)
                  .toArray(CompletableFuture[]::new));
        });
  }

  /**
   * This method blocks to create an asset custom Type on the CTP project defined by the supplied
   * {@code ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type ensureAssetsCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {

    return createTypeIfNotAlreadyExisting(
        typeKey, locale, name, singletonList(ResourceTypeId.ASSET), ctpClient);
  }

  /**
   * Creates an {@link AssetDraft} with the given key and name.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @return an {@link AssetDraft} with the given key and name.
   */
  public static AssetDraft createAssetDraft(
      @Nonnull final String assetKey, @Nonnull final LocalizedString assetName) {
    return createAssetDraftBuilder(assetKey, assetName).build();
  }

  /**
   * Creates an {@link AssetDraft} with the given key and name. The asset draft created
   * will have custom field with the type id supplied ({@code assetCustomTypeId} and the fields
   * built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeId the asset custom type id.
   * @return an {@link AssetDraft} with the given key and name. The asset draft created
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
   * Creates an {@link AssetDraft} with the given key and name. The asset draft created
   * will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   * fields will be defined by the {@code customFieldsJsonMap} supplied.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeId the asset custom type id.
   * @param customFieldsJsonMap the custom fields of the asset custom type.
   * @return an {@link AssetDraft} with the given key and name. The asset draft created
   *     will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   *     fields will be defined by the {@code customFieldsJsonMap} supplied.
   */
  public static AssetDraft createAssetDraft(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeId,
      @Nonnull final FieldContainer customFieldsJsonMap) {
    return createAssetDraftBuilder(assetKey, assetName)
        .custom(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(assetCustomTypeId))
                .fields(customFieldsJsonMap)
                .build())
        .build();
  }

  /**
   * Creates an {@link AssetDraft} with the given key and name. The asset draft created
   * will have custom field with the type key supplied ({@code assetCustomTypeKey} and the fields
   * built from the method {@link ITUtils#createCustomFieldsJsonMap()}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeKey the asset custom type key.
   * @return an {@link AssetDraft} with the given key and name. The asset draft created
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
   * Creates an {@link AssetDraft} with the given key and name. The asset draft created
   * will have custom field with the type key supplied ({@code assetCustomTypeKey} and the custom
   * fields will be defined by the {@code customFieldsJsonMap} supplied.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @param assetCustomTypeKey the asset custom type key.
   * @param customFieldsJsonMap the custom fields of the asset custom type.
   * @return an {@link AssetDraft} with the given key and name. The asset draft created
   *     will have custom field with the type id supplied ({@code assetCustomTypeId} and the custom
   *     fields will be defined by the {@code customFieldsJsonMap} supplied.
   */
  public static AssetDraft createAssetDraftWithKey(
      @Nonnull final String assetKey,
      @Nonnull final LocalizedString assetName,
      @Nonnull final String assetCustomTypeKey,
      @Nonnull final FieldContainer customFieldsJsonMap) {
    return createAssetDraftBuilder(assetKey, assetName)
        .custom(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.key(assetCustomTypeKey))
                .fields(customFieldsJsonMap)
                .build())
        .build();
  }

  /**
   * Creates an {@link AssetDraftBuilder} with the given key and name. The builder created
   * will contain one tag with the same value as the key and will contain one {@link
   * com.commercetools.api.models.common.AssetSource} with the uri {@code sourceUri}.
   *
   * @param assetKey asset draft key.
   * @param assetName asset draft name.
   * @return an {@link AssetDraftBuilder} with the given key and name. The builder created
   *     will contain one tag with the same value as the key and will contain one {@link
   *     com.commercetools.api.models.common.AssetSource} with the uri {@code sourceUri}.
   */
  private static AssetDraftBuilder createAssetDraftBuilder(
      @Nonnull final String assetKey, @Nonnull final LocalizedString assetName) {
    return AssetDraftBuilder.of()
        .name(assetName)
        .key(assetKey)
        .tags(singletonList(assetKey))
        .sources(singletonList(AssetSourceBuilder.of().uri("sourceUri").build()));
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
                        assertThat(createdAsset.getCustom().getFields())
                            .isEqualTo(assetDraft.getCustom().getFields());
                      });

              assertThat(createdAsset.getTags()).isEqualTo(assetDraft.getTags());
              assertThat(createdAsset.getSources()).isEqualTo(assetDraft.getSources());
            });
  }

  public static NotFoundException createNotFoundException() {
    final String json = getErrorResponseJsonString(404);

    return new NotFoundException(
        404, "", null, "", new ApiHttpResponse<>(404, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  public static ConcurrentModificationException createConcurrentModificationException() {
    final String json = getErrorResponseJsonString(409);

    return new ConcurrentModificationException(
        409, "", null, "", new ApiHttpResponse<>(409, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  public static BadGatewayException createBadGatewayException() {
    final String json = getErrorResponseJsonString(500);
    return new BadGatewayException(
        500, "", null, "", new ApiHttpResponse<>(500, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  private static String getErrorResponseJsonString(Integer errorCode) {
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(errorCode)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json;
    try {
      json = ow.writeValueAsString(errorResponse);
    } catch (JsonProcessingException e) {
      // ignore the error
      json = null;
    }
    return json;
  }

  private ITUtils() {}
}
