package com.commercetools.sync.commons;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static java.util.Optional.ofNullable;

public class BaseSyncOptions<U, V> {
    private final SphereClient ctpClient;
    private final BiConsumer<String, Throwable> errorCallBack;
    private final Consumer<String> warningCallBack;
    private int batchSize;
    private boolean allowUuid = false;
    private final TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>> beforeUpdateCallback;
    private final Function<V, V> beforeCreateCallback;

    protected BaseSyncOptions(@Nonnull final SphereClient ctpClient,
                              @Nullable final BiConsumer<String, Throwable> errorCallBack,
                              @Nullable final Consumer<String> warningCallBack,
                              final int batchSize,
                              final boolean allowUuid,
                              @Nullable final TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>>
                                  beforeUpdateCallback,
                              @Nullable final Function<V, V> beforeCreateCallback) {
        this.ctpClient = ctpClient;
        this.errorCallBack = errorCallBack;
        this.batchSize = batchSize;
        this.warningCallBack = warningCallBack;
        this.allowUuid = allowUuid;
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
     * Returns the {@code errorCallback} {@link BiConsumer}&lt;{@link String}, {@link Throwable}&gt; function set to
     * {@code this} {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs during
     * the sync process that represents an error.
     *
     * @return the {@code errorCallback} {@link BiConsumer}&lt;{@link String}, {@link Throwable}&gt; function set to
     *      {@code this} {@link BaseSyncOptions}
     */
    @Nullable
    public BiConsumer<String, Throwable> getErrorCallBack() {
        return errorCallBack;
    }

    /**
     * Returns the {@code warningCallback} {@link Consumer}&lt;{@link String}&gt; function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs
     * during the sync process that represents a warning.
     *
     * @return the {@code warningCallback} {@link Consumer}&lt;{@link String}&gt; function set to {@code this}
     *      {@link BaseSyncOptions}
     */
    @Nullable
    public Consumer<String> getWarningCallBack() {
        return warningCallBack;
    }

    /**
     * Given a {@code warningMessage} string, this method calls the {@code warningCallback} function which is set
     * to {@code this} instance of the {@link BaseSyncOptions}. If there {@code warningCallback} is null, this
     * method does nothing.
     *
     * @param warningMessage the warning message to supply to the {@code warningCallback} function.
     */
    public void applyWarningCallback(@Nonnull final String warningMessage) {
        if (this.warningCallBack != null) {
            this.warningCallBack.accept(warningMessage);
        }
    }

    /**
     * Given an {@code errorMessage} and an {@code exception}, this method calls the {@code errorCallback} function
     * which is set to {@code this} instance of the {@link BaseSyncOptions}. If there {@code errorCallback} is null,
     * this method does nothing.
     *
     * @param errorMessage the error message to supply as first param to the {@code errorCallback} function.
     * @param exception    optional {@link Throwable} instance to supply to the {@code errorCallback} function as a
     *                     second param.
     */
    public void applyErrorCallback(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        if (this.errorCallBack != null) {
            this.errorCallBack.accept(errorMessage, exception);
        }
    }

    /**
     *
     * @param errorMessage the error message to supply as first param to the {@code errorCallback} function.
     * @see #applyErrorCallback(String, Throwable) applyErrorCallback(String, Throwable)
     */
    public void applyErrorCallback(@Nonnull final String errorMessage) {
        applyErrorCallback(errorMessage, null);
    }

    /**
     * The sync expects the user to pass the keys to references in the {@code id} field of References. If the key values
     * are in UUID format, then this flag must be set to true, otherwise the sync will fail to resolve the reference.
     * This flag, if set to true, enables the user to use keys with UUID format. By default, it is set to {@code false}.
     *
     * @return a {@code boolean} flag, if set to true, enables the user to use keys with UUID format for references.
     *      By default, it is set to {@code false}.
     */
    public boolean shouldAllowUuidKeys() {
        return allowUuid;
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
     * resource of the type {@code U}, this method applies the {@code beforeUpdateCallback} function
     * which is set to {@code this} instance of the {@link BaseSyncOptions} and returns the result. If the
     * {@code beforeUpdateCallback} is null, this method does nothing to the supplied list of update actions and returns
     * the same list. If the result of the callback is null, an empty list is returned.
     *
     * @param updateActions the list of update actions to apply the {@code beforeUpdateCallback} function on.
     * @param newResourceDraft the new resource draft that is being compared to the old resource.
     * @param oldResource the old resource that is being compared to the new draft.
     * @return a list of update actions after applying the {@code beforeUpdateCallback} function on. If the
     *         {@code beforeUpdateCallback} function was null, the supplied list of {@code updateActions} is returned as
     *         is. If the return of the callback was null, an empty list is returned.
     */
    @Nonnull
    public List<UpdateAction<U>> applyBeforeUpdateCallBack(@Nonnull final List<UpdateAction<U>> updateActions,
                                                           @Nonnull final V newResourceDraft,
                                                           @Nonnull final U oldResource) {
        return ofNullable(beforeUpdateCallback)
            .map(callBack -> emptyIfNull(callBack.apply(updateActions, newResourceDraft, oldResource)))
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
     *         function on. If the {@code beforeCreateCallback} function was null, the supplied resource draft is
     *         returned as is, wrapped in an optional.
     */
    @Nonnull
    public Optional<V> applyBeforeCreateCallBack(@Nonnull final V newResourceDraft) {
        return ofNullable(
                beforeCreateCallback != null ? beforeCreateCallback.apply(newResourceDraft) : newResourceDraft);
    }
}
