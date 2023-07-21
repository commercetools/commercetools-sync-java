package com.commercetools.sync.sdk2.shoppinglists;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class ShoppingListSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        ShoppingListSyncOptionsBuilder,
        ShoppingListSyncOptions,
        ShoppingList,
        ShoppingListDraft,
        ShoppingListUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private ShoppingListSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link
   * com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder} given a {@link
   * ProjectApiRoot} responsible for interaction with the target CTP project, with the default batch
   * size ({@code BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link ProjectApiRoot} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link
   *     com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder}
   */
  public static ShoppingListSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new ShoppingListSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link ShoppingListSyncOptions} enriched with all fields provided to
   * {@code this} builder
   *
   * @return new instance of {@link ShoppingListSyncOptions}
   */
  @Override
  public ShoppingListSyncOptions build() {
    return new ShoppingListSyncOptions(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }

  /**
   * Returns an instance of this class to be used in the superclass's generic methods. Please see
   * the JavaDoc in the overridden method for further details.
   *
   * @return an instance of this class.
   */
  @Override
  protected ShoppingListSyncOptionsBuilder getThis() {
    return this;
  }
}
