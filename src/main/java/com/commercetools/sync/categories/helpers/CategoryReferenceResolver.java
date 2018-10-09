package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.AssetReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public final class CategoryReferenceResolver
        extends CustomReferenceResolver<CategoryDraft, CategoryDraftBuilder, CategorySyncOptions> {
    private final AssetReferenceResolver assetReferenceResolver;
    private CategoryService categoryService;
    private static final String FAILED_TO_RESOLVE_PARENT = "Failed to resolve parent reference on "
        + "CategoryDraft with key:'%s'. Reason: %s";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "CategoryDraft with key:'%s'.";

    /**
     * Takes a {@link CategorySyncOptions} instance, a {@link CategoryService} and {@link TypeService} to instantiate a
     * {@link CategoryReferenceResolver} instance that could be used to resolve the category drafts in the
     * CTP project specified in the injected {@link CategorySyncOptions} instance.
     *
     * @param options         the container of all the options of the sync process including the CTP project client
     *                        and/or configuration and other sync-specific options.
     * @param typeService     the service to fetch the custom types for reference resolution.
     * @param categoryService the service to fetch the categories for reference resolution.
     */
    public CategoryReferenceResolver(@Nonnull final CategorySyncOptions options,
                                     @Nonnull final TypeService typeService,
                                     @Nonnull final CategoryService categoryService) {
        super(options, typeService);
        this.assetReferenceResolver = new AssetReferenceResolver(options, typeService);
        this.categoryService = categoryService;
    }

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve the custom type and parent category references to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param categoryDraft the categoryDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new categoryDraft instance with resolved category
     *          references or, in case an error occurs during reference resolution,
     *          a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<CategoryDraft> resolveReferences(@Nonnull final CategoryDraft categoryDraft) {
        return resolveCustomTypeReference(CategoryDraftBuilder.of(categoryDraft))
            .thenCompose(this::resolveParentReference)
            .thenCompose(this::resolveAssetsReferences)
            .thenApply(CategoryDraftBuilder::build);
    }

    @Nonnull
    CompletionStage<CategoryDraftBuilder> resolveAssetsReferences(
        @Nonnull final CategoryDraftBuilder categoryDraftBuilder) {

        final List<AssetDraft> categoryDraftAssets = categoryDraftBuilder.getAssets();
        if (categoryDraftAssets == null) {
            return completedFuture(categoryDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(categoryDraftAssets,
            assetReferenceResolver::resolveReferences, toList())
            .thenApply(categoryDraftBuilder::assets);
    }

    @Override
    @Nonnull
    protected CompletionStage<CategoryDraftBuilder> resolveCustomTypeReference(
        @Nonnull final CategoryDraftBuilder draftBuilder) {

        return resolveCustomTypeReference(draftBuilder,
            CategoryDraftBuilder::getCustom,
            CategoryDraftBuilder::custom,
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()));
    }

    /**
     * Given a {@link CategoryDraftBuilder} this method attempts to resolve the parent category reference to return
     * a {@link CompletionStage} which contains a new instance of the draft builder with the resolved
     * parent category reference. The key of the parent category is either taken from the expanded object or
     * taken from the id field of the reference.
     *
     * @param draftBuilder  the category draft builder to read parent category key.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} category draft instance
     *         with resolved parent category references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    CompletionStage<CategoryDraftBuilder> resolveParentReference(@Nonnull final CategoryDraftBuilder draftBuilder) {
        try {
            return getParentCategoryKey(draftBuilder)
                .map(parentCategoryKey -> fetchAndResolveParentReference(draftBuilder, parentCategoryKey))
                .orElseGet(() -> completedFuture(draftBuilder));
        } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(referenceResolutionException);
        }
    }

    @Nonnull
    public static Optional<String> getParentCategoryKey(@Nonnull final CategoryDraftBuilder draftBuilder)
        throws ReferenceResolutionException {
        return getParentCategoryKey(draftBuilder.getParent(), draftBuilder.getKey());
    }

    @Nonnull
    public static Optional<String> getParentCategoryKey(@Nonnull final CategoryDraft draft)
        throws ReferenceResolutionException {
        return getParentCategoryKey(draft.getParent(), draft.getKey());
    }

    /**
     * Given a category parent resource identifier, if it is not null the method validates the id field value. If it is
     * not valid, a {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ul>
     * <li>Checks if the id value is not null or not empty.</li>
     * </ul>
     * If the above checks pass, the id value is returned in an optional. Otherwise a
     * {@link ReferenceResolutionException} is thrown.
     *
     * <p>If the passed resource identifier is {@code null}, then an emptu optional is returned.
     *
     * @param parentCategoryResourceIdentifier the category parent resource identifier. If null - an empty optional is
     *                                         returned.
     * @param categoryKey                      the category key used in the error message if the key was not valid.
     * @return an optional containing the id or an empty optional if there is no parent reference.
     * @throws ReferenceResolutionException thrown if the key is invalid.
     */
    @Nonnull
    private static Optional<String> getParentCategoryKey(
        @Nullable final ResourceIdentifier<Category> parentCategoryResourceIdentifier,
        @Nullable final String categoryKey) throws ReferenceResolutionException {

        if (parentCategoryResourceIdentifier != null) {
            try {
                final String parentKey = getKeyFromResourceIdentifier(parentCategoryResourceIdentifier);
                return Optional.of(parentKey);
            } catch (ReferenceResolutionException referenceResolutionException) {
                throw new ReferenceResolutionException(format(FAILED_TO_RESOLVE_PARENT, categoryKey,
                    referenceResolutionException.getMessage()), referenceResolutionException);
            }
        }
        return Optional.empty();
    }

    /**
     * Given a {@link CategoryDraftBuilder} and a {@code parentCategoryKey} this method fetches the actual id of the
     * category corresponding to this key, ideally from a cache. Then it sets this id on the parent reference
     * id. If the id is not found in cache nor the CTP project, the resultant draft builder
     * would remain exactly the same as the passed category draft (without parent reference resolution).
     *
     * @param draftBuilder      the category draft builder to accept resolved references values.
     * @param parentCategoryKey the parent category key of to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} category draft builder
     *         instance with resolved parent category references or an exception.
     */
    @Nonnull
    private CompletionStage<CategoryDraftBuilder> fetchAndResolveParentReference(
        @Nonnull final CategoryDraftBuilder draftBuilder,
        @Nonnull final String parentCategoryKey) {
        return categoryService
            .fetchCachedCategoryId(parentCategoryKey)
            .thenApply(resolvedParentIdOptional -> resolvedParentIdOptional
                .map(resolvedParentId ->
                    draftBuilder.parent(Category.referenceOfId(resolvedParentId).toResourceIdentifier()))
                .orElse(draftBuilder));
    }
}
