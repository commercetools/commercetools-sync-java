package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.ReferenceImpl;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductReferenceBuilder;
import java.util.UUID;
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
  void isReferenceOfType_WithEmptyObjectNodeValueVsProductReferenceTypeId_ShouldReturnFalse() {
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
}
