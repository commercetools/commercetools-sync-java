package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public final class ProductTypeReferenceResolver
    extends BaseReferenceResolver<ProductTypeDraft, ProductTypeSyncOptions> {

    private AttributeDefinitionReferenceResolver attributeDefinitionReferenceResolver;

    /**
     * Takes a {@link ProductTypeSyncOptions} instance and a {@link ProductTypeService} to instantiate
     * a {@link AttributeDefinitionReferenceResolver} instance that could be used to resolve the AttributeDefinition
     * references on the productType draft supplied to the {@link #resolveReferences(ProductTypeDraft)} method.
     *
     * @param productTypeSyncOptions   the container of all the options of the sync process including the CTP project
     *                                 client and/or configuration and other sync-specific options.
     * @param productTypeService       the service to fetch the product type for reference resolution.
     */
    public ProductTypeReferenceResolver(@Nonnull final ProductTypeSyncOptions productTypeSyncOptions,
                                        @Nonnull final ProductTypeService productTypeService) {
        super(productTypeSyncOptions);
        this.attributeDefinitionReferenceResolver =
            new AttributeDefinitionReferenceResolver(productTypeSyncOptions, productTypeService);
    }

    /**
     * Given a {@link ProductTypeDraft} this method attempts to resolve the attribute definition references to return
     * a {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     * The keys of the references are either taken from the id fields of the references or resource identifiers.
     *
     * @param productTypeDraft the productTypeDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new productTypeDraft instance with resolved
     *         references or, in case an error occurs during reference resolution,
     *         a {@link ReferenceResolutionException}.
     */
    @Nonnull
    public CompletionStage<ProductTypeDraft> resolveReferences(@Nonnull final ProductTypeDraft productTypeDraft) {
        return resolveAttributeDefinitionsReferences(ProductTypeDraftBuilder.of(productTypeDraft))
            .thenApply(ProductTypeDraftBuilder::build);
    }

    @Nonnull
    CompletionStage<ProductTypeDraftBuilder> resolveAttributeDefinitionsReferences(
        @Nonnull final ProductTypeDraftBuilder productTypeDraftBuilder) {

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productTypeDraftBuilder.getAttributes();

        if (attributeDefinitionDrafts == null) {
            return completedFuture(productTypeDraftBuilder);
        }

        return mapValuesToFutureOfCompletedValues(attributeDefinitionDrafts,
            attributeDefinitionReferenceResolver::resolveReferences, toList())
            .thenApply(productTypeDraftBuilder::attributes);
    }
}
