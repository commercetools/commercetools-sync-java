package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeBooleanType;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeEnumType;
import com.commercetools.api.models.product_type.AttributeLocalizableTextType;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumType;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeNestedTypeBuilder;
import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributeSetTypeBuilder;
import com.commercetools.api.models.product_type.AttributeTextType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.TextInputHint;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;

public final class ProductTypeITUtils {
  private static final String LOCALISED_STRING_ATTRIBUTE_NAME = "backgroundColor";
  private static final String BOOLEAN_ATTRIBUTE_NAME = "invisibleInShop";

  public static final String PRODUCT_TYPE_KEY_1 = "key_1";
  public static final String PRODUCT_TYPE_KEY_2 = "key_2";
  public static final String PRODUCT_TYPE_KEY_3 = "key_3";
  public static final String PRODUCT_TYPE_KEY_4 = "key_4";
  public static final String PRODUCT_TYPE_KEY_5 = "key_5";

  public static final String PRODUCT_TYPE_NAME_1 = "name_1";
  public static final String PRODUCT_TYPE_NAME_2 = "name_2";
  public static final String PRODUCT_TYPE_NAME_3 = "name_3";
  public static final String PRODUCT_TYPE_NAME_4 = "name_4";
  public static final String PRODUCT_TYPE_NAME_5 = "name_5";

  public static final String PRODUCT_TYPE_DESCRIPTION_1 = "description_1";
  public static final String PRODUCT_TYPE_DESCRIPTION_2 = "description_2";
  public static final String PRODUCT_TYPE_DESCRIPTION_3 = "description_3";
  public static final String PRODUCT_TYPE_DESCRIPTION_4 = "description_4";
  public static final String PRODUCT_TYPE_DESCRIPTION_5 = "description_5";

  public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_1 =
      AttributeDefinitionDraftBuilder.of()
          .type(AttributeTextType.of())
          .name("attr_name_1")
          .label(LocalizedString.ofEnglish("attr_label_1"))
          .isRequired(true)
          .inputTip(LocalizedString.ofEnglish("inputTip1"))
          .build();

  public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_2 =
      AttributeDefinitionDraftBuilder.of()
          .type(AttributeTextType.of())
          .name("attr_name_2")
          .label(LocalizedString.ofEnglish("attr_label_2"))
          .isRequired(true)
          .inputTip(LocalizedString.ofEnglish("inputTip2"))
          .build();

  public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_3 =
      AttributeDefinitionDraftBuilder.of()
          .type(AttributeTextType.of())
          .name("attr_name_3")
          .label(LocalizedString.ofEnglish("attr_label_3"))
          .isRequired(true)
          .inputTip(LocalizedString.ofEnglish("inputTip3"))
          .build();

  public static final ProductTypeDraft productTypeDraft1 =
      ProductTypeDraftBuilder.of()
          .key(PRODUCT_TYPE_KEY_1)
          .name(PRODUCT_TYPE_NAME_1)
          .description(PRODUCT_TYPE_DESCRIPTION_1)
          .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
          .build();

  public static final ProductTypeDraft productTypeDraft2 =
      ProductTypeDraftBuilder.of()
          .key(PRODUCT_TYPE_KEY_2)
          .name(PRODUCT_TYPE_NAME_2)
          .description(PRODUCT_TYPE_DESCRIPTION_2)
          .attributes(ATTRIBUTE_DEFINITION_DRAFT_1)
          .build();

  /**
   * Populate source CTP project. Creates product type with key PRODUCT_TYPE_KEY_1,
   * PRODUCT_TYPE_NAME_1, PRODUCT_TYPE_DESCRIPTION_1 and attributes attributeDefinitionDraft1,
   * attributeDefinitionDraft2. Creates product type with key PRODUCT_TYPE_KEY_2,
   * PRODUCT_TYPE_NAME_2, PRODUCT_TYPE_DESCRIPTION_2 and attributes attributeDefinitionDraft1.
   */
  public static void populateSourceProject() {
    CTP_SOURCE_CLIENT.productTypes().post(productTypeDraft1).execute().join();
    CTP_SOURCE_CLIENT.productTypes().post(productTypeDraft2).execute().join();
  }

