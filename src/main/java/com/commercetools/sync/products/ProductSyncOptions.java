package com.commercetools.sync.products;

import com.commercetools.sync.commons.helpers.BaseSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProductSyncOptions extends BaseSyncOptions {
    // to control whether to compare to the staged data or the published ones.
    private boolean compareStaged = true;

    // to control whether to auto-publish or not
    private boolean publish = false;

    // defines which attributes
    private List<String> whiteList;
    private List<String> blackList;
    // to control whether to remove other product variants or not:
    private boolean removeOtherVariants = true;

    // optional filter which can be applied on generated list of update actions
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> filterActions;

    public ProductSyncOptions(@Nonnull final String ctpProjectKey,
                              @Nonnull final String ctpClientId,
                              @Nonnull final String ctpClientSecret,
                              @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                              @Nonnull final Consumer<String> updateActionWarningCallBack) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret, updateActionErrorCallBack, updateActionWarningCallBack);
    }

}
