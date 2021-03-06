package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomTypeReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @Test
  void mapToCustomFieldsDraft_WithNullCustomType_ShouldReturnNullCustomFields() {
    final Category mockCategory = mock(Category.class);

    final CustomFieldsDraft customFieldsDraft =
        mapToCustomFieldsDraft(mockCategory, referenceIdToKeyCache);

    assertThat(customFieldsDraft).isNull();
  }

  @Test
  void
      mapToCustomFieldsDraft_WithNonExpandedCategoryAndIdsAreCached_ShouldReturnCustomFieldsDraft() {
    // preparation
    final Category mockCategory = mock(Category.class);
    final CustomFields mockCustomFields = mock(CustomFields.class);
    final String typeKey = "typeKey";
    final String typeId = UUID.randomUUID().toString();
    final Reference<Type> mockCustomType =
        Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), typeId);
    when(mockCustomFields.getType()).thenReturn(mockCustomType);
    when(mockCategory.getCustom()).thenReturn(mockCustomFields);

    // Cache typeKey Value with typeId
    referenceIdToKeyCache.add(typeId, typeKey);

    // test
    final CustomFieldsDraft customFieldsDraft =
        mapToCustomFieldsDraft(mockCategory, referenceIdToKeyCache);

    // assertion
    assertThat(customFieldsDraft).isNotNull();
    assertThat(customFieldsDraft.getType().getKey()).isEqualTo(typeKey);
    assertThat(customFieldsDraft.getType().getId()).isNull();
  }

  @Test
  void
      mapToCustomFieldsDraft_WithNonExpandedCategoryAndIdsNotCached_ShouldReturnResourceIdentifierWithoutKey() {
    // preparation
    final Category mockCategory = mock(Category.class);
    final CustomFields mockCustomFields = mock(CustomFields.class);
    final String customTypeUuid = UUID.randomUUID().toString();
    final Reference<Type> mockCustomType =
        Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeUuid);
    when(mockCustomFields.getType()).thenReturn(mockCustomType);
    when(mockCategory.getCustom()).thenReturn(mockCustomFields);

    // test
    final CustomFieldsDraft customFieldsDraft =
        mapToCustomFieldsDraft(mockCategory, referenceIdToKeyCache);

    // assertion
    assertThat(customFieldsDraft).isNotNull();
    assertThat(customFieldsDraft.getType()).isNotNull();
    assertThat(customFieldsDraft.getType().getId()).isEqualTo(customTypeUuid);
    assertThat(customFieldsDraft.getType().getKey()).isNull();
  }
}
