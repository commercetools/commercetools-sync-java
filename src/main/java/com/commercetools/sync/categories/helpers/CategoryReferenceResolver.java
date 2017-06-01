package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class CategoryReferenceResolver extends BaseReferenceResolver<CategoryDraft, CategorySyncOptions> {
    private static final String FAILED_TO_RESOLVE_PARENT = "Failed to resolve parent reference.";
    private CategoryService categoryService;

    public CategoryReferenceResolver(@Nonnull final CategorySyncOptions options,
                                     @Nonnull final TypeService typeService,
                                     @Nonnull final CategoryService categoryService) {
        super(options, typeService);
        this.categoryService = categoryService;
    }

    @Override
    @Nonnull
    public CompletionStage<CategoryDraft> resolveReferences(@Nonnull final CategoryDraft categoryDraft) {
        return resolveCustomTypeReference(categoryDraft).thenCompose(this::getParentKeyAndResolveReference);
    }

    @Override
    @Nonnull
    protected CompletionStage<CategoryDraft> resolveCustomTypeReference(@Nonnull final CategoryDraft categoryDraft) {
        final CustomFieldsDraft custom = categoryDraft.getCustom();
        if (custom != null) {
            return getCustomTypeId(custom)
                .thenApply(resolvedTypeIdOptional ->
                    resolvedTypeIdOptional.filter(StringUtils::isNotBlank)
                                          .map(resolvedTypeId ->
                                              CategoryDraftBuilder.of(categoryDraft)
                                                                  .custom(CustomFieldsDraft.ofTypeIdAndJson(
                                                                      resolvedTypeId, custom.getFields()))
                                                                  .build())
                                          .orElseGet(() -> CategoryDraftBuilder.of(categoryDraft).build()));
        }
        return CompletableFuture.completedFuture(CategoryDraftBuilder.of(categoryDraft).build());
    }

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve the parent category reference to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * parent category reference. The externalId of the parent category is either taken from the expanded object or
     * taken from the id field of the reference.
     *
     * @param categoryDraft the categoryDraft to resolve it's parent reference.
     * @return a {@link CompletionStage} that contains as a result a new categoryDraft instance with resolved parent
     *      category references or, in case an error occurs during reference resolution,
     *      a {@link ReferenceResolutionException}.
     */
    @Nonnull
    private CompletionStage<CategoryDraft> getParentKeyAndResolveReference(@Nonnull final CategoryDraft categoryDraft) {
        CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft);
        final Reference<Category> parentCategoryReference = categoryDraft.getParent();
        if (parentCategoryReference != null) {
            try {
                final String keyFromExpansion = getExternalIdFromExpansion(parentCategoryReference);
                final String parentCategoryExternalId = getKeyFromExpansionOrReference(
                    keyFromExpansion, parentCategoryReference);
                return resolveParentReference(categoryDraft, parentCategoryExternalId);
            } catch (ReferenceResolutionException exception) {
                final ReferenceResolutionException referenceResolutionException = new ReferenceResolutionException(
                    buildErrorMessage(FAILED_TO_RESOLVE_PARENT, exception), exception);
                return CompletableFutureUtils.exceptionallyCompletedFuture(referenceResolutionException);
            }
        }
        return CompletableFuture.completedFuture(categoryDraftBuilder.build());
    }

    /**
     * Given a {@link CategoryDraft} and a {@code parentCategoryExternalId} this method fetches the actual id of the
     * category corresponding to this external id, ideally from a cache. Then it sets this id on the parent reference
     * id. If the id is not found in cache nor the CTP project, the resultant draft would remain exactly the same as
     * the passed category draft (without parent reference resolution).
     *
     * @param categoryDraft the categoryDraft to resolve it's parent reference.
     * @param parentCategoryExternalId the parent category external id of to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result a new categoryDraft instance with resolved parent
     *      category references or an exception.
     */
    @Nonnull
    private CompletionStage<CategoryDraft> resolveParentReference(@Nonnull final CategoryDraft categoryDraft,
                                                                  @Nonnull final String parentCategoryExternalId) {
        return categoryService.fetchCachedCategoryId(parentCategoryExternalId)
                              .thenApply(resolvedParentIdOptional -> resolvedParentIdOptional
                                  .filter(StringUtils::isNotBlank)
                                  .map(resolvedParentId ->
                                      CategoryDraftBuilder.of(categoryDraft)
                                                          .parent(Reference.of(Category.referenceTypeId(),
                                                              resolvedParentId))
                                                          .build())
                                  .orElseGet(() -> CategoryDraftBuilder.of(categoryDraft).build()));
    }

    /**
     * Helper method that returns the value of the external id field from the passed category {@link Reference} object,
     * if expanded. Otherwise, returns null.
     *
     * @return the value of the external id field from the passed category {@link Reference} object, if expanded.
     *          Otherwise, returns null.
     */
    @Nullable
    private static String getExternalIdFromExpansion(@Nonnull final Reference<Category> parentCategoryReference) {
        return isReferenceExpanded(parentCategoryReference) ? parentCategoryReference.getObj().getExternalId() : null;
    }

}
