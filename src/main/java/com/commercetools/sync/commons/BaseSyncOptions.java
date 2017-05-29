package com.commercetools.sync.commons;

import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BaseSyncOptions {
    private final SphereClient ctpClient;
    private final BiConsumer<String, Throwable> errorCallBack;
    private final Consumer<String> warningCallBack;
    private boolean removeOtherLocales = true;
    private boolean removeOtherSetEntries = true;
    private boolean removeOtherCollectionEntries = true;
    private boolean removeOtherProperties = true;
    private boolean allowUuid = false;

    protected BaseSyncOptions(@Nonnull final SphereClient ctpClient,
                              final BiConsumer<String, Throwable> errorCallBack,
                              final Consumer<String> warningCallBack,
                              final boolean removeOtherLocales,
                              final boolean removeOtherSetEntries,
                              final boolean removeOtherCollectionEntries,
                              final boolean removeOtherProperties,
                              final boolean allowUuid) {
        this.ctpClient = ctpClient;
        this.errorCallBack = errorCallBack;
        this.warningCallBack = warningCallBack;
        this.removeOtherLocales = removeOtherLocales;
        this.removeOtherSetEntries = removeOtherSetEntries;
        this.removeOtherCollectionEntries = removeOtherCollectionEntries;
        this.removeOtherProperties = removeOtherProperties;
        this.allowUuid = allowUuid;
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
     * Returns the {@code errorCallBack} {@link BiConsumer&lt;String, Throwable&gt;} function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs during the sync
     * process that represents an error.
     *
     * @return the {@code errorCallBack} {@link BiConsumer&lt;String, Throwable&gt;} function set to {@code this}
     *      {@link BaseSyncOptions}
     */
    public BiConsumer<String, Throwable> getErrorCallBack() {
        return errorCallBack;
    }

    /**
     * Returns the {@code warningCallBack} {@link Consumer&lt;String&gt;} function set to {@code this}
     * {@link BaseSyncOptions}. It represents the callback that is called whenever an event occurs
     * during the sync process that represents a warning.
     *
     * @return the {@code warningCallBack} {@link Consumer&lt;String&gt;} function set to {@code this}
     *      {@link BaseSyncOptions}
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
     *      existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     *      it deletes the existing object properties.
     */
    public boolean shouldRemoveOtherLocales() {
        return removeOtherLocales;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add additional Set entries without deleting
     * existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     * it deletes the existing Set entries.
     *
     * @return a {@code boolean} flag which enables the sync module to add additional Set entries without deleting
     *      existing ones, if set to {@code false}. If set to true, which is the default value of the option,
     *      it deletes the existing Set entries.
     */
    public boolean shouldRemoveOtherSetEntries() {
        return removeOtherSetEntries;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add collection (e.g. Assets, Images etc.) entries
     * without deleting existing ones, if set to {@code false}. If set to true, which is the default value of the
     * option, it deletes the existing collection entries.
     *
     * @return a {@code boolean} flag which enables the sync module to add collection (e.g. Assets, Images etc.) entries
     *      without deleting existing ones, if set to {@code false}. If set to true, which is the default value of the
     *      option, it deletes the existing collection entries.
     */
    public boolean shouldRemoveOtherCollectionEntries() {
        return removeOtherCollectionEntries;
    }

    /**
     * Returns a {@code boolean} flag which enables the sync module to add additional object properties (e.g. custom
     * fields, product attributes, etc..) without deleting existing ones, if set to {@code false}. If set to true,
     * which is the default value of the option, it deletes the existing object properties.
     *
     * @return a {@code boolean} flag which enables the sync module to add additional object properties (e.g. custom
     *      fields, product attributes, etc..) without deleting existing ones, if set to {@code false}. If set to true,
     *      which is the default value of the option, it deletes the existing object properties.
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
     * which is set to {@code this} instance of the {@link BaseSyncOptions}. If there {@code errorCallBack} is null,
     * this method does nothing.
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

    /**
     * The sync expects the user to pass the keys to references in the {@code id} field of References. If the key values
     * are in UUID format, then this flag must be set to true, otherwise the sync will fail to resolve the reference.
     * This flag, if set to true, enables the user to use keys with UUID format. By default, it is set to {@code false}.
     *
     * @return a {@code boolean} flag, if set to true, enables the user to use keys with UUID format for references.
     * By default, it is set to {@code false}.
     */
    public boolean isUuidAllowed() {
        return allowUuid;
    }
}
