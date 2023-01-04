package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.ProductTypeUpdateBuilder;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.ApiMethod;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

public final class ProductTypeServiceImpl extends BaseService<ProductTypeSyncOptions>
    implements ProductTypeService {

  private final Map<String, Map<String, AttributeMetaData>> productsAttributesMetaData =
      new ConcurrentHashMap<>();

  public ProductTypeServiceImpl(@Nonnull final ProductTypeSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> categoryKeys) {
    return super.cacheKeysToIds(categoryKeys, GraphQlQueryResource.PRODUCT_TYPES);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {
    ByProjectKeyProductTypesGet query =
        syncOptions
            .getCtpClient()
            .productTypes()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return fetchCachedResourceId(key, query);
  }

  @Nonnull
  private static Map<String, AttributeMetaData> getAttributeMetaDataMap(
      @Nonnull final ProductType productType) {
    return productType.getAttributes().stream()
        .map(AttributeMetaData::of)
        .collect(
            Collectors.toMap(AttributeMetaData::getName, attributeMetaData -> attributeMetaData));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Map<String, AttributeMetaData>>>
      fetchCachedProductAttributeMetaDataMap(@Nonnull final String productTypeId) {

    if (productsAttributesMetaData.isEmpty()) {
      return fetchAndCacheProductMetaData(productTypeId);
    }
    return CompletableFuture.completedFuture(
        Optional.ofNullable(productsAttributesMetaData.get(productTypeId)));
  }

  @Nonnull
  private CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchAndCacheProductMetaData(
      @Nonnull final String productTypeId) {
    final Consumer<List<ProductType>> productTypePageConsumer =
        productTypePage ->
            productTypePage.forEach(
                type -> {
                  final String id = type.getId();
                  productsAttributesMetaData.put(id, getAttributeMetaDataMap(type));
                });
    ByProjectKeyProductTypesGet byProjectKeyProductTypesGet =
        this.syncOptions.getCtpClient().productTypes().get();

    return QueryUtils.queryAll(byProjectKeyProductTypesGet, productTypePageConsumer)
        .thenApply(result -> Optional.ofNullable(productsAttributesMetaData.get(productTypeId)));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<ProductType>> fetchMatchingProductTypesByKeys(
      @Nonnull final Set<String> productTypeKeys) {
    if (productTypeKeys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(productTypeKeys, CHUNK_SIZE);

    final List<ByProjectKeyProductTypesGet> fetchByKeysRequests =
        chunkedKeys.stream()
            .map(
                keys ->
                    keys.stream()
                        .filter(key -> !isBlank(key))
                        .map(StringEscapeUtils::escapeJava)
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")))
            .map(commaSeparatedKeys -> format("key in (%s)", commaSeparatedKeys))
            .map(
                whereQuery ->
                    syncOptions
                        .getCtpClient()
                        .productTypes()
                        .get()
                        .addWhere(whereQuery)
                        .withLimit(CHUNK_SIZE)
                        .withWithTotal(false))
            .collect(toList());

    // todo: what happens on error ?
    return collectionOfFuturesToFutureOfCollection(
            fetchByKeysRequests.stream().map(ApiMethod::execute).collect(Collectors.toList()),
            Collectors.toList())
        .thenApply(
            pagedProductTypeResponses ->
                pagedProductTypeResponses.stream()
                    .map(ApiHttpResponse::getBody)
                    .map(ProductTypePagedQueryResponse::getResults)
                    .flatMap(Collection::stream)
                    .peek(
                        productType -> keyToIdCache.put(productType.getKey(), productType.getId()))
                    .collect(Collectors.toSet()));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductType>> createProductType(
      @Nonnull final ProductTypeDraft productTypeDraft) {
    final String draftKey = productTypeDraft.getKey();

    if (isBlank(draftKey)) {
      syncOptions.applyErrorCallback(
          new SyncException(format(CREATE_FAILED, draftKey, "Draft key is blank!")),
          null,
          productTypeDraft,
          null);
      return CompletableFuture.completedFuture(Optional.empty());
    } else {
      return syncOptions
          .getCtpClient()
          .productTypes()
          .post(productTypeDraft)
          .execute()
          .handle(
              ((resource, exception) -> {
                if (exception == null && resource.getBody() != null) {
                  keyToIdCache.put(draftKey, resource.getBody().getId());
                  return Optional.of(resource.getBody());
                } else if (exception != null) {
                  syncOptions.applyErrorCallback(
                      new SyncException(
                          format(CREATE_FAILED, draftKey, exception.getMessage()), exception),
                      null,
                      productTypeDraft,
                      null);
                  return Optional.empty();
                } else {
                  return Optional.empty();
                }
              }));
    }
  }

  @Nonnull
  @Override
  public CompletionStage<ProductType> updateProductType(
      @Nonnull final ProductType productType,
      @Nonnull final List<ProductTypeUpdateAction> updateActions) {

    final List<List<ProductTypeUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<ProductType>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productType));

    for (final List<ProductTypeUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedProductType ->
                      syncOptions
                          .getCtpClient()
                          .productTypes()
                          .withId(updatedProductType.getId())
                          .post(
                              ProductTypeUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedProductType.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductType>> fetchProductType(@Nullable final String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .productTypes()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            productType -> {
              keyToIdCache.put(productType.getKey(), productType.getId());
              return Optional.of(productType);
            })
        .exceptionally(
            throwable -> {
              if (throwable.getCause() instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo - to check with the team: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final ByProjectKeyProductTypesGet query) {
    return fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key,
      @Nonnull final Function<ProductType, String> keyMapper,
      @Nonnull final ByProjectKeyProductTypesGet query) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }
    return fetchAndCache(key, keyMapper, query);
  }

  private CompletionStage<Optional<String>> fetchAndCache(
      @Nullable final String key,
      @Nonnull final Function<ProductType, String> keyMapper,
      @Nonnull final ByProjectKeyProductTypesGet query) {
    final Consumer<List<ProductType>> pageConsumer =
        page ->
            page.forEach(resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()));

    return QueryUtils.queryAll(query, pageConsumer)
        .thenApply(result -> Optional.ofNullable(keyToIdCache.getIfPresent(key)));
  }
}
