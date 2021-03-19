package com.commercetools.sync.shoppinglists;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShoppingListSyncOptions
    extends BaseSyncOptions<ShoppingList, ShoppingListDraft, ShoppingList> {

  ShoppingListSyncOptions(
      @Nonnull final SphereClient ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<ShoppingListDraft>,
                  Optional<ShoppingList>,
                  List<UpdateAction<ShoppingList>>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<ShoppingListDraft>, Optional<ShoppingList>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<UpdateAction<ShoppingList>>,
                  ShoppingListDraft,
                  ShoppingList,
                  List<UpdateAction<ShoppingList>>>
              beforeUpdateCallback,
      @Nullable final Function<ShoppingListDraft, ShoppingListDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
