package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.ProductReferenceResolutionUtils.mapToProductDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.common.*;
import com.commercetools.api.models.custom_object.*;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductTransformUtils {

  /**
   * Transforms products by resolving the references and map them to ProductDrafts.
   *
   * <p>This method replaces the ids on attribute references with keys and resolves(fetch key values
   * for the reference id's) non null and unexpanded references of the product{@link Product} by
   * using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the Product to ProductDraft by performing reference resolution considering idToKey
   * value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param products the products to replace the references and attributes id's with keys.
   * @return a new list which contains productDrafts which have all their references and attributes
   *     references resolved and already replaced with keys.
   */
  @Nonnull
  public static CompletableFuture<List<ProductDraft>> toProductDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<ProductProjection> products) {

    final ProductTransformUtils.ProductTransformServiceImpl productTransformService =
        new ProductTransformUtils.ProductTransformServiceImpl(client, referenceIdToKeyCache);
    return productTransformService.toProductDrafts(products);
  }

  private static class ProductTransformServiceImpl extends BaseTransformServiceImpl {

    private static final String FAILED_TO_REPLACE_REFERENCES_ON_ATTRIBUTES =
        "Failed to replace referenced resource ids with keys on the attributes of the products in "
            + "the current fetched page from the source project. This page will not be synced to the target "
            + "project.";

    public ProductTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<ProductDraft>> toProductDrafts(
        @Nonnull final List<ProductProjection> products) {

      return replaceAttributeReferenceIdsWithKeys(products)
          .handle(
              (productsResolved, throwable) -> {
                if (throwable != null) {
                  throw new ReferenceTransformException(
                      FAILED_TO_REPLACE_REFERENCES_ON_ATTRIBUTES, throwable);
                }
                return productsResolved;
              })
          .thenCompose(
              productsWithAttributesResolved ->
                  transformReferencesAndMapToProductDrafts(productsWithAttributesResolved))
          .toCompletableFuture();
    }

    @Nonnull
    private CompletionStage<List<ProductDraft>> transformReferencesAndMapToProductDrafts(
        @Nonnull final List<ProductProjection> products) {

      final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
      transformReferencesToRunParallel.add(this.transformProductTypeReference(products));
      transformReferencesToRunParallel.add(this.transformTaxCategoryReference(products));
      transformReferencesToRunParallel.add(this.transformStateReference(products));
      transformReferencesToRunParallel.add(this.transformCategoryReference(products));
      transformReferencesToRunParallel.add(this.transformPricesChannelReference(products));
      transformReferencesToRunParallel.add(this.transformCustomTypeReference(products));
      transformReferencesToRunParallel.add(this.transformPricesCustomerGroupReference(products));

      return CompletableFuture.allOf(
              transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
          .thenApply(ignore -> mapToProductDrafts(products, this.referenceIdToKeyCache));
    }

    @Nonnull
    private CompletableFuture<Void> transformProductTypeReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> productTypeIds =
          products.stream()
              .map(ProductProjection::getProductType)
              .map(ProductTypeReference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(productTypeIds, GraphQlQueryResource.PRODUCT_TYPES);
    }

    @Nonnull
    private CompletableFuture<Void> transformTaxCategoryReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> taxCategoryIds =
          products.stream()
              .map(ProductProjection::getTaxCategory)
              .filter(Objects::nonNull)
              .map(TaxCategoryReference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(taxCategoryIds, GraphQlQueryResource.TAX_CATEGORIES);
    }

    @Nonnull
    private CompletableFuture<Void> transformStateReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> stateIds =
          products.stream()
              .map(ProductProjection::getState)
              .filter(Objects::nonNull)
              .map(StateReference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(stateIds, GraphQlQueryResource.STATES);
    }

    @Nonnull
    private CompletableFuture<Void> transformCategoryReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> categoryIds =
          products.stream()
              .map(ProductProjection::getCategories)
              .filter(Objects::nonNull)
              .map(
                  categories ->
                      categories.stream()
                          .map(CategoryReference::getId)
                          .collect(Collectors.toList()))
              .flatMap(Collection::stream)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(categoryIds, GraphQlQueryResource.CATEGORIES);
    }

    @Nonnull
    private CompletableFuture<Void> transformPricesChannelReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> channelIds =
          products.stream()
              .map(product -> product.getAllVariants())
              .map(
                  productVariants ->
                      productVariants.stream()
                          .filter(Objects::nonNull)
                          .map(
                              productVariant ->
                                  productVariant.getPrices().stream()
                                      .map(Price::getChannel)
                                      .filter(Objects::nonNull)
                                      .map(ChannelReference::getId)
                                      .collect(toList()))
                          .flatMap(Collection::stream)
                          .collect(toList()))
              .flatMap(Collection::stream)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(channelIds, GraphQlQueryResource.CHANNELS);
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomTypeReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> setOfTypeIds = new HashSet<>();
      setOfTypeIds.addAll(collectPriceCustomTypeIds(products));
      setOfTypeIds.addAll(collectAssetCustomTypeIds(products));

      return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
    }

    private Set<String> collectPriceCustomTypeIds(@Nonnull List<ProductProjection> products) {
      return products.stream()
          .map(product -> product.getAllVariants())
          .map(
              productVariants ->
                  productVariants.stream()
                      .filter(Objects::nonNull)
                      .map(
                          productVariant ->
                              productVariant.getPrices().stream()
                                  .map(Price::getCustom)
                                  .filter(Objects::nonNull)
                                  .map(CustomFields::getType)
                                  .map(TypeReference::getId)
                                  .collect(toList()))
                      .flatMap(Collection::stream)
                      .collect(toList()))
          .flatMap(Collection::stream)
          .collect(toSet());
    }

    private Set<String> collectAssetCustomTypeIds(@Nonnull List<ProductProjection> products) {
      return products.stream()
          .map(product -> product.getAllVariants())
          .map(
              productVariants ->
                  productVariants.stream()
                      .filter(Objects::nonNull)
                      .map(
                          productVariant ->
                              productVariant.getAssets().stream()
                                  .map(Asset::getCustom)
                                  .filter(Objects::nonNull)
                                  .map(CustomFields::getType)
                                  .map(TypeReference::getId)
                                  .collect(toList()))
                      .flatMap(Collection::stream)
                      .collect(toList()))
          .flatMap(Collection::stream)
          .collect(toSet());
    }

    @Nonnull
    private CompletableFuture<Void> transformPricesCustomerGroupReference(
        @Nonnull final List<ProductProjection> products) {

      final Set<String> customerGroupIds =
          products.stream()
              .map(product -> product.getAllVariants())
              .map(
                  productVariants ->
                      productVariants.stream()
                          .filter(Objects::nonNull)
                          .map(
                              productVariant ->
                                  productVariant.getPrices().stream()
                                      .map(Price::getCustomerGroup)
                                      .filter(Objects::nonNull)
                                      .map(CustomerGroupReference::getId)
                                      .collect(toList()))
                          .flatMap(Collection::stream)
                          .collect(toList()))
              .flatMap(Collection::stream)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(
          customerGroupIds, GraphQlQueryResource.CUSTOMER_GROUPS);
    }

    /**
     * Replaces the ids on attribute references with keys.
     *
     * <p>Note: this method mutates the products passed by changing the reference keys with ids.
     *
     * @param products the products to replace the reference attributes ids with keys on.
     * @return products with all their attributes references resolvable and already replaced with
     *     keys.
     */
    @Nonnull
    public CompletionStage<List<ProductProjection>> replaceAttributeReferenceIdsWithKeys(
        @Nonnull final List<ProductProjection> products) {

      final List<Reference> allAttributeReferences = getAllReferences(products);

      return getIdToKeys(allAttributeReferences)
          .thenApply(
              ignored -> {
                replaceReferences(allAttributeReferences);
                return products;
              });
    }

    @Nonnull
    private List<Reference> getAllReferences(@Nonnull final List<ProductProjection> products) {
      return products.stream()
          .map(this::getAllReferences)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    @Nonnull
    private List<Reference> getAllReferences(@Nonnull final ProductProjection product) {
      final List<ProductVariant> allVariants = product.getAllVariants();
      return getAttributeReferences(allVariants);
    }

    @Nonnull
    private static List<Reference> getAttributeReferences(
        @Nonnull final List<ProductVariant> variants) {

      return variants.stream()
          .map(ProductVariant::getAttributes)
          .flatMap(Collection::stream)
          .map(AttributeUtils::getAttributeReferences)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    private void replaceReferences(@Nonnull final List<Reference> references) {
      references.forEach(
          reference -> reference.setId(this.referenceIdToKeyCache.get(reference.getId())));
    }

    /**
     * Given a {@link Set}s of references of product attributes, this method first checks if there
     * is a key mapping for each id in the {@code idToKey} cache. If there exists a mapping for all
     * the ids, the method returns a future containing the existing {@code idToKey} cache as it is.
     * If there is at least one missing mapping, it attempts to make a GraphQL request (note: rest
     * request for custom objects) to CTP to fetch all ids and keys of every missing product,
     * category, productType or custom object Id in a combined request. For each fetched key/id
     * pair, the method will insert it into the {@code idToKey} cache and then return the cache in a
     * {@link CompletableFuture} after the request is successful.
     *
     * @param allAttributeReferences all references of product attributes to find a id -> key
     *     mapping for.
     * @return a ReferenceIdToKeyCache instance that manages cache of id to key representing
     *     products, categories, productTypes and customObjects in the CTP project defined by the
     *     injected {@code ctpClient}.
     */
    @Nonnull
    CompletableFuture<Void> getIdToKeys(@Nonnull final List<Reference> allAttributeReferences) {

      final Set<Reference> nonCachedReferences = getNonCachedReferences(allAttributeReferences);
      final Map<GraphQlQueryResource, Set<String>> map =
          buildMapOfRequestTypeToReferencedIds(nonCachedReferences);

      final Set<String> nonCachedCustomObjectIds = map.remove(GraphQlQueryResource.CUSTOM_OBJECTS);

      if (map.values().isEmpty()
          || map.values().stream().allMatch(referencedIdsSet -> referencedIdsSet.isEmpty())) {
        return fetchCustomObjectKeys(nonCachedCustomObjectIds);
      }

      final List<GraphQLRequest> collectedRequests =
          map.keySet().stream()
              .map(
                  resource -> {
                    List<List<String>> chunk = ChunkUtils.chunk(map.get(resource), CHUNK_SIZE);
                    return createGraphQLRequests(chunk, resource);
                  })
              .flatMap(Collection::stream)
              .collect(toList());

      return ChunkUtils.executeChunks(getCtpClient(), collectedRequests)
          .thenAccept(this::cacheResourceReferenceKeys)
          .thenCompose(ignored -> fetchCustomObjectKeys(nonCachedCustomObjectIds));
    }

    @Nonnull
    private CompletableFuture<Void> fetchCustomObjectKeys(
        @Nullable final Set<String> nonCachedCustomObjectIds) {

      if (nonCachedCustomObjectIds == null || nonCachedCustomObjectIds.isEmpty()) {
        return CompletableFuture.completedFuture(null);
      }

      final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedCustomObjectIds, CHUNK_SIZE);

      // As the referenced custom object might not included in the products because reference
      // expansion wasn't provided
      // we have no clue about it's container. That's why we use this deprecated Request object for
      // now.
      final List<ByProjectKeyCustomObjectsGet> chunkedRequests =
          chunkedIds.stream()
              .map(
                  ids ->
                      getCtpClient()
                          .customObjects()
                          .get()
                          .withWhere("id in :ids")
                          .withPredicateVar("ids", ids)
                          .withLimit(CHUNK_SIZE)
                          .withWithTotal(false))
              .collect(toList());

      return ChunkUtils.executeChunks(chunkedRequests)
          .thenAccept(
              chunk -> {
                chunk.forEach(
                    response -> {
                      CustomObjectPagedQueryResponse responseBody = response.getBody();
                      responseBody
                          .getResults()
                          .forEach(
                              customObject -> {
                                this.referenceIdToKeyCache.add(
                                    customObject.getId(),
                                    CustomObjectCompositeIdentifier.of(customObject).toString());
                              });
                    });
              });
    }
  }
}
