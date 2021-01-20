package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.categories.CategorySyncOptionsBuilder.of;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
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
            SyncException,
            Optional<CategoryDraft>,
            Optional<Category>,
            List<UpdateAction<Category>>>
        errorCallback =
            (exception, oldResource, newResource, updateActions) ->
                errorMessages.add(exception.getMessage());

    // Mock sync options
    final CategorySyncOptions categorySyncOptions =
        of(mock(SphereClient.class)).errorCallback(errorCallback).build();

    final Optional<UpdateAction<Category>> updateAction =
        buildTypedSetCustomTypeUpdateAction(
            null,
            new HashMap<>(),
            mock(Asset.class),
            new AssetCustomActionBuilder(),
            1,
            Asset::getId,
            assetResource -> Asset.resourceTypeId(),
            Asset::getKey,
            categorySyncOptions);

    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .isEqualTo(
            "Failed to build 'setCustomType' update action on the asset with"
                + " id 'null'. Reason: New Custom Type id is blank (null/empty).");
    assertThat(updateAction).isEmpty();
  }
}
