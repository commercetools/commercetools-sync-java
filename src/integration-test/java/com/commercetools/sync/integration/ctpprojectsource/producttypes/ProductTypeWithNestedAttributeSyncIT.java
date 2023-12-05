package com.commercetools.sync.integration.ctpprojectsource.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_DESCRIPTION_5;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_KEY_5;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.PRODUCT_TYPE_NAME_5;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.assertAttributesAreEqual;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.getProductTypeByKey;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.populateProjectWithNestedAttributes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.removeAttributeReferencesAndDeleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeNestedTypeBuilder;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeSetTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.producttypes.utils.ProductTypeTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  void sync_WithProductTypeReferencingItselfAsAttribute_ShouldCreateProductType() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("selfReferenceAttr")
            .label(LocalizedString.ofEnglish("selfReferenceAttr"))
            .type(
                AttributeSetTypeBuilder.of()
                    .elementType(
                        AttributeNestedTypeBuilder.of()
                            .typeReference(
                                ProductTypeReferenceBuilder.of().id(PRODUCT_TYPE_KEY_5).build())
                            .build())
                    .build())
            .isSearchable(true)
            .isRequired(false)
            .build();

    final ProductTypeDraft oldProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(nestedTypeAttr)
            .build();

    // Sync productDraft with attribute referencing itself to source project
    new ProductTypeSync(ProductTypeSyncOptionsBuilder.of(CTP_SOURCE_CLIENT).build())
        .sync(List.of(oldProductTypeDraft))
        .toCompletableFuture()
        .join();
    final ProductType oldProductType =
        CTP_SOURCE_CLIENT
            .productTypes()
            .withKey(PRODUCT_TYPE_KEY_5)
            .get()
            .executeBlocking()
            .getBody();

    // test
    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        ProductTypeTransformUtils.toProductTypeDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, List.of(oldProductType))
            .thenCompose(newDrafts -> productTypeSync.sync(newDrafts))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    final Optional<ProductType> newProductType =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);
    assertThat(newProductType).isPresent();
    assertThat(newProductType)
        .hasValueSatisfying(
            productType -> {
              assertAttributesAreEqual(productType.getAttributes(), List.of(nestedTypeAttr));
              final AttributeDefinition attributeDefinition1 = productType.getAttributes().get(0);
              assertThat(attributeDefinition1.getType()).isInstanceOf(AttributeSetType.class);
              final AttributeSetType attributeSetType =
                  (AttributeSetType) attributeDefinition1.getType();
              assertThat(attributeSetType.getElementType()).isInstanceOf(AttributeNestedType.class);
              final AttributeNestedType attributeNestedType =
                  (AttributeNestedType) attributeSetType.getElementType();
              assertThat(attributeNestedType.getTypeReference())
                  .isInstanceOf(ProductTypeReference.class);
              assertThat(attributeNestedType.getTypeReference().getId())
                  .isEqualTo(productType.getId());
            });
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
