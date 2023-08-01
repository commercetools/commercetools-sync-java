package com.commercetools.sync.sdk2.channels.helpers;

import com.commercetools.api.models.channel.ChannelSetCustomFieldActionBuilder;
import com.commercetools.api.models.channel.ChannelSetCustomTypeActionBuilder;
import com.commercetools.api.models.channel.ChannelUpdateAction;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChannelCustomActionBuilder implements GenericCustomActionBuilder<ChannelUpdateAction> {

  @Nonnull
  @Override
  public ChannelUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {
    return ChannelSetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public ChannelUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {
    return ChannelSetCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public ChannelUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return ChannelSetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
