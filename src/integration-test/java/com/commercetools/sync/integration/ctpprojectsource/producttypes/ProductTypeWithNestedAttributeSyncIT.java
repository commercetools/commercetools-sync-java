package com.commercetools.sync.integration.ctpprojectsource.producttypes;

import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeDefinitionLabel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateSourcesProjectWithNestedAttributes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateTargetProjectWithNestedAttributes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.removeAttributeReferencesAndDeleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceReplacementUtils.buildProductTypeQuery;
import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceReplacementUtils.replaceProductTypesReferenceIdsWithKeys;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeWithNestedAttributeSyncIT {
    private ProductTypeSyncOptions productTypeSyncOptions;
    private List<UpdateAction<ProductType>> builtUpdateActions;
    private List<String> errorMessages;
    private List<Throwable> exceptions;

    /**
     * Deletes product types from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @BeforeEach
    void setup() {
        removeAttributeReferencesAndDeleteProductTypes(CTP_SOURCE_CLIENT);
        removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
        populateSourcesProjectWithNestedAttributes();

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
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        removeAttributeReferencesAndDeleteProductTypes(CTP_SOURCE_CLIENT);
        removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithEmptyTargetProject_ShouldReturnProperStatistics() {
        // preparation
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(buildProductTypeQuery(1))
            .toCompletableFuture()
            .join()
            .getResults();

        final List<ProductTypeDraft> productTypeDrafts =
            replaceProductTypesReferenceIdsWithKeys(productTypes);

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(4, 4, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 4 product types were processed in total"
                + " (4 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }

    @Test
    void sync_WithOneDraftPerBatchOnEmptyProject_ShouldReturnProperStatistics() {
        // preparation
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(buildProductTypeQuery(1))
            .toCompletableFuture()
            .join()
            .getResults();

        final List<ProductTypeDraft> productTypeDrafts =
            replaceProductTypesReferenceIdsWithKeys(productTypes);

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

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(4, 4, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 4 product types were processed in total"
                + " (4 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {
        // preparation
        populateTargetProjectWithNestedAttributes();
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
                .execute(buildProductTypeQuery(1))
                .toCompletableFuture()
                .join()
                .getResults();

        final List<ProductTypeDraft> productTypeDrafts =
                replaceProductTypesReferenceIdsWithKeys(productTypes);

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(4, 1, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 4 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {
        // preparation
        populateTargetProjectWithNestedAttributes();
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
                .execute(buildProductTypeQuery(1))
                .toCompletableFuture()
                .join()
                .getResults();

        //only update the nested types
        final List<ProductTypeDraft> productTypeDrafts = replaceProductTypesReferenceIdsWithKeys(productTypes)
                .stream()
                .map(productType -> {
                    final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productType
                            .getAttributes()
                            .stream()
                            .map(attribute -> {
                                if (attribute.getAttributeType() instanceof NestedAttributeType) {
                                    return AttributeDefinitionDraftBuilder.of(attribute)
                                            .label(ofEnglish("new-label"))
                                            .build();
                                }
                                return AttributeDefinitionDraftBuilder.of(attribute).build();
                            })
                            .collect(Collectors.toList());

                    return ProductTypeDraftBuilder
                            .of(productType)
                            .attributes(attributeDefinitionDrafts)
                            .build();
                })
                .collect(Collectors.toList());

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(productTypeDrafts)
                .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(builtUpdateActions).containsExactly(
            ChangeAttributeDefinitionLabel.of("nestedattr", ofEnglish("new-label")),
            ChangeAttributeDefinitionLabel.of("nestedattr2", ofEnglish("new-label"))
        );
        assertThat(productTypeSyncStatistics).hasValues(4, 1, 1, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage()).isEqualTo(
            "Summary: 4 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }
}
