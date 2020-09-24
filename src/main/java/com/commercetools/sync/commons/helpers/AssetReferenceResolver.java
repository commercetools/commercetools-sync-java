package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

public final class AssetReferenceResolver
    extends CustomReferenceResolver<AssetDraft, AssetDraftBuilder, BaseSyncOptions> {

    static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "AssetDraft with key:'%s'.";

    /**
     * Takes a {@link BaseSyncOptions} instance and a {@link TypeService} to instantiate a
     * {@link AssetReferenceResolver} instance that could be used to resolve the asset drafts in the CTP project
     * specified in the injected {@link BaseSyncOptions} instance.
     *
     * @param options         the container of all the options of the sync process including the CTP project client
     *                        and/or configuration and other sync-specific options.
     * @param typeService     the service to fetch the custom types for reference resolution.
     */
    public AssetReferenceResolver(@Nonnull final BaseSyncOptions options, @Nonnull final TypeService typeService) {
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
