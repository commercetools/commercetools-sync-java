package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.BooleanAttributeType;
import io.sphere.sdk.products.attributes.LocalizedStringAttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQueryBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceReplacementUtils.buildProductTypeQuery;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class ProductTypeITUtils {
    private static final String LOCALISED_STRING_ATTRIBUTE_NAME = "backgroundColor";
    private static final String BOOLEAN_ATTRIBUTE_NAME = "invisibleInShop";

    public static final String PRODUCT_TYPE_KEY_1 = "key_1";
    public static final String PRODUCT_TYPE_KEY_2 = "key_2";
    public static final String PRODUCT_TYPE_KEY_3 = "key_3";
    public static final String PRODUCT_TYPE_KEY_4 = "key_4";

    public static final String PRODUCT_TYPE_NAME_1 = "name_1";
    public static final String PRODUCT_TYPE_NAME_2 = "name_2";
    public static final String PRODUCT_TYPE_NAME_3 = "name_3";
    public static final String PRODUCT_TYPE_NAME_4 = "name_4";

    public static final String PRODUCT_TYPE_DESCRIPTION_1 = "description_1";
    public static final String PRODUCT_TYPE_DESCRIPTION_2 = "description_2";
    public static final String PRODUCT_TYPE_DESCRIPTION_3 = "description_3";
    public static final String PRODUCT_TYPE_DESCRIPTION_4 = "description_4";

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_1 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_1",
            LocalizedString.ofEnglish("attr_label_1"),
            true
        )
        .inputTip(LocalizedString.ofEnglish("inputTip1"))
        .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_2 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_2",
            LocalizedString.ofEnglish("attr_label_2"),
            true
        )
        .inputTip(LocalizedString.ofEnglish("inputTip2"))
        .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_3 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_3",
            LocalizedString.ofEnglish("attr_label_3"),
            true
        )
        .inputTip(LocalizedString.ofEnglish("inputTip3"))
        .build();


    public static final ProductTypeDraft productTypeDraft1 = ProductTypeDraft.ofAttributeDefinitionDrafts(
        PRODUCT_TYPE_KEY_1,
        PRODUCT_TYPE_NAME_1,
        PRODUCT_TYPE_DESCRIPTION_1,
        asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
    );

    public static final ProductTypeDraft productTypeDraft2 = ProductTypeDraft.ofAttributeDefinitionDrafts(
        PRODUCT_TYPE_KEY_2,
        PRODUCT_TYPE_NAME_2,
        PRODUCT_TYPE_DESCRIPTION_2,
        singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
    );

    /**
     * Populate source CTP project.
     * Creates product type with key PRODUCT_TYPE_KEY_1, PRODUCT_TYPE_NAME_1, PRODUCT_TYPE_DESCRIPTION_1 and
     * attributes attributeDefinitionDraft1, attributeDefinitionDraft2.
     * Creates product type with key PRODUCT_TYPE_KEY_2, PRODUCT_TYPE_NAME_2, PRODUCT_TYPE_DESCRIPTION_2 and
     * attributes attributeDefinitionDraft1.
     */
    public static void populateSourceProject() {
        CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft1)).toCompletableFuture().join();
        CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft2)).toCompletableFuture().join();
    }

    public static void populateSourcesProjectWithNestedAttributes() {
        final ProductType productType1=
                CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft1)).toCompletableFuture().join();
        final ProductType productType2 =
                CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft2)).toCompletableFuture().join();

        final AttributeDefinition nestedTypeAttr1 = AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productType1))
                .isSearchable(false) // "isSearchable=true is not supported for attribute type 'nested'."
                .build();

        final AttributeDefinition nestedTypeAttr2 = AttributeDefinitionBuilder
                .of("nestedattr2", ofEnglish("nestedattr2"), NestedAttributeType.of(productType2))
                .isSearchable(false)
                .build();

        final ProductTypeDraft productTypeDraft3 = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_3,
                PRODUCT_TYPE_NAME_3,
                PRODUCT_TYPE_DESCRIPTION_3,
                asList(AttributeDefinitionDraftBuilder.of(nestedTypeAttr1).build(),
                        AttributeDefinitionDraftBuilder.of(nestedTypeAttr2).build()));

        CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft3)).toCompletableFuture().join();

        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_4,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                singletonList(AttributeDefinitionDraftBuilder.of(nestedTypeAttr1).build()));

        CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft4)).toCompletableFuture().join();
    }

    /**
     * Populate source CTP project.
     * Creates product type with key PRODUCT_TYPE_KEY_1, PRODUCT_TYPE_NAME_1, PRODUCT_TYPE_DESCRIPTION_1 and
     * attributes attributeDefinitionDraft1, attributeDefinitionDraft2.
     */
    public static void populateTargetProject() {
        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft1)).toCompletableFuture().join();
    }

    public static void populateTargetProjectWithNestedAttributes() {
        final ProductType productType1=
                CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft1)).toCompletableFuture().join();

        final ProductType productType2 =
                CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft2)).toCompletableFuture().join();

        final AttributeDefinition nestedTypeAttr1 = AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productType1))
                .isSearchable(false)
                .build();

        final AttributeDefinition nestedTypeAttr2 = AttributeDefinitionBuilder
                .of("nestedattr2", ofEnglish("nestedattr2"), NestedAttributeType.of(productType2))
                .isSearchable(false)
                .build();

        final ProductTypeDraft productTypeDraft3 = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_3,
                PRODUCT_TYPE_NAME_3,
                PRODUCT_TYPE_DESCRIPTION_3,
                asList(AttributeDefinitionDraftBuilder.of(nestedTypeAttr1).build(),
                        AttributeDefinitionDraftBuilder.of(nestedTypeAttr2).build()));

        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft3)).toCompletableFuture().join();
    }

    /**
     * Deletes all ProductTypes from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
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
    public static void deleteProductTypes(@Nonnull final SphereClient ctpClient) {
        deleteProductTypeAttributes(ctpClient);
        queryAndExecute(ctpClient, ProductTypeQuery.of(), ProductTypeDeleteCommand::of);
    }

    /**
     * Deletes all product type attributes from the CTP project defined by the {@code ctpClient} to able to
     * delete a product-type if it is referenced by at least one product-type.
     *
     * @param ctpClient defines the CTP project to delete the product types from.
     */
    private static void deleteProductTypeAttributes(@Nonnull final SphereClient ctpClient) {
        final List<ProductType> productTypes =  ctpClient
                .execute(buildProductTypeQuery(1))
                .toCompletableFuture()
                .join()
                .getResults();

        productTypes.forEach(productType -> {
            final List<UpdateAction<ProductType>> updateActions = productType.getAttributes().stream()
                    .map(attributeDefinition -> RemoveAttributeDefinition.of(attributeDefinition.getName()))
                    .collect(Collectors.toList());

            ctpClient.execute(ProductTypeUpdateCommand.of(productType, updateActions)).toCompletableFuture().join();
        });
    }

    /**
     * This method blocks to create a product Type on the CTP project defined by the supplied {@code ctpClient} with
     * the supplied data.
     *
     * @param productTypeKey the product type key
     * @param locale         the locale to be used for specifying the product type name and field definitions names.
     * @param name           the name of the product type.
     * @param ctpClient      defines the CTP project to create the product type on.
     */
    public static void createProductType(@Nonnull final String productTypeKey,
                                         @Nonnull final Locale locale,
                                         @Nonnull final String name,
                                         @Nonnull final SphereClient ctpClient) {
        if (!productTypeExists(productTypeKey, ctpClient)) {
            final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder
                .of(productTypeKey, name, "description", buildAttributeDefinitionDrafts(locale))
                .build();
            ctpClient.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();
        }
    }

    /**
     * This method blocks to create a product type, which is defined by the JSON resource found in the supplied
     * {@code jsonResourcePath}, in the CTP project defined by the supplied {@code ctpClient}.
     *
     * @param jsonResourcePath defines the path of the JSON resource of the product type.
     * @param ctpClient        defines the CTP project to create the product type on.
     */
    public static ProductType createProductType(@Nonnull final String jsonResourcePath,
                                                @Nonnull final SphereClient ctpClient) {
        final ProductType productTypeFromJson = readObjectFromResource(jsonResourcePath, ProductType.class);
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder.of(productTypeFromJson)
                                                                         .build();
        return ctpClient.execute(ProductTypeCreateCommand.of(productTypeDraft))
                        .toCompletableFuture().join();
    }

    /**
     * Builds a list of two field definitions; one for a {@link LocalizedStringAttributeType} and one for a
     * {@link BooleanAttributeType}. The JSON of the created attribute definition list looks as follows:
     *
     * <p>"attributes": [
     * {
     * "name": "backgroundColor",
     * "label": {
     * "en": "backgroundColor"
     * },
     * "type": {
     * "name": "LocalizedString"
     * },
     * "inputHint": "SingleLine"
     * },
     * {
     * "name": "invisibleInShop",
     * "label": {
     * "en": "invisibleInShop"
     * },
     * "type": {
     * "name": "Boolean"
     * },
     * "inputHint": "SingleLine"
     * }
     * ]
     *
     * @param locale defines the locale for which the field definition names are going to be bound to.
     * @return the list of field definitions.
     */
    private static List<AttributeDefinitionDraft> buildAttributeDefinitionDrafts(@Nonnull final Locale locale) {
        return asList(
            AttributeDefinitionDraftBuilder.of(LocalizedStringAttributeType.of(), LOCALISED_STRING_ATTRIBUTE_NAME,
                LocalizedString.of(locale, LOCALISED_STRING_ATTRIBUTE_NAME), false).build(),
            AttributeDefinitionDraftBuilder.of(BooleanAttributeType.of(), BOOLEAN_ATTRIBUTE_NAME,
                LocalizedString.of(locale, BOOLEAN_ATTRIBUTE_NAME), false).build()
        );

    }

    private static boolean productTypeExists(@Nonnull final String productTypeKey,
                                             @Nonnull final SphereClient ctpClient) {
        final Optional<ProductType> productTypeOptional = ctpClient
            .execute(ProductTypeQuery.of().byKey(productTypeKey))
            .toCompletableFuture()
            .join().head();
        return productTypeOptional.isPresent();
    }

    /**
     * Tries to fetch product type of {@code key} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests.
     * @param key          key of requested product type.
     * @return {@link Optional} which may contain product type of {@code key}.
     */
    public static Optional<ProductType> getProductTypeByKey(
        @Nonnull final SphereClient sphereClient,
        @Nonnull final String key) {

        final ProductTypeQuery query = ProductTypeQueryBuilder
            .of()
            .plusPredicates(queryModel -> queryModel.key().is(key))
            .build();

        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    private ProductTypeITUtils() {
    }
}
