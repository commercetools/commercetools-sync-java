package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

public class CartDiscountSync extends BaseSync<CartDiscountDraft, CartDiscountSyncStatistics, CartDiscountSyncOptions> {


    public CartDiscountSync(@Nonnull final CartDiscountSyncOptions cartDiscountSyncOptions){
        super(null, null);

    }

    CartDiscountSync(@Nonnull final CartDiscountSyncStatistics statistics,
                     @Nonnull final CartDiscountSyncOptions syncOptions) {
        super(statistics, syncOptions);
    }

    /**
     * Iterates through the whole {@code cartDiscountDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link CartDiscountSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param cartDiscountDrafts {@link List} of {@link CartDiscountDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link CartDiscountSyncStatistics} holding statistics of all sync
     *         processes performed by this sync instance.
     */
    @Override
    protected CompletionStage<CartDiscountSyncStatistics> process(
        @Nonnull final List<CartDiscountDraft> cartDiscountDrafts) {

        final List<List<CartDiscountDraft>> batches = batchElements(cartDiscountDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, CompletableFuture.completedFuture(statistics));
    }

    @Override
    protected CompletionStage<CartDiscountSyncStatistics> processBatch(
        @Nonnull final List<CartDiscountDraft> batch) {
        return null;
    }


}
