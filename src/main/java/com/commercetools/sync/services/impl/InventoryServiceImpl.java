package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.inventories.helpers.InventoryEntryQueryBuilder.buildQueries;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.client.*;
import com.commercetools.api.models.inventory.*;
import com.commercetools.api.predicates.query.inventory.InventoryEntryQueryBuilderDsl;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.helpers.InventoryEntryIdentifier;
import com.commercetools.sync.services.InventoryService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class InventoryServiceImpl
    extends BaseService<
        InventorySyncOptions,
        InventoryEntry,
        InventoryEntryDraft,
        ByProjectKeyInventoryGet,
        InventoryPagedQueryResponse,
        ByProjectKeyInventoryByIDGet,
        InventoryEntry,
        InventoryEntryQueryBuilderDsl,
        ByProjectKeyInventoryPost>
    implements InventoryService {

  public InventoryServiceImpl(@Nonnull final InventorySyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<InventoryEntry>> fetchInventoryEntriesByIdentifiers(
      @Nonnull final Set<InventoryEntryIdentifier> identifiers) {

    final ProjectApiRoot ctpClient = this.syncOptions.getCtpClient();
    return ChunkUtils.executeChunks(buildQueries(ctpClient, identifiers))
        .thenApply(
            apiHttpResponses ->
                apiHttpResponses.stream()
                    .map(apiHttpResponse -> apiHttpResponse.getBody().getResults())
                    .flatMap(List::stream)
                    .collect(toList()))
        .thenApply(this::cacheAndMapToSet);
  }

  private HashSet<InventoryEntry> cacheAndMapToSet(@Nonnull final List<InventoryEntry> results) {
    results.forEach(
        resource ->
            keyToIdCache.put(
                String.valueOf(InventoryEntryIdentifier.of(resource)), resource.getId()));
    return new HashSet<>(results);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<InventoryEntry>> createInventoryEntry(
      @Nonnull final InventoryEntryDraft inventoryEntryDraft) {

    return super.createResource(
        inventoryEntryDraft,
        draft -> InventoryEntryIdentifier.of(draft).toString(),
        entry -> entry.getId(),
        inventoryEntry -> inventoryEntry,
        () -> syncOptions.getCtpClient().inventory().post(inventoryEntryDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<InventoryEntry> updateInventoryEntry(
      @Nonnull final InventoryEntry inventoryEntry,
      @Nonnull final List<InventoryEntryUpdateAction> updateActions) {
    final List<List<InventoryEntryUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<InventoryEntry>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, inventoryEntry));

    for (final List<InventoryEntryUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedInventory ->
                      syncOptions
                          .getCtpClient()
                          .inventory()
                          .withId(updatedInventory.getId())
                          .post(
                              InventoryEntryUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedInventory.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
