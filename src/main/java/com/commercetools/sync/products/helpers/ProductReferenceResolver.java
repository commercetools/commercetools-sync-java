package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.spotify.futures.CompletableFutures.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifier;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class ProductReferenceResolver
    extends BaseReferenceResolver<ProductDraft, ProductSyncOptions> {
  private final ProductTypeService productTypeService;
  private final CategoryService categoryService;
  private final VariantReferenceResolver variantReferenceResolver;
  private final TaxCategoryService taxCategoryService;
  private final StateService stateService;
  private final TypeService typeService;
  private final ChannelService channelService;
  private final ProductService productService;
  private final CustomerGroupService customerGroupService;
  private final CustomObjectService customObjectService;
  private final CustomerService customerService;

  public static final String FAILED_TO_RESOLVE_REFERENCE =
      "Failed to resolve '%s' resource identifier on " + "ProductDraft with key:'%s'. Reason: %s";
  public static final String PRODUCT_TYPE_DOES_NOT_EXIST =
      "Product type with key '%s' doesn't exist.";
  public static final String TAX_CATEGORY_DOES_NOT_EXIST =
      "TaxCategory with key '%s' doesn't exist.";
  public static final String CATEGORIES_DO_NOT_EXIST = "Categories with keys '%s' don't exist.";
  public static final String STATE_DOES_NOT_EXIST = "State with key '%s' doesn't exist.";

  /**
   * Takes a {@link ProductSyncOptions} instance, a {@link ProductTypeService}, a {@link
   * CategoryService}, a {@link TypeService}, a {@link ChannelService}, a {@link
   * CustomerGroupService}, a {@link TaxCategoryService}, a {@link StateService} and a {@link
   * ProductService} to instantiate a {@link ProductReferenceResolver} instance that could be used
   * to resolve the product type, categories, variants, tax category and product state references of
   * product drafts in the CTP project specified in the injected {@link ProductSyncOptions}
   * instance.
   *
   * @param productSyncOptions the container of all the options of the sync process including the
   *     CTP project client and/or configuration and other sync-specific options.
   * @param productTypeService the service to fetch the product type for reference resolution.
   * @param categoryService the service to fetch the categories for reference resolution.
   * @param typeService the service to fetch the custom types for reference resolution.
   * @param channelService the service to fetch the channels for reference resolution.
   * @param customerGroupService the service to fetch the customer groups for reference resolution.
   * @param taxCategoryService the service to fetch tax categories for reference resolution.
   * @param stateService the service to fetch product states for reference resolution.
   * @param productService the service to fetch products for product reference resolution on
   *     reference attributes.
   * @param customObjectService the service to fetch custom objects for reference resolution.
   * @param customerService the service to fetch customers for reference resolution.
   */
  public ProductReferenceResolver(
      @Nonnull final ProductSyncOptions productSyncOptions,
      @Nonnull final ProductTypeService productTypeService,
      @Nonnull final CategoryService categoryService,
      @Nonnull final TypeService typeService,
      @Nonnull final ChannelService channelService,
      @Nonnull final CustomerGroupService customerGroupService,
      @Nonnull final TaxCategoryService taxCategoryService,
      @Nonnull final StateService stateService,
      @Nonnull final ProductService productService,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final CustomerService customerService) {
    super(productSyncOptions);
    this.productTypeService = productTypeService;
    this.categoryService = categoryService;
    this.taxCategoryService = taxCategoryService;
    this.stateService = stateService;
    this.typeService = typeService;
    this.channelService = channelService;
    this.productService = productService;
    this.customerGroupService = customerGroupService;
    this.customObjectService = customObjectService;
    this.customerService = customerService;
    this.variantReferenceResolver =
        new VariantReferenceResolver(
            productSyncOptions,
            typeService,
            channelService,
            customerGroupService,
            productService,
            productTypeService,
            categoryService,
            customObjectService,
            stateService,
            customerService);
  }

  /**
   * Given a {@link ProductDraft} this method attempts to resolve the product type, categories,
   * variants, tax category and product state references to return a {@link
   * java.util.concurrent.CompletionStage} which contains a new instance of the draft with the
   * resolved references.
   *
   * @param productDraft the productDraft to resolve it's references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new
   *     productDraft instance with resolved references or, in case an error occurs during reference
   *     resolution, a {@link
   *     com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
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
    if (masterVariantDraft == null) {
      return CompletableFuture.completedFuture(draftBuilder);
    } else {
      return variantReferenceResolver
          .resolveReferences(masterVariantDraft)
          .thenApply(draftBuilder::masterVariant)
          .thenCompose(this::resolveVariantsReferences);
    }
  }

  @Nonnull
  private CompletionStage<ProductDraftBuilder> resolveVariantsReferences(
      @Nonnull final ProductDraftBuilder draftBuilder) {
    final List<ProductVariantDraft> productDraftVariants = draftBuilder.getVariants();

    if (productDraftVariants == null) {
      return CompletableFuture.completedFuture(draftBuilder);
    }
    return mapValuesToFutureOfCompletedValues(
            productDraftVariants, variantReferenceResolver::resolveReferences, toList())
        .thenApply(draftBuilder::variants);
  }

  /**
   * Given a {@link ProductDraftBuilder} this method attempts to resolve the product type to return
   * a {@link java.util.concurrent.CompletionStage} which contains a new instance of the builder
   * with the resolved product type reference.
   *
   * @param draftBuilder the productDraft to resolve its product type reference.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new builder
   *     instance with resolved product type reference or, in case an error occurs during reference
   *     resolution, a {@link
   *     com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
   */
  @Nonnull
  public CompletionStage<ProductDraftBuilder> resolveProductTypeReference(
      @Nonnull final ProductDraftBuilder draftBuilder) {

    final ProductTypeResourceIdentifier productTypeReference = draftBuilder.getProductType();
    if (productTypeReference != null && isBlank(productTypeReference.getId())) {
      String productTypeKey;
      try {
        productTypeKey = getKeyFromResourceIdentifier(productTypeReference);
      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(
            new ReferenceResolutionException(
                format(
                    FAILED_TO_RESOLVE_REFERENCE,
                    ProductTypeReference.PRODUCT_TYPE,
                    draftBuilder.getKey(),
                    referenceResolutionException.getMessage())));
      }

      return fetchAndResolveProductTypeReference(draftBuilder, productTypeKey);
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private CompletionStage<ProductDraftBuilder> fetchAndResolveProductTypeReference(
      @Nonnull final ProductDraftBuilder draftBuilder, @Nonnull final String productTypeKey) {

    return productTypeService
        .fetchCachedProductTypeId(productTypeKey)
        .thenCompose(
            resolvedProductTypeIdOptional ->
                resolvedProductTypeIdOptional
                    .map(
                        resolvedProductTypeId ->
                            completedFuture(
                                draftBuilder.productType(
                                    ProductTypeResourceIdentifierBuilder.of()
                                        .id(resolvedProductTypeId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(PRODUCT_TYPE_DOES_NOT_EXIST, productTypeKey);
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(
                                  format(
                                      FAILED_TO_RESOLVE_REFERENCE,
                                      ProductTypeReference.PRODUCT_TYPE,
                                      draftBuilder.getKey(),
                                      errorMessage)));
                        }));
  }

  /**
   * Given a {@link ProductDraftBuilder} this method attempts to resolve the categories and
   * categoryOrderHints to return a {@link java.util.concurrent.CompletionStage} which contains a
   * new instance of the builder with the resolved references.
   *
   * @param draftBuilder the productDraft to resolve its category and categoryOrderHints references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new builder
   *     instance with resolved references or, in case an error occurs during reference resolution,
   *     a {@link com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
   */
  @Nonnull
  public CompletionStage<ProductDraftBuilder> resolveCategoryReferences(
      @Nonnull final ProductDraftBuilder draftBuilder) {

    final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
        draftBuilder.getCategories();
    if (categoryResourceIdentifiers == null) {
      return CompletableFuture.completedFuture(draftBuilder);
    }

    final Set<String> categoryKeys = new HashSet<>();
    final List<CategoryResourceIdentifier> directCategoryResourceIdentifiers = new ArrayList<>();
    for (CategoryResourceIdentifier categoryResourceIdentifier : categoryResourceIdentifiers) {
      if (categoryResourceIdentifier != null && isBlank(categoryResourceIdentifier.getId())) {
        try {
          final String categoryKey = getKeyFromResourceIdentifier(categoryResourceIdentifier);
          categoryKeys.add(categoryKey);
        } catch (ReferenceResolutionException referenceResolutionException) {
          return exceptionallyCompletedFuture(
              new ReferenceResolutionException(
                  format(
                      FAILED_TO_RESOLVE_REFERENCE,
                      CategoryReference.CATEGORY,
                      draftBuilder.getKey(),
                      referenceResolutionException.getMessage())));
        }
      } else {
        directCategoryResourceIdentifiers.add(categoryResourceIdentifier);
      }
    }
    return fetchAndResolveCategoryReferences(
        draftBuilder, categoryKeys, directCategoryResourceIdentifiers);
  }

  @Nonnull
  private CompletionStage<ProductDraftBuilder> fetchAndResolveCategoryReferences(
      @Nonnull final ProductDraftBuilder draftBuilder,
      @Nonnull final Set<String> categoryKeys,
      @Nonnull final List<CategoryResourceIdentifier> directCategoryReferences) {

    final Map<String, String> categoryOrderHintsMap = new HashMap<>();
    final CategoryOrderHints categoryOrderHints = draftBuilder.getCategoryOrderHints();
    final Map<String, Category> keyToCategory = new HashMap<>();
    return categoryService
        .fetchMatchingCategoriesByKeys(categoryKeys)
        .thenApply(
            categories ->
                categories.stream()
                    .map(
                        category -> {
                          keyToCategory.put(category.getKey(), category);
                          if (categoryOrderHints != null && categoryOrderHints.values() != null) {
                            ofNullable(categoryOrderHints.values().get(category.getKey()))
                                .ifPresent(
                                    orderHintValue ->
                                        categoryOrderHintsMap.put(
                                            category.getId(), orderHintValue));
                          }

                          return CategoryResourceIdentifierBuilder.of()
                              .id(category.getId())
                              .build();
                        })
                    .collect(toList()))
        .thenCompose(
            categoryReferences -> {
              String keysNotExists =
                  categoryKeys.stream()
                      .filter(categoryKey -> !keyToCategory.containsKey(categoryKey))
                      .collect(joining(", "));

              if (!isBlank(keysNotExists)) {
                final String errorMessage = format(CATEGORIES_DO_NOT_EXIST, keysNotExists);
                return exceptionallyCompletedFuture(
                    new ReferenceResolutionException(
                        format(
                            FAILED_TO_RESOLVE_REFERENCE,
                            CategoryReference.CATEGORY,
                            draftBuilder.getKey(),
                            errorMessage)));
              }

              categoryReferences.addAll(directCategoryReferences);

              return completedFuture(
                  draftBuilder
                      .categories(categoryReferences)
                      .categoryOrderHints(
                          CategoryOrderHintsBuilder.of().values(categoryOrderHintsMap).build()));
            });
  }

  /**
   * Given a {@link ProductDraftBuilder} this method attempts to resolve the tax category to return
   * a {@link java.util.concurrent.CompletionStage} which contains a new instance of the builder
   * with the resolved tax category reference.
   *
   * @param draftBuilder the productDraft to resolve its tax category reference.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new builder
   *     instance with resolved tax category reference or, in case an error occurs during reference
   *     resolution, a {@link
   *     com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
   */
  @Nonnull
  public CompletionStage<ProductDraftBuilder> resolveTaxCategoryReference(
      @Nonnull final ProductDraftBuilder draftBuilder) {
    final TaxCategoryResourceIdentifier taxCategoryResourceIdentifier =
        draftBuilder.getTaxCategory();
    if (taxCategoryResourceIdentifier != null && isBlank(taxCategoryResourceIdentifier.getId())) {
      String taxCategoryKey;
      try {
        taxCategoryKey = getKeyFromResourceIdentifier(taxCategoryResourceIdentifier);
      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(
            new ReferenceResolutionException(
                format(
                    FAILED_TO_RESOLVE_REFERENCE,
                    TaxCategoryReference.TAX_CATEGORY,
                    draftBuilder.getKey(),
                    referenceResolutionException.getMessage())));
      }

      return fetchAndResolveTaxCategoryReference(draftBuilder, taxCategoryKey);
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private CompletionStage<ProductDraftBuilder> fetchAndResolveTaxCategoryReference(
      @Nonnull final ProductDraftBuilder draftBuilder, @Nonnull final String taxCategoryKey) {

    return taxCategoryService
        .fetchCachedTaxCategoryId(taxCategoryKey)
        .thenCompose(
            resolvedTaxCategoryIdOptional ->
                resolvedTaxCategoryIdOptional
                    .map(
                        resolvedTaxCategoryId ->
                            completedFuture(
                                draftBuilder.taxCategory(
                                    TaxCategoryResourceIdentifierBuilder.of()
                                        .id(resolvedTaxCategoryId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(TAX_CATEGORY_DOES_NOT_EXIST, taxCategoryKey);
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(
                                  format(
                                      FAILED_TO_RESOLVE_REFERENCE,
                                      TaxCategoryReference.TAX_CATEGORY,
                                      draftBuilder.getKey(),
                                      errorMessage)));
                        }));
  }

  /**
   * Given a {@link ProductDraftBuilder} this method attempts to resolve the state to return a
   * {@link java.util.concurrent.CompletionStage} which contains a new instance of the builder with
   * the resolved state reference.
   *
   * <p>Note: The key of the state reference taken from the value of the id field of the reference.
   *
   * @param draftBuilder the productDraft to resolve its state reference.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new builder
   *     instance with resolved state reference or, in case an error occurs during reference
   *     resolution, the future is completed exceptionally with a {@link
   *     com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
   */
  @Nonnull
  public CompletionStage<ProductDraftBuilder> resolveStateReference(
      @Nonnull final ProductDraftBuilder draftBuilder) {
    final StateResourceIdentifier stateReference = draftBuilder.getState();
    if (stateReference != null && isBlank(stateReference.getId())) {
      String stateKey;
      try {
        stateKey = getKeyFromResourceIdentifier(stateReference);
      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(
            new ReferenceResolutionException(
                format(
                    FAILED_TO_RESOLVE_REFERENCE,
                    StateReference.STATE,
                    draftBuilder.getKey(),
                    referenceResolutionException.getMessage())));
      }

      return fetchAndResolveStateReference(draftBuilder, stateKey);
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private CompletionStage<ProductDraftBuilder> fetchAndResolveStateReference(
      @Nonnull final ProductDraftBuilder draftBuilder, @Nonnull final String stateKey) {

    return stateService
        .fetchCachedStateId(stateKey)
        .thenCompose(
            resolvedStateIdOptional ->
                resolvedStateIdOptional
                    .map(
                        resolvedStateId ->
                            completedFuture(
                                draftBuilder.state(
                                    StateResourceIdentifierBuilder.of()
                                        .id(resolvedStateId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage = format(STATE_DOES_NOT_EXIST, stateKey);
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(
                                  format(
                                      FAILED_TO_RESOLVE_REFERENCE,
                                      StateReference.STATE,
                                      draftBuilder.getKey(),
                                      errorMessage)));
                        }));
  }

  /**
   * Calls the {@code cacheKeysToIds} service methods to fetch all the referenced keys (i.e product
   * type, product attribute) from the commercetools to populate caches for the reference
   * resolution.
   *
   * <p>Note: This method is meant be only used internally by the library to improve performance.
   *
   * @param referencedKeys a wrapper for the product references to fetch and cache the id's for.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&lt;{@link
   *     String}&gt;{@link String}&gt;&gt; in which the results of it's completions contains a map
   *     of requested references keys -&gt; ids of product references.
   */
  @Nonnull
  public CompletableFuture<Map<String, String>> populateKeyToIdCachesForReferencedKeys(
      @Nonnull final ProductBatchValidator.ReferencedKeys referencedKeys) {

    final List<CompletionStage<Map<String, String>>> futures = new ArrayList<>();

    final Set<String> productKeys = referencedKeys.getProductKeys();
    if (!productKeys.isEmpty()) {
      futures.add(productService.cacheKeysToIds(productKeys));
    }

    final Set<String> productTypeKeys = referencedKeys.getProductTypeKeys();
    if (!productTypeKeys.isEmpty()) {
      futures.add(productTypeService.cacheKeysToIds(productTypeKeys));
    }

    final Set<String> categoryKeys = referencedKeys.getCategoryKeys();
    if (!categoryKeys.isEmpty()) {
      futures.add(categoryService.cacheKeysToIds(categoryKeys));
    }

    final Set<String> taxCategoryKeys = referencedKeys.getTaxCategoryKeys();
    if (!taxCategoryKeys.isEmpty()) {
      futures.add(taxCategoryService.cacheKeysToIds(taxCategoryKeys));
    }

    final Set<String> typeKeys = referencedKeys.getTypeKeys();
    if (!typeKeys.isEmpty()) {
      futures.add(typeService.cacheKeysToIds(typeKeys));
    }

    final Set<String> channelKeys = referencedKeys.getChannelKeys();
    if (!channelKeys.isEmpty()) {
      futures.add(channelService.cacheKeysToIds(typeKeys));
    }

    final Set<String> stateKeys = referencedKeys.getStateKeys();
    if (!stateKeys.isEmpty()) {
      futures.add(stateService.cacheKeysToIds(stateKeys));
    }

    final Set<String> customerGroupKeys = referencedKeys.getCustomerGroupKeys();
    if (!customerGroupKeys.isEmpty()) {
      futures.add(customerGroupService.cacheKeysToIds(customerGroupKeys));
    }

    final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers =
        referencedKeys.getCustomObjectCompositeIdentifiers();
    if (!customObjectCompositeIdentifiers.isEmpty()) {
      futures.add(customObjectService.cacheKeysToIds(customObjectCompositeIdentifiers));
    }

    return collectionOfFuturesToFutureOfCollection(futures, toList())
        .thenCompose(
            ignored -> productService.cacheKeysToIds(Collections.emptySet())); // return all cache.
  }
}
