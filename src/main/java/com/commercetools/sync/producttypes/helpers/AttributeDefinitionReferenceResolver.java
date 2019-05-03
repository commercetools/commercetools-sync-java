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


    AttributeDefinitionReferenceResolver(@Nonnull final ProductTypeSyncOptions options,
                                         @Nonnull final ProductTypeService productTypeService) {
        super(options);
        this.productTypeService = productTypeService;
    }

    @Nonnull
    public CompletionStage<AttributeDefinitionDraft> resolveReferences(
        @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {

        final AttributeDefinitionDraftBuilder draftBuilder =
            AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft);

        return resolveNestedTypeReferenceOrSetOfNested(draftBuilder)
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
    private CompletionStage<AttributeDefinitionDraftBuilder> resolveNestedTypeReferenceOrSetOfNested(
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
