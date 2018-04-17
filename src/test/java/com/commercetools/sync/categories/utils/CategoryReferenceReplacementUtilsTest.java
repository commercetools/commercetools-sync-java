package com.commercetools.sync.categories.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceReplacementUtilsTest {

    @Test
    public void
        replaceCategoryReferenceIdsWithKeys_WithAllExpandedCategoryReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final Type mockCustomType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        // Mock asset with expanded custom type reference
        final Type assetCustomType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final Asset asset = getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(),
            assetCustomType));

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            //Mock categories parent fields with expanded category references.
            final Category mockParent = mock(Category.class);
            when(mockParent.getId()).thenReturn(parentId);
            when(mockParent.getKey()).thenReturn("parentKey" + i);
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndObj(UUID.randomUUID().toString(),
                mockParent);
            when(mockCategory.getParent()).thenReturn(parentReference);

            //Mock categories custom fields with expanded type references.
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndObj("resourceTypeId",
                mockCustomType);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            //Mock categories assets with expanded custom type references.
            when(mockCategory.getAssets()).thenReturn(singletonList(asset));

            mockCategories.add(mockCategory);
        }

        for (final Category category : mockCategories) {
            assertThat(category.getParent().getId()).isEqualTo(parentId);
            assertThat(category.getCustom().getType().getId()).isEqualTo(mockCustomType.getId());
        }
        final List<CategoryDraft> referenceReplacedDrafts =
            CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys(mockCategories);

        for (int i = 0; i < referenceReplacedDrafts.size(); i++) {
            assertThat(referenceReplacedDrafts.get(i).getParent().getId()).isEqualTo("parentKey" + i);
            assertThat(referenceReplacedDrafts.get(i).getCustom().getType().getId()).isEqualTo(mockCustomType.getKey());

            final List<AssetDraft> referenceReplacedDraftAssets = referenceReplacedDrafts.get(i).getAssets();
            assertThat(referenceReplacedDraftAssets).hasSize(1);
            assertThat(referenceReplacedDraftAssets.get(0).getCustom()).isNotNull();
            assertThat(referenceReplacedDraftAssets.get(0).getCustom().getType().getId())
                .isEqualTo(assetCustomType.getKey());
        }
    }

    @Test
    public void
        replaceCategoryReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String parentId = UUID.randomUUID().toString();
        final String customTypeId = UUID.randomUUID().toString();

        // Mock asset with non-expanded custom type reference
        final Asset asset = getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
            UUID.randomUUID().toString()));

        final List<Category> mockCategories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Category mockCategory = mock(Category.class);

            //Mock categories parent fields with non-expanded category references.
            final Reference<Category> parentReference = Reference.ofResourceTypeIdAndId(UUID.randomUUID().toString(),
                parentId);
            when(mockCategory.getParent()).thenReturn(parentReference);

            //Mock categories custom fields with non-expanded type references.
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndId("resourceTypeId",
                customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockCategory.getCustom()).thenReturn(mockCustomFields);

            //Mock categories assets with non-expanded custom type references.
            when(mockCategory.getAssets()).thenReturn(singletonList(asset));

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

            final List<AssetDraft> referenceReplacedDraftAssets = referenceReplacedDraft.getAssets();
            assertThat(referenceReplacedDraftAssets).hasSize(1);
            assertThat(referenceReplacedDraftAssets.get(0).getCustom()).isNotNull();
            assertThat(referenceReplacedDraftAssets.get(0).getCustom().getType().getId())
                .isEqualTo(asset.getCustom().getType().getId());
        }
    }

    @Test
    public void buildCategoryQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
        final CategoryQuery categoryQuery = CategoryReferenceReplacementUtils.buildCategoryQuery();
        assertThat(categoryQuery.expansionPaths()).containsExactly(
            ExpansionPath.of("custom.type"),
            ExpansionPath.of("assets[*].custom.type"),
            ExpansionPath.of("parent"));
    }
}
