package com.commercetools.sync.integration.externalsource.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createBadGatewayException;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeAddAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSync;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.producttypes.helpers.ProductTypeSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductTypeWithNestedAttributeSyncIT {

  private ProductTypeSyncOptions productTypeSyncOptions;
  private List<ProductTypeUpdateAction> builtUpdateActions;
  private List<String> errorMessages;
  private List<Throwable> exceptions;

  /**
   * Deletes product types from the target CTP project. Populates target CTP project with test data.
   */
  @BeforeEach
  void setup() {
    removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
    populateProjectWithNestedAttributes(CTP_TARGET_CLIENT);

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
  }

  /**
   * Deletes all the test data from the {@code CTP_TARGET_CLIENT} project that were set up in this
   * test class.
   */
  @AfterAll
  static void tearDown() {
    removeAttributeReferencesAndDeleteProductTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_WithUpdatedProductType_ShouldUpdateProductType() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_3)
            .name(PRODUCT_TYPE_NAME_3)
            .description(PRODUCT_TYPE_DESCRIPTION_3)
            .attributes(emptyList())
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions)
        .containsExactly(
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("nestedattr1").build(),
            ProductTypeRemoveAttributeDefinitionActionBuilder.of().name("nestedattr2").build());
    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 product types were processed in total"
                + " (0 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_3);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_3);
              assertAttributesAreEqual(productType.getAttributes(), emptyList());
            });
  }

  @Test
  void sync_WithNewProductTypeWithAnExistingReference_ShouldCreateProductType() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr3 =
        AttributeDefinitionDraftBuilder.of()
            .name("nestedattr3")
            .label(ofEnglish("nestedattr3"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id(PRODUCT_TYPE_KEY_3))))
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(nestedTypeAttr3)
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_5);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_5);
              assertThat(productType.getAttributes())
                  .hasSize(1)
                  .extracting(AttributeDefinition::getType)
                  .first()
                  .satisfies(
                      attributeType -> {
                        assertThat(attributeType).isInstanceOf(AttributeSetType.class);
                        final AttributeSetType setAttributeType = (AttributeSetType) attributeType;
                        final AttributeNestedType nestedType =
                            (AttributeNestedType) setAttributeType.getElementType();
                        assertThat(nestedType.getTypeReference().getId())
                            .isEqualTo(
                                getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_3)
                                    .map(ProductType::getId)
                                    .orElse(null));
                      });
            });
  }

  @Test
  void sync_WithNewProductTypeWithANonExistingReference_ShouldCreateProductType() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id("non-existing-ref"))))
            // isSearchable=true is not supported for attribute type 'nested' and
            // AttributeDefinitionBuilder sets it to
            // true by default
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(nestedTypeAttr)
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(1, 1, 0, 0, 1);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 1 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("non-existing-ref"))
        .containsOnlyKeys(PRODUCT_TYPE_KEY_5);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("non-existing-ref")
                .get(PRODUCT_TYPE_KEY_5))
        .containsExactly(nestedTypeAttr);

    final Optional<ProductType> oldProductTypeAfter =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);

    assertThat(oldProductTypeAfter)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_5);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_5);
              assertThat(productType.getAttributes()).isEmpty();
            });
  }

  @Test
  void sync_WithNewProductTypeWithFailedFetchOnReferenceResolution_ShouldFail() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder ->
                                productTypeReferenceBuilder.id(PRODUCT_TYPE_KEY_5)))
            // isSearchable=true is not supported for attribute type 'nested'
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft withMissingNestedTypeRef =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr)
            .build();

    final ProductTypeDraft productTypeDraft5 =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_3)
            .build();

    final ProjectApiRoot ctpClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(ctpClient)
            .batchSize(1) // this ensures the drafts are in separate batches.
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

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync
            .sync(asList(withMissingNestedTypeRef, productTypeDraft5))
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<ProductType> productType1 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
    assert productType1.isPresent();
    assertThat(errorMessages)
        .containsExactly("Failed to fetch existing product types with keys: '[key_1]'.");
    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BadGatewayException.class);
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0, 1);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (1 created, 0 updated, 0 failed to sync and 1 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        children =
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get(PRODUCT_TYPE_KEY_5);

    assertThat(children).hasSize(1);
    assertThat(children.get(PRODUCT_TYPE_KEY_1)).containsExactly(nestedTypeAttr);

    final Optional<ProductType> productType5 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);
    assertThat(productType5)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_5);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_5);
              assertThat(productType.getAttributes()).hasSize(1);
            });

    assertThat(productType1)
        .hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(2));
  }

  @Test
  void
      sync_WithUpdatedProductType_WithNewNestedAttributeInSameBatch_ShouldUpdateProductTypeAddingAttribute() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder2 ->
                                            attributeTypeBuilder2
                                                .setBuilder()
                                                .elementType(
                                                    attributeTypeBuilder3 ->
                                                        attributeTypeBuilder3
                                                            .setBuilder()
                                                            .elementType(
                                                                attributeTypeBuilder4 ->
                                                                    attributeTypeBuilder4
                                                                        .nestedBuilder()
                                                                        .typeReference(
                                                                            ProductTypeReferenceBuilder
                                                                                .of()
                                                                                .id(
                                                                                    PRODUCT_TYPE_KEY_1)
                                                                                .build()))))))
            // isSearchable=true is not supported for attribute type 'nested'
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr)
            .build();

    final ProductTypeDraft productTypeDraft5 =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_3)
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync
            .sync(asList(newProductTypeDraft, productTypeDraft5))
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<ProductType> productType1 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
    assert productType1.isPresent();
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions)
        .containsExactly(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .name("nestedattr")
                        .label(ofEnglish("nestedattr"))
                        .type(
                            attributeTypeBuilder ->
                                attributeTypeBuilder
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder1 ->
                                            attributeTypeBuilder1
                                                .setBuilder()
                                                .elementType(
                                                    attributeTypeBuilder2 ->
                                                        attributeTypeBuilder2
                                                            .setBuilder()
                                                            .elementType(
                                                                attributeTypeBuilder3 ->
                                                                    attributeTypeBuilder3
                                                                        .setBuilder()
                                                                        .elementType(
                                                                            attributeTypeBuilder4 ->
                                                                                attributeTypeBuilder4
                                                                                    .nestedBuilder()
                                                                                    .typeReference(
                                                                                        productTypeReferenceBuilder ->
                                                                                            productTypeReferenceBuilder
                                                                                                .id(
                                                                                                    productType1
                                                                                                        .get()
                                                                                                        .getId())))))))
                        // isSearchable=true is not supported for attribute type 'nested'
                        .isSearchable(false)
                        .isRequired(false)
                        .build())
                .build());
    assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
    final Optional<ProductType> productType5 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);
    assertThat(productType5)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_5);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_5);
              assertThat(productType.getAttributes()).hasSize(1);
            });

    assertThat(productType1)
        .hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(3));
  }

  @Test
  void
      sync_WithUpdatedProductType_WithNewNestedAttributeInSeparateBatch_ShouldUpdateProductTypeAddingAttribute() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder2 ->
                                            attributeTypeBuilder2
                                                .setBuilder()
                                                .elementType(
                                                    attributeTypeBuilder3 ->
                                                        attributeTypeBuilder3
                                                            .setBuilder()
                                                            .elementType(
                                                                attributeTypeBuilder4 ->
                                                                    attributeTypeBuilder4
                                                                        .nestedBuilder()
                                                                        .typeReference(
                                                                            productTypeReferenceBuilder ->
                                                                                productTypeReferenceBuilder
                                                                                    .id(
                                                                                        PRODUCT_TYPE_KEY_1)))))))
            // isSearchable=true is not supported for attribute type 'nested'
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr)
            .build();

    final ProductTypeDraft productTypeDraft5 =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_5)
            .name(PRODUCT_TYPE_NAME_5)
            .description(PRODUCT_TYPE_DESCRIPTION_5)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_3)
            .build();

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

    // tests
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync
            .sync(asList(newProductTypeDraft, productTypeDraft5))
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<ProductType> productType1 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
    assert productType1.isPresent();
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions)
        .containsExactly(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(
                    AttributeDefinitionDraftBuilder.of()
                        .name("nestedattr")
                        .label(ofEnglish("nestedattr"))
                        .type(
                            attributeTypeBuilder ->
                                attributeTypeBuilder
                                    .setBuilder()
                                    .elementType(
                                        attributeTypeBuilder1 ->
                                            attributeTypeBuilder1
                                                .setBuilder()
                                                .elementType(
                                                    attributeTypeBuilder2 ->
                                                        attributeTypeBuilder2
                                                            .setBuilder()
                                                            .elementType(
                                                                attributeTypeBuilder3 ->
                                                                    attributeTypeBuilder3
                                                                        .setBuilder()
                                                                        .elementType(
                                                                            attributeTypeBuilder4 ->
                                                                                attributeTypeBuilder4
                                                                                    .nestedBuilder()
                                                                                    .typeReference(
                                                                                        productTypeReferenceBuilder ->
                                                                                            productTypeReferenceBuilder
                                                                                                .id(
                                                                                                    productType1
                                                                                                        .get()
                                                                                                        .getId())))))))
                        // isSearchable=true is not supported for attribute type 'nested'
                        .isSearchable(false)
                        .isRequired(false)
                        .build())
                .build());
    assertThat(productTypeSyncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (1 created, 1 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
    final Optional<ProductType> productType5 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_5);
    assertThat(productType5)
        .hasValueSatisfying(
            productType -> {
              assertThat(productType.getName()).isEqualTo(PRODUCT_TYPE_NAME_5);
              assertThat(productType.getDescription()).isEqualTo(PRODUCT_TYPE_DESCRIPTION_5);
              assertThat(productType.getAttributes()).hasSize(1);
            });

    assertThat(productType1)
        .hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(3));
  }

  @Test
  void
      sync_WithUpdatedProductType_WithRemovedNestedAttributeInLaterBatch_ShouldReturnProperStatistics() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttr =
        AttributeDefinitionDraftBuilder.of()
            .name("newNested")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder ->
                                productTypeReferenceBuilder.id("non-existing-product-type")))
            // isSearchable=true is not supported for attribute type 'nested'
            .isSearchable(false)
            .isRequired(false)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr)
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

    // tests
    productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    final ProductTypeDraft newProductTypeDraftWithoutNested =
        ProductTypeDraftBuilder.of()
            .key(PRODUCT_TYPE_KEY_1)
            .name(PRODUCT_TYPE_NAME_1)
            .description(PRODUCT_TYPE_DESCRIPTION_1)
            .attributes(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2)
            .build();

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync
            .sync(singletonList(newProductTypeDraftWithoutNested))
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<ProductType> productType1 =
        getProductTypeByKey(CTP_TARGET_CLIENT, PRODUCT_TYPE_KEY_1);
    assert productType1.isPresent();
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(builtUpdateActions).isEmpty();
    assertThat(productTypeSyncStatistics).hasValues(2, 0, 0, 0, 0);
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 product types were processed in total"
                + " (0 created, 0 updated, 0 failed to sync and 0 product types with at least one NestedType or a Set"
                + " of NestedType attribute definition(s) referencing a missing product type).");

    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
    assertThat(productType1)
        .hasValueSatisfying(productType -> assertThat(productType.getAttributes()).hasSize(2));
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final AtomicInteger counter = new AtomicInteger(0);

    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("product-types") && counter.getAndIncrement() > 2) {
                    // Why counter is comparing with 2?
                    // 1. should work when fetching matching product types
                    // 2. should work when second fetching matching product types
                    // 3. fail on fetching during resolution
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createBadGatewayException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }
}
