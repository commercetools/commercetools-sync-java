package com.commercetools.sync.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.AssetCustomTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GenericUpdateActionUtilsTest {

  @Test
  void
      buildTypedSetCustomTypeUpdateAction_WithNullNewIdCategoryAsset_ShouldNotBuildCategoryUpdateAction() {
    final ArrayList<String> errorMessages = new ArrayList<>();
    final QuadConsumer<
            SyncException, Optional<CategoryDraft>, Optional<Category>, List<CategoryUpdateAction>>
        errorCallback =
            (exception, oldResource, newResource, updateActions) ->
                errorMessages.add(exception.getMessage());

    // Mock sync options
    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(errorCallback)
            .build();

    final Optional<CategoryUpdateAction> updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
            null,
            new HashMap<>(),
            AssetCustomTypeAdapter.of(mock(Asset.class)),
            new AssetCustomActionBuilder(),
            1L,
            AssetCustomTypeAdapter::getId,
            ignore -> ResourceTypeId.ASSET.getJsonName(),
            AssetCustomTypeAdapter::getKey,
            categorySyncOptions);

    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .isEqualTo(
            "Failed to build 'setCustomType' update action on the asset with"
                + " id 'null'. Reason: New Custom Type id is blank (null/empty).");
    assertThat(updateAction).isEmpty();
  }
}
