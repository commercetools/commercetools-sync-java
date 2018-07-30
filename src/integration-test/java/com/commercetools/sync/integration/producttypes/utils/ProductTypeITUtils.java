package com.commercetools.sync.integration.producttypes.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQueryBuilder;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

public class ProductTypeITUtils {
    public static final String PRODUCT_TYPE_KEY_1 = "key_1";
    public static final String PRODUCT_TYPE_KEY_2 = "key_2";

    public static final String PRODUCT_TYPE_NAME_1 = "name_1";
    public static final String PRODUCT_TYPE_NAME_2 = "name_2";

    public static final String PRODUCT_TYPE_DESCRIPTION_1 = "description_1";
    public static final String PRODUCT_TYPE_DESCRIPTION_2 = "description_2";

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_1 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_1",
            LocalizedString.ofEnglish("attr_label_1"),
            true
        )
        .attributeConstraint(AttributeConstraint.NONE)
        .inputTip(LocalizedString.ofEnglish("inputTip1"))
        .inputHint(TextInputHint.SINGLE_LINE)
        .isSearchable(false)
        .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_2 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_2",
            LocalizedString.ofEnglish("attr_label_2"),
            true
        )
        .attributeConstraint(AttributeConstraint.NONE)
        .inputTip(LocalizedString.ofEnglish("inputTip2"))
        .inputHint(TextInputHint.SINGLE_LINE)
        .isSearchable(false)
        .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_3 = AttributeDefinitionDraftBuilder
        .of(
            StringAttributeType.of(),
            "attr_name_3",
            LocalizedString.ofEnglish("attr_label_3"),
            true
        )
        .attributeConstraint(AttributeConstraint.NONE)
        .inputTip(LocalizedString.ofEnglish("inputTip3"))
        .inputHint(TextInputHint.SINGLE_LINE)
        .isSearchable(false)
        .build();


    public static final ProductTypeDraft productTypeDraft1 = ProductTypeDraft.ofAttributeDefinitionDrafts(
        PRODUCT_TYPE_KEY_1,
        PRODUCT_TYPE_NAME_1,
        PRODUCT_TYPE_DESCRIPTION_1,
        Arrays.asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
    );

    public static final ProductTypeDraft productTypeDraft2 = ProductTypeDraft.ofAttributeDefinitionDrafts(
        PRODUCT_TYPE_KEY_2,
        PRODUCT_TYPE_NAME_2,
        PRODUCT_TYPE_DESCRIPTION_2,
        Collections.singletonList(ATTRIBUTE_DEFINITION_DRAFT_1)
    );


    /**
     * Deletes all product types from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the product types will be deleted from.
     */
    public static void deleteProductTypes(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, ProductTypeQuery.of(), ProductTypeDeleteCommand::of);
    }


    /**
     * Deletes all product types from CTP projects defined by {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteProductTypesFromTargetAndSource() {
        deleteProductTypes(CTP_SOURCE_CLIENT);
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

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

    /**
     * Populate source CTP project.
     * Creates product type with key PRODUCT_TYPE_KEY_1, PRODUCT_TYPE_NAME_1, PRODUCT_TYPE_DESCRIPTION_1 and
     * attributes attributeDefinitionDraft1, attributeDefinitionDraft2.
     */
    public static void populateTargetProject() {
        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft1)).toCompletableFuture().join();
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
}
