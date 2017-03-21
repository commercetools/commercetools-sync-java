package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class CategorySyncOptions extends BaseOptions {

    public CategorySyncOptions(@Nonnull final SphereClientConfig clientConfig) {
        super(clientConfig);
    }

    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret);
    }

    // optional filter which can be applied on generated list of update actions
    private Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> filterActions(){
        return updateActions -> null;
    }
}
