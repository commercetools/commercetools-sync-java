package com.commercetools.sync.products.helpers;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.isReferenceOfType;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.AssetReferenceResolver;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.utils.SyncUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;

public final class VariantReferenceResolver
    extends BaseReferenceResolver<ProductVariantDraft, ProductSyncOptions> {
  private final PriceReferenceResolver priceReferenceResolver;
  private final AssetReferenceResolver assetReferenceResolver;
  private final ProductService productService;
  private final ProductTypeService productTypeService;
  private final CategoryService categoryService;
  private final CustomObjectService customObjectService;

  /**
   * Instantiates a {@link VariantReferenceResolver} instance that could be used to resolve the
   * variants of product drafts in the CTP project specified in the injected {@link
   * ProductSyncOptions} instance.
   *
   * @param productSyncOptions the container of all the options of the sync process including the
   *     CTP project client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the custom types for reference resolution.
   * @param channelService the service to fetch the channels for reference resolution.
   * @param customerGroupService the service to fetch the customer groups for reference resolution.
   * @param productService the service to fetch the products for reference resolution.
   * @param productTypeService the service to fetch the productTypes for reference resolution.
   * @param categoryService the service to fetch the categories for reference resolution.
   * @param customObjectService the service to fetch the custom objects for reference resolution.
   */
  public VariantReferenceResolver(
      @Nonnull final ProductSyncOptions productSyncOptions,
      @Nonnull final TypeService typeService,
      @Nonnull final ChannelService channelService,
      @Nonnull final CustomerGroupService customerGroupService,
      @Nonnull final ProductService productService,
      @Nonnull final ProductTypeService productTypeService,
      @Nonnull final CategoryService categoryService,
      @Nonnull final CustomObjectService customObjectService) {
    super(productSyncOptions);
    this.priceReferenceResolver =
        new PriceReferenceResolver(
            productSyncOptions, typeService, channelService, customerGroupService);
    this.assetReferenceResolver = new AssetReferenceResolver(productSyncOptions, typeService);
    this.productService = productService;
    this.categoryService = categoryService;
    this.productTypeService = productTypeService;
    this.customObjectService = customObjectService;
  }

  /**
   * Given a {@link ProductVariantDraft} this method attempts to resolve the prices, assets and
   * attributes to return a {@link CompletionStage} which contains a new instance of the draft with
   * the resolved references.
   *
   * <p>Note: this method will filter out any null sub resources (e.g. prices, attributes or assets)
   * under the returned resolved variant.
   *
   * @param productVariantDraft the product variant draft to resolve it's references.
   * @return a {@link CompletionStage} that contains as a result a new productDraft instance with
   *     resolved references or, in case an error occurs during reference resolution, a {@link
   *     ReferenceResolutionException}.
   */
  @Override
  public CompletionStage<ProductVariantDraft> resolveReferences(
      @Nonnull final ProductVariantDraft productVariantDraft) {
    return resolvePricesReferences(ProductVariantDraftBuilder.of(productVariantDraft))
        .thenCompose(this::resolveAssetsReferences)
        .thenCompose(this::resolveAttributesReferences)
        .thenApply(ProductVariantDraftBuilder::build);
  }

  @Nonnull
  CompletionStage<ProductVariantDraftBuilder> resolveAssetsReferences(
      @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {

    final List<AssetDraft> productVariantDraftAssets = productVariantDraftBuilder.getAssets();
    if (productVariantDraftAssets == null) {
      return completedFuture(productVariantDraftBuilder);
    }

    return mapValuesToFutureOfCompletedValues(
            productVariantDraftAssets, assetReferenceResolver::resolveReferences, toList())
        .thenApply(productVariantDraftBuilder::assets);
  }

  @Nonnull
  CompletionStage<ProductVariantDraftBuilder> resolvePricesReferences(
      @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {

    final List<PriceDraft> productVariantDraftPrices = productVariantDraftBuilder.getPrices();
    if (productVariantDraftPrices == null) {
      return completedFuture(productVariantDraftBuilder);
    }

    return mapValuesToFutureOfCompletedValues(
            productVariantDraftPrices, priceReferenceResolver::resolveReferences, toList())
        .thenApply(productVariantDraftBuilder::prices);
  }

  @Nonnull
  private CompletionStage<ProductVariantDraftBuilder> resolveAttributesReferences(
      @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {

    final List<AttributeDraft> attributeDrafts = productVariantDraftBuilder.getAttributes();
    if (attributeDrafts == null) {
      return completedFuture(productVariantDraftBuilder);
    }

    return mapValuesToFutureOfCompletedValues(
            attributeDrafts, this::resolveAttributeReference, toList())
        .thenApply(productVariantDraftBuilder::attributes);
  }

  @Nonnull
  private CompletionStage<AttributeDraft> resolveAttributeReference(
      @Nonnull final AttributeDraft attributeDraft) {

    final JsonNode attributeDraftValue = attributeDraft.getValue();

    if (attributeDraftValue == null) {
      return CompletableFuture.completedFuture(attributeDraft);
    }

    final JsonNode attributeDraftValueClone = attributeDraftValue.deepCopy();

    final List<JsonNode> allAttributeReferences =
        attributeDraftValueClone.findParents(REFERENCE_TYPE_ID_FIELD);

    if (!allAttributeReferences.isEmpty()) {
      return mapValuesToFutureOfCompletedValues(
              allAttributeReferences, this::resolveReference, toList())
          .thenApply(
              ignoredResult ->
                  AttributeDraft.of(attributeDraft.getName(), attributeDraftValueClone));
    }

    return CompletableFuture.completedFuture(attributeDraft);
  }

  @Nonnull
  private CompletionStage<Void> resolveReference(@Nonnull final JsonNode referenceValue) {
    return getResolvedId(referenceValue)
        .thenAccept(
            optionalId ->
                optionalId.ifPresent(
                    id -> ((ObjectNode) referenceValue).put(REFERENCE_ID_FIELD, id)));
  }

  @Nonnull
  private CompletionStage<Optional<String>> getResolvedId(@Nonnull final JsonNode referenceValue) {

    if (isReferenceOfType(referenceValue, Product.referenceTypeId())) {
      return getResolvedIdFromKeyInReference(referenceValue, productService::getIdFromCacheOrFetch);
    }

    if (isReferenceOfType(referenceValue, Category.referenceTypeId())) {
      return getResolvedIdFromKeyInReference(
          referenceValue, categoryService::fetchCachedCategoryId);
    }

    if (isReferenceOfType(referenceValue, ProductType.referenceTypeId())) {
      return getResolvedIdFromKeyInReference(
          referenceValue, productTypeService::fetchCachedProductTypeId);
    }

    if (isReferenceOfType(referenceValue, CustomObject.referenceTypeId())) {
      return getResolvedIdFromKeyInReference(referenceValue, this::resolveCustomObjectReference);
    }

    return CompletableFuture.completedFuture(Optional.empty());
  }

  @Nonnull
  private CompletionStage<Optional<String>> getResolvedIdFromKeyInReference(
      @Nonnull final JsonNode referenceValue,
      @Nonnull final Function<String, CompletionStage<Optional<String>>> resolvedIdFetcher) {

    final JsonNode idField = referenceValue.get(REFERENCE_ID_FIELD);
    return idField != null && !Objects.equals(idField, NullNode.getInstance())
        ? resolvedIdFetcher.apply(idField.asText())
        : CompletableFuture.completedFuture(Optional.empty());
  }

  private CompletionStage<Optional<String>> resolveCustomObjectReference(
      @Nonnull final String resolvedIdText) {

    if (SyncUtils.isUuid(resolvedIdText)) {
      return completedFuture(Optional.empty());
    }

    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(resolvedIdText);

    return customObjectService.fetchCachedCustomObjectId(customObjectCompositeIdentifier);
  }
}
