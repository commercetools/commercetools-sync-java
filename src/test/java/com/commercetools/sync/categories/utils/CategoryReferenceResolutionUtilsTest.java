package com.commercetools.sync.categories.utils;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CategoryReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void mapToCategoryDrafts_WithNonExpandedCategoryReferences_ShouldReturnReferencesWithKeys() {
    final String parentId = UUID.randomUUID().toString();
    final String parentKey = "parentKey";

    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";

    final String assetCustomTypeId = UUID.randomUUID().toString();
    final String assetCustomTypeKey = "customTypeKey";
    final Asset asset =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), assetCustomTypeId));

    final List<Category> mockCategories = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      final Category mockCategory = mock(Category.class);

      final Reference<Category> parentReference =
          Reference.ofResourceTypeIdAndId(UUID.randomUUID().toString(), parentId);
      when(mockCategory.getParent()).thenReturn(parentReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCategory.getCustom()).thenReturn(mockCustomFields);

      when(mockCategory.getAssets()).thenReturn(singletonList(asset));

      mockCategories.add(mockCategory);
    }

    final Map<String, String> idToKeyValueMap = new HashMap<>();

    idToKeyValueMap.put(parentId, parentKey);
    idToKeyValueMap.put(assetCustomTypeId, assetCustomTypeKey);
    idToKeyValueMap.put(customTypeId, customTypeKey);

    referenceIdToKeyCache.addAll(idToKeyValueMap);

    for (final Category category : mockCategories) {
      assertThat(category.getParent().getId()).isEqualTo(parentId);
      assertThat(category.getCustom().getType().getId()).isEqualTo(customTypeId);
    }

    final List<CategoryDraft> referenceReplacedDrafts =
        CategoryReferenceResolutionUtils.mapToCategoryDrafts(mockCategories, referenceIdToKeyCache);

    for (CategoryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getParent().getKey()).isEqualTo(parentKey);
      assertThat(referenceReplacedDraft.getCustom().getType().getKey()).isEqualTo(customTypeKey);

      final List<AssetDraft> referenceReplacedDraftAssets = referenceReplacedDraft.getAssets();
      assertThat(referenceReplacedDraftAssets).hasSize(1);
      assertThat(referenceReplacedDraftAssets.get(0).getCustom()).isNotNull();
      assertThat(referenceReplacedDraftAssets.get(0).getCustom().getType().getKey())
          .isEqualTo(assetCustomTypeKey);
    }
  }

  @Test
  void mapToCategoryDrafts_WithoutReferences_ShouldNotReturnReferences() {
    final List<Category> mockCategories = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      final Category mockCategory = mock(Category.class);
      when(mockCategory.getParent()).thenReturn(null);
      when(mockCategory.getCustom()).thenReturn(null);
      when(mockCategory.getAssets()).thenReturn(emptyList());
      mockCategories.add(mockCategory);
    }
    final List<CategoryDraft> referenceReplacedDrafts =
        CategoryReferenceResolutionUtils.mapToCategoryDrafts(mockCategories, referenceIdToKeyCache);
    for (CategoryDraft referenceReplacedDraft : referenceReplacedDrafts) {
      assertThat(referenceReplacedDraft.getParent()).isNull();
      assertThat(referenceReplacedDraft.getCustom()).isNull();
      assertThat(referenceReplacedDraft.getAssets()).isEmpty();
    }
  }
}
