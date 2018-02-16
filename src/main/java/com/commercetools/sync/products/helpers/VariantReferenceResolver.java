package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.concurrent.CompletableFuture.completedFuture;


public final class VariantReferenceResolver extends BaseReferenceResolver<ProductVariantDraft, ProductSyncOptions> {
    private final PriceReferenceResolver priceReferenceResolver;
    private final ProductService productService;

    public static final String REFERENCE_TYPE_ID_FIELD = "typeId";
    public static final String REFERENCE_ID_FIELD = "id";

    /**
     * Takes a {@link ProductSyncOptions} instance, {@link TypeService}, a {@link ChannelService} and a
     * {@link ProductService} to instantiate a {@link VariantReferenceResolver} instance that could be used to resolve
     * the prices and variants of variant drafts in the CTP project specified in the injected {@link ProductSyncOptions}
     * instance.
     *
     * @param productSyncOptions the container of all the options of the sync process including the CTP project client
     *                           and/or configuration and other sync-specific options.
     * @param typeService        the service to fetch the custom types for reference resolution.
     * @param channelService     the service to fetch the channels for reference resolution.
     * @param productService     the service to fetch the products for reference resolution.
     */
    public VariantReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final TypeService typeService,
                                    @Nonnull final ChannelService channelService,
                                    @Nonnull final ProductService productService) {
        super(productSyncOptions);
        this.priceReferenceResolver = new PriceReferenceResolver(productSyncOptions, typeService, channelService);
        this.productService = productService;
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
            .thenCompose(this::resolveAttributesReferences)
            .thenApply(ProductVariantDraftBuilder::build);
    }

    CompletionStage<ProductVariantDraftBuilder> resolvePricesReferences(
        @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {
        final List<PriceDraft> productVariantDraftPrices = productVariantDraftBuilder.getPrices();
        if (productVariantDraftPrices == null) {
            return completedFuture(productVariantDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(productVariantDraftPrices, priceReferenceResolver::resolveReferences)
            .thenApply(productVariantDraftBuilder::prices);
    }

    CompletionStage<ProductVariantDraftBuilder> resolveAttributesReferences(
        @Nonnull final ProductVariantDraftBuilder productVariantDraftBuilder) {
        final List<AttributeDraft> attributeDrafts = productVariantDraftBuilder.getAttributes();
        if (attributeDrafts == null) {
            return completedFuture(productVariantDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(attributeDrafts, this::resolveAttributeReference)
            .thenApply(productVariantDraftBuilder::attributes);
    }

    CompletionStage<AttributeDraft> resolveAttributeReference(@Nonnull final AttributeDraft attributeDraft) {
        final JsonNode attributeDraftValue = attributeDraft.getValue();
        if (attributeDraftValue == null) {
            return CompletableFuture.completedFuture(attributeDraft);
        }
        if (attributeDraftValue.isArray()) {
            return resolveAttributeSetReferences(attributeDraft);
        } else {
            if (isProductReference(attributeDraftValue)) {
                return getResolvedIdFromKeyInReference(attributeDraftValue)
                    .thenApply(productIdOptional ->
                        productIdOptional.map(productId ->
                            AttributeDraft.of(attributeDraft.getName(), createProductReferenceJson(productId)))
                                         .orElse(attributeDraft));
            }
            return CompletableFuture.completedFuture(attributeDraft);
        }
    }

    private CompletionStage<AttributeDraft> resolveAttributeSetReferences(
        @Nonnull final AttributeDraft attributeDraft) {
        final JsonNode attributeDraftValue = attributeDraft.getValue();
        final Spliterator<JsonNode> attributeReferencesIterator = attributeDraftValue.spliterator();

        final Stream<JsonNode> attributeReferenceStream = StreamSupport.stream(attributeReferencesIterator, false)
                                                                       .filter(Objects::nonNull)
                                                                       .filter(reference -> !reference.isNull());

        return mapValuesToFutureOfCompletedValues(attributeReferenceStream, this::resolveAttributeReferenceValue)
            .thenApply(resolved -> AttributeDraft.of(attributeDraft.getName(), resolved));
    }

    private CompletionStage<JsonNode> resolveAttributeReferenceValue(@Nonnull final JsonNode referenceValue) {
        if (isProductReference(referenceValue)) {
            return getResolvedIdFromKeyInReference(referenceValue)
                .thenApply(productIdOptional ->
                    productIdOptional.map(this::createProductReferenceJson)
                                     .orElse(referenceValue));
        }
        return CompletableFuture.completedFuture(referenceValue);
    }

    static boolean isProductReference(@Nonnull final JsonNode referenceValue) {
        return getReferenceTypeIdIfReference(referenceValue)
            .map(referenceTypeId -> Objects.equals(referenceTypeId, Product.referenceTypeId()))
            .orElse(false);
    }

    private static Optional<String> getReferenceTypeIdIfReference(@Nonnull final JsonNode referenceValue) {
        final JsonNode typeId = referenceValue.get(REFERENCE_TYPE_ID_FIELD);
        return Optional.ofNullable(typeId).map(JsonNode::asText);
    }

    CompletionStage<Optional<String>> getResolvedIdFromKeyInReference(@Nonnull final JsonNode referenceValue) {
        final JsonNode idField = referenceValue.get(REFERENCE_ID_FIELD);
        return idField != null
            ? productService.getIdFromCacheOrFetch(idField.asText())
            : CompletableFuture.completedFuture(Optional.empty());
    }

    private JsonNode createProductReferenceJson(@Nonnull final String productId) {
        final ObjectNode productReferenceJsonNode = JsonNodeFactory.instance.objectNode();
        productReferenceJsonNode.put(REFERENCE_ID_FIELD, productId);
        productReferenceJsonNode.put(REFERENCE_TYPE_ID_FIELD, Product.referenceTypeId());
        return productReferenceJsonNode;
    }
}

