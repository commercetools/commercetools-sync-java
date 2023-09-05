package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductReferenceBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ResourceIdentifierUtilsTest {

  @Test
  void isReferenceOfType_WithReferenceValueVsEmptyStringAsReferenceTypeId_ShouldReturnFalse() {
    // test
    final boolean isReference =
        isReferenceOfType(ProductReferenceBuilder.of().id("id").build(), "");

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithEmptyReferenceValueVsProductReferenceTypeId_ShouldReturnFalse() {
    // test
    final boolean isReference = isReferenceOfType(new ReferenceImpl(), ProductReference.PRODUCT);

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithCategoryReferenceVsProductReferenceTypeId_ShouldReturnFalse() {
    // preparation
    final CategoryReference categoryReference = CategoryReferenceBuilder.of().id("id").build();

    // test
    final boolean isReference = isReferenceOfType(categoryReference, ProductReference.PRODUCT);

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithCategoryReferenceVsCategoryReferenceTypeId_ShouldReturnTrue() {
    // preparation
    final CategoryReference categoryReference = CategoryReferenceBuilder.of().id("id").build();

    // test
    final boolean isReference = isReferenceOfType(categoryReference, CategoryReference.CATEGORY);

    // assertion
    assertThat(isReference).isTrue();
  }

  @Test
  void isReferenceOfType_WithJsonValueVsEmptyStringAsReferenceTypeId_ShouldReturnFalse() {
    // test
    final ObjectNode productReference = JsonNodeFactory.instance.objectNode();
    productReference.put("typeId", ProductReference.PRODUCT);
    productReference.put("id", "id");
    final boolean isReference = isReferenceOfType(productReference, "");

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithEmptyObjectNodeValueVsProductReferenceTypeId_ShouldReturnFalse() {
    // test
    final ObjectNode emptyReference = JsonNodeFactory.instance.objectNode();
    final boolean isReference = isReferenceOfType(emptyReference, ProductReference.PRODUCT);

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithNullNodeValueVsProductReferenceTypeId_ShouldReturnFalse() {
    // test
    final NullNode nullNode = JsonNodeFactory.instance.nullNode();
    final boolean isReference = isReferenceOfType(nullNode, ProductReference.PRODUCT);

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithJsonOfCategoryReferenceVsProductReferenceTypeId_ShouldReturnFalse() {
    // preparation
    final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
    categoryReference.put("typeId", CategoryReference.CATEGORY);
    categoryReference.put("id", "id");
    // test
    final boolean isReference = isReferenceOfType(categoryReference, ProductReference.PRODUCT);

    // assertion
    assertThat(isReference).isFalse();
  }

  @Test
  void isReferenceOfType_WithJsonOfCategoryReferenceVsCategoryReferenceTypeId_ShouldReturnTrue() {
    // preparation
    final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
    categoryReference.put("typeId", CategoryReference.CATEGORY);
    categoryReference.put("id", "id");

    // test
    final boolean isReference = isReferenceOfType(categoryReference, CategoryReference.CATEGORY);

    // assertion
    assertThat(isReference).isTrue();
  }
}
