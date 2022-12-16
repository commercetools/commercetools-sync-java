package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

// todo: reuse duplicated code between TypeService and CustomerService
public final class CustomerGroupServiceImpl extends BaseService<BaseSyncOptions>
    implements CustomerGroupService {

  public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keysToCache) {
    final Set<String> keysNotCached = getKeysNotCached(keysToCache);

    if (keysNotCached.isEmpty()) {
      return CompletableFuture.completedFuture(keyToIdCache.asMap());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keysNotCached, CHUNK_SIZE);

    String query =
        "query fetchIdKeyPairs($where: String, $limit: Int) {\n"
            + "  customerGroups(limit: $limit, where: $where) {\n"
            + "    results {\n"
            + "      id\n"
            + "      key\n"
            + "    }\n"
            + "  }\n"
            + "}";

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
                  .map(r -> r.getBody().getData())
                  // todo: set limit to -1, the payload will have errors object but what to do with
                  // it ?
                  //                  .filter(Objects::nonNull)
                  .forEach(
                      data -> {
                        ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                        final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
                        final Iterator<JsonNode> elements =
                            jsonNode.get("customerGroups").get("results").elements();
                        while (elements.hasNext()) {
                          JsonNode idAndKey = elements.next();
                          keyToIdCache.put(
                              idAndKey.get("key").asText(), idAndKey.get("id").asText());
                        }
                      });
              return keyToIdCache.asMap();
            });
  }

  @Nonnull
  public CompletionStage<Optional<CustomerGroup>> fetchCustomerGroupByKey(
      @Nullable final String key) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .customerGroups()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            customerGroup -> {
              keyToIdCache.put(customerGroup.getKey(), customerGroup.getId());
              return Optional.of(customerGroup);
            })
        .exceptionally(
            throwable -> {
              if (throwable instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nonnull final String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }

    return fetchCustomerGroupByKey(key)
        .thenApply(customerGroup -> customerGroup.map(CustomerGroup::getId));
  }
}
