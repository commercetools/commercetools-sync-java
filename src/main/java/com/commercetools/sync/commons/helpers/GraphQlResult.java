package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.models.ResourceKeyId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class GraphQlResult {
    private final Set<ResourceKeyId> results;

    @JsonCreator
    protected GraphQlResult(@JsonProperty("results") final Set<ResourceKeyId> results) {
        this.results = results;
    }

    public Set<ResourceKeyId> getResults() {
        return results;
    }

}
