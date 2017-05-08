package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.CtpClient;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class BaseSyncOptionsBuilder<T extends BaseSyncOptionsBuilder<T, S>, S extends BaseSyncOptions> {
    protected CtpClient ctpClient;
    protected BiConsumer<String, Throwable> errorCallBack;
    protected Consumer<String> warningCallBack;
    protected boolean removeOtherLocales = true;
    protected boolean removeOtherSetEntries = true;
    protected boolean removeOtherCollectionEntries = true;
    protected boolean removeOtherProperties = true;

    /**
     * Sets the {@code errorCallBack} function of the sync module. This callback will be called whenever an event occurs
     * that leads to an error alert from the sync process.
     *
     * @param errorCallBack the new value to set to the error callback.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T setErrorCallBack(@Nonnull final BiConsumer<String, Throwable> errorCallBack) {
        this.errorCallBack = errorCallBack;
        return getThis();
    }

    /**
     * Sets the {@code warningCallBack} function of the sync module. This callback will be called whenever an event
     * occurs that leads to a warning alert from the sync process.
     *
     * @param warningCallBack the new value to set to the warning callback.
     * @return {@code this} instance of {@link BaseSyncOptionsBuilder}
     */
    public T setWarningCallBack(@Nonnull final Consumer<String> warningCallBack) {
        this.warningCallBack = warningCallBack;
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
    public T setRemoveOtherLocales(final boolean removeOtherLocales) {
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
    public T setRemoveOtherSetEntries(final boolean removeOtherSetEntries) {
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
    public T setRemoveOtherCollectionEntries(final boolean removeOtherCollectionEntries) {
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
    public T setRemoveOtherProperties(final boolean removeOtherProperties) {
        this.removeOtherProperties = removeOtherProperties;
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
