package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public final class ProductReferenceResolver extends BaseReferenceResolver<ProductDraft, ProductSyncOptions> {
    private final ProductTypeService productTypeService;
    private final CategoryService categoryService;
    private final VariantReferenceResolver variantReferenceResolver;
    private final TaxCategoryService taxCategoryService;
    private final StateService stateService;

    private static final String FAILED_TO_RESOLVE_PRODUCT_TYPE = "Failed to resolve product type reference on "
        + "ProductDraft with key:'%s'.";
    private static final String FAILED_TO_RESOLVE_REFERENCE = "Failed to resolve reference '%s' on ProductDraft with "
        + "key:'%s'. Reason: %s";

    /**
     * Takes a {@link ProductSyncOptions} instance, a {@link ProductTypeService}, a {@link CategoryService}, a
     * {@link TypeService}, a {@link ChannelService}, a {@link CustomerGroupService}, a {@link TaxCategoryService},
     * a {@link StateService} and a {@link ProductService} to instantiate a {@link ProductReferenceResolver} instance
     * that could be used to resolve the product type, categories, variants, tax category and product state references
     * of product drafts in the CTP project specified in the injected {@link ProductSyncOptions} instance.
     *
     * @param productSyncOptions   the container of all the options of the sync process including the CTP project client
     *                             and/or configuration and other sync-specific options.
     * @param productTypeService   the service to fetch the product type for reference resolution.
     * @param categoryService      the service to fetch the categories for reference resolution.
     * @param typeService          the service to fetch the custom types for reference resolution.
     * @param channelService       the service to fetch the channels for reference resolution.
     * @param customerGroupService the service to fetch the customer groups for reference resolution.
     * @param taxCategoryService   the service to fetch tax categories for reference resolution.
     * @param stateService         the service to fetch product states for reference resolution.
     * @param productService       the service to fetch products for product reference resolution on reference
     *                             attributes.
     */
    public ProductReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final ProductTypeService productTypeService,
                                    @Nonnull final CategoryService categoryService,
                                    @Nonnull final TypeService typeService,
                                    @Nonnull final ChannelService channelService,
                                    @Nonnull final CustomerGroupService customerGroupService,
                                    @Nonnull final TaxCategoryService taxCategoryService,
                                    @Nonnull final StateService stateService,
                                    @Nonnull final ProductService productService) {
        super(productSyncOptions);
        this.productTypeService = productTypeService;
        this.categoryService = categoryService;
        this.taxCategoryService = taxCategoryService;
        this.stateService = stateService;
        this.variantReferenceResolver =
            new VariantReferenceResolver(productSyncOptions, typeService, channelService, customerGroupService,
                productService);
    }

    /**
     * Given a {@link ProductDraft} this method attempts to resolve the product type, categories, variants, tax
     * category and product state references to return a {@link CompletionStage} which contains a new instance of the
     * draft with the resolved references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param productDraft the productDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved references
     *         or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    @Override
    public CompletionStage<ProductDraft> resolveReferences(@Nonnull final ProductDraft productDraft) {
        return resolveProductTypeReference(ProductDraftBuilder.of(productDraft))
            .thenCompose(this::resolveCategoryReferences)
            .thenCompose(this::resolveAllVariantsReferences)
            .thenCompose(this::resolveTaxCategoryReference)
            .thenCompose(this::resolveStateReference)
            .thenApply(ProductDraftBuilder::build);
    }

    @Nonnull
    private CompletionStage<ProductDraftBuilder> resolveAllVariantsReferences(
            @Nonnull final ProductDraftBuilder draftBuilder) {
        final ProductVariantDraft masterVariantDraft = draftBuilder.getMasterVariant();
        if (masterVariantDraft != null) {
            return variantReferenceResolver.resolveReferences(masterVariantDraft)
                                           .thenApply(draftBuilder::masterVariant)
                                           .thenCompose(this::resolveVariantsReferences);
        }
        return resolveVariantsReferences(draftBuilder);
    }

    @Nonnull
    private CompletionStage<ProductDraftBuilder> resolveVariantsReferences(
        @Nonnull final ProductDraftBuilder draftBuilder) {
        final List<ProductVariantDraft> productDraftVariants = draftBuilder.getVariants();

        return mapValuesToFutureOfCompletedValues(productDraftVariants,
            variantReferenceResolver::resolveReferences, toList()).thenApply(draftBuilder::variants);
    }

    /**
     * Given a {@link ProductDraftBuilder} this method attempts to resolve the product type to return a
     * {@link CompletionStage} which contains a new instance of the builder with the resolved product type reference.
     * The key of the product type reference is either taken from the expanded reference or taken from the value of the
     * id field.
     *
     * @param draftBuilder the productDraft to resolve its product type reference.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved product type
     *         reference or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ProductDraftBuilder> resolveProductTypeReference(
        @Nonnull final ProductDraftBuilder draftBuilder) {
        final ResourceIdentifier<ProductType> productTypeResourceIdentifier = draftBuilder.getProductType();
        return getProductTypeId(productTypeResourceIdentifier,
            format(FAILED_TO_RESOLVE_PRODUCT_TYPE, draftBuilder.getKey()))
            .thenApply(resolvedProductTypeIdOptional -> {
                resolvedProductTypeIdOptional.ifPresent(resolvedTypeId -> draftBuilder
                    .productType(ResourceIdentifier.ofId(resolvedTypeId, ProductType.referenceTypeId())));
                return draftBuilder;
            });
    }

    /**
     * Given a {@link ProductDraftBuilder} this method attempts to resolve the categories and categoryOrderHints to
     * return a {@link CompletionStage} which contains a new instance of the builder with the resolved references.
     * The key of the category references is either taken from the expanded references or taken from the value of the
     * id fields.
     *
     * @param draftBuilder the productDraft to resolve its category and categoryOrderHints references.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved references or,
     *         in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ProductDraftBuilder> resolveCategoryReferences(
        @Nonnull final ProductDraftBuilder draftBuilder) {
        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = draftBuilder.getCategories();
        final Set<String> categoryKeys = new HashSet<>();
        for (ResourceIdentifier<Category> categoryResourceIdentifier: categoryResourceIdentifiers) {
            if (categoryResourceIdentifier != null) {
                try {
                    final String categoryKey = getKeyFromResourceIdentifier(categoryResourceIdentifier,
                        options.shouldAllowUuidKeys());
                    categoryKeys.add(categoryKey);
                } catch (ReferenceResolutionException referenceResolutionException) {
                    return exceptionallyCompletedFuture(
                        new ReferenceResolutionException(
                            format(FAILED_TO_RESOLVE_REFERENCE, categoryResourceIdentifier.getTypeId(),
                                draftBuilder.getKey(),referenceResolutionException.getMessage())));
                }
            }
        }
        return fetchAndResolveCategoryReferences(draftBuilder, categoryKeys);
    }

    /**
     * Given a {@link ProductDraftBuilder} and a {@link Set} of {@code categoryKeys} this method fetches the categories
     * corresponding to these keys. Then it sets the category references on the {@code draftBuilder}. It also replaces
     * the category keys on the {@link CategoryOrderHints} map of the {@code productDraft}. If the category is not found
     * in the CTP project, the resultant draft would remain exactly the same as the passed product draft
     * (without reference resolution).
     *
     * @param draftBuilder the product draft builder to resolve it's category references.
     * @param categoryKeys the category keys of to resolve their actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved category
     *          references or an exception.
     */
    @Nonnull
    private CompletionStage<ProductDraftBuilder> fetchAndResolveCategoryReferences(
            @Nonnull final ProductDraftBuilder draftBuilder,
            @Nonnull final Set<String> categoryKeys) {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        final CategoryOrderHints categoryOrderHints = draftBuilder.getCategoryOrderHints();

        return categoryService.fetchMatchingCategoriesByKeys(categoryKeys)
            .thenApply(categories ->
                categories.stream().map(category -> {
                    final Reference<Category> categoryReference = category.toReference();
                    if (categoryOrderHints != null) {
                        ofNullable(categoryOrderHints.get(category.getKey()))
                            .ifPresent(orderHintValue -> categoryOrderHintsMap.put(category.getId(), orderHintValue));
                    }
                    return categoryReference;
                }).collect(toList()))
            .thenApply(categoryReferences -> draftBuilder
                .categories(categoryReferences)
                .categoryOrderHints(CategoryOrderHints.of(categoryOrderHintsMap)));
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

    /**
     * Given a {@link ProductDraftBuilder} this method attempts to resolve the tax category to return a
     * {@link CompletionStage} which contains a new instance of the builder with the resolved tax category reference.
     * The key of the tax category reference is either taken from the expanded reference or taken from the value of the
     * id field.
     *
     * @param draftBuilder the productDraft to resolve its tax category reference.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved tax category
     *         reference or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ProductDraftBuilder> resolveTaxCategoryReference(
        @Nonnull final ProductDraftBuilder draftBuilder) {
        return resolveReference(draftBuilder, draftBuilder.getTaxCategory(),
            taxCategoryService::fetchCachedTaxCategoryId, TaxCategory::referenceOfId, ProductDraftBuilder::taxCategory);
    }

    /**
     * Given a {@link ProductDraftBuilder} this method attempts to resolve the state to return a {@link CompletionStage}
     * which contains a new instance of the builder with the resolved state reference. The key of the state reference is
     * either taken from the expanded reference or taken from the value of the id field.
     *
     * @param draftBuilder the productDraft to resolve its state reference.
     * @return a {@link CompletionStage} that contains as a result a new builder instance with resolved state
     *         reference or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ProductDraftBuilder> resolveStateReference(
        @Nonnull final ProductDraftBuilder draftBuilder) {
        return resolveReference(draftBuilder, draftBuilder.getState(),
            stateService::fetchCachedStateId, State::referenceOfId, ProductDraftBuilder::state);
    }

    /**
     * Common function to resolve references from key.
     *
     * @param draftBuilder        {@link ProductDraftBuilder} to update
     * @param reference           reference instance from which key is read
     * @param keyToIdMapper       function which calls respective service to fetch the reference by key
     * @param idToReferenceMapper function which creates {@link Reference} instance from fetched id
     * @param referenceSetter     function which will set the resolved reference to the {@code productDraft}
     * @param <T>                 type of reference (e.g. {@link State}, {@link TaxCategory}
     * @return {@link CompletionStage} containing {@link ProductDraftBuilder} with resolved &lt;T&gt; reference.
     */
    @Nonnull
    private <T> CompletionStage<ProductDraftBuilder> resolveReference(
            @Nonnull final ProductDraftBuilder draftBuilder,
            @Nullable final Reference<T> reference,
            @Nonnull final Function<String, CompletionStage<Optional<String>>> keyToIdMapper,
            @Nonnull final Function<String, Reference<T>> idToReferenceMapper,
            @Nonnull final BiFunction<ProductDraftBuilder, Reference<T>, ProductDraftBuilder> referenceSetter) {

        if (reference == null) {
            return completedFuture(draftBuilder);
        }

        try {
            final String resourceKey = getKeyFromResourceIdentifier(reference, options.shouldAllowUuidKeys());
            return keyToIdMapper.apply(resourceKey)
                .thenApply(optId -> optId
                    .map(idToReferenceMapper)
                    .map(referenceToSet -> referenceSetter.apply(draftBuilder, referenceToSet))
                    .orElse(draftBuilder));
        } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_REFERENCE, reference.getTypeId(), draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
        }
    }

}
