package com.commercetools.sync.commons;

import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// TODO: IMPLEMENTATION AFTER CATEGORY SYNC GITHUB ISSUE#5
// TODO: JAVADOC
// TODO: TESTING
public class BaseOptions {
    // optional (for sync methods only) client options like CTP credentials,
    // retry count on error, CTP API host, timeout, withHeaders, stripSensitiveData
    private SphereClientConfig clientConfig;
    private BlockingSphereClient CTPclient;

    // Services
    private TypeService typeService;

    // Custom Callbacks
    private BiConsumer<String, Throwable> updateActionErrorCallBack;
    private Consumer<String> updateActionWarningCallBack;

    // for just adding more localizations and not deleting existing ones.
    private boolean removeOtherLocales = true;
    // for just accumulating entries in Sets instead of fully syncing.
    private boolean removeOtherSetEntries = true;
    // for just accumulating entries in sub-resources / collections like Assets, Images etc.
    private boolean removeOtherCollectionEntries = true;
    // for just adding object properties, custom fields or product attributes instead of deleting
    private boolean removeOtherProperties = true;
    // to control whether to compare to the staged data or the published ones.
    private boolean compareStaged = true;
    // to control whether to auto-publish or not
    private boolean publish = false;
    // defines which attributes
    private List<String> whiteList;
    private List<String> blackList;

    public BaseOptions(@Nonnull final String ctpProjectKey,
                       @Nonnull final String ctpClientId,
                       @Nonnull final String ctpClientSecret,
                       @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                       @Nonnull final Consumer<String> updateActionWarningCallBack) {
        this.clientConfig = SphereClientConfig.of(ctpProjectKey,
                ctpClientId, ctpClientSecret);
        CTPclient = CTPUtils.createClient(clientConfig);
        this.updateActionErrorCallBack = updateActionErrorCallBack;
        this.updateActionWarningCallBack = updateActionWarningCallBack;
    }

    public void callUpdateActionErrorCallBack(@Nonnull final String errorMessage,
                                              @Nonnull final Throwable exception) {
        if (getUpdateActionErrorCallBack() != null) {
            getUpdateActionErrorCallBack().accept(errorMessage, exception);
        }
    }

    public void callUpdateActionErrorCallBack(@Nonnull final String errorMessage) {
        if (getUpdateActionErrorCallBack() != null) {
            getUpdateActionErrorCallBack().accept(errorMessage, null);
        }
    }

    public void callUpdateActionWarningCallBack(@Nonnull final String warningMessage) {
        if (getUpdateActionWarningCallBack() != null) {
            getUpdateActionWarningCallBack().accept(warningMessage);
        }
    }

    public BlockingSphereClient getCTPclient() {
        return CTPclient;
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
            typeService = new TypeServiceImpl(CTPclient);
        }
        return typeService;
    }
}
