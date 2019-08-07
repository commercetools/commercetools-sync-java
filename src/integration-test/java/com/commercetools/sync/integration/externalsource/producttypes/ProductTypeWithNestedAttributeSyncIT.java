package com.commercetools.sync.integration.externalsource.producttypes;


import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.assertAttributesAreEqual;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.getProductTypeByKey;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateTargetProjectWithNestedAttributes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeWithNestedAttributeSyncIT {

    /**
     * Deletes product types from the target CTP project.
     * Populates target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        populateTargetProjectWithNestedAttributes();
    }

    /**
     * Deletes all the test data from the {@code CTP_TARGET_CLIENT} project that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithUpdatedProductType_ShouldUpdateProductType() {
        // preparation
        final Optional<ProductType> productType1 =
                getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3);

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts =
                productType1.get().getAttributes().stream()
                        .map(attribute -> {
                            if (attribute.getAttributeType() instanceof NestedAttributeType) {
                                return AttributeDefinitionDraftBuilder.of(attribute)
                                        .name(String.format("new_%s", attribute.getName()))
                                        .build();
                            }
                            return AttributeDefinitionDraftBuilder.of(attribute).build();
                        })
                        .collect(Collectors.toList());

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_3,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                attributeDefinitionDrafts
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(singletonList(newProductTypeDraft))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0);

        final Optional<ProductType> oldProductTypeAfter =
                getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertAttributesAreEqual(productType.getAttributes(), attributeDefinitionDrafts);
        });
    }

    @Test
    void sync_WithNewProductType_ShouldCreateProductType() {
        // preparation
        final Optional<ProductType> productType2 =
                getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_2);

        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder.of(
                AttributeDefinitionBuilder
                    .of("nestedattr", ofEnglish("nestedattr"),
                        SetAttributeType.of(NestedAttributeType.of(productType2.get())))
                    .build())
                .build();


        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_4,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                singletonList(nestedTypeAttr)
        );

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(singletonList(newProductTypeDraft))
                .toCompletableFuture().join();


        // assertions
        assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0);


        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertAttributesAreEqual(productType.getAttributes(), singletonList(nestedTypeAttr));
        });
    }

    @Test
    void sync_WithUpdatedProductType_WithNewNestedAttribute_ShouldUpdateProductTypeAddingAttribute() {
        // preparation
        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_4,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                singletonList(ATTRIBUTE_DEFINITION_DRAFT_3));

        final ProductType productType4 =
                CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(productTypeDraft4)).toCompletableFuture().join();

        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder.of(
                AttributeDefinitionBuilder
                        .of("nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productType4))
                        .isSearchable(false)
                        .build())
                .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_1,
                PRODUCT_TYPE_NAME_1,
                PRODUCT_TYPE_DESCRIPTION_1,
                asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
                .of(CTP_TARGET_CLIENT)
                .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(singletonList(newProductTypeDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(productTypeSyncStatistics).hasValues(4, 0, 1, 0);

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType ->
            assertAttributesAreEqual(productType.getAttributes(),
                asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr)
            ));
    }
}
