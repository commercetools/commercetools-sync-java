package com.commercetools.sync.shoppinglists;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import com.commercetools.sync.services.impl.ShoppingListServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListReferenceResolver;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.shoppinglists.utils.ShoppingListSyncUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * This class syncs shopping list drafts with corresponding shopping list resources in the CTP
 * project.
 */
public class ShoppingListSync
    extends BaseSync<
        ShoppingList,
        ShoppingListDraft,
        ShoppingListUpdateAction,
        ShoppingListSyncStatistics,
        ShoppingListSyncOptions> {

  private static final String CTP_SHOPPING_LIST_UPDATE_FAILED =
      "Failed to update shopping lists with key: '%s'. Reason: %s";
  private static final String CTP_SHOPPING_LIST_FETCH_FAILED =
      "Failed to fetch existing shopping lists with keys: " + "'%s'.";
  private static final String FAILED_TO_PROCESS =
      "Failed to process the ShoppingListDraft with key:'%s'. Reason: %s";

  private final ShoppingListService shoppingListService;
  private final ShoppingListReferenceResolver shoppingListReferenceResolver;
  private final ShoppingListBatchValidator shoppingListBatchValidator;

  /**
   * Takes a {@link ShoppingListSyncOptions} to instantiate a new {@link ShoppingListSync} instance
   * that could be used to sync shopping list drafts in the CTP project specified in the injected
   * {@link ShoppingListSyncOptions} instance.
   *
   * @param shoppingListSyncOptions the container of all the options of the sync process including
   *     the CTP project client and/or configuration and other sync-specific options.
   */
  public ShoppingListSync(@Nonnull final ShoppingListSyncOptions shoppingListSyncOptions) {

    this(
        shoppingListSyncOptions,
        new ShoppingListServiceImpl(shoppingListSyncOptions),
        new CustomerServiceImpl(
            CustomerSyncOptionsBuilder.of(shoppingListSyncOptions.getCtpClient()).build()),
        new TypeServiceImpl(shoppingListSyncOptions));
  }

  /**
   * Takes a {@link ShoppingListSyncOptions} and service instances to instantiate a new {@link
   * ShoppingListSync} instance that could be used to sync shopping list drafts in the CTP project
   * specified in the injected {@link ShoppingListSyncOptions} instance.
   *
   * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and
   * passed to.
   *
   * @param syncOptions the container of all the options of the sync process including the CTP
   *     project client and/or configuration and other sync-specific options.
   * @param shoppingListService the shopping list service which is responsible for fetching/caching
   *     the ShoppingLists from the CTP project.
   * @param customerService the customer service which is responsible for fetching/caching the
   *     Customers from the CTP project.
   * @param typeService the type service which is responsible for fetching/caching the Types from
   *     the CTP project.
   */
  protected ShoppingListSync(
      @Nonnull final ShoppingListSyncOptions syncOptions,
      @Nonnull final ShoppingListService shoppingListService,
      @Nonnull final CustomerService customerService,
      @Nonnull final TypeService typeService) {

    super(new ShoppingListSyncStatistics(), syncOptions);
    this.shoppingListService = shoppingListService;
    this.shoppingListReferenceResolver =
        new ShoppingListReferenceResolver(getSyncOptions(), customerService, typeService);
    this.shoppingListBatchValidator =
        new ShoppingListBatchValidator(getSyncOptions(), getStatistics());
  }

  /**
   * Iterates through the whole {@code ShoppingListDraft}'s list and accumulates its valid drafts to
   * batches. Every batch is then processed by {@link
   * ShoppingListSync#processBatch(java.util.List)}.
   *
   * <p><strong>Inherited doc:</strong> {@inheritDoc}
   *
   * @param shoppingListDrafts {@link java.util.List} of {@link ShoppingListDraft}'s that would be
   *     synced into CTP project.
   * @return {@link java.util.concurrent.CompletionStage} with {@link ShoppingListSyncStatistics}
   *     holding statistics of all sync processes performed by this sync instance.
   */
  @Override
  protected CompletionStage<ShoppingListSyncStatistics> process(
      @Nonnull final List<ShoppingListDraft> shoppingListDrafts) {

    final List<List<ShoppingListDraft>> batches =
        batchElements(shoppingListDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  @Override
  protected CompletionStage<ShoppingListSyncStatistics> processBatch(
      @Nonnull final List<ShoppingListDraft> batch) {

    final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys>
        validationResult = shoppingListBatchValidator.validateAndCollectReferencedKeys(batch);

    final Set<ShoppingListDraft> shoppingListDrafts = validationResult.getLeft();
    if (shoppingListDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return completedFuture(statistics);
    }

    return shoppingListReferenceResolver
        .populateKeyToIdCachesForReferencedKeys(validationResult.getRight())
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getRight();
              if (cachingException != null) {
                handleError(
                    "Failed to build a cache of keys to ids.",
                    cachingException,
                    null,
                    null,
                    null,
                    shoppingListDrafts.size());
                return CompletableFuture.completedFuture(null);
              }

              final Set<String> shoppingListDraftKeys =
                  shoppingListDrafts.stream().map(ShoppingListDraft::getKey).collect(toSet());

              return shoppingListService
                  .fetchMatchingShoppingListsByKeys(shoppingListDraftKeys)
                  .handle(ImmutablePair::new)
                  .thenCompose(
                      fetchResponse -> {
                        final Set<ShoppingList> fetchedShoppingLists = fetchResponse.getLeft();
                        final Throwable exception = fetchResponse.getRight();

                        if (exception != null) {
                          final String errorMessage =
                              format(CTP_SHOPPING_LIST_FETCH_FAILED, shoppingListDraftKeys);
                          handleError(
                              errorMessage,
                              exception,
                              null,
                              null,
                              null,
                              shoppingListDraftKeys.size());
                          return CompletableFuture.completedFuture(null);
                        } else {
                          return syncBatch(fetchedShoppingLists, shoppingListDrafts);
                        }
                      });
            })
        .thenApply(
            ignoredResult -> {
              statistics.incrementProcessed(batch.size());
              return statistics;
            });
  }

  @Nonnull
  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<ShoppingList> oldShoppingLists,
      @Nonnull final Set<ShoppingListDraft> newShoppingListDrafts) {

    final Map<String, ShoppingList> keyShoppingListsMap =
        oldShoppingLists.stream().collect(toMap(ShoppingList::getKey, identity()));

    return CompletableFuture.allOf(
        newShoppingListDrafts.stream()
            .map(
                shoppingListDraft ->
                    shoppingListReferenceResolver
                        .resolveReferences(shoppingListDraft)
                        .thenCompose(
                            resolvedShoppingListDraft ->
                                syncDraft(keyShoppingListsMap, resolvedShoppingListDraft))
                        .exceptionally(
                            completionException -> {
                              final String errorMessage =
                                  format(
                                      FAILED_TO_PROCESS,
                                      shoppingListDraft.getKey(),
                                      completionException.getMessage());
                              handleError(
                                  errorMessage,
                                  completionException,
                                  null,
                                  shoppingListDraft,
                                  null,
                                  1);
                              return null;
                            }))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  @Nonnull
  private CompletionStage<Void> syncDraft(
      @Nonnull final Map<String, ShoppingList> keyShoppingListMap,
      @Nonnull final ShoppingListDraft newShoppingListDraft) {

    final ShoppingList shoppingListFromMap = keyShoppingListMap.get(newShoppingListDraft.getKey());
    return Optional.ofNullable(shoppingListFromMap)
        .map(oldShoppingList -> buildActionsAndUpdate(oldShoppingList, newShoppingListDraft))
        .orElseGet(() -> applyCallbackAndCreate(newShoppingListDraft));
  }

  @Nonnull
  private CompletionStage<Void> buildActionsAndUpdate(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingListDraft) {

    final List<ShoppingListUpdateAction> updateActions =
        ShoppingListSyncUtils.buildActions(oldShoppingList, newShoppingListDraft, syncOptions);

    final List<ShoppingListUpdateAction> updateActionsAfterCallback =
        syncOptions.applyBeforeUpdateCallback(updateActions, newShoppingListDraft, oldShoppingList);

    if (!updateActionsAfterCallback.isEmpty()) {
      return updateShoppinglist(oldShoppingList, newShoppingListDraft, updateActionsAfterCallback);
    }

    return completedFuture(null);
  }

  @Nonnull
  private CompletionStage<Void> updateShoppinglist(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingListDraft,
      @Nonnull final List<ShoppingListUpdateAction> updateActionsAfterCallback) {

    return shoppingListService
        .updateShoppingList(oldShoppingList, updateActionsAfterCallback)
        .handle(ImmutablePair::of)
        .thenCompose(
            updateResponse -> {
              final Throwable exception = updateResponse.getValue();
              if (exception != null) {
                return executeSupplierIfConcurrentModificationException(
                    exception,
                    () -> fetchAndUpdate(oldShoppingList, newShoppingListDraft),
                    () -> {
                      final String errorMessage =
                          format(
                              CTP_SHOPPING_LIST_UPDATE_FAILED,
                              newShoppingListDraft.getKey(),
                              exception.getMessage());
                      handleError(
                          errorMessage,
                          exception,
                          oldShoppingList,
                          newShoppingListDraft,
                          updateActionsAfterCallback,
                          1);
                      return CompletableFuture.completedFuture(null);
                    });
              } else {
                statistics.incrementUpdated();
                return CompletableFuture.completedFuture(null);
              }
            });
  }

  @Nonnull
  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final ShoppingList oldShoppingList,
      @Nonnull final ShoppingListDraft newShoppingListDraft) {

    final String shoppingListKey = oldShoppingList.getKey();
    return shoppingListService
        .fetchShoppingList(shoppingListKey)
        .handle(ImmutablePair::of)
        .thenCompose(
            fetchResponse -> {
              final Optional<ShoppingList> fetchedShoppingListOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        CTP_SHOPPING_LIST_UPDATE_FAILED,
                        shoppingListKey,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                handleError(
                    errorMessage, exception, oldShoppingList, newShoppingListDraft, null, 1);
                return CompletableFuture.completedFuture(null);
              }

              return fetchedShoppingListOptional
                  .map(
                      fetchedShoppingList ->
                          buildActionsAndUpdate(fetchedShoppingList, newShoppingListDraft))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                CTP_SHOPPING_LIST_UPDATE_FAILED,
                                shoppingListKey,
                                "Not found when attempting to fetch while retrying after concurrency modification.");
                        handleError(
                            errorMessage, null, oldShoppingList, newShoppingListDraft, null, 1);
                        return CompletableFuture.completedFuture(null);
                      });
            });
  }

  @Nonnull
  private CompletionStage<Void> applyCallbackAndCreate(
      @Nonnull final ShoppingListDraft shoppingListDraft) {

    return syncOptions
        .applyBeforeCreateCallback(shoppingListDraft)
        .map(
            draft ->
                shoppingListService
                    .createShoppingList(draft)
                    .thenAccept(
                        optional -> {
                          if (optional.isPresent()) {
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                        }))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }
}
