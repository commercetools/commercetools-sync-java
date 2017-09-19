package com.commercetools.sync.products.helpers;

import com.commercetools.sync.categories.helpers.CategoryReferenceResolver;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ProductReferenceResolver extends BaseReferenceResolver<ProductDraft, ProductSyncOptions> {
    private ProductTypeService productTypeService;
    private CategoryService categoryService;
    private static final String FAILED_TO_RESOLVE_PRODUCT_TYPE = "Failed to resolve product type reference on "
        + "ProductDraft with key:'%s'.";
    private static final String FAILED_TO_RESOLVE_CATEGORY = "Failed to resolve category reference on "
        + "ProductDraft with key:'%s'. Reason: %s";

    public ProductReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final ProductTypeService productTypeService,
                                    @Nonnull final CategoryService categoryService) {
        super(productSyncOptions);
        this.productTypeService = productTypeService;
        this.categoryService = categoryService;
    }

    /**
     * Given a {@link ProductDraft} this method attempts to resolve the product type and category references to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param productDraft the productDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved references
     * or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    @Override
    public CompletionStage<ProductDraft> resolveReferences(@Nonnull final ProductDraft productDraft) {
        return resolveProductTypeReference(productDraft).thenCompose(this::resolveCategoryReferences);
    }

    @Nonnull
    private CompletionStage<ProductDraft> resolveProductTypeReference(@Nonnull final ProductDraft productDraft) {
        final ResourceIdentifier<ProductType> productTypeResourceIdentifier = productDraft.getProductType();
        return getProductTypeId(productTypeResourceIdentifier,
            format(FAILED_TO_RESOLVE_PRODUCT_TYPE, productDraft.getKey()))
            .thenApply(resolvedProductTypeIdOptional ->
                resolvedProductTypeIdOptional.map(resolvedTypeId ->
                    ProductDraftBuilder.of(productDraft)
                                       .productType(ResourceIdentifier.ofId(resolvedTypeId,
                                           ProductType.referenceTypeId()))
                                       .build())
                                             .orElseGet(() -> ProductDraftBuilder.of(productDraft).build()));
    }

    @Nonnull
    private CompletionStage<ProductDraft> resolveCategoryReferences(@Nonnull final ProductDraft productDraft) {
        final Set<Reference<Category>> categories = productDraft.getCategories();
        final Set<String> categoryKeys = new HashSet<>();

        categories.forEach(categoryReference -> {
            if (categoryReference != null) {
                try {
                    getCategoryKey(productDraft, categoryReference, options.shouldAllowUuidKeys())
                        .map(categoryKeys::add);
                } catch (ReferenceResolutionException referenceResolutionException) {
                    options.applyErrorCallback(format(FAILED_TO_RESOLVE_CATEGORY, productDraft.getKey(),
                        referenceResolutionException), referenceResolutionException);
                }
            }
        });
        return fetchAndResolveCategoryReference(productDraft, categoryKeys);
    }


    @Nonnull
    private static Optional<String> getCategoryKey(@Nonnull final ProductDraft productDraft,
                                                   @Nonnull final Reference<Category> categoryReference,
                                                   final boolean shouldAllowUuidKeys)
        throws ReferenceResolutionException {
        final String keyFromExpansion = CategoryReferenceResolver.getKeyFromExpansion(categoryReference);
        try {
            final String keyFromExpansionOrReference =
                getKeyFromExpansionOrReference(shouldAllowUuidKeys, keyFromExpansion, categoryReference);
            return Optional.of(keyFromExpansionOrReference);
        } catch (ReferenceResolutionException referenceResolutionException) {
            throw new ReferenceResolutionException(format(FAILED_TO_RESOLVE_CATEGORY, productDraft.getKey(),
                referenceResolutionException.getMessage()), referenceResolutionException);
        }
    }

    /**
     * Given a {@link ProductDraft} and a {@link Set} of {@code categoryKeys} this method fetches the categories
     * corresponding to these keys. Then it sets the category references on the {@code productDraft}. It also replaces
     * the category keys on the {@link CategoryOrderHints} map of the {@code productDraft}. If the category is not found
     * in the CTP project, the resultant draft would remain exactly the same as the passed product draft
     * (without reference resolution).
     *
     * @param productDraft the product draft to resolve it's category references.
     * @param categoryKeys the category keys of to resolve their actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved category
     *          references or an exception.
     */
    @Nonnull
    private CompletionStage<ProductDraft> fetchAndResolveCategoryReference(@Nonnull final ProductDraft productDraft,
                                                                           @Nonnull final Set<String> categoryKeys) {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        final CategoryOrderHints categoryOrderHints = productDraft.getCategoryOrderHints();

        return categoryService.fetchMatchingCategoriesByKeys(categoryKeys)
                              .thenApply(categories ->
                                      categories.stream().map(category -> {
                                          final Reference<Category> categoryReference = category.toReference();
                                          if (categoryOrderHints != null && !categoryOrderHints.getAsMap().isEmpty()) {
                                              final String categoryOrderHintValue = categoryOrderHints
                                                  .get(category.getKey());
                                              categoryOrderHintsMap
                                                  .put(category.getId(), categoryOrderHintValue);
                                          }
                                          return categoryReference;
                                      }).collect(Collectors.toList()))
                              .thenApply(categoryReferences -> ProductDraftBuilder.of(productDraft)
                                                                                  .categories(categoryReferences)
                                                                                  .categoryOrderHints(
                                                                                      CategoryOrderHints
                                                                                          .of(categoryOrderHintsMap))
                                                                                  .build());
    }


    /**
     * Given a {@link ProductType} this method fetches the product type reference id.
     *
     * @param productTypeResourceIdentifier   the productType ResourceIdentifier object.
     * @param referenceResolutionErrorMessage the message containing the information about the draft to attach to the
     *                                        {@link ReferenceResolutionException} in case it occurs.
     * @return a {@link CompletionStage} that contains as a result an optional which either contains the custom type id
     *         if it exists or empty if it doesn't.
     */
    @Nonnull
    private CompletionStage<Optional<String>> getProductTypeId(@Nonnull final ResourceIdentifier<ProductType>
                                                                   productTypeResourceIdentifier,
                                                               @Nonnull final String referenceResolutionErrorMessage) {
        try {
            final String productTypeKey = getKeyFromResourceIdentifier(productTypeResourceIdentifier,
                options.shouldAllowUuidKeys());
            return productTypeService.fetchCachedProductTypeId(productTypeKey);
        } catch (ReferenceResolutionException exception) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format("%s Reason: %s", referenceResolutionErrorMessage, exception.getMessage()), exception));
        }
    }

}
