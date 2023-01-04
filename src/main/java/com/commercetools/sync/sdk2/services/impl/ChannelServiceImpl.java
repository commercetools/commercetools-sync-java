package com.commercetools.sync.sdk2.services.impl;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyChannelsGet;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.ChannelService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
      @Nonnull final Set<String> channelKeys) {
    return super.cacheKeysToIds(channelKeys, GraphQlQueryResource.CHANNELS);
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
