package com.commercetools.sync.integration.ctpprojectsource.producttypes;

import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

class ProductTypeSyncIT {

    /**
     * Deletes product types from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @BeforeEach
    void setup() {
        deleteProductTypesFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteProductTypesFromTargetAndSource();
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(ProductTypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<ProductTypeDraft> productTypeDrafts = productTypes
            .stream()
            .map(ProductTypeDraftBuilder::of)
            .map(ProductTypeDraftBuilder::build)
            .collect(Collectors.toList());

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<ProductType> productTypes = CTP_SOURCE_CLIENT
            .execute(ProductTypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<ProductTypeDraft> productTypeDrafts = productTypes
            .stream()
            .map(productType -> {
                final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productType
                    .getAttributes()
                    .stream()
                    .map(attribute -> AttributeDefinitionDraftBuilder.of(attribute).build())
                    .collect(Collectors.toList());

                return ProductTypeDraftBuilder
                    .of(
                        productType.getKey(),
                        "newName",
                        productType.getDescription(),
                        attributeDefinitionDrafts)
                    .build();
            })
            .collect(Collectors.toList());

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(productTypeDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
        assertThat(productTypeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
    }
}
