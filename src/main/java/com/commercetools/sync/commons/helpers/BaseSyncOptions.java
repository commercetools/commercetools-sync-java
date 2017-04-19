package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.CTPUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BaseSyncOptions {
    // CTP Client configuration
    private SphereClientConfig clientConfig;
    private long timeout = 30;
    private TimeUnit timeoutTimeUnit = TimeUnit.SECONDS;
    private BlockingSphereClient ctpClient;

    // Custom Callbacks
    private BiConsumer<String, Throwable> updateActionErrorCallBack;
    private Consumer<String> updateActionWarningCallBack;

    // Services
    private TypeService typeService;

    // Additional Options, still should be implemented GITHUB ISSUE#5

    // Add additional localizations without deleting existing ones.
    private boolean removeOtherLocales = true;

    // Add additional Set entries without deleting existing ones.
    private boolean removeOtherSetEntries = true;

    //  Add additional collection (e.g. Assets, Images etc.) entries without deleting existing ones.
    private boolean removeOtherCollectionEntries = true;

    // Add additional object properties (e.g. custom fields, product attributes, etc..) without deleting existing ones.
    private boolean removeOtherProperties = true;


    public BaseSyncOptions(@Nonnull final String ctpProjectKey,
                           @Nonnull final String ctpClientId,
                           @Nonnull final String ctpClientSecret) {
        this.clientConfig = SphereClientConfig.of(ctpProjectKey,
                ctpClientId, ctpClientSecret);
        ctpClient = CTPUtils.createClient(clientConfig, timeout, timeoutTimeUnit);
    }

    public BaseSyncOptions(@Nonnull final SphereClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        ctpClient = CTPUtils.createClient(clientConfig, timeout, timeoutTimeUnit);
    }

    public BaseSyncOptions(@Nonnull final String ctpProjectKey,
                           @Nonnull final String ctpClientId,
                           @Nonnull final String ctpClientSecret,
                           @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                           @Nonnull final Consumer<String> updateActionWarningCallBack) {
        this.clientConfig = SphereClientConfig.of(ctpProjectKey,
                ctpClientId, ctpClientSecret);
        ctpClient = CTPUtils.createClient(clientConfig, timeout, timeoutTimeUnit);
        this.updateActionErrorCallBack = updateActionErrorCallBack;
        this.updateActionWarningCallBack = updateActionWarningCallBack;
    }

    public BaseSyncOptions(@Nonnull final SphereClientConfig clientConfig,
                           @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                           @Nonnull final Consumer<String> updateActionWarningCallBack) {
        this.clientConfig = clientConfig;
        ctpClient = CTPUtils.createClient(clientConfig, timeout, timeoutTimeUnit);
        this.updateActionErrorCallBack = updateActionErrorCallBack;
        this.updateActionWarningCallBack = updateActionWarningCallBack;
    }

    public void callUpdateActionErrorCallBack(@Nonnull final String errorMessage,
                                              @Nullable final Throwable exception) {
        if (getUpdateActionErrorCallBack() != null) {
            getUpdateActionErrorCallBack().accept(errorMessage, exception);
        }
    }

    public void callUpdateActionWarningCallBack(@Nonnull final String warningMessage) {
        if (getUpdateActionWarningCallBack() != null) {
            getUpdateActionWarningCallBack().accept(warningMessage);
        }
    }

    public BlockingSphereClient getCtpClient() {
        return ctpClient;
    }

    public SphereClientConfig getClientConfig() {
        return clientConfig;
    }

    public BiConsumer<String, Throwable> getUpdateActionErrorCallBack() {
        return updateActionErrorCallBack;
    }

    public Consumer<String> getUpdateActionWarningCallBack() {
        return updateActionWarningCallBack;
    }

    public TypeService getTypeService() {
        if (typeService == null) {
            typeService = new TypeServiceImpl(ctpClient);
        }
        return typeService;
    }

    public boolean shouldRemoveOtherLocales() {
        return removeOtherLocales;
    }

    public boolean shouldRemoveOtherSetEntries() {
        return removeOtherSetEntries;
    }

    public boolean shouldRemoveOtherCollectionEntries() {
        return removeOtherCollectionEntries;
    }

    public boolean shouldRemoveOtherProperties() {
        return removeOtherProperties;
    }
}
