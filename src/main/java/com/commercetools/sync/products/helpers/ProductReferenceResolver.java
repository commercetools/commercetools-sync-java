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
import java.util.ArrayList;
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

//todo (ahmetoz) all javadocs needs to be refactored with the new requirements.
public final class ProductReferenceResolver extends BaseReferenceResolver<ProductDraft, ProductSyncOptions> {
    private final ProductTypeService productTypeService;
    private final CategoryService categoryService;
    private final VariantReferenceResolver variantReferenceResolver;
    private final TaxCategoryService taxCategoryService;
    private final StateService stateService;

    public static final String FAILED_TO_RESOLVE_REFERENCE = "Failed to resolve '%s' resource identifier on "
        + "ProductDraft with key:'%s'. Reason: %s";
    public static final String PRODUCT_TYPE_DOES_NOT_EXIST = "Product type with key '%s' doesn't exist.";
    public static final String TAX_CATEGORY_DOES_NOT_EXIST = "TaxCategory with key '%s' doesn't exist.";
    public static final String CATEGORIES_DO_NOT_EXIST = "Categories with keys '%s' don't exist.";

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
                productService, productTypeService, categoryService);
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

        final ResourceIdentifier<ProductType> productTypeReference = draftBuilder.getProductType();
        if (productTypeReference != null && productTypeReference.getId() == null) {
            String productTypeKey;
            try {
                productTypeKey = getKeyFromResourceIdentifier(productTypeReference);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_REFERENCE, ProductType.referenceTypeId(), draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
            }

            return fetchAndResolveProductTypeReference(draftBuilder, productTypeKey);
        }
        return completedFuture(draftBuilder);
    }

    /**
     * Given a {@link ProductDraftBuilder} and a {@code productTypeKey} this method fetches the actual id of the
     * product type corresponding to this key, ideally from a cache. Then it sets this id on the product type reference
     * id.
     *
     * @param draftBuilder   the product draft builder to accept resolved references values.
     * @param productTypeKey the product type key of to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} product draft builder
     *         instance with resolved product type reference or an exception.
     */
    @Nonnull
    private CompletionStage<ProductDraftBuilder> fetchAndResolveProductTypeReference(
        @Nonnull final ProductDraftBuilder draftBuilder,
        @Nonnull final String productTypeKey) {

        return productTypeService
            .fetchCachedProductTypeId(productTypeKey)
            .thenCompose(resolvedProductTypeIdOptional -> resolvedProductTypeIdOptional
                .map(resolvedProductTypeId ->
                    completedFuture(draftBuilder.productType(
                        ProductType.referenceOfId(resolvedProductTypeId).toResourceIdentifier())))
                .orElseGet(() -> {
                    final String errorMessage = format(PRODUCT_TYPE_DOES_NOT_EXIST, productTypeKey);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_REFERENCE, ProductType.referenceTypeId(), draftBuilder.getKey(),
                            errorMessage)));
                }));
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
        final List<ResourceIdentifier<Category>> directCategoryResourceIdentifiers = new ArrayList<>();
        for (ResourceIdentifier<Category> categoryResourceIdentifier : categoryResourceIdentifiers) {
            if (categoryResourceIdentifier != null && categoryResourceIdentifier.getId() == null) {
                try {
                    final String categoryKey = getKeyFromResourceIdentifier(categoryResourceIdentifier);
                    categoryKeys.add(categoryKey);
                } catch (ReferenceResolutionException referenceResolutionException) {
                    return exceptionallyCompletedFuture(
                        new ReferenceResolutionException(
                            format(FAILED_TO_RESOLVE_REFERENCE, Category.referenceTypeId(),
                                draftBuilder.getKey(), referenceResolutionException.getMessage())));
                }
            } else {
                directCategoryResourceIdentifiers.add(categoryResourceIdentifier);
            }
        }
        return fetchAndResolveCategoryReferences(draftBuilder, categoryKeys, directCategoryResourceIdentifiers);
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
     *         references or an exception.
     */
    @Nonnull
    private CompletionStage<ProductDraftBuilder> fetchAndResolveCategoryReferences(
        @Nonnull final ProductDraftBuilder draftBuilder,
        @Nonnull final Set<String> categoryKeys,
        @Nonnull final List<ResourceIdentifier<Category>> directCategoryReferences) {

        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        final CategoryOrderHints categoryOrderHints = draftBuilder.getCategoryOrderHints();
        final Map<String, Category> keyToCategory = new HashMap<>();
        return categoryService
            .fetchMatchingCategoriesByKeys(categoryKeys)
            .thenApply(categories -> categories
                .stream()
                .map(category -> {
                    keyToCategory.put(category.getKey(), category);
                    if (categoryOrderHints != null) {
                        // todo (ahmetoz), what about the direct references ?
                        ofNullable(categoryOrderHints.get(category.getKey()))
                            .ifPresent(orderHintValue ->
                                categoryOrderHintsMap.put(category.getId(), orderHintValue));
                    }

                    return Category.referenceOfId(category.getId()).toResourceIdentifier();
                })
                .collect(toSet()))
            .thenCompose(categoryReferences -> {
                String keysNotExists = categoryKeys
                    .stream()
                    .filter(categoryKey -> !keyToCategory.containsKey(categoryKey))
                    .collect(joining(", "));

                if (!isBlank(keysNotExists)) {
                    final String errorMessage = format(CATEGORIES_DO_NOT_EXIST, keysNotExists);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_REFERENCE, Category.resourceTypeId(), draftBuilder.getKey(),
                            errorMessage)));
                }

                categoryReferences.addAll(directCategoryReferences);

                return completedFuture(draftBuilder
                    .categories(categoryReferences)
                    .categoryOrderHints(CategoryOrderHints.of(categoryOrderHintsMap)));
            });
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
        final ResourceIdentifier<TaxCategory> taxCategoryResourceIdentifier =
            draftBuilder.getTaxCategory();
        if (taxCategoryResourceIdentifier != null && taxCategoryResourceIdentifier.getId() == null) {
            String taxCategoryKey;
            try {
                taxCategoryKey = getKeyFromResourceIdentifier(taxCategoryResourceIdentifier);
            } catch (ReferenceResolutionException referenceResolutionException) {
                return exceptionallyCompletedFuture(new ReferenceResolutionException(
                    format(FAILED_TO_RESOLVE_REFERENCE, TaxCategory.referenceTypeId(), draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
            }

            return fetchAndResolveTaxCategoryReference(draftBuilder, taxCategoryKey);
        }
        return completedFuture(draftBuilder);
    }

    /**
     * Given a {@link ProductDraftBuilder} and a {@code taxCategoryKey} this method fetches the actual id of the
     * product type corresponding to this key, ideally from a cache. Then it sets this id on the product type reference
     * id. // todo (ahmet) complete the docs.
     *
     * @param draftBuilder   the product draft builder to accept resolved references values.
     * @param taxCategoryKey the tax category type key of to resolve it's actual id on the draft.
     * @return a {@link CompletionStage} that contains as a result the same {@code draftBuilder} product draft builder
     *         instance with resolved tax category type reference or an exception.
     */
    @Nonnull
    private CompletionStage<ProductDraftBuilder> fetchAndResolveTaxCategoryReference(
        @Nonnull final ProductDraftBuilder draftBuilder,
        @Nonnull final String taxCategoryKey) {

        return taxCategoryService
            .fetchCachedTaxCategoryId(taxCategoryKey)
            .thenCompose(resolvedTaxCategoryIdOptional -> resolvedTaxCategoryIdOptional
                .map(resolvedTaxCategoryId ->
                    completedFuture(draftBuilder.taxCategory(
                        TaxCategory.referenceOfId(resolvedTaxCategoryId).toResourceIdentifier())))
                .orElseGet(() -> {
                    final String errorMessage = format(TAX_CATEGORY_DOES_NOT_EXIST, taxCategoryKey);
                    return exceptionallyCompletedFuture(new ReferenceResolutionException(
                        format(FAILED_TO_RESOLVE_REFERENCE, ProductType.referenceTypeId(), draftBuilder.getKey(),
                            errorMessage)));
                }));
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
        try {
            return resolveResourceIdentifier(draftBuilder, draftBuilder.getState(),
                stateService::fetchCachedStateId, State::referenceOfId, ProductDraftBuilder::state);
        } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(new ReferenceResolutionException(
                format(FAILED_TO_RESOLVE_REFERENCE, State.referenceTypeId(), draftBuilder.getKey(),
                    referenceResolutionException.getMessage())));
        }
    }

    /**
     * Common function to resolve references from key.
     *
     * @param draftBuilder                 {@link ProductDraftBuilder} to update
     * @param resourceIdentifier           resourceIdentifier instance from which key is read
     * @param keyToIdMapper                function which calls respective service to fetch the reference by key
     * @param idToResourceIdentifierMapper function which creates {@link ResourceIdentifier} instance from fetched id
     * @param resourceIdentifierSetter     function which will set the resolved reference to the {@code productDraft}
     * @param <T>                          type of reference (e.g. {@link State}, {@link TaxCategory}
     * @return {@link CompletionStage} containing {@link ProductDraftBuilder} with resolved &lt;T&gt; reference.
     */
    @Nonnull
    private <T, S extends ResourceIdentifier<T>> CompletionStage<ProductDraftBuilder> resolveResourceIdentifier(
        @Nonnull final ProductDraftBuilder draftBuilder,
        @Nullable final S resourceIdentifier,
        @Nonnull final Function<String, CompletionStage<Optional<String>>> keyToIdMapper,
        @Nonnull final Function<String, S> idToResourceIdentifierMapper,
        @Nonnull final BiFunction<ProductDraftBuilder, S, ProductDraftBuilder> resourceIdentifierSetter)
        throws ReferenceResolutionException {

        if (resourceIdentifier == null) {
            return completedFuture(draftBuilder);
        }

        final String resourceKey = getIdFromResourceIdentifier(resourceIdentifier);

        return keyToIdMapper
            .apply(resourceKey)
            .thenApply(optId -> optId
                .map(idToResourceIdentifierMapper)
                .map(resourceIdentifierToSet -> resourceIdentifierSetter.apply(draftBuilder, resourceIdentifierToSet))
                .orElse(draftBuilder));
    }

}
