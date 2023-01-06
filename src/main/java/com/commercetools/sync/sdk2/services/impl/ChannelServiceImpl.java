package com.commercetools.sync.sdk2.services.impl;

import static java.lang.String.format;

import com.commercetools.api.client.ByProjectKeyChannelsGet;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.ChannelService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class ChannelServiceImpl
    extends BaseService<BaseSyncOptions, Channel, ByProjectKeyChannelsGet>
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
