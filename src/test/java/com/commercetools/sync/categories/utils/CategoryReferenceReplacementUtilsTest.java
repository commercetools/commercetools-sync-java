package com.commercetools.sync.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceReplacementUtilsTest {

    @Test
    public void
        replaceCategoryReferenceIdsWithKeys_WithAllExpandedCategoryReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final String customTypeId = UUID.randomUUID().toString();
        final String customTypeKey = "customTypeKey";
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(customTypeId);
        when(mockCustomType.getKey()).thenReturn(customTypeKey);

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            // Simulate reference expansion
            final Category mockParent = mock(Category.class);
            when(mockParent.getId()).thenReturn(parentId);
            when(mockParent.getKey()).thenReturn("parentKey" + i);
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndObj(UUID.randomUUID().toString(),
                mockParent);
            when(mockCategory.getParent()).thenReturn(parentReference);

            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndObj("resourceTypeId",
                mockCustomType);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            mockCategories.add(mockCategory);
        }

        for (final Category category : mockCategories) {
            assertThat(category.getParent().getId()).isEqualTo(parentId);
            assertThat(category.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
        final List<CategoryDraft> referenceReplacedDrafts =
            CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys(mockCategories);

        for (int i = 0; i < referenceReplacedDrafts.size(); i++) {
            assertThat(referenceReplacedDrafts.get(i).getParent().getId()).isEqualTo("parentKey" + i);
            assertThat(referenceReplacedDrafts.get(i).getCustom().getType().getId()).isEqualTo(customTypeKey);
        }
    }

    @Test
    public void
        replaceCategoryReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final String customTypeId = UUID.randomUUID().toString();

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            // Simulate no reference expansion
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndId(UUID.randomUUID().toString(),
                parentId);
            when(mockCategory.getParent()).thenReturn(parentReference);


            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndId("resourceTypeId",
                customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            mockCategories.add(mockCategory);
        }

        for (final Category category : mockCategories) {
            assertThat(category.getParent().getId()).isEqualTo(parentId);
            assertThat(category.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
        final List<CategoryDraft> referenceReplacedDrafts =
            CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys(mockCategories);

        for (CategoryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getParent().getId()).isEqualTo(parentId);
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
    }
}
