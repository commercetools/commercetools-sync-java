package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductSyncUtils;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.commercetools.sync.commons.utils.SyncUtils.batchDrafts;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {

    private final ProductService service;

    public ProductSync(final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, ProductService.of(productSyncOptions.getCtpClient()));
    }

    ProductSync(final ProductSyncOptions productSyncOptions, final ProductService productService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.service = productService;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        for (ProductDraft productDraft : resourceDrafts) {
            try {
                CompletionStage<Optional<Product>> fetch = service.fetch(productDraft.getKey());
                fetch.thenCompose(productOptional -> productOptional
                    .map(product -> syncProduct(product, productDraft))
                    .orElseGet(() -> createProduct(productDraft))
                ).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }
            statistics.incrementProcessed();
        }
        return completedFuture(statistics);
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> syncBatches(@Nonnull List<List<ProductDraft>> batches,
                                                                 @Nonnull CompletionStage<ProductSyncStatistics> result) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<ProductDraft> firstBatch = batches.remove(0);
        return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> processBatch(@Nonnull List<ProductDraft> batch) {
        final List<List<ProductDraft>> batches = batchDrafts(batch, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    private CompletionStage<Void> createProduct(final ProductDraft productDraft) {
        return publishIfNeeded(service.create(productDraft))
            .thenRun(statistics::incrementCreated);
    }

    private CompletionStage<Void> syncProduct(final Product oldProduct, final ProductDraft newProduct) {
        return revertIfNeeded(oldProduct).thenCompose(preparedProduct -> {
            List<UpdateAction<Product>> updateActions =
                ProductSyncUtils.buildActions(oldProduct, newProduct, syncOptions);
            if (!updateActions.isEmpty()) {
                return publishIfNeeded(service.update(oldProduct, updateActions))
                    .thenRun(statistics::incrementUpdated);
            }
            return publishIfNeeded(completedFuture(oldProduct)).thenAccept(published -> {
                if (published) {
                    statistics.incrementUpdated();
                }
            });
        });
    }

    private CompletionStage<Product> revertIfNeeded(final Product product) {
        CompletionStage<Product> productStage = completedFuture(product);
        if (syncOptions.shouldRevertStagedChanges()) {
            if (product.getMasterData().hasStagedChanges()) {
                productStage = service.revert(product);
            }
        }
        return productStage;
    }

    private CompletionStage<Boolean> publishIfNeeded(final CompletionStage<Product> productStage) {
        if (syncOptions.shouldPublish()) {
            return productStage.thenCompose(product -> {
                final ProductCatalogData data = product.getMasterData();
                if (!data.isPublished() || data.hasStagedChanges()) {
                    return service.publish(product)
                                  .thenApply(publishedProduct -> true);
                }
                return productStage.thenApply(publishedProduct -> false);
            });
        }
        return productStage.thenApply(publishedProduct -> false);
    }

}
