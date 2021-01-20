package com.commercetools.sync.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public class ResourceKeyIdGraphQlResult implements GraphQlBaseResult<ResourceKeyId> {
  private final Set<ResourceKeyId> results;

  @JsonCreator
  protected ResourceKeyIdGraphQlResult(@JsonProperty("results") final Set<ResourceKeyId> results) {
    this.results = results;
  }

  public Set<ResourceKeyId> getResults() {
    return results;
  }
}
