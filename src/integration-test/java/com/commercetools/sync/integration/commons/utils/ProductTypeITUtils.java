package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.BooleanAttributeType;
import io.sphere.sdk.products.attributes.LocalizedStringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Arrays.asList;

public final class ProductTypeITUtils {
    private static final String LOCALISED_STRING_ATTRIBUTE_NAME = "backgroundColor";
    private static final String BOOLEAN_ATTRIBUTE_NAME = "invisibleInShop";

    /**
     * Deletes all ProductTypes from CTP projects defined by the {@code sphereClient}.
     *
     * @param sphereClient defines the CTP project to delete the ProductTypes from.
     */
    private static void deleteProductTypes(@Nonnull final SphereClient sphereClient) {
        final Consumer<List<ProductType>> productTypePageDeleter =
            productTypePage -> {
                final List<CompletableFuture<ProductType>> deletionFutures =
                    productTypePage.stream()
                                   .map(ProductTypeDeleteCommand::of)
                                   .map(sphereClient::execute)
                                   .map(CompletionStage::toCompletableFuture)
                                   .collect(Collectors.toList());
                CompletableFuture.allOf(deletionFutures.toArray(new CompletableFuture[deletionFutures.size()]))
                                 .join();
            };
        CtpQueryUtils.queryAll(sphereClient, ProductTypeQuery.of(), productTypePageDeleter);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} ProductTypes from CTP
     * projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteProductTypesFromTargetAndSource() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        deleteProductTypes(CTP_SOURCE_CLIENT);
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
                .of(productTypeKey, name, "description", buildAttributeDefinitions(locale))
                .build();
            ctpClient.execute(ProductTypeCreateCommand.of(productTypeDraft)).toCompletableFuture().join();
        }
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
    private static List<AttributeDefinition> buildAttributeDefinitions(@Nonnull final Locale locale) {
        return asList(
            AttributeDefinitionBuilder.of(LOCALISED_STRING_ATTRIBUTE_NAME,
                LocalizedString.of(locale, LOCALISED_STRING_ATTRIBUTE_NAME), LocalizedStringAttributeType.of())
                                      .build(),
            AttributeDefinitionBuilder.of(BOOLEAN_ATTRIBUTE_NAME,
                LocalizedString.of(locale, BOOLEAN_ATTRIBUTE_NAME), BooleanAttributeType.of())
                                      .build()
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
}
