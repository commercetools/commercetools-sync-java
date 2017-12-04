package com.commercetools.sync.commons.utils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.commons.utils.SyncUtils.replaceCustomTypeIdWithKeys;
import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncUtilsTest {

    @Test
    public void batchElements_WithValidSize_ShouldReturnCorrectBatches() {
        final int numberOfCategoryDrafts = 160;
        final int batchSize = 10;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchElements(categoryDrafts, 10);
        assertThat(batches.size()).isEqualTo(numberOfCategoryDrafts / batchSize);
    }

    @Test
    public void batchCategories_WithEmptyListAndAnySize_ShouldReturnNoBatches() {
        final List<List<CategoryDraft>> batches = batchElements(new ArrayList<>(), 100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    public void batchElements_WithNegativeSize_ShouldReturnNoBatches() {
        final int numberOfCategoryDrafts = 160;
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();

        for (int i = 0; i < numberOfCategoryDrafts; i++) {
            categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "key" + i, "parentKey",
                "customTypeId", new HashMap<>()));
        }
        final List<List<CategoryDraft>> batches = batchElements(categoryDrafts, -100);
        assertThat(batches.size()).isEqualTo(0);
    }

    @Test
    public void replaceCustomTypeIdWithKeys_WithNullCustomType_ShouldReturnNullCustomFields() {
        final Category mockCategory = mock(Category.class);
        final CustomFieldsDraft customFieldsDraft = replaceCustomTypeIdWithKeys(mockCategory);
        assertThat(customFieldsDraft).isNull();
    }

    @Test
    public void replaceCustomTypeIdWithKeys_WithExpandedCategory_ShouldReturnCustomFieldsDraft() {
        final Category mockCategory = mock(Category.class);
        final CustomFields mockCustomFields  = mock(CustomFields.class);
        final Type mockType = mock(Type.class);
        final String typeKey = "typeKey";
        when(mockType.getKey()).thenReturn(typeKey);
        final Reference<Type> mockCustomType = Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(),
            mockType);
        when(mockCustomFields.getType()).thenReturn(mockCustomType);
        when(mockCategory.getCustom()).thenReturn(mockCustomFields);

        final CustomFieldsDraft customFieldsDraft = replaceCustomTypeIdWithKeys(mockCategory);
        assertThat(customFieldsDraft).isNotNull();
        assertThat(customFieldsDraft.getType().getId()).isEqualTo(typeKey);
    }

    @Test
    public void replaceCustomTypeIdWithKeys_WithNonExpandedCategory_ShouldReturnReferenceWithoutReplacedKey() {
        final Category mockCategory = mock(Category.class);
        final CustomFields mockCustomFields  = mock(CustomFields.class);
        final String customTypeUuid = UUID.randomUUID().toString();
        final Reference<Type> mockCustomType = Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
            customTypeUuid);
        when(mockCustomFields.getType()).thenReturn(mockCustomType);
        when(mockCategory.getCustom()).thenReturn(mockCustomFields);

        final CustomFieldsDraft customFieldsDraft = replaceCustomTypeIdWithKeys(mockCategory);
        assertThat(customFieldsDraft).isNotNull();
        assertThat(customFieldsDraft.getType()).isNotNull();
        assertThat(customFieldsDraft.getType().getId()).isEqualTo(customTypeUuid);
    }

    @Test
    public void replaceReferenceIdWithKey_WithNullReference_ShouldReturnNullReference() {
        final Reference<Object> keyReplacedReference = replaceReferenceIdWithKey(null,
            () -> Reference.of(Category.referenceTypeId(), "id"));
        assertThat(keyReplacedReference).isNull();
    }

    @Test
    public void replaceReferenceIdWithKey_WithExpandedCategoryReference_ShouldReturnCategoryReferenceWithKey() {
        final String categoryKey = "categoryKey";
        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn(categoryKey);

        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndObj(Category.referenceTypeId(),
            mockCategory);

        final Reference<Category> keyReplacedReference = replaceReferenceIdWithKey(categoryReference,
            () -> Category.referenceOfId(categoryReference.getObj().getKey()));
        assertThat(keyReplacedReference).isNotNull();
        assertThat(keyReplacedReference.getId()).isEqualTo(categoryKey);
    }

    @Test
    public void replaceReferenceIdWithKey_WithNonExpandedCategoryReference_ShouldReturnReferenceWithoutReplacedKey() {
        final String categoryUuid = UUID.randomUUID().toString();
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(Category.referenceTypeId(),
            categoryUuid);

        final Reference<Category> keyReplacedReference = replaceReferenceIdWithKey(categoryReference,
            () -> Category.referenceOfId(categoryReference.getObj().getKey()));
        assertThat(keyReplacedReference).isNotNull();
        assertThat(keyReplacedReference.getId()).isEqualTo(categoryUuid);
    }
}
