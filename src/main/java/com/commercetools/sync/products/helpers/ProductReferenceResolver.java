package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.getKeyFromResourceIdentifier;
import static java.lang.String.format;

//TODO: Should extend/reuse the BaseReferenceResolver.
public class ProductReferenceResolver {
    private ProductTypeService productTypeService;
    private ProductSyncOptions productSyncOptions;
    private static final String FAILED_TO_RESOLVE_PRODUCT_TYPE = "Failed to resolve product type reference on "
        + "ProductDraft with key:'%s'.";

    public ProductReferenceResolver(@Nonnull final ProductSyncOptions productSyncOptions,
                                    @Nonnull final ProductTypeService productTypeService) {
        this.productTypeService = productTypeService;
        this.productSyncOptions = productSyncOptions;
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
    public CompletionStage<ProductDraft> resolveReferences(@Nonnull final ProductDraft productDraft) {
        return resolveProductTypeReference(productDraft);
        //.thenCompose(this::resolveParentReference); TODO: OTHER REFERENCES ON PRODUCT
    }


    @Nonnull
    protected CompletionStage<ProductDraft> resolveProductTypeReference(@Nonnull final ProductDraft productDraft) {
        final ResourceIdentifier<ProductType> productTypeResourceIdentifier = productDraft.getProductType();
        return getProductTypeId(productTypeResourceIdentifier,
            format(FAILED_TO_RESOLVE_PRODUCT_TYPE, productDraft.getKey()))
            .thenApply(resolvedProductTypeIdOptional ->
                resolvedProductTypeIdOptional.map(resolvedTypeId ->
                    ProductDraftBuilder.of(productDraft)
                                       .productType(ResourceIdentifier.ofId(resolvedTypeId,
                                           Product.referenceTypeId()))
                                       .build())
                                             .orElseGet(() -> ProductDraftBuilder.of(productDraft).build()));
    }

    /**
     * Given a {@link ProductType} this method fetches the custom type reference id.
     *
     * @param productTypeResourceIdentifier   the productType object.
     * @param referenceResolutionErrorMessage the message containing the information about the draft to attach to the
     *                                        {@link ReferenceResolutionException} in case it occurs.
     * @return a {@link CompletionStage} that contains as a result an optional which either contains the custom type id
     *          if it exists or empty if it doesn't.
     */
    private CompletionStage<Optional<String>> getProductTypeId(@Nonnull final ResourceIdentifier<ProductType>
                                                                   productTypeResourceIdentifier,
                                                               @Nonnull final String referenceResolutionErrorMessage) {
        try {
            final String productTypeKey = getKeyFromResourceIdentifier(productTypeResourceIdentifier,
                productSyncOptions.shouldAllowUuidKeys());
            return productTypeService.fetchCachedProductTypeId(productTypeKey);
        } catch (ReferenceResolutionException exception) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format("%s Reason: %s", referenceResolutionErrorMessage, exception.getMessage()), exception));
        }
    }

}
