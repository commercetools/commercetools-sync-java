package com.commercetools.sync.producttypes.helpers;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProductTypeSyncStatisticsTest {

  @Test
  void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
    // preparation
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        mock(ProductTypeSyncStatistics.class);
    when(productTypeSyncStatistics.getCreated()).thenReturn(new AtomicInteger(1));
    when(productTypeSyncStatistics.getFailed()).thenReturn(new AtomicInteger(2));
    when(productTypeSyncStatistics.getUpdated()).thenReturn(new AtomicInteger(3));
    when(productTypeSyncStatistics.getProcessed()).thenReturn(new AtomicInteger(6));
    when(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .thenReturn(0);
    when(productTypeSyncStatistics.getReportMessage()).thenCallRealMethod();

    // test and assertion
    assertThat(productTypeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 6 product types were processed in total "
                + "(1 created, 3 updated, 2 failed to sync and 0 product types with at least one NestedType or a Set of"
                + " NestedType attribute definition(s) referencing a missing product type).");
  }

  @Test
  void getNumberOfProductTypesWithMissingNestedProductTypes_WithEmptyMap_ShouldReturn0() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test and assertion
    assertThat(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .isEqualTo(0);
  }

  @Test
  void
      getNumberOfProductTypesWithMissingNestedProductTypes_WithMissingButNoReferencingProductType_ShouldBe0() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    missingProductTypeReferences.put("missing1", new ConcurrentHashMap<>());
    missingProductTypeReferences.put("missing2", new ConcurrentHashMap<>());

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test and assertion
    assertThat(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .isEqualTo(0);
  }

  @Test
  void
      getNumberOfProductTypesWithMissingNestedProductTypes_WithOneReferencingProdTypes_ShouldBe1() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test and assertion
    assertThat(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .isEqualTo(1);
  }

  @Test
  void
      getNumberOfProductTypesWithMissingNestedProductTypes_WithSameReferencingProdTypes_CountDistinct() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test and assertion
    assertThat(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .isEqualTo(2);
  }

  @Test
  void
      getNumberOfProductTypesWithMissingNestedProductTypes_WithDifferentReferencingProdTypes_CountDistinct() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-3", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test and assertion
    assertThat(productTypeSyncStatistics.getNumberOfProductTypesWithMissingNestedProductTypes())
        .isEqualTo(4);
  }

  @Test
  void putMissingNestedProductType_WithNullExistingReferencingProductTypes_ShouldCreateANewMap() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missingPT")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    definitionDrafts.add(referencingAttributeDefinitionDraft);
    productTypesReferencingMissing1.put("referencingPT", definitionDrafts);

    missingProductTypeReferences.put("missingPT", productTypesReferencingMissing1);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "newMissing", "referencingPT", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(2);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("newMissing"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("newMissing")
                .get("referencingPT"))
        .contains(referencingAttributeDefinitionDraft);
  }

  @Test
  void putMissingNestedProductType_WithNullExistingAttributeDefs_ShouldCreateANewMap() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missingPT")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    definitionDrafts.add(referencingAttributeDefinitionDraft);

    missingProductTypeReferences.put("missingPT", productTypesReferencingMissing1);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missingPT", "referencingPT", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missingPT"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missingPT")
                .get("referencingPT"))
        .contains(referencingAttributeDefinitionDraft);
  }

  @Test
  void
      putMissingNestedProductType_WithIdenticalOneReferencingAttrToAnEmptyMap_ShouldOverwriteExisting() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missingPT")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    definitionDrafts.add(referencingAttributeDefinitionDraft);
    productTypesReferencingMissing1.put("referencingPT", definitionDrafts);

    missingProductTypeReferences.put("missingPT", productTypesReferencingMissing1);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missingPT", "referencingPT", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missingPT"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missingPT")
                .get("referencingPT"))
        .contains(referencingAttributeDefinitionDraft);
  }

  @Test
  void putMissingNestedProductType_WithOnlyOneReferencingAttrToAnEmptyMap_ShouldHaveOnly1() {
    // preparation
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(new ConcurrentHashMap<>());

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missingPT")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missingPT", "referencingPT", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missingPT"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missingPT")
                .get("referencingPT"))
        .contains(referencingAttributeDefinitionDraft);
  }

  @Test
  void putMissingNestedProductType_WithOnlyOneReferencingAttrToAnEmptyMap_ShouldHaveMultiple() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-3", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missingPT")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missingPT", "referencingPT", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(3);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missingPT"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missingPT")
                .get("referencingPT"))
        .contains(referencingAttributeDefinitionDraft);
  }

  @Test
  void putMissingNestedProductType_WithAdditionalAttribute_ShouldAppendAttribute() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    final AttributeDefinitionDraft existingReferencingAttr =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missing1")),
                "attr-name-1",
                ofEnglish("label"),
                true)
            .build();
    definitionDrafts.add(existingReferencingAttr);

    productTypesReferencingMissing1.put("referencing-product-type-1", definitionDrafts);
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-3", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missing1")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missing1", "referencing-product-type-1", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(2);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing1"))
        .hasSize(2);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missing1")
                .get("referencing-product-type-1"))
        .containsExactlyInAnyOrder(existingReferencingAttr, referencingAttributeDefinitionDraft);
  }

  @Test
  void
      putMissingNestedProductType_WithExistingReferencingPTypeToAnotherMissingRef_ShouldAppendAttribute() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    final AttributeDefinitionDraft existingReferencingAttr =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missing1")),
                "attr-name-1",
                ofEnglish("label"),
                true)
            .build();
    definitionDrafts.add(existingReferencingAttr);

    productTypesReferencingMissing1.put("referencing-product-type-1", definitionDrafts);
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-3", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    final AttributeDefinitionDraft referencingAttributeDefinitionDraft =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missing1")),
                "attr-name",
                ofEnglish("label"),
                true)
            .build();

    // test
    productTypeSyncStatistics.putMissingNestedProductType(
        "missing3", "referencing-product-type-1", referencingAttributeDefinitionDraft);

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(3);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing3"))
        .hasSize(1);
    assertThat(
            productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents()
                .get("missing3")
                .get("referencing-product-type-1"))
        .containsExactly(referencingAttributeDefinitionDraft);
  }

  @Test
  void removeReferencingProductTypeKey_WithEmptyMap_ShouldRemoveNothing() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.removeReferencingProductTypeKey("foo");

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).isEmpty();
  }

  @Test
  void removeReferencingProductTypeKey_WithOneOccurrence_ShouldRemoveOccurrence() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();
    final ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean> definitionDrafts =
        ConcurrentHashMap.newKeySet();
    final AttributeDefinitionDraft existingReferencingAttr =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(ProductType.referenceOfId("missing1")),
                "attr-name-1",
                ofEnglish("label"),
                true)
            .build();
    definitionDrafts.add(existingReferencingAttr);

    productTypesReferencingMissing1.put("referencing-product-type-1", definitionDrafts);
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-3", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.removeReferencingProductTypeKey("referencing-product-type-1");

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(2);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing1"))
        .containsOnlyKeys("referencing-product-type-2");
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing2"))
        .containsOnlyKeys("referencing-product-type-3", "referencing-product-type-4");
  }

  @Test
  void removeReferencingProductTypeKey_WithMultipleOccurrence_ShouldRemoveAllOccurrences() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();

    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing1.put(
        "referencing-product-type-2", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.removeReferencingProductTypeKey("referencing-product-type-1");

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(2);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing1"))
        .containsOnlyKeys("referencing-product-type-2");
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing2"))
        .containsOnlyKeys("referencing-product-type-4");
  }

  @Test
  void removeReferencingProductTypeKey_WithOnlyOccurrenceForMissingRef_ShouldRemoveMissingRef() {
    // preparation
    final ConcurrentHashMap<
            String,
            ConcurrentHashMap<
                String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>>
        missingProductTypeReferences = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing1 = new ConcurrentHashMap<>();

    productTypesReferencingMissing1.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());

    final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
        productTypesReferencingMissing2 = new ConcurrentHashMap<>();
    productTypesReferencingMissing2.put(
        "referencing-product-type-1", ConcurrentHashMap.newKeySet());
    productTypesReferencingMissing2.put(
        "referencing-product-type-4", ConcurrentHashMap.newKeySet());

    missingProductTypeReferences.put("missing1", productTypesReferencingMissing1);
    missingProductTypeReferences.put("missing2", productTypesReferencingMissing2);

    final ProductTypeSyncStatistics productTypeSyncStatistics =
        new ProductTypeSyncStatistics(missingProductTypeReferences);

    // test
    productTypeSyncStatistics.removeReferencingProductTypeKey("referencing-product-type-1");

    // assertion
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
    assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents().get("missing2"))
        .containsOnlyKeys("referencing-product-type-4");
  }
}
