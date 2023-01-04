package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyChannelsGet;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.services.ChannelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

public final class ChannelServiceImpl extends BaseService<BaseSyncOptions>
    implements ChannelService {

  private final Set<ChannelRoleEnum> channelRoles;

  public ChannelServiceImpl(
      @Nonnull final BaseSyncOptions syncOptions,
      @Nonnull final Set<ChannelRoleEnum> channelRoles) {
    super(syncOptions);
    this.channelRoles = channelRoles;
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> keysToCache) {
    final Set<String> keysNotCached = getKeysNotCached(keysToCache);

    if (keysNotCached.isEmpty()) {
      return CompletableFuture.completedFuture(keyToIdCache.asMap());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keysNotCached, CHUNK_SIZE);

    String query =
        "query fetchIdKeyPairs($where: String, $limit: Int) {\n"
            + "  channels(limit: $limit, where: $where) {\n"
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
                            jsonNode.get("channels").get("results").elements();
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
  @Override
  public CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key) {
    ByProjectKeyChannelsGet query =
        syncOptions
            .getCtpClient()
            .channels()
            .get()
            .withWhere("key = :key")
            .withPredicateVar("key", key);

    return fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key,
      @Nonnull final Function<Channel, String> keyMapper,
      @Nonnull final ByProjectKeyChannelsGet query) {

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
      @Nonnull final Function<Channel, String> keyMapper,
      @Nonnull final ByProjectKeyChannelsGet query) {
    final Consumer<List<Channel>> pageConsumer =
        page ->
            page.forEach(resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()));

    return QueryUtils.queryAll(query, pageConsumer)
        .thenApply(result -> Optional.ofNullable(keyToIdCache.getIfPresent(key)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> createChannel(@Nonnull final String key) {
    ChannelDraft channelDraft =
        ChannelDraftBuilder.of().key(key).roles(List.copyOf(channelRoles)).build();

    return syncOptions
        .getCtpClient()
        .channels()
        .post(channelDraft)
        .execute()
        .handle(
            ((resource, exception) -> {
              if (exception == null && resource.getBody() != null) {
                keyToIdCache.put(key, resource.getBody().getId());
                return Optional.of(resource.getBody());
              } else if (exception != null) {
                syncOptions.applyErrorCallback(
                    new SyncException(
                        format(CREATE_FAILED, key, exception.getMessage()), exception),
                    null,
                    channelDraft,
                    null);
                return Optional.empty();
              } else {
                return Optional.empty();
              }
            }));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Channel>> createAndCacheChannel(@Nonnull final String key) {

    return createChannel(key)
        .thenApply(
            channelOptional -> {
              channelOptional.ifPresent(channel -> keyToIdCache.put(key, channel.getId()));
              return channelOptional;
            });
  }
}
