package com.commercetools.sync.integration.sdk2.commons.utils;

import static java.util.Arrays.asList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.type.CustomFieldBooleanType;
import com.commercetools.api.models.type.CustomFieldBooleanTypeBuilder;
import com.commercetools.api.models.type.CustomFieldLocalizedStringType;
import com.commercetools.api.models.type.CustomFieldLocalizedStringTypeBuilder;
import com.commercetools.api.models.type.CustomFieldSetTypeBuilder;
import com.commercetools.api.models.type.CustomFieldStringTypeBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

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

  private static CompletionStage<Optional<Type>> typeExists(
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
   * Builds a {@link FieldContainer} for the custom fields to their {@link JsonNode} values that
   * looks as follows in JSON format:
   *
   * <p>"fields": {"invisibleInShop": false, "backgroundColor": { "en": "red", "de": "rot"}}
   *
   * @return FieldContainer includes a Map of the custom fields to their JSON values with dummy
   *     data.
   */
  public static FieldContainer createCustomFieldsJsonMap() {
    final FieldContainerBuilder customFields = FieldContainerBuilder.of();
    customFields.addValue(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
    customFields.addValue(
        LOCALISED_STRING_CUSTOM_FIELD_NAME,
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
    return customFields.build();
  }

  private ITUtils() {}
}
