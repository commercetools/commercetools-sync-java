package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.AssetReferenceResolver;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;


public final class VariantReferenceResolver extends BaseReferenceResolver<ProductVariantDraft, ProductSyncOptions> {
    private final PriceReferenceResolver priceReferenceResolver;
    private final AssetReferenceResolver assetReferenceResolver;
    private final ProductService productService;
    private final CategoryService categoryService;

    public static final String REFERENCE_TYPE_ID_FIELD = "typeId";
    public static final String REFERENCE_ID_FIELD = "id";

    /**
     * Takes a {@link ProductSyncOptions} instance, {@link TypeService}, a {@link ChannelService}, a
     * {@link CustomerGroupService} and a {@link ProductService} to instantiate a {@link VariantReferenceResolver}
     * instance that could be used to resolve the variants of product drafts in the CTP project specified in the
     * injected {@link ProductSyncOptions} instance.
     *
     * @param productSyncOptions   the container of all the options of the sync process including the CTP project client
     *                             and/or configuration and other sync-specific options.
     * @param typeService          the service to fetch the custom types for reference resolution.
     * @param channelService       the service to fetch the channels for reference resolution.
     * @param customerGroupService the service to fetch the customer groups for reference resolution.
     * @param productService       the service to fetch the products for reference resolution.
     * @param categoryService      the service to fetch the categories for reference resolution.
     */
    public VariantReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final TypeService typeService,
                                    @Nonnull final ChannelService channelService,
                                    @Nonnull final CustomerGroupService customerGroupService,
                                    @Nonnull final ProductService productService,
                                    @Nonnull final CategoryService categoryService) {
        super(productSyncOptions);
        this.priceReferenceResolver = new PriceReferenceResolver(productSyncOptions, typeService, channelService,
            customerGroupService);
        this.assetReferenceResolver = new AssetReferenceResolver(productSyncOptions, typeService);
        this.productService = productService;
        this.categoryService = categoryService;
    }


    /**
     * Given a {@link ProductVariantDraft} this method attempts to resolve the prices and attributes to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param productVariantDraft the product variant draft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new productDraft instance with resolved references
     *         or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
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

        return mapValuesToFutureOfCompletedValues(productVariantDraftAssets,
            assetReferenceResolver::resolveReferences, toList()).thenApply(productVariantDraftBuilder::assets);
    }

    @Nonnull
    CompletionStage<ProductVariantDraftBuilder> resolvePricesReferences(
        @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {

        final List<PriceDraft> productVariantDraftPrices = productVariantDraftBuilder.getPrices();
        if (productVariantDraftPrices == null) {
            return completedFuture(productVariantDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(productVariantDraftPrices,
            priceReferenceResolver::resolveReferences, toList())
            .thenApply(productVariantDraftBuilder::prices);
    }

    @Nonnull
    private CompletionStage<ProductVariantDraftBuilder> resolveAttributesReferences(
        @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {

        final List<AttributeDraft> attributeDrafts = productVariantDraftBuilder.getAttributes();
        if (attributeDrafts == null) {
            return completedFuture(productVariantDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(attributeDrafts, this::resolveAttributeReference, toList())
                                     .thenApply(productVariantDraftBuilder::attributes);
    }

    @Nonnull
    private CompletionStage<AttributeDraft> resolveAttributeReference(@Nonnull final AttributeDraft attributeDraft) {

        final JsonNode attributeDraftValue = attributeDraft.getValue();

        if (attributeDraftValue == null) {
            return CompletableFuture.completedFuture(attributeDraft);
        }

        final JsonNode attributeDraftValueClone = attributeDraftValue.deepCopy();

        final List<JsonNode> allAttributeReferences = attributeDraftValueClone.findParents(REFERENCE_TYPE_ID_FIELD);

        if (!allAttributeReferences.isEmpty()) {
            return mapValuesToFutureOfCompletedValues(allAttributeReferences, this::resolveReference, toList())
                .thenApply(ignoredResult -> AttributeDraft.of(attributeDraft.getName(), attributeDraftValueClone));
        }

        return CompletableFuture.completedFuture(attributeDraft);
    }

    @Nonnull
    private CompletionStage<Void> resolveReference(@Nonnull final JsonNode referenceValue) {

        if (isProductReference(referenceValue)) {
            return getResolvedIdFromKeyInReference(referenceValue, productService::getIdFromCacheOrFetch)
                .thenAccept(productIdOptional ->
                    productIdOptional.ifPresent(id -> ((ObjectNode) referenceValue).put(REFERENCE_ID_FIELD, id)));
        }

        if (isCategoryReference(referenceValue)) {
            return getResolvedIdFromKeyInReference(referenceValue, categoryService::fetchCachedCategoryId)
                .thenAccept(categoryIdOptional ->
                    categoryIdOptional.ifPresent(id -> ((ObjectNode) referenceValue).put(REFERENCE_ID_FIELD, id)));
        }

        return CompletableFuture.completedFuture(null);
    }

    static boolean isProductReference(@Nonnull final JsonNode referenceValue) {
        return getReferenceTypeId(referenceValue)
            .map(referenceTypeId -> Objects.equals(referenceTypeId, Product.referenceTypeId()))
            .orElse(false);
    }

    private static boolean isCategoryReference(@Nonnull final JsonNode referenceValue) {
        return getReferenceTypeId(referenceValue)
            .map(referenceTypeId -> Objects.equals(referenceTypeId, Category.referenceTypeId()))
            .orElse(false);
    }

    @Nonnull
    private static Optional<String> getReferenceTypeId(@Nonnull final JsonNode referenceValue) {
        final JsonNode typeId = referenceValue.get(REFERENCE_TYPE_ID_FIELD);
        return Optional.ofNullable(typeId).map(JsonNode::asText);
    }

    @Nonnull
    private CompletionStage<Optional<String>> getResolvedIdFromKeyInReference(
        @Nonnull final JsonNode referenceValue,
        @Nonnull final Function<String, CompletionStage<Optional<String>>> cacheFetcher) {

        final JsonNode idField = referenceValue.get(REFERENCE_ID_FIELD);

        return idField != null && !Objects.equals(idField, NullNode.getInstance())
            ? cacheFetcher.apply(idField.asText())
            : CompletableFuture.completedFuture(Optional.empty());
    }
}

