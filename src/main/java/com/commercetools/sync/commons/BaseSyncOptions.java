package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.CtpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BaseSyncOptions {
    private CtpClient ctpClient;
    private BiConsumer<String, Throwable> errorCallBack;
    private Consumer<String> warningCallBack;
    private boolean removeOtherLocales = true;
    private boolean removeOtherSetEntries = true;
    private boolean removeOtherCollectionEntries = true;
    private boolean removeOtherProperties = true;

    protected BaseSyncOptions(@Nonnull final CtpClient ctpClient,
                              final BiConsumer<String, Throwable> errorCallBack,
                              final Consumer<String> warningCallBack,
                              final boolean removeOtherLocales,
                              final boolean removeOtherSetEntries,
                              final boolean removeOtherCollectionEntries,
                              final boolean removeOtherProperties) {
        this.ctpClient = ctpClient;
        this.errorCallBack = errorCallBack;
        this.warningCallBack = warningCallBack;
        this.removeOtherLocales = removeOtherLocales;
        this.removeOtherSetEntries = removeOtherSetEntries;
        this.removeOtherCollectionEntries = removeOtherCollectionEntries;
        this.removeOtherProperties = removeOtherProperties;
    }

    /**
     * Returns the {@link CtpClient} instance set to {@code this} {@link BaseSyncOptions} that contains instance of the
     * {@link io.sphere.sdk.client.SphereClientConfig} and {@link io.sphere.sdk.client.BlockingSphereClient}.
     *
     * @return the {@link CtpClient} instance set to {@code this} {@link BaseSyncOptions} that contains instance of the
     * {@link io.sphere.sdk.client.SphereClientConfig} and {@link io.sphere.sdk.client.BlockingSphereClient}.
     */
    public CtpClient getCtpClient() {
        return ctpClient;
    }

    /**
     * Returns the {@code errorCallBack} {@link BiConsumer<String, Throwable>} function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs during the sync
     * process that represents an error.
     *
     * @return the {@code errorCallBack} {@link BiConsumer<String, Throwable>} function set to {@code this}
     * {@link BaseSyncOptions}
     */
    public BiConsumer<String, Throwable> getErrorCallBack() {
        return errorCallBack;
    }

    /**
     * Returns the {@code warningCallBack} {@link Consumer<String>} function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs
     * during the sync process that represents a warning.
     *
     * @return the {@code warningCallBack} {@link Consumer<String>} function set to {@code this}
     * {@link BaseSyncOptions}
     */
    public Consumer<String> getWarningCallBack() {
        return warningCallBack;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add additional localizations without deleting
     * existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing object properties.
     *
     * @return a {@code boolean} flag which enables the sync module to add additional localizations without deleting
     * existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing object properties.
     */
    public boolean shouldRemoveOtherLocales() {
        return removeOtherLocales;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add additional Set entries without deleting
     * existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing Set entries.
     * <p>
     * Returns a {@code boolean} flag which enables the sync module to add additional Set entries without deleting
     * existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing Set entries.
     */
    public boolean shouldRemoveOtherSetEntries() {
        return removeOtherSetEntries;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add collection (e.g. Assets, Images etc.) entries
     * without deleting existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing collection entries.
     *
     * @return a {@code boolean} flag which enables the sync module to add collection (e.g. Assets, Images etc.) entries
     * without deleting existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing collection entries.
     */
    public boolean shouldRemoveOtherCollectionEntries() {
        return removeOtherCollectionEntries;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add additional object properties (e.g. custom fields,
     * product attributes, etc..) without deleting existing ones, if set to {@code false}. If set to true, which is the
     * default value of the option, it deletes the existing object properties.
     *
     * @return a {@code boolean} flag which enables the sync module to add additional object properties (e.g. custom fields,
     * product attributes, etc..) without deleting existing ones, if set to {@code false}. If set to true, which is the
     * default value of the option, it deletes the existing object properties.
     */
    public boolean shouldRemoveOtherProperties() {
        return removeOtherProperties;
    }

    /**
     * Given a {@code warningMessage} string, this method calls the {@code warningCallBack} function which is set
     * to {@code this} instance of the {@link BaseSyncOptions}. If there {@code warningCallBack} is null, this
     * method does nothing.
     *
     * @param warningMessage the warning message to supply to the {@code warningCallBack} function.
     */
    public void applyWarningCallback(@Nonnull final String warningMessage) {
        if (this.warningCallBack != null) {
            this.warningCallBack.accept(warningMessage);
        }
    }

    /**
     * Given an {@code errorMessage} and an {@code exception}, this method calls the {@code errorCallBack} function
     * which is set to {@code this} instance of the {@link BaseSyncOptions}. If there {@code errorCallBack} is null, this
     * method does nothing.
     *
     * @param errorMessage the error message to supply as first param to the {@code errorCallBack} function.
     * @param exception    the {@link Throwable} instance to supply to the {@code errorCallBack} function as a
     *                     second param.
     */
    public void applyErrorCallback(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        if (this.errorCallBack != null) {
            this.errorCallBack.accept(errorMessage, exception);
        }
    }
}
