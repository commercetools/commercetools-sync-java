package com.commercetools.sync.producttypes.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.producttypes.MockBuilderUtils.createMockAttributeDefinitionDraftBuilder;
import static com.commercetools.sync.producttypes.MockBuilderUtils.createMockProductTypeDraftBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.producttypes.MockBuilderUtils;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductTypeReferenceResolutionUtilsTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void setup() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void mapToProductDrafts_WithEmptyList_ShouldReturnEmptyList() {
    // preparation
    final List<ProductType> productTypes = emptyList();

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts).isEmpty();
  }

  @Test
  void mapToProductDrafts_WithNullProductType_ShouldReturnEmptyList() {
    // preparation
    final List<ProductType> productTypes = singletonList(null);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts).isEmpty();
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNoAttributeDefs_ShouldReturnProductType() {
    // preparation
    final ProductType productTypeFoo =
        MockBuilderUtils.createMockProductTypeBuilder().key("foo").attributes(emptyList()).build();

    final List<ProductType> productTypes = singletonList(productTypeFoo);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts).hasSize(1);
    assertThat(productTypeDrafts.get(0).getKey()).isEqualTo("foo");
    assertThat(productTypeDrafts.get(0).getAttributes()).isEmpty();
  }

  @Test
  void mapToProductDrafts_WithNoReferences_ShouldReturnCorrectProductTypeDrafts() {
    final AttributeDefinition stringAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().build();

    final AttributeDefinition numberAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .name("b")
            .label(ofEnglish("b"))
            .type(AttributeTypeBuilder::numberBuilder)
            .build();

    final ProductType productTypeFoo =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("ProductTypeFoo")
            .attributes(singletonList(stringAttr))
            .build();

    final ProductType productTypeBar =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("ProductTypeBar")
            .attributes(singletonList(numberAttr))
            .build();

    final List<ProductType> productTypes = asList(productTypeFoo, productTypeBar);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .containsExactlyInAnyOrder(
            createMockProductTypeDraftBuilder()
                .key(productTypeFoo.getKey())
                .attributes(createMockAttributeDefinitionDraftBuilder().build())
                .build(),
            createMockProductTypeDraftBuilder()
                .key(productTypeBar.getKey())
                .attributes(
                    createMockAttributeDefinitionDraftBuilder()
                        .name(numberAttr.getName())
                        .label(numberAttr.getLabel())
                        .type(numberAttr.getType())
                        .build())
                .build());
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithAnCachedRefNestedType_ShouldReplaceRef() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductType referencedProductType =
        MockBuilderUtils.createMockProductTypeBuilder().key(referencedProductTypeKey).build();

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.nestedBuilder().typeReference(productTypeReference))
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .build();

    final ProductType productType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    referenceIdToKeyCache.add(referencedProductTypeId, referencedProductTypeKey);
    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeDefinitionDraft attributeDefinitionDraft =
                  productTypeDraft.get(0).getAttributes().get(0);
              assertThat(
                      ((AttributeNestedType) attributeDefinitionDraft.getType())
                          .getTypeReference()
                          .getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNonCachedRefNestedType_ShouldNotReplaceRef() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
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
                                    .typeReference(productTypeReference)))
            .build();

    final ProductType productType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeSetType setAttributeType =
                  (AttributeSetType) productTypeDraft.get(0).getAttributes().get(0).getType();
              final AttributeNestedType nestedAttributeType =
                  (AttributeNestedType) setAttributeType.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductTypeId);
            });
  }

  @Test
  void mapToProductDrafts_WithSetOfNestedType_ShouldReplaceRef() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductType referencedProductType =
        MockBuilderUtils.createMockProductTypeBuilder().key(referencedProductTypeKey).build();

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(productTypeReference)))
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .build();

    final ProductType productType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    referenceIdToKeyCache.add(referencedProductTypeId, referencedProductTypeKey);
    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeSetType setAttributeType =
                  (AttributeSetType) productTypeDraft.get(0).getAttributes().get(0).getType();
              final AttributeNestedType nestedAttributeType =
                  (AttributeNestedType) setAttributeType.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithNestedTypeWithSetOfSet_ShouldReplaceRef() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductType referencedProductType =
        MockBuilderUtils.createMockProductTypeBuilder().key(referencedProductTypeKey).build();

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
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
                                                .nestedBuilder()
                                                .typeReference(productTypeReference))))
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .build();

    final ProductType productType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    referenceIdToKeyCache.add(referencedProductTypeId, referencedProductTypeKey);
    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeSetType setAttributeType =
                  (AttributeSetType) productTypeDraft.get(0).getAttributes().get(0).getType();
              final AttributeSetType setOfSet =
                  (AttributeSetType) setAttributeType.getElementType();
              final AttributeNestedType nestedAttributeType =
                  (AttributeNestedType) setOfSet.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithNestedTypeWithSetOfSetOfSet_ShouldReplaceRef() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductType referencedProductType =
        MockBuilderUtils.createMockProductTypeBuilder().key(referencedProductTypeKey).build();

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
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
                                                            .nestedBuilder()
                                                            .typeReference(productTypeReference)))))
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .build();

    final ProductType productType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    referenceIdToKeyCache.add(referencedProductTypeId, referencedProductTypeKey);
    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache);

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeSetType setAttributeType =
                  (AttributeSetType) productTypeDraft.get(0).getAttributes().get(0).getType();
              final AttributeSetType setOfSet =
                  (AttributeSetType) setAttributeType.getElementType();
              final AttributeSetType setOfSetOfSet = (AttributeSetType) setOfSet.getElementType();
              final AttributeNestedType nestedAttributeType =
                  (AttributeNestedType) setOfSetOfSet.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }
}
