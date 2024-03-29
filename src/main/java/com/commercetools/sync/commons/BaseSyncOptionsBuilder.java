package com.commercetools.sync.commons;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;

public abstract class BaseSyncOptionsBuilder<
    SyncOptionsBuilderT extends
        BaseSyncOptionsBuilder<
                SyncOptionsBuilderT,
                SyncOptionsT,
                ResourceT,
                ResourceDraftT,
                ResourceUpdateActionT>,
    SyncOptionsT extends BaseSyncOptions<ResourceT, ResourceDraftT, ResourceUpdateActionT>,
    ResourceT,
    ResourceDraftT,
    ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>> {

  protected ProjectApiRoot ctpClient;
  protected QuadConsumer<
          SyncException, Optional<ResourceDraftT>, Optional<ResourceT>, List<ResourceUpdateActionT>>
      errorCallback;
  protected TriConsumer<SyncException, Optional<ResourceDraftT>, Optional<ResourceT>>
      warningCallback;
  protected int batchSize = 30;
  protected TriFunction<
          List<ResourceUpdateActionT>, ResourceDraftT, ResourceT, List<ResourceUpdateActionT>>
      beforeUpdateCallback;
  protected Function<ResourceDraftT, ResourceDraftT> beforeCreateCallback;
  protected long cacheSize = 10_000;

  /**
   * Sets the {@code errorCallback} function of the sync module. This callback will be called
   * whenever an event occurs that leads to an error alert from the sync process.
   *
   * @param errorCallback the new value to set to the error callback.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT errorCallback(
      @Nonnull
          final QuadConsumer<
                  SyncException,
                  Optional<ResourceDraftT>,
                  Optional<ResourceT>,
                  List<ResourceUpdateActionT>>
              errorCallback) {
    this.errorCallback = errorCallback;
    return getThis();
  }

  /**
   * Sets the {@code warningCallback} function of the sync module. This callback will be called
   * whenever an event occurs that leads to a warning alert from the sync process.
   *
   * @param warningCallback the new value to set to the warning callback.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT warningCallback(
      @Nonnull
          final TriConsumer<SyncException, Optional<ResourceDraftT>, Optional<ResourceT>>
              warningCallback) {
    this.warningCallback = warningCallback;
    return getThis();
  }

  /**
   * Set option that indicates batch size for sync process. During the sync there is a need for
   * fetching existing resources so that they can be compared with the new resource drafts. That's
   * why the input is sliced into batches and then processed. It allows to reduce the query size for
   * fetching all resources processed in one batch. E.g. value of 30 means that 30 entries from
   * input list would be accumulated and one API call will be performed for fetching entries
   * responding to them. Then comparision and sync are performed.
   *
   * <p>This batch size is set to 30 by default.
   *
   * @param batchSize int that indicates batch size of resources to process. Has to be positive or
   *     else will be ignored and default value of 30 would be used.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT batchSize(final int batchSize) {
    if (batchSize > 0) {
      this.batchSize = batchSize;
    }
    return getThis();
  }

  /**
   * Sets the cache size that indicates the key to id cache size of the sync process. To increase
   * performance during the sync some resource keys mapped to ids are cached which are required for
   * resolving references. To keep the cache performant outdated entries are evicted when a certain
   * size is reached.
   *
   * <p>Note: This cache size is set to 10.000 by default.
   *
   * @param cacheSize a long number value that indicates cache size of the key to id cache used for
   *     reference resolution. Has to be positive or else will be ignored and default value of
   *     10.000 would be used.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT cacheSize(final long cacheSize) {
    if (cacheSize > 0) {
      this.cacheSize = cacheSize;
    }
    return getThis();
  }

  /**
   * Sets the beforeUpdateCallback {@link TriFunction} which can be applied on the supplied list of
   * update actions generated from comparing an old resource of type {@code U} (e.g. {@link
   * Product}) to a new draft of type {@code V} (e.g. {@link ProductDraft}). It results in a
   * resultant list after the specified {@link TriFunction} {@code beforeUpdateCallback} function
   * has been applied. This can be used to intercept the sync process before issuing an update
   * request and to be able to manipulate the update actions. <b>Note</b>: Specifying a callback
   * that returns a {@code null} value or empty list will skip issuing the update request.
   *
   * @param beforeUpdateCallback function which can be applied on generated list of update actions.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT beforeUpdateCallback(
      @Nonnull
          final TriFunction<
                  List<ResourceUpdateActionT>,
                  ResourceDraftT,
                  ResourceT,
                  List<ResourceUpdateActionT>>
              beforeUpdateCallback) {
    this.beforeUpdateCallback = beforeUpdateCallback;
    return getThis();
  }

  /**
   * Sets the beforeCreateCallback {@link Function} which can be applied on a new resource draft of
   * type {@code V} (e.g. {@link ProductDraft}) before it's created by the sync. It results in a
   * resource draft of the same type which is the result of the application of the specified {@link
   * Function} {@code beforeCreateCallback} function. This can be used to intercept the sync process
   * before creating the resource draft and to be able to manipulate it. <b>Note</b>: Specifying a
   * callback that returns a {@code null} value will skip draft creation.
   *
   * @param beforeCreateCallback function which can be applied on a new draft before it's created by
   *     the sync.
   * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
   */
  public SyncOptionsBuilderT beforeCreateCallback(
      @Nonnull final Function<ResourceDraftT, ResourceDraftT> beforeCreateCallback) {
    this.beforeCreateCallback = beforeCreateCallback;
    return getThis();
  }

  /**
   * Creates new instance of {@code S} which extends {@link BaseSyncOptions} enriched with all
   * attributes provided to {@code this} builder.
   *
   * @return new instance of S which extends {@link BaseSyncOptions}
   */
  protected abstract SyncOptionsT build();

  /**
   * Returns {@code this} instance of {@code T}, which extends {@link BaseSyncOptionsBuilder}. The
   * purpose of this method is to make sure that {@code this} is an instance of a class which
   * extends {@link BaseSyncOptionsBuilder} in order to be used in the generic methods of the class.
   * Otherwise, without this method, the methods above would need to cast {@code this to T} which
   * could lead to a runtime error of the class was extended in a wrong way.
   *
   * @return an instance of the class that overrides this method.
   */
  protected abstract SyncOptionsBuilderT getThis();
}
