package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static com.commercetools.sync.commons.utils.AssetReferenceReplacementUtils.replaceAssetsReferencesIdsWithKeys;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class AssetReferenceReplacementUtilsTest {

    @Test
    public void replaceAssetsReferencesIdsWithKeys_WithAllExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        // preparation
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        final Asset asset =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));

        // test
        final List<AssetDraft> referenceReplacedDrafts = replaceAssetsReferencesIdsWithKeys(singletonList(asset));

        // assertion
        referenceReplacedDrafts
            .forEach(referenceReplacedDraft -> {
                assertThat(referenceReplacedDraft.getCustom()).isNotNull();
                assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customType.getKey());
            });
    }

    @Test
    public void replaceAssetsReferencesIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceOnlyExpandedRefs() {
        // preparation
        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

        final Asset asset1 =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));
        final Asset asset2 =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
                UUID.randomUUID().toString()));

        // test
        final List<AssetDraft> referenceReplacedDrafts = replaceAssetsReferencesIdsWithKeys(asList(asset1, asset2));

        // assertion
        assertThat(referenceReplacedDrafts).hasSize(2);
        assertThat(referenceReplacedDrafts.get(0).getCustom()).isNotNull();
        assertThat(referenceReplacedDrafts.get(0).getCustom().getType().getId()).isEqualTo(customType.getKey());
        assertThat(referenceReplacedDrafts.get(1).getCustom()).isNotNull();
        assertThat(referenceReplacedDrafts.get(1).getCustom().getType().getId())
            .isEqualTo(asset2.getCustom().getType().getId());
    }

    @Test
    public void replaceAssetsReferencesIdsWithKeys_WithNonExpandedRefs_ShouldReturnReferencesWithoutReplacedKeys() {
        // preparation
        final String customTypeId = UUID.randomUUID().toString();

        final Asset asset = getAssetMockWithCustomFields(
            Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeId));

        // test
        final List<AssetDraft> referenceReplacedDrafts = replaceAssetsReferencesIdsWithKeys(singletonList(asset));

        // assertion
        referenceReplacedDrafts
            .forEach(referenceReplacedDraft -> {
                assertThat(referenceReplacedDraft.getCustom()).isNotNull();
                assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
            });
    }

}