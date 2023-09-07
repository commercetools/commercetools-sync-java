package com.commercetools.sync.producttypes.helpers;

import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.services.ProductTypeService;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class AttributeDefinitionReferenceResolver
    extends BaseReferenceResolver<AttributeDefinitionDraft, ProductTypeSyncOptions> {
  private final ProductTypeService productTypeService;

  public AttributeDefinitionReferenceResolver(
      @Nonnull final ProductTypeSyncOptions options,
      @Nonnull final ProductTypeService productTypeService) {
    super(options);
    this.productTypeService = productTypeService;
  }

  /**
   * Given an {@link AttributeDefinitionDraft} this method attempts to resolve the ProductType
   * references, which can exist on attributeDefinition with an AttributeType: NestedType or SetType
   * of NestedType, to return a {@link CompletionStage} which contains a new instance of the draft
   * with the resolved references.
   *
   * @param attributeDefinitionDraft the attributeDefinitionDraft to resolve its references.
   * @return a {@link CompletionStage} that contains as a result a new attributeDefinitionDraft
   *     instance with resolved references or if there is no productType existing with the given key
   *     the draft will be returned as is without the reference resolved. In case an error occurs
   *     during reference resolution, a {@link ReferenceResolutionException} is thrown.
   */
  @Nonnull
  public CompletionStage<AttributeDefinitionDraft> resolveReferences(
      @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {

    final AttributeDefinitionDraftBuilder draftBuilder =
        AttributeDefinitionDraftBuilder.of(attributeDefinitionDraft);

    return resolveNestedAttributeTypeReferences(draftBuilder)
        .handle(ImmutablePair::new)
        .thenCompose(
            result -> {
              final Throwable exception = result.getValue();
              final AttributeDefinitionDraftBuilder resolvedBuilder = result.getKey();
              if (exception == null) {
                return completedFuture(resolvedBuilder.build());
              } else {
                final String errorMessage =
                    format(
                        "Failed to resolve references on attribute definition with name '%s'.",
                        attributeDefinitionDraft.getName());
                return exceptionallyCompletedFuture(
                    new ReferenceResolutionException(errorMessage, exception.getCause()));
              }
            });
  }

  @Nonnull
  private CompletionStage<AttributeDefinitionDraftBuilder> resolveNestedAttributeTypeReferences(
      @Nonnull final AttributeDefinitionDraftBuilder attributeDefinitionDraftBuilder) {

    final AttributeType attributeType = attributeDefinitionDraftBuilder.getType();

    if (attributeType instanceof AttributeNestedType) {
      return resolveNestedTypeReference((AttributeNestedType) attributeType)
          .thenApply(attributeDefinitionDraftBuilder::type);

    } else if (attributeType instanceof AttributeSetType) {
      final AttributeSetType setAttributeType = (AttributeSetType) attributeType;
      final AttributeType elementType = setAttributeType.getElementType();

      final AtomicInteger maxDepth = new AtomicInteger();
      AttributeType nestedAttributeType = elementType;

      if (elementType instanceof AttributeSetType) {
        maxDepth.incrementAndGet();
        nestedAttributeType = getNestedAttributeType((AttributeSetType) elementType, maxDepth);
      }
      if (nestedAttributeType instanceof AttributeNestedType) {
        return resolveNestedAttributeTypeReferences(
            attributeDefinitionDraftBuilder, maxDepth, (AttributeNestedType) nestedAttributeType);
      }
    }
    return completedFuture(attributeDefinitionDraftBuilder);
  }

  @Nonnull
  private CompletionStage<AttributeDefinitionDraftBuilder> resolveNestedAttributeTypeReferences(
      @Nonnull final AttributeDefinitionDraftBuilder attributeDefinitionDraftBuilder,
      @Nonnull final AtomicInteger maxDepth,
      @Nonnull final AttributeNestedType nestedAttributeType) {

    /*
     As SDK types (e.g AttributeDefinitionDraftBuilder) are immutable,
     whole object needs to be created to change typeReference after resolving the reference.

     For instance, for an AttributeType type structure below which has 3 SetAttributeType and 1 NestedAttributeType,
     It would create first NestedAttributeType and then wrap it with SetAttributeType and so on so forth until creating
     the same object again.

     "type": {
        "name": "set",
        "elementType": {
          "name": "set",
          "elementType": {
            "name": "set",
            "elementType": {
              "name": "nested",
              "typeReference": {
                "typeId": "product-type",
                "id": "36b14c6d-31e9-4bc5-9191-f35a773268ed"
              }
            }
          }
        }
      }
    */
    return resolveNestedTypeReference(nestedAttributeType)
        .thenApply(
            resolvedNestedAttributeType -> {
              AttributeSetType setAttributeTypeChain =
                  AttributeSetTypeBuilder.of().elementType(resolvedNestedAttributeType).build();
              for (int i = 0; i < maxDepth.get(); i++) {
                setAttributeTypeChain =
                    AttributeSetTypeBuilder.of().elementType(setAttributeTypeChain).build();
              }
              return setAttributeTypeChain;
            })
        .thenApply(attributeDefinitionDraftBuilder::type);
  }

  @Nullable
  private AttributeType getNestedAttributeType(
      @Nonnull final AttributeSetType setAttributeType, @Nonnull final AtomicInteger maxDepth) {
    final AttributeType elementType = setAttributeType.getElementType();

    if (elementType instanceof AttributeSetType) {
      maxDepth.incrementAndGet();
      return getNestedAttributeType((AttributeSetType) elementType, maxDepth);
    }

    return elementType;
  }

  @Nonnull
  private CompletionStage<AttributeNestedType> resolveNestedTypeReference(
      @Nonnull final AttributeNestedType nestedAttributeType) {

    final ProductTypeReference typeReference = nestedAttributeType.getTypeReference();

    return resolveProductTypeReference(typeReference)
        .thenApply(
            optionalResolvedReference ->
                optionalResolvedReference
                    .map(
                        resolvedReference ->
                            AttributeNestedTypeBuilder.of()
                                .typeReference(resolvedReference)
                                .build())
                    .orElse(nestedAttributeType));
  }

  @Nonnull
  private CompletionStage<Optional<ProductTypeReference>> resolveProductTypeReference(
      @Nonnull final ProductTypeReference typeReference) {

    final String resourceKey;
    try {
      resourceKey = getIdFromReference(typeReference);
    } catch (ReferenceResolutionException exception) {
      return exceptionallyCompletedFuture(
          new ReferenceResolutionException(
              "Failed to resolve NestedType productType reference.", exception));
    }
    return productTypeService
        .fetchCachedProductTypeId(resourceKey)
        .thenApply(
            optionalId -> optionalId.map(id -> ProductTypeReferenceBuilder.of().id(id).build()));
  }
}
