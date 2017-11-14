package com.commercetools.sync.commons;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class BaseSyncOptionsBuilder<T extends BaseSyncOptionsBuilder<T, S, U, V>,
    S extends BaseSyncOptions, U, V> {

    protected SphereClient ctpClient;
    protected BiConsumer<String, Throwable> errorCallback;
    protected Consumer<String> warningCallback;
    protected int batchSize = 30;
    protected boolean removeOtherLocales = true;
    protected boolean removeOtherSetEntries = true;
    protected boolean removeOtherCollectionEntries = true;
    protected boolean removeOtherProperties = true;
    protected boolean allowUuid = false;
    protected TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>> beforeUpdateCallback;

    /**
     * Sets the {@code errorCallback} function of the sync module. This callback will be called whenever an event occurs
     * that leads to an error alert from the sync process.
     *
     * @param errorCallback the new value to set to the error callback.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T errorCallback(@Nonnull final BiConsumer<String, Throwable> errorCallback) {
        this.errorCallback = errorCallback;
        return getThis();
    }

    /**
     * Sets the {@code warningCallback} function of the sync module. This callback will be called whenever an event
     * occurs that leads to a warning alert from the sync process.
     *
     * @param warningCallback the new value to set to the warning callback.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T warningCallback(@Nonnull final Consumer<String> warningCallback) {
        this.warningCallback = warningCallback;
        return getThis();
    }

    /**
     * Set option that indicates batch size for sync process. During the sync there is a need for fetching existing
     * resources so that they can be compared with the new resource drafts. That's why the input is sliced into
     * batches and then processed. It allows to reduce the query size for fetching all resources processed in one
     * batch.
     * E.g. value of 30 means that 30 entries from input list would be accumulated and one API call will be performed
     * for fetching entries responding to them. Then comparision and sync are performed.
     *
     * <p>This batch size is set to 30 by default.
     *
     * @param batchSize int that indicates batch size of resources to process. Has to be positive
     *                  or else will be ignored and default value of 30 would be used.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T batchSize(final int batchSize) {
        if (batchSize > 0) {
            this.batchSize = batchSize;
        }
        return getThis();
    }

    /**
     * Sets the {@code removeOtherLocales} boolean flag which adds additional localizations without deleting
     * existing ones. If set to true, which is the default value of the option, it deletes the
     * existing localizations. If set to false, it doesn't delete the existing ones.
     *
     * @param removeOtherLocales new value to set to the boolean flag.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T removeOtherLocales(final boolean removeOtherLocales) {
        this.removeOtherLocales = removeOtherLocales;
        return getThis();
    }

    /**
     * Sets the {@code removeOtherSetEntries} boolean flag which adds additional Set entries without deleting
     * existing ones. If set to true, which is the default value of the option, it deletes the
     * existing Set entries. If set to false, it doesn't delete the existing ones.
     *
     * @param removeOtherSetEntries new value to set to the boolean flag.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T removeOtherSetEntries(final boolean removeOtherSetEntries) {
        this.removeOtherSetEntries = removeOtherSetEntries;
        return getThis();
    }

    /**
     * Sets the {@code removeOtherSetEntries} boolean flag which adds collection (e.g. Assets, Images etc.) entries
     * without deleting existing ones. If set to true, which is the default value of the option, it deletes the
     * existing collection entries. If set to false, it doesn't delete the existing ones.
     *
     * @param removeOtherCollectionEntries new value to set to the boolean flag.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T removeOtherCollectionEntries(final boolean removeOtherCollectionEntries) {
        this.removeOtherCollectionEntries = removeOtherCollectionEntries;
        return getThis();
    }

    /**
     * Sets the {@code removeOtherProperties} boolean flag which adds additional object properties (e.g. custom fields,
     * product attributes, etc..) without deleting existing ones. If set to true, which is the default value of the
     * option, it deletes the existing object properties. If set to false, it doesn't delete the existing ones.
     *
     * @param removeOtherProperties new value to set to the boolean flag.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T removeOtherProperties(final boolean removeOtherProperties) {
        this.removeOtherProperties = removeOtherProperties;
        return getThis();
    }

    /**
     * The sync expects the user to pass the keys to references in the {@code id} field of References. If the key values
     * are in UUID format, then this flag must be set to true, otherwise the sync will fail to resolve the reference.
     * This flag, if set to true, enables the user to use keys with UUID format. By default, it is set to {@code false}.
     *
     * @param allowUuid new value to set to the boolean flag.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T allowUuidKeys(final boolean allowUuid) {
        this.allowUuid = allowUuid;
        return getThis();
    }

    /**
     * Sets the beforeUpdateCallback {@link TriFunction} which can be applied on the supplied list of update actions
     * generated from comparing an old resource of type {@code U} (e.g. {@link io.sphere.sdk.products.Product}) to a new
     * draft of type {@code V} (e.g. {@link io.sphere.sdk.products.ProductDraft}). It results in a resultant list after
     * the specified {@link TriFunction} {@code beforeUpdateCallback} function has been applied.
     *
     * @param beforeUpdateCallback function which can be applied on generated list of update actions.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T beforeUpdateCallback(@Nonnull final TriFunction<List<UpdateAction<U>>, V, U, List<UpdateAction<U>>>
                                      beforeUpdateCallback) {
        this.beforeUpdateCallback = beforeUpdateCallback;
        return getThis();
    }

    /**
     * Creates new instance of {@code S} which extends {@link BaseSyncOptions} enriched with all attributes provided to
     * {@code this} builder.
     *
     * @return new instance of S which extends {@link BaseSyncOptions}
     */
    protected abstract S build();

    /**
     * Returns {@code this} instance of {@code T}, which extends {@link BaseSyncOptionsBuilder}. The purpose of this
     * method is to make sure that {@code this} is an instance of a class which extends {@link BaseSyncOptionsBuilder}
     * in order to be used in the generic methods of the class. Otherwise, without this method, the methods above would
     * need to cast {@code this to T} which could lead to a runtime error of the class was extended in a wrong way.
     *
     * @return an instance of the class that overrides this method.
     */
    protected abstract T getThis();
}
