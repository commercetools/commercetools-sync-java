package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssetReferenceResolutionUtilsTest {

  @Test
  void mapToAssetDrafts_WithAllExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
    // preparation
    final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

    final Asset asset =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));

    // test
    final List<AssetDraft> referenceReplacedDrafts = mapToAssetDrafts(singletonList(asset));

    // assertion
    referenceReplacedDrafts.forEach(
        referenceReplacedDraft -> {
          assertThat(referenceReplacedDraft.getCustom()).isNotNull();
          assertThat(referenceReplacedDraft.getCustom().getType().getKey())
              .isEqualTo(customType.getKey());
        });
  }

  @Test
  void mapToAssetDrafts_WithSomeExpandedReferences_ShouldReplaceOnlyExpandedRefs() {
    // preparation
    final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

    final Asset asset1 =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));
    final Asset asset2 =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), UUID.randomUUID().toString()));

    // test
    final List<AssetDraft> referenceReplacedDrafts = mapToAssetDrafts(asList(asset1, asset2));

    // assertion
    assertThat(referenceReplacedDrafts).hasSize(2);
    assertThat(referenceReplacedDrafts.get(0).getCustom()).isNotNull();
    assertThat(referenceReplacedDrafts.get(0).getCustom().getType().getKey())
        .isEqualTo(customType.getKey());
    assertThat(referenceReplacedDrafts.get(1).getCustom()).isNotNull();
    assertThat(referenceReplacedDrafts.get(1).getCustom().getType().getId())
        .isEqualTo(asset2.getCustom().getType().getId());
  }

  @Test
  void mapToAssetDrafts_WithNonExpandedRefs_ShouldReturnReferencesWithoutReplacedKeys() {
    // preparation
    final String customTypeId = UUID.randomUUID().toString();

    final Asset asset =
        getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeId));

    // test
    final List<AssetDraft> referenceReplacedDrafts = mapToAssetDrafts(singletonList(asset));

    // assertion
    referenceReplacedDrafts.forEach(
        referenceReplacedDraft -> {
          assertThat(referenceReplacedDraft.getCustom()).isNotNull();
          assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
        });
  }
}
