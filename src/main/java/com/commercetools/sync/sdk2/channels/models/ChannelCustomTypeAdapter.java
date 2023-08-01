package com.commercetools.sync.sdk2.channels.models;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.Custom;
import com.commercetools.sync.sdk2.commons.utils.CustomUpdateActionUtils;
import javax.annotation.Nullable;

/** Adapt Customer with {@link Custom} interface to be used on {@link CustomUpdateActionUtils} */
public final class ChannelCustomTypeAdapter implements Custom {

  private final Channel channel;

  private ChannelCustomTypeAdapter(Channel channel) {
    this.channel = channel;
  }

  /**
   * Get Id of the {@link Channel}
   *
   * @return the {@link Channel#getId()}
   */
  @Override
  public String getId() {
    return this.channel.getId();
  }

  /**
   * Get typeId of the {@link Channel} see: https://docs.commercetools.com/api/types#referencetype
   *
   * @return the typeId "channel"
   */
  @Override
  public String getTypeId() {
    return "channel";
  }

  /**
   * Get custom fields of the {@link Channel}
   *
   * @return the {@link CustomFields}
   */
  @Nullable
  @Override
  public CustomFields getCustom() {
    return this.channel.getCustom();
  }

  /**
   * Build an adapter to be used for preparing custom type actions of with the given {@link Channel}
   *
   * @param channel the {@link Channel}
   * @return the {@link ChannelCustomTypeAdapter}
   */
  public static ChannelCustomTypeAdapter of(Channel channel) {
    return new ChannelCustomTypeAdapter(channel);
  }
}
