package com.commercetools.sync.commons.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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



}
