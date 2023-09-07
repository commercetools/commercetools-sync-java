package com.commercetools.sync.categories.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
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
        MockUtils.getAssetMockWithCustomFields(
            TypeReferenceBuilder.of().id(assetCustomTypeId).build());

    final List<Category> mockCategories = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      final Category mockCategory = mock(Category.class);

      final CategoryReference parentReference = CategoryReferenceBuilder.of().id(parentId).build();
      when(mockCategory.getName()).thenReturn(LocalizedString.ofEnglish("test"));
      when(mockCategory.getSlug()).thenReturn(LocalizedString.ofEnglish("test"));
      when(mockCategory.getParent()).thenReturn(parentReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
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
      when(mockCategory.getName()).thenReturn(LocalizedString.ofEnglish("test"));
      when(mockCategory.getSlug()).thenReturn(LocalizedString.ofEnglish("test"));
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
