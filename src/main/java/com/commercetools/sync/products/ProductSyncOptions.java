package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProductSyncOptions extends BaseOptions {
    // to control whether to remove other product variants or not:
    private boolean removeOtherVariants = true;

    public ProductSyncOptions(@Nonnull final String ctpProjectKey,
                              @Nonnull final String ctpClientId,
                              @Nonnull final String ctpClientSecret,
                              @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                              @Nonnull final Consumer<String> updateActionWarningCallBack) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret, updateActionErrorCallBack, updateActionWarningCallBack);
    }

    // optional filter which can be applied on generated list of update actions
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> filterActions(){
        return updateActions -> null;
    }

}
