package com.commercetools.sync.commons.helpers;

import static java.lang.String.format;

import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.TypeService;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class AssetReferenceResolver
    extends CustomReferenceResolver<AssetDraft, AssetDraftBuilder, BaseSyncOptions> {

  static final String FAILED_TO_RESOLVE_CUSTOM_TYPE =
      "Failed to resolve custom type reference on AssetDraft with key:'%s'.";

  /**
   * Takes a {@link BaseSyncOptions} instance and a {@link
   * com.commercetools.sync.services.TypeService} to instantiate a {@link AssetReferenceResolver}
   * instance that could be used to resolve the asset drafts in the CTP project specified in the
   * injected {@link BaseSyncOptions} instance.
   *
   * @param options the container of all the options of the sync process including the CTP project
   *     client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the custom types for reference resolution.
   */
  public AssetReferenceResolver(
      @Nonnull final BaseSyncOptions options, @Nonnull final TypeService typeService) {
    super(options, typeService);
  }

  @Override
  @Nonnull
  public CompletionStage<AssetDraft> resolveReferences(@Nonnull final AssetDraft assetDraft) {
    return resolveCustomTypeReference(AssetDraftBuilder.of(assetDraft))
        .thenApply(AssetDraftBuilder::build);
  }

  @Override
  @Nonnull
  protected CompletionStage<AssetDraftBuilder> resolveCustomTypeReference(
      @Nonnull final AssetDraftBuilder assetDraftBuilder) {

    return resolveCustomTypeReference(
        assetDraftBuilder,
        AssetDraftBuilder::getCustom,
        AssetDraftBuilder::custom,
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, assetDraftBuilder.getKey()));
  }
}
