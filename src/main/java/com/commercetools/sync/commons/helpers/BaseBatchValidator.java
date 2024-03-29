package com.commercetools.sync.commons.helpers;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public abstract class BaseBatchValidator<
    ResourceDraftT,
    SyncOptionsT extends BaseSyncOptions,
    SyncStatisticsT extends BaseSyncStatistics> {
  private final SyncOptionsT syncOptions;
  private final SyncStatisticsT syncStatistics;

  public BaseBatchValidator(
      @Nonnull final SyncOptionsT syncOptions, @Nonnull final SyncStatisticsT syncStatistics) {
    this.syncOptions = syncOptions;
    this.syncStatistics = syncStatistics;
  }

  /**
   * Given the {@link List}&lt;{@code D}&gt; (e.g.{@link CustomerDraft}) of drafts this method
   * attempts to validate drafts and collect referenced keys from the draft and return an {@link
   * ImmutablePair}&lt;{@link Set}&lt;{@code D}&gt;, ?&gt; which contains the {@link Set} of valid
   * drafts and referenced keys.
   *
   * @param drafts the drafts to validate.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@code D}&gt;, ?&gt; which contains the {@link
   *     Set} of valid drafts and referenced keys.
   */
  public abstract ImmutablePair<Set<ResourceDraftT>, ?> validateAndCollectReferencedKeys(
      @Nonnull final List<ResourceDraftT> drafts);

  protected <T> void collectReferencedKeyFromResourceIdentifier(
      @Nullable final ResourceIdentifier resourceIdentifier,
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
      @Nullable final List<AssetDraft> assetDrafts, @Nonnull final Consumer<String> keyConsumer) {

    if (assetDrafts != null) {
      assetDrafts.forEach(
          assetDraft ->
              collectReferencedKeyFromCustomFieldsDraft(assetDraft.getCustom(), keyConsumer));
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
