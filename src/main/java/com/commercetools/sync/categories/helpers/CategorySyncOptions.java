package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CategorySyncOptions extends BaseSyncOptions {
    // Filter function which can be applied on generated list of update actions
    private Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter;

    // Services
    private CategoryService categoryService;

    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret);
    }

    public CategorySyncOptions(@Nonnull final SphereClientConfig sphereClientConfig) {
        super(sphereClientConfig);
    }

    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret,
                               @Nonnull final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret);
        this.updateActionsFilter = updateActionsFilter;
    }

    public CategorySyncOptions(@Nonnull final SphereClientConfig sphereClientConfig,
                               @Nonnull final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter) {
        super(sphereClientConfig);
        this.updateActionsFilter = updateActionsFilter;
    }

    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret,
                               @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                               @Nonnull final Consumer<String> updateActionWarningCallBack) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret, updateActionErrorCallBack, updateActionWarningCallBack);
    }

    public CategorySyncOptions(@Nonnull final SphereClientConfig sphereClientConfig,
                               @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                               @Nonnull final Consumer<String> updateActionWarningCallBack) {
        super(sphereClientConfig, updateActionErrorCallBack, updateActionWarningCallBack);
    }

    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret,
                               @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                               @Nonnull final Consumer<String> updateActionWarningCallBack,
                               @Nonnull final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret, updateActionErrorCallBack, updateActionWarningCallBack);
        this.updateActionsFilter = updateActionsFilter;
    }

    public CategorySyncOptions(@Nonnull final SphereClientConfig sphereClientConfig,
                               @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                               @Nonnull final Consumer<String> updateActionWarningCallBack,
                               @Nonnull final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter) {
        super(sphereClientConfig, updateActionErrorCallBack, updateActionWarningCallBack);
        this.updateActionsFilter = updateActionsFilter;
    }

    public Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> getUpdateActionsFilter() {
        return updateActionsFilter;
    }

    public CategoryService getCategoryService() {
        if (categoryService == null) {
            categoryService = new CategoryServiceImpl(getCtpClient());
        }
        return categoryService;
    }
}
