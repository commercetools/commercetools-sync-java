package com.commercetools.sync.products.service.impl;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.products.utils.ProductReferenceResolutionUtils.mapToProductDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.service.ProductTransformService;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.AttributeContainer;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductLike;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.types.CustomFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class ProductTransformServiceImpl extends BaseTransformServiceImpl
    implements ProductTransformService {

  private static final String FAILED_TO_REPLACE_REFERENCES_ON_ATTRIBUTES =
      "Failed to replace referenced resource ids with keys on the attributes of the products in "
          + "the current fetched page from the source project. This page will not be synced to the target "
          + "project.";

  public ProductTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
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
        .thenApply(ignore -> mapToProductDrafts(products, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> transformProductTypeReference(
      @Nonnull final List<ProductProjection> products) {

    final Set<String> productTypeIds =
        products.stream().map(ProductLike::getProductType).map(Reference::getId).collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(productTypeIds, GraphQlQueryResources.PRODUCT_TYPES);
  }

  @Nonnull
  private CompletableFuture<Void> transformTaxCategoryReference(
      @Nonnull final List<ProductProjection> products) {

    final Set<String> taxCategoryIds =
        products.stream()
            .map(ProductLike::getTaxCategory)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(taxCategoryIds, GraphQlQueryResources.TAX_CATEGORIES);
  }

  @Nonnull
  private CompletableFuture<Void> transformStateReference(
      @Nonnull final List<ProductProjection> products) {

    final Set<String> stateIds =
        products.stream()
            .map(ProductProjection::getState)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(stateIds, GraphQlQueryResources.STATES);
  }

  @Nonnull
  private CompletableFuture<Void> transformCategoryReference(
      @Nonnull final List<ProductProjection> products) {

    final Set<String> categoryIds =
        products.stream()
            .map(product -> product.getCategories())
            .filter(Objects::nonNull)
            .map(
                categories ->
                    categories.stream().map(Reference::getId).collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(categoryIds, GraphQlQueryResources.CATEGORIES);
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
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(channelIds, GraphQlQueryResources.CHANNELS);
  }

  @Nonnull
  private CompletableFuture<Void> transformCustomTypeReference(
      @Nonnull final List<ProductProjection> products) {

    final Set<String> setOfTypeIds = new HashSet<>();
    setOfTypeIds.addAll(collectPriceCustomTypeIds(products));
    setOfTypeIds.addAll(collectAssetCustomTypeIds(products));

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
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
                                .map(Reference::getId)
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
                                .map(Reference::getId)
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
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(
        customerGroupIds, GraphQlQueryResources.CUSTOMER_GROUPS);
  }

  /**
   * Replaces the ids on attribute references with keys. If a product has at least one irresolvable
   * reference, it will be filtered out and not returned in the new list.
   *
   * <p>Note: this method mutates the products passed by changing the reference keys with ids.
   *
   * @param products the products to replace the reference attributes ids with keys on.
   * @return a new list which contains only products which have all their attributes references
   *     resolvable and already replaced with keys.
   */
  @Nonnull
  public CompletionStage<List<ProductProjection>> replaceAttributeReferenceIdsWithKeys(
      @Nonnull final List<ProductProjection> products) {

    final List<JsonNode> allAttributeReferences = getAllReferences(products);

    final List<JsonNode> allProductReferences =
        getReferencesByTypeId(allAttributeReferences, Product.referenceTypeId());

    final List<JsonNode> allCategoryReferences =
        getReferencesByTypeId(allAttributeReferences, Category.referenceTypeId());

    final List<JsonNode> allProductTypeReferences =
        getReferencesByTypeId(allAttributeReferences, ProductType.referenceTypeId());

    final List<JsonNode> allCustomObjectReferences =
        getReferencesByTypeId(allAttributeReferences, CustomObject.referenceTypeId());

    final List<JsonNode> allStateReferences =
        getReferencesByTypeId(allAttributeReferences, State.referenceTypeId());

    final List<JsonNode> allCustomerReferences =
        getReferencesByTypeId(allAttributeReferences, Customer.referenceTypeId());

    return getIdToKeys(
            getIds(allProductReferences),
            getIds(allCategoryReferences),
            getIds(allProductTypeReferences),
            getIds(allCustomObjectReferences),
            getIds(allStateReferences),
            getIds(allCustomerReferences))
        .thenApply(
            referenceIdToKeyCache -> {
              final List<ProductProjection> validProducts =
                  filterOutWithIrresolvableReferences(products, referenceIdToKeyCache);
              replaceReferences(getAllReferences(validProducts), referenceIdToKeyCache);
              return validProducts;
            });
  }

  @Nonnull
  private List<JsonNode> getAllReferences(@Nonnull final List<ProductProjection> products) {
    return products.stream()
        .map(this::getAllReferences)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Nonnull
  private List<JsonNode> getAllReferences(@Nonnull final ProductProjection product) {
    final List<ProductVariant> allVariants = product.getAllVariants();
    return getAttributeReferences(allVariants);
  }

  @Nonnull
  private static List<JsonNode> getAttributeReferences(
      @Nonnull final List<ProductVariant> variants) {

    return variants.stream()
        .map(AttributeContainer::getAttributes)
        .flatMap(Collection::stream)
        .map(Attribute::getValueAsJsonNode)
        .map(ProductTransformServiceImpl::getReferences)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Nonnull
  private static List<JsonNode> getReferences(@Nonnull final JsonNode attributeValue) {
    return attributeValue.findParents(REFERENCE_TYPE_ID_FIELD);
  }

  @Nonnull
  private List<JsonNode> getReferencesByTypeId(
      @Nonnull final List<JsonNode> references, @Nonnull final String typeId) {
    return references.stream()
        .filter(reference -> typeId.equals(reference.get(REFERENCE_TYPE_ID_FIELD).asText()))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static Set<String> getIds(@Nonnull final List<JsonNode> references) {
    return references.stream().map(ProductTransformServiceImpl::getId).collect(toSet());
  }

  @Nonnull
  private static String getId(@Nonnull final JsonNode ref) {
    return ref.get(REFERENCE_ID_FIELD).asText();
  }

  @Nonnull
  private List<ProductProjection> filterOutWithIrresolvableReferences(
      @Nonnull final List<ProductProjection> products,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return products.stream()
        .filter(
            product -> {
              final Set<JsonNode> irresolvableReferences =
                  getIrresolvableReferences(product, referenceIdToKeyCache);
              return irresolvableReferences.isEmpty();
            })
        .collect(Collectors.toList());
  }

  private Set<JsonNode> getIrresolvableReferences(
      @Nonnull final ProductProjection product,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return getAllReferences(product).stream()
        .filter(
            reference -> {
              String id = getId(reference);
              return (!referenceIdToKeyCache.containsKey(id)
                  || KEY_IS_NOT_SET_PLACE_HOLDER.equals(referenceIdToKeyCache.get(id)));
            })
        .collect(toSet());
  }

  private static void replaceReferences(
      @Nonnull final List<JsonNode> references,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    references.forEach(
        reference -> {
          final String id = reference.get(REFERENCE_ID_FIELD).asText();
          final String key = referenceIdToKeyCache.get(id);
          ((ObjectNode) reference).put(REFERENCE_ID_FIELD, key);
        });
  }

  /**
   * Given 4 {@link Set}s of ids of products, categories, productTypes and custom objects, this
   * method first checks if there is a key mapping for each id in the {@code idToKey} cache. If
   * there exists a mapping for all the ids, the method returns a future containing the existing
   * {@code idToKey} cache as it is. If there is at least one missing mapping, it attempts to make a
   * GraphQL request (note: rest request for custom objects) to CTP to fetch all ids and keys of
   * every missing product, category, productType or custom object Id in a combined request. For
   * each fetched key/id pair, the method will insert it into the {@code idToKey} cache and then
   * return the cache in a {@link CompletableFuture} after the request is successful.
   *
   * @param productIds the product ids to find a key mapping for.
   * @param categoryIds the category ids to find a key mapping for.
   * @param productTypeIds the productType ids to find a key mapping for.
   * @param customObjectIds the custom object ids to find a key mapping for.
   * @return a ReferenceIdToKeyCache instance that manages cache of id to key representing products,
   *     categories, productTypes and customObjects in the CTP project defined by the injected
   *     {@code ctpClient}.
   */
  @Nonnull
  CompletableFuture<ReferenceIdToKeyCache> getIdToKeys(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds,
      @Nonnull final Set<String> customObjectIds,
      @Nonnull final Set<String> stateIds,
      @Nonnull final Set<String> customerIds) {

    final Set<String> nonCachedProductIds = getNonCachedReferenceIds(productIds);
    final Set<String> nonCachedCategoryIds = getNonCachedReferenceIds(categoryIds);
    final Set<String> nonCachedProductTypeIds = getNonCachedReferenceIds(productTypeIds);
    final Set<String> nonCachedCustomObjectIds = getNonCachedReferenceIds(customObjectIds);
    final Set<String> nonCachedStateIds = getNonCachedReferenceIds(stateIds);
    final Set<String> nonCachedCustomerIds = getNonCachedReferenceIds(customerIds);

    if (nonCachedProductIds.isEmpty()
        && nonCachedCategoryIds.isEmpty()
        && nonCachedProductTypeIds.isEmpty()
        && nonCachedStateIds.isEmpty()
        && nonCachedCustomerIds.isEmpty()) {
      return fetchCustomObjectKeys(nonCachedCustomObjectIds);
    }

    List<List<String>> productIdsChunk = ChunkUtils.chunk(nonCachedProductIds, CHUNK_SIZE);
    List<List<String>> categoryIdsChunk = ChunkUtils.chunk(nonCachedCategoryIds, CHUNK_SIZE);
    List<List<String>> productTypeIdsChunk = ChunkUtils.chunk(nonCachedProductTypeIds, CHUNK_SIZE);
    List<List<String>> stateIdsChunk = ChunkUtils.chunk(nonCachedStateIds, CHUNK_SIZE);
    List<List<String>> customerIdsChunk = ChunkUtils.chunk(nonCachedCustomerIds, CHUNK_SIZE);

    List<ResourceIdsGraphQlRequest> collectedRequests = new ArrayList<>();

    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(productIdsChunk, GraphQlQueryResources.PRODUCTS));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(categoryIdsChunk, GraphQlQueryResources.CATEGORIES));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(productTypeIdsChunk, GraphQlQueryResources.PRODUCT_TYPES));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(stateIdsChunk, GraphQlQueryResources.STATES));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(customerIdsChunk, GraphQlQueryResources.CUSTOMERS));

    return ChunkUtils.executeChunks(getCtpClient(), collectedRequests)
        .thenApply(ChunkUtils::flattenGraphQLBaseResults)
        .thenApply(
            results -> {
              cacheResourceReferenceKeys(results);
              return referenceIdToKeyCache;
            })
        .thenCompose(ignored -> fetchCustomObjectKeys(nonCachedCustomObjectIds));
  }

  @Nonnull
  private CompletableFuture<ReferenceIdToKeyCache> fetchCustomObjectKeys(
      @Nonnull final Set<String> nonCachedCustomObjectIds) {

    if (nonCachedCustomObjectIds.isEmpty()) {
      return CompletableFuture.completedFuture(referenceIdToKeyCache);
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedCustomObjectIds, CHUNK_SIZE);

    final List<CustomObjectQuery<JsonNode>> chunkedRequests =
        chunkedIds.stream()
            .map(
                ids ->
                    CustomObjectQuery.ofJsonNode()
                        .plusPredicates(p -> p.id().isIn(ids))
                        .withLimit(CHUNK_SIZE)
                        .withFetchTotal(false))
            .collect(toList());

    return ChunkUtils.executeChunks(getCtpClient(), chunkedRequests)
        .thenApply(ChunkUtils::flattenPagedQueryResults)
        .thenApply(
            customObjects -> {
              customObjects.forEach(
                  customObject -> {
                    referenceIdToKeyCache.add(
                        customObject.getId(),
                        CustomObjectCompositeIdentifier.of(customObject).toString());
                  });
              return referenceIdToKeyCache;
            });
  }
}
