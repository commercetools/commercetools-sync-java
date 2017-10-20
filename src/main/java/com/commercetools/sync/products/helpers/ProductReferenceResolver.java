package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class ProductReferenceResolver extends BaseReferenceResolver<ProductDraft, ProductSyncOptions> {
    private final ProductTypeService productTypeService;
    private final CategoryService categoryService;
    private final PriceReferenceResolver priceReferenceResolver;
    private final TaxCategoryService taxCategoryService;
    private final StateService stateService;


    private static final String FAILED_TO_RESOLVE_PRODUCT_TYPE = "Failed to resolve product type reference on "
        + "ProductDraft with key:'%s'.";
    private static final String FAILED_TO_RESOLVE_CATEGORY = "Failed to resolve category reference on "
        + "ProductDraft with key:'%s'. Reason: %s";

    /**
     * Takes a {@link ProductSyncOptions} instance, a {@link ProductTypeService} and {@link CategoryService} to
     * instantiate a {@link ProductReferenceResolver} instance that could be used to resolve the product type and
     * category references of product drafts in the CTP project specified in the injected {@link ProductSyncOptions}
     * instance.
     *
     * @param productSyncOptions the container of all the options of the sync process including the CTP project client
     *                           and/or configuration and other sync-specific options.
     * @param productTypeService the service to fetch the product type for reference resolution.
     * @param categoryService    the service to fetch the categories for reference resolution.
     * @param typeService        the service to fetch the custom types for reference resolution.
     * @param channelService     the service to fetch the channels for reference resolution.
     * @param taxCategoryService the service to fetch tax categories for reference resolution.
     * @param stateService       the service to fetch product states for reference resolution
     */
    public ProductReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final ProductTypeService productTypeService,
                                    @Nonnull final CategoryService categoryService,
                                    @Nonnull final TypeService typeService,
                                    @Nonnull final ChannelService channelService,
                                    @Nonnull final TaxCategoryService taxCategoryService,
                                    @Nonnull final StateService stateService) {
        super(productSyncOptions);
        this.productTypeService = productTypeService;
        this.categoryService = categoryService;
        this.taxCategoryService = taxCategoryService;
        this.stateService = stateService;
        this.priceReferenceResolver = new PriceReferenceResolver(productSyncOptions, typeService, channelService);
    }

    /**
     * Given a {@link ProductDraft} this method attempts to resolve the product type and category references to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param productDraft the productDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved references
     *         or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    @Override
    public CompletionStage<ProductDraft> resolveReferences(@Nonnull final ProductDraft productDraft) {
        return resolveProductTypeReference(productDraft)
            .thenCompose(this::resolveCategoryReferences)
            .thenCompose(this::resolveProductPricesReferences)
            .thenCompose(this::resolveTaxCategoryReferences)
            .thenCompose(this::resolveStateReferences);
            //.thenApply(ProductDraftBuilder::build);
            // TODO: akovalenko: fix it when new draft builders released
    }

    @Nonnull
    private CompletionStage<ProductDraft> resolveProductPricesReferences(@Nonnull final ProductDraft productDraft) {
        final ProductVariantDraft productDraftMasterVariant = productDraft.getMasterVariant();
        if (productDraftMasterVariant != null) {
            return resolveProductVariantPriceReferences(productDraftMasterVariant)
                .thenApply(resolvedMasterVariant ->
                    ProductDraftBuilder.of(productDraft)
                                       .masterVariant(resolvedMasterVariant).build())
                .thenCompose(this::resolveProductVariantsPriceReferences);
        }
        return resolveProductVariantsPriceReferences(productDraft);
    }

    @Nonnull
    private CompletionStage<ProductDraft> resolveProductVariantsPriceReferences(
        @Nonnull final ProductDraft productDraft) {
        final List<ProductVariantDraft> productDraftVariants = productDraft.getVariants();
        if (productDraftVariants == null) {
            return CompletableFuture.completedFuture(productDraft);
        }

        final List<CompletableFuture<ProductVariantDraft>> resolvedVariantFutures =
            productDraftVariants.stream()
                                .filter(Objects::nonNull)
                                .map(this::resolveProductVariantPriceReferences)
                                .map(CompletionStage::toCompletableFuture)
                                .collect(Collectors.toList());
        return
            CompletableFuture.allOf(
                resolvedVariantFutures.toArray(new CompletableFuture[resolvedVariantFutures.size()]))
                             .thenApply(result -> resolvedVariantFutures.stream()
                                                                        .map(CompletableFuture::join)
                                                                        .collect(Collectors.toList()))
                             .thenApply(resolvedVariants ->
                                 ProductDraftBuilder.of(productDraft).variants(resolvedVariants).build());
    }

    private CompletionStage<ProductVariantDraft> resolveProductVariantPriceReferences(
        @Nonnull final ProductVariantDraft productVariantDraft) {
        final List<PriceDraft> productVariantDraftPrices = productVariantDraft.getPrices();
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of(productVariantDraft);

        if (productVariantDraftPrices == null) {
            return completedFuture(productVariantDraftBuilder.build());
        }

        final List<CompletableFuture<PriceDraft>> resolvedPriceDraftFutures =
            productVariantDraftPrices.stream()
                                     .map(priceReferenceResolver::resolveReferences)
                                     .map(CompletionStage::toCompletableFuture)
                                     .collect(Collectors.toList());
        return CompletableFuture
            .allOf(resolvedPriceDraftFutures.toArray(new CompletableFuture[resolvedPriceDraftFutures.size()]))
            .thenApply(result -> resolvedPriceDraftFutures.stream()
                                                          .map(CompletableFuture::join)
                                                          .collect(Collectors.toList()))
            .thenApply(resolvedPriceDrafts -> productVariantDraftBuilder.prices(resolvedPriceDrafts).build());
    }

    @Nonnull
    CompletionStage<ProductDraft> resolveProductTypeReference(@Nonnull final ProductDraft productDraft) {
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
        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = productDraft.getCategories();
        final Set<String> categoryKeys = new HashSet<>();

        categoryResourceIdentifiers.forEach(categoryResourceIdentifier -> {
            if (categoryResourceIdentifier != null) {
                try {
                    final String categoryKey = getKeyFromResourceIdentifier(categoryResourceIdentifier,
                        options.shouldAllowUuidKeys());
                    categoryKeys.add(categoryKey);
                } catch (ReferenceResolutionException referenceResolutionException) {
                    options.applyErrorCallback(format(FAILED_TO_RESOLVE_CATEGORY, productDraft.getKey(),
                        referenceResolutionException), referenceResolutionException);
                }
            }
        });
        return fetchAndResolveCategoryReferences(productDraft, categoryKeys);
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
    private CompletionStage<ProductDraft> fetchAndResolveCategoryReferences(@Nonnull final ProductDraft productDraft,
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
    private CompletionStage<Optional<String>> getProductTypeId(
            @Nonnull final ResourceIdentifier<ProductType> productTypeResourceIdentifier,
            @Nonnull final String referenceResolutionErrorMessage) {
        try {
            final String productTypeKey = getKeyFromResourceIdentifier(productTypeResourceIdentifier,
                options.shouldAllowUuidKeys());
            return productTypeService.fetchCachedProductTypeId(productTypeKey);
        } catch (ReferenceResolutionException exception) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format("%s Reason: %s", referenceResolutionErrorMessage, exception.getMessage()), exception));
        }
    }

    @Nonnull
    CompletionStage<ProductDraft> resolveTaxCategoryReferences(@Nonnull final ProductDraft productDraft) {
        return resolveReference(productDraft,
            ProductDraft::getTaxCategory, taxCategoryService::fetchCachedTaxCategoryId, TaxCategory::referenceOfId,
            ProductDraftBuilder::taxCategory);
    }

    @Nonnull
    CompletionStage<ProductDraft> resolveStateReferences(@Nonnull final ProductDraft productDraft) {
        return resolveReference(productDraft,
            ProductDraft::getState, stateService::fetchCachedStateId, State::referenceOfId, ProductDraftBuilder::state);
    }

    /**
     * Common function to resolve references from key.
     *
     * @param productDraft        {@link ProductDraft} to update
     * @param referenceProvider   function which returns the reference which should be resolver from the
     *                            {@code productDraft}
     * @param keyToIdMapper       function which calls respective service to fetch the reference by key
     * @param idToReferenceMapper function which creates {@link Reference} instance from fetched id
     * @param referenceSetter     function which will set the resolved reference to the {@code productDraft}
     * @param <T>                 type of reference (e.g. {@link State}, {@link TaxCategory}
     * @return {@link CompletionStage} containing {@link ProductDraft} with resolved &lt;T&gt; reference.
     */
    @Nonnull
    private <T> CompletionStage<ProductDraft> resolveReference(
            @Nonnull final ProductDraft productDraft,
            @Nonnull final Function<ProductDraft, Reference<T>> referenceProvider,
            @Nonnull final Function<String, CompletionStage<Optional<String>>> keyToIdMapper,
            @Nonnull final Function<String, Reference<T>> idToReferenceMapper,
            @Nonnull final BiFunction<ProductDraftBuilder, Reference<T>, ProductDraftBuilder> referenceSetter) {
        final Reference<T> reference = referenceProvider.apply(productDraft);

        if (reference == null) {
            return completedFuture(productDraft);
        }

        try {
            final String stateKey = getKeyFromResourceIdentifier(reference, options.shouldAllowUuidKeys());
            return keyToIdMapper.apply(stateKey)
                .thenApply(optId -> optId
                    .map(idToReferenceMapper)
                    // up-casting (ProductDraft) is required to allow orElse(ProductDraft) chaining,
                    // because ProductDraftBuilder#build() returns more specific ProductDraftDsl
                    .map(stateReference -> (ProductDraft)
                        referenceSetter.apply(ProductDraftBuilder.of(productDraft), stateReference).build())
                    .orElse(productDraft));
        } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format("Failed to resolve reference '%s' on ProductDraft with key:'%s'. Reason: %s",
                        reference.getTypeId(), productDraft.getKey(), referenceResolutionException.getMessage())));
        }
    }

}
