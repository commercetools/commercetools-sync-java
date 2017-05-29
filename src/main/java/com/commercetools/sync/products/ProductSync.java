package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {

    private final ProductService productService;
    private final UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilder;

    public ProductSync(final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, ProductService.of(productSyncOptions.getCtpClient()),
                ProductUpdateActionsBuilder.of());
    }

    ProductSync(final ProductSyncOptions productSyncOptions, final ProductService productService,
                final UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilder) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.productService = productService;
        this.updateActionsBuilder = updateActionsBuilder;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        for (ProductDraft productDraft : resourceDrafts) {
            try {
                CompletionStage<Optional<Product>> fetchStage = productService.fetch(productDraft.getKey());
                fetchStage.thenCompose(productOptional ->
                        productOptional
                                .map(product -> syncProduct(product, productDraft))
                                .orElseGet(() -> productService.create(productDraft)
                                        .thenRun(statistics::incrementCreated)))
                        .toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }
            statistics.incrementProcessed();
        }
        return CompletableFuture.completedFuture(statistics);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> syncProduct(final Product product, final ProductDraft productDraft) {
        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(product, productDraft);
        if (!updateActions.isEmpty()) {
            return productService.update(product, updateActions)
                    .thenRun(statistics::incrementUpdated);
        }
        return CompletableFuture.completedFuture(null);
    }

}
