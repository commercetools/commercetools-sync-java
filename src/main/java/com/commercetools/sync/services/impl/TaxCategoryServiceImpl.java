package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.TaxCategoryService;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TaxCategoryServiceImpl implements TaxCategoryService {
    private final ProductSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public TaxCategoryServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.isEmpty()) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<TaxCategory>> taxCategoryPageConsumer = taxCategoryPage ->
            taxCategoryPage.forEach(taxCategory -> {
                final String fetchedTaxCategoryKey = taxCategory.getKey();
                final String id = taxCategory.getId();
                if (StringUtils.isNotBlank(fetchedTaxCategoryKey)) {
                    keyToIdCache.put(fetchedTaxCategoryKey, id);
                } else {
                    syncOptions.applyWarningCallback(format("TaxCategory with id: '%s' has no key set. Keys are"
                        + " required for taxCategory matching.", id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), TaxCategoryQuery.of(), taxCategoryPageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }
}
