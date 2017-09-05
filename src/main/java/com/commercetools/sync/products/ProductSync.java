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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static com.commercetools.sync.commons.utils.SyncUtils.batchDrafts;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {

    private final ProductService productService;

    public ProductSync(@Nonnull final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, ProductService.of(productSyncOptions.getCtpClient()));
    }

    ProductSync(@Nonnull final ProductSyncOptions productSyncOptions, @Nonnull final ProductService productService) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        final List<List<ProductDraft>> batches = batchDrafts(resourceDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> syncBatches(@Nonnull final List<List<ProductDraft>> batches,
                                                                 @Nonnull final CompletionStage<ProductSyncStatistics>
                                                                     result) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<ProductDraft> firstBatch = batches.remove(0);
        return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> processBatch(@Nonnull final List<ProductDraft> batch) {
        for (ProductDraft productDraft : batch) {
            try {
                productService.fetch(productDraft.getKey())
                              .thenCompose(productOptional -> productOptional
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

    @Nonnull
    private CompletionStage<Void> syncProduct(@Nonnull final Product oldProduct,
                                              @Nonnull final ProductDraft newProduct) {
        return revertIfNeeded(oldProduct).thenCompose(preparedProduct -> {
            List<UpdateAction<Product>> updateActions =
                ProductSyncUtils.buildActions(oldProduct, newProduct, syncOptions);
            if (!updateActions.isEmpty()) {
                return productService.update(oldProduct, updateActions)
                                     .thenCompose(this::publishIfNeeded)
                                     .thenRun(statistics::incrementUpdated);
            }
            return publishIfNeeded(oldProduct)
                .thenAccept(published -> {
                    if (published) {
                        statistics.incrementUpdated();
                    }
                });
        });
    }

    @Nonnull
    private CompletionStage<Product> revertIfNeeded(@Nonnull final Product product) {
        if (syncOptions.shouldRevertStagedChanges()) {
            if (product.getMasterData().hasStagedChanges()) {
                return productService.revert(product);
            }
        }
        return CompletableFuture.completedFuture(product);
    }

    @Nonnull
    private CompletionStage<Boolean> publishIfNeeded(@Nonnull final Product product) {
        if (syncOptions.shouldPublish()) {
            final ProductCatalogData data = product.getMasterData();
            if (!data.isPublished() || data.hasStagedChanges()) {
                return productService.publish(product)
                                     .thenApply(publishedProduct -> true);
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    @Nonnull
    private CompletionStage<Void> createProduct(final ProductDraft productDraft) {
        return productService.create(productDraft)
                             .thenCompose(this::publishIfNeeded)
                             .thenRun(statistics::incrementCreated);
    }
}
