package com.commercetools.sync.integration.externalsource.producttypes;


import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ATTRIBUTE_DEFINITION_DRAFT_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_3;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_4;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.assertAttributesAreEqual;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.removeAttributeReferencesAndDeleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.getProductTypeByKey;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateTargetProjectWithNestedAttributes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ProductTypeWithNestedAttributeSyncIT {

    private ProductTypeSyncOptions productTypeSyncOptions;
    private List<UpdateAction<ProductType>> builtUpdateActions;
    private List<String> errorMessages;
    private List<Throwable> exceptions;

    /**
     * Deletes product types from the target CTP project.
     * Populates target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
        populateTargetProjectWithNestedAttributes();

        builtUpdateActions = new ArrayList<>();
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();

        productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .beforeUpdateCallback((actions, draft, oldProductType) -> {
                builtUpdateActions.addAll(actions);
                return actions;
            })
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();
    }

    /**
     * Deletes all the test data from the {@code CTP_TARGET_CLIENT} project that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithUpdatedProductType_ShouldUpdateProductType() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_3,
                PRODUCT_TYPE_NAME_3,
                PRODUCT_TYPE_DESCRIPTION_3,
                emptyList()
        );

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(singletonList(newProductTypeDraft))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).containsExactly(
            RemoveAttributeDefinition.of("nestedattr"),
            RemoveAttributeDefinition.of("nestedattr2")
        );
        assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 product types were processed in total"
                + " (0 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        final Optional<ProductType> oldProductTypeAfter =
                getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_3);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_3);
            assertAttributesAreEqual(productType.getAttributes(), emptyList());
        });
    }

    @Test
    void sync_WithNewProductTypeWithAnExistingReference_ShouldCreateProductType() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"),
                    NestedAttributeType.of(ProductType.referenceOfId(PRODUCT_TYPE_KEY_1)))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_4,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                singletonList(nestedTypeAttr)
        );

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(singletonList(newProductTypeDraft))
                .toCompletableFuture()
                .join();

        // assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertThat(productType.getAttributes())
                .hasSize(1)
                .extracting(AttributeDefinition::getAttributeType)
                .first()
                .satisfies(attributeType -> {
                    assertThat(attributeType).isInstanceOf(NestedAttributeType.class);
                    final NestedAttributeType nestedType = (NestedAttributeType) attributeType;
                    assertThat(nestedType.getTypeReference().getId())
                        .isEqualTo(
                            getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1)
                                .map(Resource::getId)
                                .orElse(null));
                });
        });
    }

    @Test
    void sync_WithNewProductTypeWithANonExistingReference_ShouldCreateProductType() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"),
                    SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId("non-existing-ref"))))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_4,
            PRODUCT_TYPE_NAME_4,
            PRODUCT_TYPE_DESCRIPTION_4,
            singletonList(nestedTypeAttr)
        );

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 1);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 1 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("non-existing-ref"))
            .containsOnlyKeys(PRODUCT_TYPE_KEY_4);
        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()
                                            .get("non-existing-ref")
                                            .get(PRODUCT_TYPE_KEY_4)).containsExactly(nestedTypeAttr);

        final Optional<ProductType> oldProductTypeAfter = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);

        assertThat(oldProductTypeAfter).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertThat(productType.getAttributes()).isEmpty();
        });
    }

    @Test
    void sync_WithNewProductTypeWithFailedFetchOnReferenceResolution_ShouldFail() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"),
                    NestedAttributeType.of(ProductType.referenceOfId(PRODUCT_TYPE_KEY_4)))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft withMissingNestedTypeRef = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_4,
            PRODUCT_TYPE_NAME_4,
            PRODUCT_TYPE_DESCRIPTION_4,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_3));

        final SphereClient ctpClient = spy(CTP_TARGET_CLIENT);
        final BadGatewayException badGatewayException = new BadGatewayException();
        when(ctpClient.execute(any(ResourceKeyIdGraphQlRequest.class))).thenCallRealMethod(); // should work on caching
        when(ctpClient.execute(any(ProductTypeQuery.class)))
            .thenCallRealMethod() // should work when fetching matching product types
            .thenCallRealMethod() // should work when second fetching matching product types
            .thenReturn(exceptionallyCompletedFuture(badGatewayException)) // fail on fetching during resolution
            .thenCallRealMethod(); // call the real method for the rest of the calls

        productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(ctpClient)
            .batchSize(1) // this ensures the drafts are in separate batches.
            .beforeUpdateCallback((actions, draft, oldProductType) -> {
                builtUpdateActions.addAll(actions);
                return actions;
            })
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(asList(withMissingNestedTypeRef, productTypeDraft4))
            .toCompletableFuture().join();

        // assertions
        final Optional<ProductType> productType1 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assert productType1.isPresent();
        assertThat(errorMessages).containsExactly("Failed to fetch existing product types with keys: '[key_1]'.");
        assertThat(exceptions).hasOnlyOneElementSatisfying(exception ->
            assertThat(exception.getCause()).hasCauseExactlyInstanceOf(BadGatewayException.class));
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0, 1);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 1 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
        final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
            children = productTypeSyncStatistics
            .getProductTypeKeysWithMissingParents().get(PRODUCT_TYPE_KEY_4);

        assertThat(children).hasSize(1);
        assertThat(children.get(PRODUCT_TYPE_KEY_1)).containsExactly(nestedTypeAttr);

        final Optional<ProductType> productType4 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);
        assertThat(productType4).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertThat(productType.getAttributes()).hasSize(1);
        });

        assertThat(productType1).hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(2));
    }

    @Test
    void sync_WithUpdatedProductType_WithNewNestedAttributeInSameBatch_ShouldUpdateProductTypeAddingAttribute() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"),
                    NestedAttributeType.of(ProductType.referenceOfId(PRODUCT_TYPE_KEY_1)))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_1,
                PRODUCT_TYPE_NAME_1,
                PRODUCT_TYPE_DESCRIPTION_1,
                asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_4,
            PRODUCT_TYPE_NAME_4,
            PRODUCT_TYPE_DESCRIPTION_4,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_3));


        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(asList(newProductTypeDraft, productTypeDraft4))
                .toCompletableFuture().join();

        // assertions
        final Optional<ProductType> productType1 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assert productType1.isPresent();
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).containsExactly(
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(AttributeDefinitionBuilder
                    .of("nestedattr", ofEnglish("nestedattr"),
                        NestedAttributeType.of(productType1.get()))
                    .build())
                // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets
                // it to true by default
                .searchable(false)
                .build())
        );
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
        final Optional<ProductType> productType4 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);
        assertThat(productType4).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertThat(productType.getAttributes()).hasSize(1);
        });

        assertThat(productType1).hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(3));
    }

    @Test
    void sync_WithUpdatedProductType_WithNewNestedAttributeInSeparateBatch_ShouldUpdateProductTypeAddingAttribute() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("nestedattr", ofEnglish("nestedattr"),
                    NestedAttributeType.of(ProductType.referenceOfId(PRODUCT_TYPE_KEY_1)))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_4,
            PRODUCT_TYPE_NAME_4,
            PRODUCT_TYPE_DESCRIPTION_4,
            singletonList(ATTRIBUTE_DEFINITION_DRAFT_3));

        productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(1)
            .beforeUpdateCallback((actions, draft, oldProductType) -> {
                builtUpdateActions.addAll(actions);
                return actions;
            })
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();


        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(asList(newProductTypeDraft, productTypeDraft4))
            .toCompletableFuture().join();

        // assertions
        final Optional<ProductType> productType1 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assert productType1.isPresent();
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).containsExactly(
            AddAttributeDefinition.of(AttributeDefinitionDraftBuilder
                .of(AttributeDefinitionBuilder
                    .of("nestedattr", ofEnglish("nestedattr"),
                        NestedAttributeType.of(productType1.get()))
                    .build())
                // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets
                // it to true by default
                .searchable(false)
                .build())
        );
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
        final Optional<ProductType> productType4 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_4);
        assertThat(productType4).hasValueSatisfying(productType -> {
            assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_4);
            assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_4);
            assertThat(productType.getAttributes()).hasSize(1);
        });

        assertThat(productType1).hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(3));
    }

    @Test
    void sync_WithUpdatedProductType_WithRemovedNestedAttributeInLaterBatch_ShouldReturnProperStatistics() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
            .of(AttributeDefinitionBuilder
                .of("newNested", ofEnglish("nestedattr"),
                    NestedAttributeType.of(ProductType.referenceOfId("non-existing-product-type")))
                .build())
            // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets it to
            // true by default
            .searchable(false)
            .build();

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // tests
        productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        final ProductTypeDraft newProductTypeDraftWithoutNested = ProductTypeDraft.ofAttributeDefinitionDrafts(
            PRODUCT_TYPE_KEY_1,
            PRODUCT_TYPE_NAME_1,
            PRODUCT_TYPE_DESCRIPTION_1,
            asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2));

        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraftWithoutNested))
            .toCompletableFuture().join();

        // assertions
        final Optional<ProductType> productType1 = getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
        assert productType1.isPresent();
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(2, 0, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (0 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
        assertThat(productType1).hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(2));
    }
}
