package com.commercetools.sync.commons.utils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.isReferenceOfType;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.toResourceIdentifierIfNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceIdentifierUtilsTest {

    @Test
    void toResourceIdentifierIfNotNull_WithNullResource_ShouldReturnNull() {
        assertThat(toResourceIdentifierIfNotNull(null)).isNull();
    }

    @Test
    void toResourceIdentifierIfNotNull_WithNonNullResource_ShouldReturnCorrectResourceIdentifier() {
        final Category category = mock(Category.class);
        when(category.getId()).thenReturn(UUID.randomUUID().toString());
        when(category.toResourceIdentifier()).thenCallRealMethod();
        when(category.toReference()).thenCallRealMethod();

        final ResourceIdentifier<Category> categoryResourceIdentifier = toResourceIdentifierIfNotNull(category);

        assertThat(categoryResourceIdentifier).isNotNull();
        assertThat(categoryResourceIdentifier.getId()).isEqualTo(category.getId());
        assertThat(categoryResourceIdentifier.getTypeId()).isEqualTo(Category.resourceTypeId());
    }

    @Test
    void toResourceIdentifierIfNotNull_WithNonNullReference_ShouldReturnCorrectResourceIdentifier() {
        final Reference<Category> categoryReference = Category.referenceOfId("foo");

        final ResourceIdentifier<Category> categoryResourceIdentifier = toResourceIdentifierIfNotNull(
            categoryReference);

        assertThat(categoryResourceIdentifier).isNotNull();
        assertThat(categoryResourceIdentifier.getId()).isEqualTo("foo");
        assertThat(categoryResourceIdentifier.getTypeId()).isEqualTo(Category.resourceTypeId());
    }

    @Test
    void isReferenceOfType_WithEmptyObjectNodeValueVsEmptyStringAsReferenceTypeId_ShouldReturnFalse() {
        // preparation
        final ObjectNode emptyObjectNode = JsonNodeFactory.instance.objectNode();

        // test
        final boolean isReference = isReferenceOfType(emptyObjectNode, "");

        // assertion
        assertThat(isReference).isFalse();
    }

    @Test
    void isReferenceOfType_WithEmptyObjectNodeValueVsProductReferenceTypeId_ShouldReturnFalse() {
        // preparation
        final ObjectNode emptyObjectNode = JsonNodeFactory.instance.objectNode();

        // test
        final boolean isReference = isReferenceOfType(emptyObjectNode, Product.referenceTypeId());

        // assertion
        assertThat(isReference).isFalse();
    }

    @Test
    void isReferenceOfType_WithTextNodeVsProductReferenceTypeId_ShouldReturnFalse() {
        // preparation
        final TextNode textNode = JsonNodeFactory.instance.textNode("foo");

        // test
        final boolean isReference = isReferenceOfType(textNode, Product.referenceTypeId());

        // assertion
        assertThat(isReference).isFalse();
    }

    @Test
    void isReferenceOfType_WithInvalidReferenceVsProductReferenceTypeId_ShouldReturnFalse() {
        // preparation
        final ObjectNode invalidReferenceObject = JsonNodeFactory.instance.objectNode();
        invalidReferenceObject.put("anyString", "anyValue");

        // test
        final boolean isReference = isReferenceOfType(invalidReferenceObject, Product.referenceTypeId());

        // assertion
        assertThat(isReference).isFalse();
    }

    @Test
    void isReferenceOfType_WithCategoryReferenceVsProductReferenceTypeId_ShouldReturnFalse() {
        // preparation
        final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
        categoryReference.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        categoryReference.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());

        // test
        final boolean isReference = isReferenceOfType(categoryReference, Product.referenceTypeId());

        // assertion
        assertThat(isReference).isFalse();
    }

    @Test
    void isReferenceOfType_WithCategoryReferenceVsCategoryReferenceTypeId_ShouldReturnTrue() {
        // preparation
        final ObjectNode categoryReference = JsonNodeFactory.instance.objectNode();
        categoryReference.put(REFERENCE_TYPE_ID_FIELD, Category.referenceTypeId());
        categoryReference.put(REFERENCE_ID_FIELD, UUID.randomUUID().toString());

        // test
        final boolean isReference = isReferenceOfType(categoryReference, Category.referenceTypeId());

        // assertion
        assertThat(isReference).isTrue();
    }

}
