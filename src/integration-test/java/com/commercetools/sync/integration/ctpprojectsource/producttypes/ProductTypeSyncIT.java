package com.commercetools.sync.integration.ctpprojectsource.producttypes;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.producttypes.helpers.ResourceToDraftConverters;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeSyncIT {

  /**
   * Deletes product types from source and target CTP projects. Populates source and target CTP
   * projects with test data.
   */
  @BeforeEach
  void setup() {
    deleteProductTypesFromTargetAndSource();
    populateSourceProject();
    populateTargetProject();
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    deleteProductTypesFromTargetAndSource();
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<ProductType> productTypes =
        CTP_SOURCE_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    final List<ProductTypeDraft> productTypeDrafts =
        productTypes.stream()
            .map(ResourceToDraftConverters::toProductTypeDraft)
            .collect(Collectors.toList());

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<ProductType> productTypes =
        CTP_SOURCE_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    final List<ProductTypeDraft> productTypeDrafts =
        productTypes.stream()
            .map(
                productType -> {
                  final List<AttributeDefinitionDraft> attributeDefinitionDrafts =
                      productType.getAttributes().stream()
                          .map(
                              attribute ->
                                  ResourceToDraftConverters.toAttributeDefinitionDraftBuilder(
                                          attribute)
                                      .build())
                          .collect(Collectors.toList());

                  return ProductTypeDraftBuilder.of()
                      .key(productType.getKey())
                      .name("newName")
                      .description(productType.getDescription())
                      .attributes(attributeDefinitionDrafts)
                      .build();
                })
            .collect(Collectors.toList());

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }
}
