package com.commercetools.sync.integration.sdk2.ctpprojectsource.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.populateProjectWithNestedAttributes;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.removeAttributeReferencesAndDeleteProductTypes;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSync;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.sdk2.producttypes.utils.ProductTypeTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeWithNestedAttributeSyncIT {
  private ProductTypeSyncOptions productTypeSyncOptions;
  private List<ProductTypeUpdateAction> builtUpdateActions;
  private List<String> errorMessages;
  private List<Throwable> exceptions;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  /**
   * Deletes product types from source and target CTP projects. Populates source and target CTP
   * projects with test data.
   */
  @BeforeEach
  void setup() {
    removeAttributeReferencesAndDeleteProductTypes(CTP_SOURCE_CLIENT);
    removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
    populateProjectWithNestedAttributes(CTP_SOURCE_CLIENT);

    builtUpdateActions = new ArrayList<>();
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();

    productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .beforeUpdateCallback(
                (actions, draft, oldProductType) -> {
                  builtUpdateActions.addAll(actions);
                  return actions;
                })
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    removeAttributeReferencesAndDeleteProductTypes(CTP_SOURCE_CLIENT);
    removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_WithEmptyTargetProject_ShouldReturnProperStatistics() {
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
        ProductTypeTransformUtils.toProductTypeDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, productTypes)
            .join();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(4, 4, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 4 product types were processed in total"
                + " (4 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }

  @Test
  void sync_WithOneDraftPerBatchOnEmptyProject_ShouldReturnProperStatistics() {
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
        ProductTypeTransformUtils.toProductTypeDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, productTypes)
            .join();

    productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .batchSize(1)
            .beforeUpdateCallback(
                (actions, draft, oldProductType) -> {
                  builtUpdateActions.addAll(actions);
                  return actions;
                })
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(4, 4, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 4 product types were processed in total"
                + " (4 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {
    // preparation
    populateProjectWithNestedAttributes(CTP_TARGET_CLIENT);
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
        ProductTypeTransformUtils.toProductTypeDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, productTypes)
            .join();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(4, 0, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 4 product types were processed in total"
                + " (0 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {
    // preparation
    populateProjectWithNestedAttributes(CTP_TARGET_CLIENT);

    final List<ProductType> productTypes =
        CTP_SOURCE_CLIENT
            .productTypes()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductTypePagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    // only update the nested types
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, productTypes)
            .join()
            .stream()
            .map(
                productType -> {
                  final List<AttributeDefinitionDraft> attributeDefinitionDrafts =
                      productType.getAttributes().stream()
                          .map(
                              attribute -> {
                                if (attribute.getType() instanceof AttributeNestedType) {
                                  return AttributeDefinitionDraftBuilder.of(attribute)
                                      .label(ofEnglish("new-label"))
                                      .build();
                                }
                                return AttributeDefinitionDraftBuilder.of(attribute).build();
                              })
                          .collect(Collectors.toList());

                  return ProductTypeDraftBuilder.of(productType)
                      .attributes(attributeDefinitionDrafts)
                      .build();
                })
            .collect(Collectors.toList());

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(productTypeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions)
        .containsExactly(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName("nestedattr2")
                .label(ofEnglish("new-label"))
                .build());
    assertThat(productTypeSyncStatistics).hasValues(4, 0, 1, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 4 product types were processed in total"
                + " (0 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");
  }
}
