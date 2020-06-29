package com.commercetools.sync.commons;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static java.util.Optional.ofNullable;

/**
 * @param <V> Resource Draft (e.g. {@link io.sphere.sdk.products.ProductDraft},
 *             {@link io.sphere.sdk.categories.CategoryDraft}, etc..
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link io.sphere.sdk.categories.Category}, etc..
 */
public class BaseSyncOptions<U, V> {
    private final SphereClient ctpClient;
    private final QuadConsumer<SyncException, Optional<V>, Optional<U>, List<UpdateAction<U>>>
            errorCallback;
    private final TriConsumer<SyncException, Optional<V>, Optional<U>> warningCallback;
    private int batchSize;
    private final TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>> beforeUpdateCallback;
    private final Function<V, V> beforeCreateCallback;

    protected BaseSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadConsumer<SyncException, Optional<V>, Optional<U>, List<UpdateAction<U>>>
                errorCallback,
        @Nullable final TriConsumer<SyncException, Optional<V>, Optional<U>> warningCallback,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>>
            beforeUpdateCallback,
        @Nullable final Function<V, V> beforeCreateCallback) {
        this.ctpClient = ctpClient;
        this.errorCallback = errorCallback;
        this.batchSize = batchSize;
        this.warningCallback = warningCallback;
        this.beforeUpdateCallback = beforeUpdateCallback;
        this.beforeCreateCallback = beforeCreateCallback;
    }

    /**
     * Returns the {@link SphereClient} responsible for interaction with the target CTP project.
     *
     * @return the {@link SphereClient} responsible for interaction with the target CTP project.
     */
    public SphereClient getCtpClient() {
        return ctpClient;
    }

    /**
     * Returns the {@code errorCallback} {@link QuadConsumer}&lt;{@link SyncException},
     * {@link Optional}&lt;{@code V}&gt;, {@link Optional}&lt;{@code U}&gt;,
     * {@link List}&lt;{@link UpdateAction}&lt;{@code U}&gt;&gt;&gt; function set to
     * {@code this} {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs during
     * the sync process that represents an error.
     *
     * @return the {@code errorCallback} {@link QuadConsumer}&lt;{@link SyncException},
     *      {@link Optional}&lt;{@code V}&gt;, {@link Optional}&lt;{@code U}&gt;,
     *      {@link List}&lt;{@link UpdateAction}&lt;{@code U}&gt;&gt; function set to
     *      {@code this} {@link BaseSyncOptions}
     */
    @Nullable
    public QuadConsumer<SyncException, Optional<V>, Optional<U>, List<UpdateAction<U>>> getErrorCallback() {
        return errorCallback;
    }

    /**
     * Returns the {@code warningCallback} {@link TriConsumer}&lt;{@link SyncException},
     * {@link Optional}&lt;{@code V}&gt;, {@link Optional}&lt;{@code U}&gt;&gt; function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs
     * during the sync process that represents a warning.
     *
     * @return the {@code warningCallback} {@link TriConsumer}&lt;{@link SyncException},
     *      {@link Optional}&lt;{@code V}&gt;, {@link Optional}&lt;{@code U}&gt;&gt; function set to {@code this}
     *      {@link BaseSyncOptions}
     */
    @Nullable
    public TriConsumer<SyncException, Optional<V>, Optional<U>> getWarningCallback() {
        return warningCallback;
    }

    /**
     * Given an {@code exception}, {@code oldResource} and {@code newResourceDraft} this method calls the
     * {@code warningCallback} function which is set to {@code this} instance of the
     * {@link BaseSyncOptions}. If there {@code warningCallback} is null, this
     * method does nothing.
     *
     * @param exception the exception to supply to the {@code warningCallback} function.
     * @param oldResource the old resource that is being compared to the new draft.
     * @param newResourceDraft the new resource draft that is being compared to the old resource.
     */
    public void applyWarningCallback(@Nonnull final SyncException exception, @Nullable final U oldResource,
        @Nullable final V newResourceDraft) {
        if (this.warningCallback != null) {
            this.warningCallback.accept(exception, Optional.ofNullable(newResourceDraft),
                Optional.ofNullable(oldResource));
        }
    }

    /**
     * Given an {@code errorMessage} and an {@code exception}, this method calls the {@code errorCallback} function
     * which is set to {@code this} instance of the {@link BaseSyncOptions}. If there {@code errorCallback} is null,
     * this method does nothing.
     *
     * @param exception {@link Throwable} instance to supply as first param to the {@code errorCallback} function.
     * @param oldResource the old resource that is being compared to the new draft.
     * @param newResourceDraft the new resource draft that is being compared to the old resource.
     * @param updateActions the list of update actions.
     */
    public void applyErrorCallback(@Nonnull final SyncException exception, @Nullable final U oldResource,
        @Nullable final V newResourceDraft, @Nullable final List<UpdateAction<U>> updateActions) {
        if (this.errorCallback != null) {
            this.errorCallback.accept(exception, Optional.ofNullable(newResourceDraft),
                Optional.ofNullable(oldResource), updateActions);
        }
    }

    /**
     *
     * @param syncException {@link Throwable} instance to supply as first param to the {@code errorCallback} function.
     * @see #applyErrorCallback(SyncException exception, Object oldResource, Object newResource, List updateActions)
     */
    public void applyErrorCallback(@Nonnull final SyncException syncException) {
        applyErrorCallback(syncException, null, null, null);
    }

    /**
     *
     * @param errorMessage the error message to supply as part of first param to the {@code errorCallback} function.
     * @see #applyErrorCallback(SyncException exception, Object oldResource, Object newResource, List updateActions)
     */
    public void applyErrorCallback(@Nonnull final String errorMessage) {
        applyErrorCallback(new SyncException(errorMessage), null, null, null);
    }

    /**
     * Gets the batch size used in the sync process. During the sync there is a need for fetching existing
     * resources so that they can be compared with the new resource drafts. That's why the input is sliced into
     * batches and then processed. It allows to reduce the query size for fetching all resources processed in one
     * batch.
     * E.g. value of 30 means that 30 entries from input list would be accumulated and one API call will be performed
     * for fetching entries responding to them. Then comparision and sync are performed.
     *
     * <p>This batch size is set to 30 by default.
     * @return option that indicates capacity of batch of resources to process.
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Returns the {@code beforeUpdateCallback} {@link TriFunction}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     * {@code U}&gt;&gt;, {@code V}, {@code U}, {@link List}&lt;{@link UpdateAction}&lt;{@code U}&gt;&gt;&gt; function
     * set to {@code this} {@link BaseSyncOptions}. It represents a callback function which is applied (if set) on the
     * generated list of update actions to produce a resultant list after the filter function has been applied.
     *
     * @return the {@code beforeUpdateCallback} {@link TriFunction}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     *         {@code U}&gt;&gt;, {@code V}, {@code U}, {@link List}&lt;{@link UpdateAction}&lt;{@code U}&gt;&gt;&gt;
     *         function set to {@code this} {@link BaseSyncOptions}.
     */
    @Nullable
    public TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>> getBeforeUpdateCallback() {
        return beforeUpdateCallback;
    }

    /**
     * Returns the {@code beforeCreateCallback} {@link Function}&lt;{@code V}, {@link Optional}&lt;{@code V}&gt;&gt;
     * function set to {@code this} {@link BaseSyncOptions}. It represents a callback function which is applied (if set)
     * on the resource draft of type {@code V} before it is created to produce a resultant draft after the filter
     * function has been applied.
     *
     * @return the {@code beforeUpdateCallback} {@link Function}&lt;{@code V}, {@link Optional}&lt;{@code V}&gt;&gt;
     *         function set to {@code this} {@link BaseSyncOptions}.
     */
    @Nullable
    public Function<V, V> getBeforeCreateCallback() {
        return beforeCreateCallback;
    }

    /**
     * Given a {@link List} of {@link UpdateAction}, a new resource draft of type {@code V} and the old existing
     * resource of the type {@code U}, this method applies the {@code beforeUpdateCallback} function which is set to
     * {@code this} instance of the {@link BaseSyncOptions} and returns the result. If the {@code beforeUpdateCallback}
     * is null or {@code updateActions} is empty, this method does nothing to the supplied list of {@code updateActions}
     * and returns the same list. If the result of the callback is null, an empty list is returned.
     *
     * @param updateActions the list of update actions to apply the {@code beforeUpdateCallback} function on.
     * @param newResourceDraft the new resource draft that is being compared to the old resource.
     * @param oldResource the old resource that is being compared to the new draft.
     * @return a list of update actions after applying the {@code beforeUpdateCallback} function on. If the
     *         {@code beforeUpdateCallback} function is null or {@code updateActions} is empty, the supplied list of
     *         {@code updateActions} is returned as is. If the return of the callback is null, an empty list is
     *         returned.
     */
    @Nonnull
    public List<UpdateAction<U>> applyBeforeUpdateCallback(@Nonnull final List<UpdateAction<U>> updateActions,
                                                           @Nonnull final V newResourceDraft,
                                                           @Nonnull final U oldResource) {

        return ofNullable(beforeUpdateCallback)
            .filter(callback -> !updateActions.isEmpty())
            .map(filteredCallback -> emptyIfNull(filteredCallback.apply(updateActions, newResourceDraft, oldResource)))
            .orElse(updateActions);
    }

    /**
     * Given a new resource draft of type {@code V} this method applies the {@code beforeCreateCallback} function
     * which is set to {@code this} instance of the {@link BaseSyncOptions} and returns the result.
     * <ul>
     *     <li>If the {@code beforeCreateCallback} is null, this method does nothing to the supplied resource draft and
     *     returns it as is, wrapped in an optional.</li>
     *     <li>If the return of {@code beforeCreateCallback} is null, an empty optional is returned.</li>
     *     <li>Otherwise, the result of the {@code beforeCreateCallback} is returned in the optional.</li>
     * </ul>
     *
     * @param newResourceDraft the new resource draft that should be created.
     * @return an optional containing the resultant resource draft after applying the {@code beforeCreateCallback}
     *         function on. If the {@code beforeCreateCallback} function is null, the supplied resource draft is
     *         returned as is, wrapped in an optional.
     */
    @Nonnull
    public Optional<V> applyBeforeCreateCallback(@Nonnull final V newResourceDraft) {
        return ofNullable(
                beforeCreateCallback != null ? beforeCreateCallback.apply(newResourceDraft) : newResourceDraft);
    }
}
