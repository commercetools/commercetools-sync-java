package com.commercetools.sync.categories.helpers;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

public final class CategoryReferenceResolver
        extends CustomReferenceResolver<CategoryDraft, CategoryDraftBuilder, CategorySyncOptions> {

    private CategoryService categoryService;
    private static final String FAILED_TO_RESOLVE_PARENT = "Failed to resolve parent reference on "
        + "CategoryDraft with key:'%s'. Reason: %s";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference on "
        + "CategoryDraft with key:'%s'.";

    public CategoryReferenceResolver(@Nonnull final CategorySyncOptions options,
                                     @Nonnull final TypeService typeService,
                                     @Nonnull final CategoryService categoryService) {
        super(options, typeService);
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
    public CompletionStage<CategoryDraft> resolveReferences(@Nonnull final CategoryDraft categoryDraft) {
        return resolveCustomTypeReference(categoryDraft)
            .thenCompose(this::resolveParentReference)
            .thenApply(CategoryDraftBuilder::build);
    }

    @Override
    @Nonnull
    protected CompletionStage<CategoryDraftBuilder> resolveCustomTypeReference(
            @Nonnull final CategoryDraft categoryDraft) {
        final CustomFieldsDraft custom = categoryDraft.getCustom();
        final CategoryDraftBuilder draftBuilder = CategoryDraftBuilder.of(categoryDraft);
        if (custom != null) {
            return getCustomTypeId(custom, format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()))
                .thenApply(resolvedTypeIdOptional ->
                    resolvedTypeIdOptional.map(resolvedTypeId ->
                            draftBuilder.custom(CustomFieldsDraft.ofTypeIdAndJson(resolvedTypeId, custom.getFields())))
                        .orElse(draftBuilder));
        }
        return CompletableFuture.completedFuture(draftBuilder);
    }

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve the parent category reference to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * parent category reference. The key of the parent category is either taken from the expanded object or
     * taken from the id field of the reference.
     *
     * @param draftBuilder  the category draft builder to read keys and then set resolved values
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} category draft instance
     *         with resolved parent category references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    CompletionStage<CategoryDraftBuilder> resolveParentReference(@Nonnull final CategoryDraftBuilder draftBuilder) {
        try {
            return getParentCategoryKey(draftBuilder, options.shouldAllowUuidKeys())
                .map(parentCategoryKey -> fetchAndResolveParentReference(draftBuilder, parentCategoryKey))
                .orElseGet(() -> CompletableFuture.completedFuture(draftBuilder));
        } catch (ReferenceResolutionException referenceResolutionException) {
            return CompletableFutureUtils
                .exceptionallyCompletedFuture(referenceResolutionException);
        }
    }

    /**
     * Given a {@link CategoryDraft} and a {@code parentCategoryKey} this method fetches the actual id of the
     * category corresponding to this key, ideally from a cache. Then it sets this id on the parent reference
     * id. If the id is not found in cache nor the CTP project, the resultant draft would remain exactly the same as
     * the passed category draft (without parent reference resolution).
     *
     * @param draftBuilder the category draft builder to accept resolved references values.
     * @param parentCategoryKey the parent category key of to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} category draft builder
     *         instance with resolved parent category references or an exception.
     */
    @Nonnull
    private CompletionStage<CategoryDraftBuilder> fetchAndResolveParentReference(
            @Nonnull final CategoryDraftBuilder draftBuilder,
            @Nonnull final String parentCategoryKey) {
        return categoryService.fetchCachedCategoryId(parentCategoryKey)
            .thenApply(resolvedParentIdOptional -> resolvedParentIdOptional
                .map(resolvedParentId -> draftBuilder
                    .parent(Category.referenceOfId(resolvedParentId)))
                .orElse(draftBuilder));
    }

    /**
     * Given a category parent, this method first checks if this value is meaningful. If it is,
     * the method tries to get the key of the parent either from the expanded object or gets it from the id value on
     * the reference in an optional. If the id value has a UUID format and the supplied boolean value
     * {@code shouldAllowUuidKeys} is false, then a {@link ReferenceResolutionException} is thrown. If there is a parent
     * reference but an blank (null/empty) key value, then a {@link ReferenceResolutionException} is also thrown. If
     * there is no parent reference set, then an empty optional is returned.
     *
     * @param parentCategoryReference the category parent reference. If empty - empty result is returned
     * @param categoryKey             the category key to be logged if the reference resolution fails
     * @param shouldAllowUuidKeys     a flag that specifies whether the key could be in UUID format or not.
     * @return an optional containing the id or an empty optional if there is no parent reference.
     * @throws ReferenceResolutionException thrown if the key is invalid (empty/null/in UUID when the flag is false).
     */
    public static Optional<String> getParentCategoryKey(@Nullable final Reference<Category> parentCategoryReference,
                                                        @Nullable final String categoryKey,
                                                        final boolean shouldAllowUuidKeys)
        throws ReferenceResolutionException {
        if (parentCategoryReference != null) {
            final String keyFromExpansion = getKeyFromExpansion(parentCategoryReference);
            try {
                return Optional
                    .of(getKeyFromExpansionOrReference(shouldAllowUuidKeys, keyFromExpansion, parentCategoryReference));
            } catch (ReferenceResolutionException referenceResolutionException) {
                throw new ReferenceResolutionException(format(FAILED_TO_RESOLVE_PARENT, categoryKey,
                    referenceResolutionException.getMessage()), referenceResolutionException);
            }
        }
        return Optional.empty();
    }

    /**
     * @see #getParentCategoryKey(Reference, String, boolean)
     */
    public static Optional<String> getParentCategoryKey(@Nonnull final CategoryDraft draft,
                                                        final boolean shouldAllowUuidKeys)
        throws ReferenceResolutionException {
        return getParentCategoryKey(draft.getParent(), draft.getKey(), shouldAllowUuidKeys);
    }

    /**
     * @see #getParentCategoryKey(Reference, String, boolean)
     */
    public static Optional<String> getParentCategoryKey(@Nonnull final CategoryDraftBuilder draftBuilder,
                                                        final boolean shouldAllowUuidKeys)
        throws ReferenceResolutionException {
        return getParentCategoryKey(draftBuilder.getParent(), draftBuilder.getKey(), shouldAllowUuidKeys);
    }

    /**
     * Helper method that returns the value of the key field from the passed category {@link Reference} object,
     * if expanded. Otherwise, returns null.
     *
     * @param categoryReference the category reference to get the key from it's expansion.
     * @return the value of the key field from the passed category {@link Reference} object, if expanded.
     *          Otherwise, returns null.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE can't occur because of the isReferenceExpanded check.
    public static String getKeyFromExpansion(@Nonnull final Reference<Category> categoryReference) {
        return isReferenceExpanded(categoryReference) ? categoryReference.getObj().getKey() : null;
    }

}
