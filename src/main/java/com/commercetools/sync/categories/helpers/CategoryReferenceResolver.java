package com.commercetools.sync.categories.helpers;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.AssetReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public final class CategoryReferenceResolver
    extends CustomReferenceResolver<CategoryDraft, CategoryDraftBuilder, CategorySyncOptions> {
  private final AssetReferenceResolver assetReferenceResolver;
  private final CategoryService categoryService;
  private final TypeService typeService;

  static final String FAILED_TO_RESOLVE_PARENT =
      "Failed to resolve parent reference on CategoryDraft with key:'%s'. Reason: %s";
  static final String FAILED_TO_RESOLVE_CUSTOM_TYPE =
      "Failed to resolve custom type reference on CategoryDraft with key:'%s'.";
  static final String PARENT_CATEGORY_DOES_NOT_EXIST =
      "Parent category with key '%s' doesn't exist.";

  /**
   * Takes a {@link CategorySyncOptions} instance, a {@link CategoryService} and {@link TypeService}
   * to instantiate a {@link CategoryReferenceResolver} instance that could be used to resolve the
   * category drafts in the CTP project specified in the injected {@link CategorySyncOptions}
   * instance.
   *
   * @param options the container of all the options of the sync process including the CTP project
   *     client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the custom types for reference resolution.
   * @param categoryService the service to fetch the categories for reference resolution.
   */
  public CategoryReferenceResolver(
      @Nonnull final CategorySyncOptions options,
      @Nonnull final TypeService typeService,
      @Nonnull final CategoryService categoryService) {
    super(options, typeService);
    this.assetReferenceResolver = new AssetReferenceResolver(options, typeService);
    this.categoryService = categoryService;
    this.typeService = typeService;
  }

  /**
   * Given a {@link CategoryDraft} this method attempts to resolve the custom type and parent
   * category references to return a {@link CompletionStage} which contains a new instance of the
   * draft with the resolved references.
   *
   * @param categoryDraft the categoryDraft to resolve its references.
   * @return a {@link CompletionStage} that contains as a result a new categoryDraft instance with
   *     resolved category references or, in case an error occurs during reference resolution, a
   *     {@link ReferenceResolutionException}.
   */
  @Override
  @Nonnull
  public CompletionStage<CategoryDraft> resolveReferences(
      @Nonnull final CategoryDraft categoryDraft) {
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

    return mapValuesToFutureOfCompletedValues(
            categoryDraftAssets, assetReferenceResolver::resolveReferences, toList())
        .thenApply(categoryDraftBuilder::assets);
  }

  @Override
  @Nonnull
  protected CompletionStage<CategoryDraftBuilder> resolveCustomTypeReference(
      @Nonnull final CategoryDraftBuilder draftBuilder) {

    return resolveCustomTypeReference(
        draftBuilder,
        CategoryDraftBuilder::getCustom,
        CategoryDraftBuilder::custom,
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, draftBuilder.getKey()));
  }

  @Nonnull
  CompletionStage<CategoryDraftBuilder> resolveParentReference(
      @Nonnull final CategoryDraftBuilder draftBuilder) {
    final CategoryResourceIdentifier parent = draftBuilder.getParent();
    if (parent != null && parent.getId() == null) {
      try {
        final String parentCategoryKey = getParentCategoryKey(parent, draftBuilder.getKey());
        return fetchAndResolveParentReference(draftBuilder, parentCategoryKey);

      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(referenceResolutionException);
      }
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private static String getParentCategoryKey(
      @Nonnull final CategoryResourceIdentifier parentCategoryResourceIdentifier,
      @Nullable final String categoryKey)
      throws ReferenceResolutionException {

    try {
      return getKeyFromResourceIdentifier(parentCategoryResourceIdentifier);
    } catch (ReferenceResolutionException referenceResolutionException) {
      throw new ReferenceResolutionException(
          format(FAILED_TO_RESOLVE_PARENT, categoryKey, referenceResolutionException.getMessage()),
          referenceResolutionException);
    }
  }

  @Nonnull
  private CompletionStage<CategoryDraftBuilder> fetchAndResolveParentReference(
      @Nonnull final CategoryDraftBuilder draftBuilder, @Nullable final String parentCategoryKey) {

    if (StringUtils.isBlank(parentCategoryKey)) {
      return completedFuture(draftBuilder);
    }
    return categoryService
        .fetchCachedCategoryId(parentCategoryKey)
        .thenCompose(
            resolvedParentIdOptional ->
                resolvedParentIdOptional
                    .map(
                        resolvedParentId ->
                            completedFuture(
                                draftBuilder.parent(
                                    CategoryResourceIdentifierBuilder.of()
                                        .id(resolvedParentId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(
                                  FAILED_TO_RESOLVE_PARENT,
                                  draftBuilder.getKey(),
                                  format(PARENT_CATEGORY_DOES_NOT_EXIST, parentCategoryKey));
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(errorMessage));
                        }));
  }

  /**
   * Calls the {@code cacheKeysToIds} service methods to fetch all the referenced keys (category and
   * type) from the commercetools to populate caches for the reference resolution.
   *
   * <p>Note: This method is meant be only used internally by the library to improve performance.
   *
   * @param referencedKeys a wrapper for the category references to fetch and cache the id's for.
   * @return {@link CompletionStage}&lt;{@link Map}&lt;{@link String}&gt;{@link String}&gt;&gt; in
   *     which the results of it's completions contains a map of requested references keys -&gt; ids
   *     of parent category references.
   */
  @Nonnull
  public CompletableFuture<Map<String, String>> populateKeyToIdCachesForReferencedKeys(
      @Nonnull final CategoryBatchValidator.ReferencedKeys referencedKeys) {

    final List<CompletionStage<Map<String, String>>> futures = new ArrayList<>();

    futures.add(categoryService.cacheKeysToIds(referencedKeys.getCategoryKeys()));

    final Set<String> typeKeys = referencedKeys.getTypeKeys();
    if (!typeKeys.isEmpty()) {
      futures.add(typeService.cacheKeysToIds(typeKeys));
    }

    return collectionOfFuturesToFutureOfCollection(futures, toList())
        .thenApply(maps -> maps.get(0));
  }
}
