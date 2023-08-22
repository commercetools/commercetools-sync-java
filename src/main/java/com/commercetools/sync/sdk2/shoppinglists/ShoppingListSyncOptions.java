package com.commercetools.sync.sdk2.shoppinglists;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShoppingListSyncOptions
    extends BaseSyncOptions<ShoppingList, ShoppingListDraft, ShoppingListUpdateAction> {

  ShoppingListSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<ShoppingListDraft>,
                  Optional<ShoppingList>,
                  List<ShoppingListUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<ShoppingListDraft>, Optional<ShoppingList>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<ShoppingListUpdateAction>,
                  ShoppingListDraft,
                  ShoppingList,
                  List<ShoppingListUpdateAction>>
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
