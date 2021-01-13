package com.commercetools.sync.producttypes.utils;

import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils.buildProductTypeQuery;
import static com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.NumberAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductTypeReferenceResolutionUtilsTest {

  @Test
  void mapToProductDrafts_WithEmptyList_ShouldReturnEmptyList() {
    // preparation
    final List<ProductType> productTypes = emptyList();

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts).isEmpty();
  }

  @Test
  void mapToProductDrafts_WithNullProductType_ShouldReturnEmptyList() {
    // preparation
    final List<ProductType> productTypes = singletonList(null);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts).isEmpty();
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNoAttributeDefs_ShouldReturnProductType() {
    // preparation
    final ProductType productTypeFoo = mock(ProductType.class);
    when(productTypeFoo.getKey()).thenReturn("foo");
    when(productTypeFoo.getAttributes()).thenReturn(emptyList());

    final List<ProductType> productTypes = singletonList(productTypeFoo);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(ProductTypeDraftBuilder.of(productTypeFoo).build());
  }

  @Test
  void mapToProductDrafts_WithNoReferences_ShouldReturnCorrectProductTypeDrafts() {
    final AttributeDefinition stringAttr =
        AttributeDefinitionBuilder.of("a", ofEnglish("a"), StringAttributeType.of()).build();

    final AttributeDefinition numberAttr =
        AttributeDefinitionBuilder.of("b", ofEnglish("b"), NumberAttributeType.of()).build();

    final ProductType productTypeFoo = mock(ProductType.class);
    when(productTypeFoo.getKey()).thenReturn("ProductTypeFoo");
    when(productTypeFoo.getAttributes()).thenReturn(singletonList(stringAttr));

    final ProductType productTypeBar = mock(ProductType.class);
    when(productTypeBar.getKey()).thenReturn("ProductTypeBar");
    when(productTypeBar.getAttributes()).thenReturn(singletonList(numberAttr));

    final List<ProductType> productTypes = asList(productTypeFoo, productTypeBar);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(
            ProductTypeDraftBuilder.of(productTypeFoo).build(),
            ProductTypeDraftBuilder.of(productTypeBar).build());
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithAnExpandedRefNestedType_ShouldReplaceRef() {
    // preparation
    final ProductType referencedProductType = mock(ProductType.class);
    when(referencedProductType.getKey()).thenReturn("referencedProductType");

    final Reference<ProductType> productTypeReference =
        spy(ProductType.reference(referencedProductType));

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productTypeReference))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .hasOnlyOneElementSatisfying(
            productTypeDraft -> {
              final NestedAttributeType nestedAttributeType =
                  (NestedAttributeType) productTypeDraft.getAttributes().get(0).getAttributeType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNonExpandedRefNestedType_ShouldFail() {
    // preparation
    final Reference<ProductType> productTypeReference =
        ProductType.referenceOfId("referencedProductType");

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productTypeReference))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    assertThatThrownBy(() -> mapToProductTypeDrafts(productTypes))
        .isExactlyInstanceOf(ReferenceReplacementException.class)
        .hasMessageContaining("Some errors occurred during reference replacement.")
        .hasMessageContaining(
            "Failed to replace some references on the productType with key 'withNestedTypeAttr'")
        .hasMessageContaining(
            "Failed to replace some references on the attributeDefinition with name 'nestedattr'."
                + " Cause: ProductType reference is not expanded.");
  }

  @Test
  void mapToProductDrafts_WithSetOfNestedType_ShouldReplaceRef() {
    // preparation
    final ProductType referencedProductType = mock(ProductType.class);
    when(referencedProductType.getKey()).thenReturn("referencedProductType");

    final Reference<ProductType> productTypeReference =
        spy(ProductType.reference(referencedProductType));
    when(productTypeReference.getObj()).thenReturn(referencedProductType);

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr",
                ofEnglish("nestedattr"),
                SetAttributeType.of(NestedAttributeType.of(productTypeReference)))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .hasOnlyOneElementSatisfying(
            productTypeDraft -> {
              final SetAttributeType setAttributeType =
                  (SetAttributeType) productTypeDraft.getAttributes().get(0).getAttributeType();
              final NestedAttributeType nestedAttributeType =
                  (NestedAttributeType) setAttributeType.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithSetOfNestedTypeNonExpanded_ShouldFail() {
    // preparation
    final Reference<ProductType> productTypeReference =
        ProductType.referenceOfId("referencedProductType");

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr",
                ofEnglish("nestedattr"),
                SetAttributeType.of(NestedAttributeType.of(productTypeReference)))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    assertThatThrownBy(() -> mapToProductTypeDrafts(productTypes))
        .isExactlyInstanceOf(ReferenceReplacementException.class)
        .hasMessageContaining("Some errors occurred during reference replacement. Causes:\n")
        .hasMessageContaining(
            "\tFailed to replace some references on the productType with key 'withNestedTypeAttr'"
                + ". Causes:\n")
        .hasMessageContaining(
            "\t\tFailed to replace some references on the attributeDefinition with name "
                + "'nestedattr'. Cause: ProductType reference is not expanded.");
  }

  @Test
  void mapToProductDrafts_WithNestedTypeWithSetOfSet_ShouldReplaceRef() {
    // preparation
    final ProductType referencedProductType = mock(ProductType.class);
    when(referencedProductType.getKey()).thenReturn("referencedProductType");

    final Reference<ProductType> productTypeReference =
        spy(ProductType.reference(referencedProductType));
    when(productTypeReference.getObj()).thenReturn(referencedProductType);

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr",
                ofEnglish("nestedattr"),
                SetAttributeType.of(
                    SetAttributeType.of(NestedAttributeType.of(productTypeReference))))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .hasOnlyOneElementSatisfying(
            productTypeDraft -> {
              final SetAttributeType setAttributeType =
                  (SetAttributeType) productTypeDraft.getAttributes().get(0).getAttributeType();
              final SetAttributeType setOfSet =
                  (SetAttributeType) setAttributeType.getElementType();
              final NestedAttributeType nestedAttributeType =
                  (NestedAttributeType) setOfSet.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNonExpandedSetOfRefNestedType_ShouldFail() {
    // preparation
    final Reference<ProductType> productTypeReference =
        ProductType.referenceOfId("referencedProductType");

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr", ofEnglish("nestedattr"), NestedAttributeType.of(productTypeReference))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    assertThatThrownBy(() -> mapToProductTypeDrafts(productTypes))
        .isExactlyInstanceOf(ReferenceReplacementException.class)
        .hasMessageContaining("Some errors occurred during reference replacement. Causes:\n")
        .hasMessageContaining(
            "\tFailed to replace some references on the productType with key 'withNestedTypeAttr'"
                + ". Causes:\n")
        .hasMessageContaining(
            "\t\tFailed to replace some references on the attributeDefinition with name "
                + "'nestedattr'. Cause: ProductType reference is not expanded.");
  }

  @Test
  void mapToProductDrafts_WithNestedTypeWithSetOfSetOfSet_ShouldReplaceRef() {
    // preparation
    final ProductType referencedProductType = mock(ProductType.class);
    when(referencedProductType.getKey()).thenReturn("referencedProductType");

    final Reference<ProductType> productTypeReference =
        spy(ProductType.reference(referencedProductType));
    when(productTypeReference.getObj()).thenReturn(referencedProductType);

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr",
                ofEnglish("nestedattr"),
                SetAttributeType.of(
                    SetAttributeType.of(
                        SetAttributeType.of(NestedAttributeType.of(productTypeReference)))))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    // test
    final List<ProductTypeDraft> productTypeDrafts = mapToProductTypeDrafts(productTypes);

    // assertion
    assertThat(productTypeDrafts)
        .hasOnlyOneElementSatisfying(
            productTypeDraft -> {
              final SetAttributeType setAttributeType =
                  (SetAttributeType) productTypeDraft.getAttributes().get(0).getAttributeType();
              final SetAttributeType setOfSet =
                  (SetAttributeType) setAttributeType.getElementType();
              final SetAttributeType setOfSetOfSet = (SetAttributeType) setOfSet.getElementType();
              final NestedAttributeType nestedAttributeType =
                  (NestedAttributeType) setOfSetOfSet.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }

  @Test
  void buildProductTypeQuery_WithNoParam_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final ProductTypeQuery productTypeQuery = buildProductTypeQuery();
    assertThat(productTypeQuery.expansionPaths())
        .containsExactly(ExpansionPath.of("attributes[*].type.typeReference"));
  }

  @Test
  void buildProductTypeQuery_With0MaxSetDepth_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final ProductTypeQuery productTypeQuery = buildProductTypeQuery(0);
    assertThat(productTypeQuery.expansionPaths())
        .containsExactly(ExpansionPath.of("attributes[*].type.typeReference"));
  }

  @Test
  void buildProductTypeQuery_With1MaxSetDepth_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final ProductTypeQuery productTypeQuery = buildProductTypeQuery(1);
    assertThat(productTypeQuery.expansionPaths())
        .containsExactly(
            ExpansionPath.of("attributes[*].type.typeReference"),
            ExpansionPath.of("attributes[*].type.elementType.typeReference"));
  }

  @Test
  void buildProductTypeQuery_With2MaxSetDepth_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final ProductTypeQuery productTypeQuery = buildProductTypeQuery(2);
    assertThat(productTypeQuery.expansionPaths())
        .containsExactly(
            ExpansionPath.of("attributes[*].type.typeReference"),
            ExpansionPath.of("attributes[*].type.elementType.typeReference"),
            ExpansionPath.of("attributes[*].type.elementType.elementType.typeReference"));
  }

  @Test
  void buildProductTypeQuery_With3MaxSetDepth_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final ProductTypeQuery productTypeQuery = buildProductTypeQuery(3);
    assertThat(productTypeQuery.expansionPaths())
        .containsExactly(
            ExpansionPath.of("attributes[*].type.typeReference"),
            ExpansionPath.of("attributes[*].type.elementType.typeReference"),
            ExpansionPath.of("attributes[*].type.elementType.elementType.typeReference"),
            ExpansionPath.of(
                "attributes[*].type.elementType.elementType.elementType.typeReference"));
  }
}
