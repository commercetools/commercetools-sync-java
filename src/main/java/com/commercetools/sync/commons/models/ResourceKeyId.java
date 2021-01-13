package com.commercetools.sync.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;

public final class ResourceKeyId implements GraphQlBaseResource {
  private final String key;
  private final String id;

  @JsonCreator
  public ResourceKeyId(
      @JsonProperty("key") @Nonnull final String key,
      @JsonProperty("id") @Nonnull final String id) {
    this.key = key;
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  @Override
  public String getId() {
    return id;
  }
}
