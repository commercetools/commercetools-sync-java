package com.commercetools.sync.commons;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static java.lang.String.format;

import com.commercetools.sync.commons.models.FetchCustomObjectsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CleanupUnresolvedReferenceCustomObjects {

  private final SphereClient sphereClient;
  private final Statistics statistics;
  private int pageSize = 500;
  private Consumer<Throwable> errorCallback;

  private CleanupUnresolvedReferenceCustomObjects(@Nonnull final SphereClient sphereClient) {
    this.sphereClient = sphereClient;
    this.statistics = new Statistics();
  }

  /**
   * Creates new instance of {@link CleanupUnresolvedReferenceCustomObjects} which has the
   * functionality to run cleanup helpers.
   *
   * @param sphereClient the client object.
   * @return new instance of {@link CleanupUnresolvedReferenceCustomObjects}
   */
  public static CleanupUnresolvedReferenceCustomObjects of(
      @Nonnull final SphereClient sphereClient) {
    return new CleanupUnresolvedReferenceCustomObjects(sphereClient);
  }

  /**
   * Sets the {@code errorCallback} function of the cleanup. This callback will be called whenever
   * an event occurs that leads to an error alert from the cleanup process.
   *
   * @param errorCallback the new value to set to the error callback.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public CleanupUnresolvedReferenceCustomObjects errorCallback(
      @Nonnull final Consumer<Throwable> errorCallback) {
    this.errorCallback = errorCallback;
    return this;
  }

  /**
   * Given an {@code exception}, this method calls the {@code errorCallback} function. If {@code
   * errorCallback} is null, this method does nothing.
   *
   * @param exception {@link Throwable} instance to supply as first param to the {@code
   *     errorCallback} function.
   */
  private void applyErrorCallback(@Nonnull final Throwable exception) {
    if (this.errorCallback != null) {
      this.errorCallback.accept(exception);
    }
  }

  /**
   * Configures the pageSize (limit), the maximum number of results to return from the grapqhl
   * query, which can be set using the limit query parameter. The default page size is 500.
   *
   * <p>NOTE: Changing this value might negatively impact the performance of the cleanup and must be
   * tested properly.
   *
   * @param pageSize int that indicates batch size of resources to process.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public CleanupUnresolvedReferenceCustomObjects pageSize(final int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * Deletes the unresolved reference custom objects persisted by commercetools-sync-java library to
   * handle reference resolution. The custom objects will be deleted if it hasn't been modified for
   * the specified amount of days as given {@code deleteDaysAfterLastModification}.
   *
   * <p>Note: Keeping the unresolved references forever can negatively influence the performance of
   * your project, so deleting unused data ensures the best performance for your project.
   *
   * @param deleteDaysAfterLastModification Days to query. The custom objects will be deleted if it
   *     hasn't been modified for the specified amount of days.
   * @return an instance of {@link CompletableFuture}&lt;{@link
   *     CleanupUnresolvedReferenceCustomObjects.Statistics} &gt; which contains the processing
   *     time, the total number of custom objects that were deleted and failed to delete, and a
   *     proper summary message of the statistics.
   */
  public CompletableFuture<Statistics> cleanup(final int deleteDaysAfterLastModification) {

    return CompletableFuture.allOf(
            cleanupUnresolvedProductReferences(deleteDaysAfterLastModification),
            cleanupUnresolvedStateReferences(deleteDaysAfterLastModification))
        .thenApply(ignoredResult -> statistics);
  }

  private CompletableFuture<Void> cleanup(
      @Nonnull final String containerName, final int deleteDaysAfterLastModification) {
    final Consumer<Set<ResourceKeyId>> pageConsumer =
        resourceKeyIds -> deleteCustomObjects(containerName, resourceKeyIds);

    return queryAll(
            sphereClient,
            getRequest(containerName, deleteDaysAfterLastModification),
            pageConsumer,
            pageSize)
        .toCompletableFuture();
  }

  private CompletableFuture<Void> cleanupUnresolvedProductReferences(
      final int deleteDaysAfterLastModification) {
    return cleanup(
        UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CONTAINER_KEY,
        deleteDaysAfterLastModification);
  }

  private CompletableFuture<Void> cleanupUnresolvedStateReferences(
      final int deleteDaysAfterLastModification) {
    return cleanup(
        UnresolvedTransitionsServiceImpl.CUSTOM_OBJECT_CONTAINER_KEY,
        deleteDaysAfterLastModification);
  }

  /**
   * Prepares a graphql request to fetch the unresolved reference custom objects based on the given
   * {@code containerName} and {@code deleteDaysAfterLastModification}.
   *
   * @param containerName container name (i.e
   *     "commercetools-sync-java.UnresolvedReferencesService.productDrafts")
   * @param deleteDaysAfterLastModification Days to query lastModifiedAt. The custom objects will be
   *     deleted if it hasn't been modified for the specified amount of days.
   */
  private FetchCustomObjectsGraphQlRequest getRequest(
      @Nonnull final String containerName, final int deleteDaysAfterLastModification) {

    final Instant lastModifiedAt =
        Instant.now().minus(deleteDaysAfterLastModification, ChronoUnit.DAYS);
    return new FetchCustomObjectsGraphQlRequest(containerName, lastModifiedAt);
  }

  /**
   * Deletes all custom objects in the given {@link List} representing a page of custom object's key
   * and ids.
   *
   * <p>Note: The deletion is done concurrently but it's blocked by page consumer to avoid race
   * conditions like fetching and removing same custom objects in same time.
   *
   * @param resourceKeyIdSet a page of custom object's key and ids.
   */
  private void deleteCustomObjects(
      @Nonnull final String containerName, @Nonnull final Set<ResourceKeyId> resourceKeyIdSet) {

    CompletableFuture.allOf(
            resourceKeyIdSet.stream()
                .map(resourceKeyId -> executeDeletion(containerName, resourceKeyId))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();
  }

  private CompletionStage<Optional<CustomObject<JsonNode>>> executeDeletion(
      @Nonnull final String containerName, @Nonnull final ResourceKeyId resourceKeyId) {

    return sphereClient
        .execute(
            CustomObjectDeleteCommand.of(containerName, resourceKeyId.getKey(), JsonNode.class))
        .handle(this::handleDeleteCallback);
  }

  private Optional<CustomObject<JsonNode>> handleDeleteCallback(
      @Nonnull final CustomObject<JsonNode> resource, @Nullable final Throwable throwable) {

    if (throwable == null) {
      statistics.totalDeleted.incrementAndGet();
      return Optional.of(resource);
    } else if (throwable instanceof NotFoundException) {
      return Optional.empty();
    }

    applyErrorCallback(throwable);
    statistics.totalFailed.incrementAndGet();
    return Optional.empty();
  }

  public static class Statistics {
    final AtomicInteger totalDeleted;
    final AtomicInteger totalFailed;

    private Statistics() {
      this.totalDeleted = new AtomicInteger();
      this.totalFailed = new AtomicInteger();
    }

    public int getTotalDeleted() {
      return totalDeleted.get();
    }

    public int getTotalFailed() {
      return totalFailed.get();
    }

    public String getReportMessage() {
      return format(
          "Summary: %s custom objects were deleted in total (%s failed to delete).",
          getTotalDeleted(), getTotalFailed());
    }
  }
}
