package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class BaseBatchValidator<D, O extends BaseSyncOptions, S extends BaseSyncStatistics> {

    private final O syncOptions;
    private final S syncStatistics;

    public BaseBatchValidator(@Nonnull final O syncOptions,
                              @Nonnull final S syncStatistics) {
        this.syncOptions = syncOptions;
        this.syncStatistics = syncStatistics;
    }

    /**
     * d
     * @param drafts d
     * @return d
     */
    public abstract ImmutablePair<Set<D>, ?>  validateAndCollectReferencedKeys(@Nonnull final List<D> drafts);

    protected  <T> void collectReferencedKeyFromResourceIdentifier(
        @Nullable final ResourceIdentifier<T> resourceIdentifier,
        @Nonnull final Consumer<String> keyConsumer) {

        if (resourceIdentifier != null && !isBlank(resourceIdentifier.getKey())) {
            keyConsumer.accept(resourceIdentifier.getKey());
        }
    }

    protected void collectReferencedKeyFromCustomFieldsDraft(
        @Nullable final CustomFieldsDraft customFieldsDraft,
        @Nonnull final Consumer<String> keyConsumer) {

        if (customFieldsDraft != null) {
            collectReferencedKeyFromResourceIdentifier(customFieldsDraft.getType(), keyConsumer);
        }
    }

    protected void collectReferencedKeysFromAssetDrafts(
        @Nullable final List<AssetDraft> assetDrafts,
        @Nonnull final Consumer<String> keyConsumer) {

        if (assetDrafts != null) {
            assetDrafts.forEach(assetDraft ->
                collectReferencedKeyFromCustomFieldsDraft(assetDraft.getCustom(), keyConsumer));
        }
    }

    protected <T> void collectReferencedKeysKeyFromReference(
        @Nullable final Reference<T> reference,
        @Nonnull final Consumer<String> keyInReferenceSupplier) {

        if (reference != null && !isBlank(reference.getId())) {
            keyInReferenceSupplier.accept(reference.getKey());
        }
    }

    protected void handleError(@Nonnull final SyncException syncException) {
        this.syncOptions.applyErrorCallback(syncException);
        this.syncStatistics.incrementFailed();
    }

    protected void handleError(@Nonnull final String errorMessage) {
        handleError(new SyncException(errorMessage));
    }
}