  public static void populateProjectWithNestedAttributes(@Nonnull final ProjectApiRoot ctpClient) {
    final ProductType productType1 =
        ctpClient.productTypes().post(productTypeDraft1).execute().join().getBody();
    final ProductType productType2 =
        ctpClient.productTypes().post(productTypeDraft2).execute().join().getBody();
    final AttributeDefinitionDraft nestedTypeAttr1 =
        AttributeDefinitionDraftBuilder.of()
            .type(
                AttributeSetTypeBuilder.of()
                    .elementType(
                        AttributeSetTypeBuilder.of()
                            .elementType(
                                AttributeSetTypeBuilder.of()
                                    .elementType(
                                        AttributeNestedTypeBuilder.of()
                                            .typeReference(
                                                ProductTypeReferenceBuilder.of()
                                                    .id(productType1.getId())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .isSearchable(true)
            .build();

    final AttributeDefinitionDraft nestedTypeAttr2 =
        AttributeDefinitionDraftBuilder.of()
            .type(
                AttributeNestedTypeBuilder.of()
                    .typeReference(
                        ProductTypeReferenceBuilder.of().id(productType2.getId()).build())
                    .build())
            .name("nestedattr2")
            .label(LocalizedString.ofEnglish("nestedattr2"))
            .isSearchable(true)
            .build();

    final ProductTypeDraft productTypeDraft3 =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_3)
            .name(PRODUCT_TYPE_NAME_3)
            .description(PRODUCT_TYPE_DESCRIPTION_3)
            .attributes(nestedTypeAttr1, nestedTypeAttr2)
            .build();

    ctpClient.productTypes().post(productTypeDraft3).execute().join();

    final ProductTypeDraft productTypeDraft4 =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_4)
            .name(PRODUCT_TYPE_NAME_4)
            .description(PRODUCT_TYPE_DESCRIPTION_4)
            .attributes(nestedTypeAttr1)
            .build();

    ctpClient.productTypes().post(productTypeDraft4).execute().join();
  }

  /**
   * Populate source CTP project. Creates product type with key PRODUCT_TYPE_KEY_1,
   * PRODUCT_TYPE_NAME_1, PRODUCT_TYPE_DESCRIPTION_1 and attributes attributeDefinitionDraft1,
   * attributeDefinitionDraft2.
   */
  public static void populateTargetProject() {
    CTP_TARGET_CLIENT.productTypes().post(productTypeDraft1).execute().join();
  }

  /**
   * Deletes all ProductTypes from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteProductTypesFromTargetAndSource() {
    deleteProductTypes(CTP_TARGET_CLIENT);
    deleteProductTypes(CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes all product types from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the product types from.
   */
  public static void deleteProductTypes(@Nonnull final ProjectApiRoot ctpClient) {
    deleteProductTypesWithRetry(ctpClient);
  }

  /**
   * Deletes all attributes with references and then product types from the CTP project defined by
   * the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the product types from.
   */
  public static void removeAttributeReferencesAndDeleteProductTypes(
      @Nonnull final ProjectApiRoot ctpClient) {
    deleteProductTypeAttributes(ctpClient);
    deleteProductTypes(ctpClient);
  }

  private static void deleteProductTypesWithRetry(@Nonnull final ProjectApiRoot ctpClient) {
    Consumer<List<ProductType>> productTypesConsumer =
        productTypes -> {
          CompletableFuture.allOf(
                  productTypes.stream()
                      .map(productType -> deleteProductTypeWithRetry(ctpClient, productType))
                      .map(CompletionStage::toCompletableFuture)
                      .toArray(CompletableFuture[]::new))
              .join();
        };
    QueryUtils.queryAll(ctpClient.productTypes().get(), productTypesConsumer)
        .handle(
            (result, throwable) -> {
              if (throwable != null && !(throwable instanceof NotFoundException)) {
                return throwable;
              }
              return result;
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<ProductType> deleteProductTypeWithRetry(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final ProductType productType) {
    // todo: add retry with RetryHandler.concurrentModification()
    return ctpClient
        .productTypes()
        .delete(productType)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Deletes all product type attributes from the CTP project defined by the {@code ctpClient} to
   * able to delete a product type if it is referenced by at least one product type.
   *
   * @param ctpClient defines the CTP project to delete the product types from.
   */
  private static void deleteProductTypeAttributes(@Nonnull final ProjectApiRoot ctpClient) {
    final ConcurrentHashMap<ProductType, Set<ProductTypeUpdateAction>> productTypesToUpdate =
        new ConcurrentHashMap<>();

    QueryUtils.queryAll(
            ctpClient.productTypes().get(),
            productTypes -> {
              productTypes.forEach(
                  productType -> {
                    final Set<ProductTypeUpdateAction> removeActions =
                        productType.getAttributes().stream()
                            .map(
                                attributeDefinition ->
                                    ProductTypeRemoveAttributeDefinitionActionBuilder.of()
                                        .name(attributeDefinition.getName())
                                        .build())
                            .collect(Collectors.toSet());

                    productTypesToUpdate.put(productType, removeActions);
                  });
            })
        .thenCompose(
            aVoid -> {
              return CompletableFuture.allOf(
                  productTypesToUpdate.entrySet().stream()
                      .map(entry -> removeAttributeDefinitionWithRetry(ctpClient, entry))
                      .toArray(CompletableFuture[]::new));
            })
        .toCompletableFuture()
        .join();

    try {
      // The removal of the attributes is eventually consistent.
      // Here with one second break we are slowing down the ITs a little bit so CTP could remove the
      // attributes.
      // see: SUPPORT-8408
      Thread.sleep(1000);
    } catch (InterruptedException expected) {
    }
  }

  private static CompletionStage<ProductType> removeAttributeDefinitionWithRetry(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final Map.Entry<ProductType, Set<ProductTypeUpdateAction>> entry) {

    // todo: add retry with RetryHandler.concurrentModification()

    return ctpClient
        .productTypes()
        .update(entry.getKey(), new ArrayList<>(entry.getValue()))
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * This method blocks to create a product Type on the CTP project defined by the supplied {@code
   * ctpClient} with the supplied data.
   *
   * @param productTypeKey the product type key
   * @param locale the locale to be used for specifying the product type name and field definitions
   *     names.
   * @param name the name of the product type.
   * @param ctpClient defines the CTP project to create the product type on.
   */
  public static void createProductType(
      @Nonnull final String productTypeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {
    if (!productTypeExists(productTypeKey, ctpClient)) {
      ProductTypeDraft productTypeDraft =
          ProductTypeDraft.builder()
              .key(productTypeKey)
              .name(name)
              .description("description")
              .attributes(buildAttributeDefinitionDrafts(locale))
              .build();
      ctpClient.productTypes().post(productTypeDraft).execute().join();
    }
  }

  /**
   * This method blocks to create a product type, which is defined by the JSON resource found in the
   * supplied {@code jsonResourcePath}, in the CTP project defined by the supplied {@code
   * ctpClient}.
   *
   * @param jsonResourcePath defines the path of the JSON resource of the product type.
   * @param ctpClient defines the CTP project to create the product type on.
   */
  //  public static ProductType createProductType(
  //      @Nonnull final String jsonResourcePath, @Nonnull final SphereClient ctpClient) {
  //    final ProductType productTypeFromJson =
  //        readObjectFromResource(jsonResourcePath, ProductType.class);
  //    final ProductTypeDraft productTypeDraft =
  //        ProductTypeDraftBuilder.of(productTypeFromJson).build();
  //    return ctpClient
  //        .execute(ProductTypeCreateCommand.of(productTypeDraft))
  //        .toCompletableFuture()
  //        .join();
  //  }

  /**
   * Builds a list of two field definitions; one for a {@link
   * io.sphere.sdk.products.attributes.LocalizedStringAttributeType} and one for a {@link
   * io.sphere.sdk.products.attributes.BooleanAttributeType}. The JSON of the created attribute
   * definition list looks as follows:
   *
   * <p>"attributes": [ { "name": "backgroundColor", "label": { "en": "backgroundColor" }, "type": {
   * "name": "LocalizedString" }, "inputHint": "SingleLine" }, { "name": "invisibleInShop", "label":
   * { "en": "invisibleInShop" }, "type": { "name": "Boolean" }, "inputHint": "SingleLine" } ]
   *
   * @param locale defines the locale for which the field definition names are going to be bound to.
   * @return the list of field definitions.
   */
  private static List<AttributeDefinitionDraft> buildAttributeDefinitionDrafts(
      @Nonnull final Locale locale) {
    return asList(
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeLocalizableTextType.of())
            .name(LOCALISED_STRING_ATTRIBUTE_NAME)
            .label(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        locale.toLanguageTag(), LOCALISED_STRING_ATTRIBUTE_NAME))
            .isRequired(false)
            .build(),
        AttributeDefinitionDraftBuilder.of()
            .type(AttributeBooleanType.of())
            .name(BOOLEAN_ATTRIBUTE_NAME)
            .label(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(locale.toLanguageTag(), BOOLEAN_ATTRIBUTE_NAME))
            .isRequired(false)
            .build());
  }

  private static boolean productTypeExists(
      @Nonnull final String productTypeKey, @Nonnull final ProjectApiRoot ctpClient) {
    try {
      final ProductType productType =
          ctpClient.productTypes().withKey(productTypeKey).get().execute().join().getBody();
      return productType != null;
    } catch (Exception e) {
      if (e.getCause() instanceof NotFoundException) {
        return false;
      }
      throw e;
    }
  }

  /**
   * Tries to fetch product type of {@code key} using {@code cptClient}.
   *
   * @param cptClient sphere client used to execute requests.
   * @param key key of requested product type.
   * @return {@link java.util.Optional} which may contain product type of {@code key}.
   */
  public static Optional<ProductType> getProductTypeByKey(
      @Nonnull final ProjectApiRoot cptClient, @Nonnull final String key) {
    return Optional.of(cptClient.productTypes().withKey(key).get().execute().join().getBody());
  }

  public static void assertAttributesAreEqual(
      @Nonnull final List<AttributeDefinition> attributes,
      @Nonnull final List<AttributeDefinitionDraft> attributesDrafts) {

    assertThat(attributes).hasSameSizeAs(attributesDrafts);
    IntStream.range(0, attributesDrafts.size())
        .forEach(
            index -> {
              final AttributeDefinition attribute = attributes.get(index);
              final AttributeDefinitionDraft attributeDraft = attributesDrafts.get(index);

              assertThat(attribute.getName()).isEqualTo(attributeDraft.getName());

              assertThat(attribute.getLabel()).isEqualTo(attributeDraft.getLabel());

              assertThat(attribute.getType()).isEqualTo(attributeDraft.getType());

              assertThat(attribute.getInputHint())
                  .isEqualTo(
                      ofNullable(attributeDraft.getInputHint()).orElse(TextInputHint.SINGLE_LINE));

              assertThat(attribute.getInputTip()).isEqualTo(attributeDraft.getInputTip());

              assertThat(attribute.getIsRequired()).isEqualTo(attributeDraft.getIsRequired());

              assertThat(attribute.getIsSearchable())
                  .isEqualTo(ofNullable(attributeDraft.getIsSearchable()).orElse(true));

              assertThat(attribute.getAttributeConstraint())
                  .isEqualTo(
                      ofNullable(attributeDraft.getAttributeConstraint())
                          .orElse(AttributeConstraintEnum.NONE));

              if (attribute.getType().getClass() == AttributeEnumType.class) {
                assertPlainEnumsValuesAreEqual(
                    ((AttributeEnumType) attribute.getType()).getValues(),
                    ((AttributeEnumType) attributeDraft.getType()).getValues());
              } else if (attribute.getType().getClass() == AttributeLocalizedEnumType.class) {
                assertLocalizedEnumsValuesAreEqual(
                    ((AttributeLocalizedEnumType) attribute.getType()).getValues(),
                    ((AttributeLocalizedEnumType) attributeDraft.getType()).getValues());
              }
            });
  }

  private static void assertPlainEnumsValuesAreEqual(
      @Nonnull final List<AttributePlainEnumValue> enumValues,
      @Nonnull final List<AttributePlainEnumValue> enumValuesDrafts) {

    IntStream.range(0, enumValuesDrafts.size())
        .forEach(
            index -> {
              final AttributePlainEnumValue enumValue = enumValues.get(index);
              final AttributePlainEnumValue enumValueDraft = enumValuesDrafts.get(index);

              assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
              assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
            });
  }

  private static void assertLocalizedEnumsValuesAreEqual(
      @Nonnull final List<AttributeLocalizedEnumValue> enumValues,
      @Nonnull final List<AttributeLocalizedEnumValue> enumValuesDrafts) {

    IntStream.range(0, enumValuesDrafts.size())
        .forEach(
            index -> {
              final AttributeLocalizedEnumValue enumValue = enumValues.get(index);
              final AttributeLocalizedEnumValue enumValueDraft = enumValuesDrafts.get(index);

              assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
              assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
            });
  }

  private ProductTypeITUtils() {}
}
