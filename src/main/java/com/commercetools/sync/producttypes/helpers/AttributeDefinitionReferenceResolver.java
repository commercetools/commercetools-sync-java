package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class AttributeDefinitionReferenceResolver
    extends BaseReferenceResolver<AttributeDefinitionDraft, ProductTypeSyncOptions> {
    private ProductTypeService productTypeService;


    public AttributeDefinitionReferenceResolver(@Nonnull final ProductTypeSyncOptions options,
                                                @Nonnull final ProductTypeService productTypeService) {
        super(options);
        this.productTypeService = productTypeService;
    }

    /**
     * Given an {@link AttributeDefinitionDraft} this method attempts to resolve the ProductType references, which can
     * exist on attributeDefinition with an AttributeType: NestedType or SetType of NestedType, to return a
     * {@link CompletionStage} which contains a new instance of the draft with the resolved references.
     * The keys of the references are taken from the id field of the references.
     *
     * @param attributeDefinitionDraft the attributeDefinitionDraft to resolve its references.
     * @return a {@link CompletionStage} that contains as a result a new attributeDefinitionDraft instance with resolved
     *         references or if there is no productType existing with the given key the draft will be returned as is
     *         without the reference resolved. In case an error occurs during reference resolution, a
     *         {@link ReferenceResolutionException} is thrown.
     */
    @Nonnull
    public CompletionStage<AttributeDefinitionDraft> resolveReferences(
        @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {

        final AttributeDefinitionDraftBuilder draftBuilder =
            AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft);

        return resolveReferences(draftBuilder)
            .handle(ImmutablePair::new)
            .thenCompose(result -> {
                final Throwable exception = result.getValue();
                final AttributeDefinitionDraftBuilder resolvedBuilder = result.getKey();
                if (exception == null) {
                    return completedFuture(resolvedBuilder.build());
                } else {
                    final String errorMessage =
                        format("Failed to resolve references on attribute definition with name '%s'.",
                            attributeDefinitionDraft.getName());
                    return exceptionallyCompletedFuture(
                        new ReferenceResolutionException(errorMessage, exception.getCause()));
                }
            });

    }

    @Nonnull
    private CompletionStage<AttributeDefinitionDraftBuilder> resolveReferences(
        @Nonnull final AttributeDefinitionDraftBuilder attributeDefinitionDraftBuilder) {

        final AttributeType attributeType = attributeDefinitionDraftBuilder.getAttributeType();

        if (attributeType instanceof NestedAttributeType) {
            return resolveNestedTypeReference((NestedAttributeType) attributeType)
                .thenApply(attributeDefinitionDraftBuilder::attributeType);

        } else if (attributeType instanceof SetAttributeType) {
            final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
            final AttributeType elementType = setAttributeType.getElementType();

            if (elementType instanceof NestedAttributeType) {

                return resolveNestedTypeReference((NestedAttributeType) elementType)
                    .thenApply(SetAttributeType::of)
                    .thenApply(attributeDefinitionDraftBuilder::attributeType);
            }
        }
        return completedFuture(attributeDefinitionDraftBuilder);
    }

    @Nonnull
    private CompletionStage<NestedAttributeType> resolveNestedTypeReference(
        @Nonnull final NestedAttributeType nestedAttributeType) {

        final Reference<ProductType> typeReference = nestedAttributeType.getTypeReference();

        return resolveProductTypeReference(typeReference)
            .thenApply(optionalResolvedReference ->
                optionalResolvedReference.map(NestedAttributeType::of)
                                         .orElse(nestedAttributeType));
    }

    @Nonnull
    private CompletionStage<Optional<Reference<ProductType>>> resolveProductTypeReference(
        @Nonnull final Reference<ProductType> typeReference) {

        final String resourceKey;
        try {
            resourceKey = getKeyFromResourceIdentifier(typeReference);
        } catch (ReferenceResolutionException exception) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException("Failed to resolve NestedType productType reference.", exception));
        }
        return productTypeService.fetchCachedProductTypeId(resourceKey)
                                 .thenApply(optionalId -> optionalId.map(ProductType::referenceOfId));
    }

}
