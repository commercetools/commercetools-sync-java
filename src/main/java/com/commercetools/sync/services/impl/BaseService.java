package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.DomainResource;
import com.commercetools.api.models.PagedQueryResourceRequest;
import com.commercetools.api.models.ResourcePagedQueryResponse;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.ApiMethod;
import io.vrap.rmf.base.client.BodyApiMethod;
import io.vrap.rmf.base.client.Draft;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

abstract class BaseService<
    SyncOptionsT extends BaseSyncOptions,
    ResourceT extends DomainResource<ResourceT>,
    ResourceDraftT extends Draft<ResourceDraftT>,
    PagedQueryRequestT extends PagedQueryResourceRequest<PagedQueryRequestT, PagedQueryResponseT>,
    PagedQueryResponseT extends ResourcePagedQueryResponse<ResourceT>,
    GetOneResourceQueryT extends ApiMethod<GetOneResourceQueryT, ResourceT>,
    QueryResultT,
    PostRequestT extends BodyApiMethod<PostRequestT, QueryResultT, ResourceDraftT>> {

  final SyncOptionsT syncOptions;
  protected final Cache<String, String> keyToIdCache;

  protected static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;
  static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

  /*
   * To be more practical, considering 41 characters as an average for key and sku fields
   * (key and sku field doesn't have limit except for ProductType(256)) We chunk them in 250
   * (keys or sku) we will have a query around 11.000 characters(also considered some
   * conservative space for headers). Above this size it could return - Error 414 (Request-URI Too Large)
   */
  static final int CHUNK_SIZE = 250;

  BaseService(@Nonnull final SyncOptionsT syncOptions) {
    this.syncOptions = syncOptions;
    this.keyToIdCache =
        Caffeine.newBuilder()
            .maximumSize(syncOptions.getCacheSize())
            .executor(Runnable::run)
            .build();
  }

  /**
   * Given a set of keys this method collects all keys which aren't already contained in the cache
   * {@code keyToIdCache}
   *
   * @param keys {@link Set} of keys
   * @return a {@link Set} of keys which aren't already contained in the cache or empty
   */
  @Nonnull
  protected Set<String> getKeysNotCached(@Nonnull final Set<String> keys) {
    return keys.stream()
        .filter(StringUtils::isNotBlank)
        .filter(key -> !keyToIdCache.asMap().containsKey(key))
        .collect(Collectors.toSet());
  }

  @Nonnull
  public CompletionStage<Map<String, String>> cacheKeysToIdsUsingGraphQl(
      @Nonnull final Set<String> keysToCache, @Nonnull final GraphQlQueryResource queryResource) {
    final Set<String> keysNotCached = getKeysNotCached(keysToCache);

    if (keysNotCached.isEmpty()) {
      return CompletableFuture.completedFuture(keyToIdCache.asMap());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keysNotCached, CHUNK_SIZE);

    String query =
        format(
            "query fetchIdKeyPairs($where: String, $limit: Int) {%n"
                + "  %s(limit: $limit, where: $where) {%n"
                + "    results {%n"
                + "      id%n"
                + "      key%n"
                + "    }%n"
                + "  }%n"
                + "}",
            queryResource.getName());

    final List<GraphQLRequest> graphQLRequests =
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
                    GraphQLVariablesMapBuilder.of()
                        .addValue("where", whereQuery)
                        .addValue("limit", CHUNK_SIZE)
                        .build())
            .map(variables -> GraphQLRequestBuilder.of().query(query).variables(variables).build())
            .collect(Collectors.toList());

    return collectionOfFuturesToFutureOfCollection(
            graphQLRequests.stream()
                .map(
                    graphQLRequest ->
                        syncOptions.getCtpClient().graphql().post(graphQLRequest).execute())
                .collect(Collectors.toList()),
            Collectors.toList())
        .thenApply(
            graphQlResults -> {
              graphQlResults.stream()
                  .map(ApiHttpResponse::getBody)
                  .filter(Objects::nonNull)
                  .map(GraphQLResponse::getData)
                  .filter(Objects::nonNull)
                  .forEach(
                      data -> {
                        ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                        final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
                        final Iterator<JsonNode> elements =
                            jsonNode.get(queryResource.getName()).get("results").elements();
                        while (elements.hasNext()) {
                          final JsonNode idAndKey = elements.next();
                          keyToIdCache.put(
                              idAndKey.get("key").asText(), idAndKey.get("id").asText());
                        }
                      });
              return keyToIdCache.asMap();
            });
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key,
      @Nonnull final Function<ResourceT, String> keyMapper,
      @Nonnull final PagedQueryRequestT query) {

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
      @Nonnull final Function<ResourceT, String> keyMapper,
      @Nonnull final PagedQueryRequestT query) {
    final Consumer<List<ResourceT>> pageConsumer =
        page ->
            page.forEach(resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()));

    return QueryUtils.queryAll(query, pageConsumer)
        .thenApply(result -> Optional.ofNullable(keyToIdCache.getIfPresent(key)));
  }

  @Nonnull
  CompletionStage<Optional<ResourceT>> createResource(
      @Nonnull final ResourceDraftT draft,
      @Nonnull final Function<ResourceDraftT, String> keyMapper,
      @Nonnull final Function<QueryResultT, String> idMapper,
      @Nonnull final Function<QueryResultT, ResourceT> resourceMapper,
      @Nonnull final Supplier<PostRequestT> createCommand) {
    final String draftKey = keyMapper.apply(draft);

    if (isBlank(draftKey)) {
      syncOptions.applyErrorCallback(
          new SyncException(format(CREATE_FAILED, draftKey, "Draft key is blank!")),
          null,
          draft,
          null);
      return CompletableFuture.completedFuture(Optional.empty());
    } else {
      return this.executeCreateCommand(
          draft, draftKey, idMapper, resourceMapper, createCommand.get());
    }
  }

  @Nonnull
  CompletionStage<Optional<ResourceT>> executeCreateCommand(
      @Nonnull final ResourceDraftT draft,
      @Nonnull final String key,
      @Nonnull final Function<QueryResultT, String> idMapper,
      @Nonnull final Function<QueryResultT, ResourceT> resourceMapper,
      @Nonnull final PostRequestT createCommand) {
    return createCommand
        .execute()
        .handle(
            ((result, exception) -> {
              if (exception != null) {
                syncOptions.applyErrorCallback(
                    new SyncException(
                        format(CREATE_FAILED, key, exception.getMessage()), exception),
                    null,
                    draft,
                    null);
                return Optional.empty();
              } else if (result != null) {
                QueryResultT resultBody = result.getBody();
                keyToIdCache.put(key, idMapper.apply(resultBody));
                return Optional.of(resourceMapper.apply(resultBody));
              } else {
                return Optional.empty();
              }
            }));
  }

  CompletionStage<Set<ResourceT>> fetchMatchingResources(
      @Nonnull final Set<String> keys,
      @Nonnull final Function<ResourceT, String> keyMapper,
      @Nonnull final Function<Set<String>, PagedQueryRequestT> keysQueryMapper) {
    if (keys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    return fetchWithChunks(keysQueryMapper, keys)
        .thenApply(
            chunk -> {
              final Set<ResourceT> returnedSet = new HashSet<>();
              chunk.forEach(
                  response -> {
                    PagedQueryResponseT responseBody = response.getBody();
                    responseBody
                        .getResults()
                        .forEach(
                            resource -> {
                              returnedSet.add(resource);
                              keyToIdCache.put(keyMapper.apply(resource), resource.getId());
                            });
                  });
              return returnedSet;
            });
  }

  private CompletableFuture<List<ApiHttpResponse<PagedQueryResponseT>>> fetchWithChunks(
      @Nonnull final Function<Set<String>, PagedQueryRequestT> keysQueryMapper,
      @Nonnull final Set<String> keysNotCached) {

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keysNotCached, CHUNK_SIZE);

    final List<PagedQueryResourceRequest> keysQueryMapperList =
        chunkedKeys.stream()
            .map(
                _keys ->
                    keysQueryMapper
                        .apply(new HashSet<>(_keys))
                        .withLimit(CHUNK_SIZE)
                        .withWithTotal(false))
            .collect(toList());

    return ChunkUtils.executeChunks(keysQueryMapperList);
  }

  /**
   * Given a resource key, this method fetches a resource that matches this given key in the CTP
   * project defined in a potentially injected {@link com.commercetools.api.client.ProjectApiRoot}.
   * If there is no matching resource an empty {@link Optional} will be returned in the returned
   * future. A mapping of the key to the id of the fetched resource is persisted in an in -memory
   * map.
   *
   * @param key the key of the resource to fetch
   * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion
   *     contains an {@link Optional} that contains the matching {@code T} if exists, otherwise
   *     empty.
   */
  @Nonnull
  CompletionStage<Optional<ResourceT>> fetchResource(
      @Nullable final String key, @Nonnull final GetOneResourceQueryT query) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    return query
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            resource -> {
              keyToIdCache.put(key, resource.getId());
              return Optional.of(resource);
            })
        .exceptionally(
            exception -> {
              if (exception != null && exception.getCause() instanceof NotFoundException) {
                // if resource is not found, return empty optional
                return Optional.empty();
              }
              if (exception instanceof RuntimeException) {
                // if exception can be rethrown, cast it to runtime exception and rethrow
                throw (RuntimeException) exception;
              }
              // if exception is checked, it cannot be rethrown per se and must be wrapped
              throw new CompletionException(exception);
            });
  }
}
