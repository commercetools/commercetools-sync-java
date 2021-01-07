package com.commercetools.sync.commons.models;

import java.util.Set;

public interface GraphQlBaseResult<T extends GraphQlBaseResource> {
    Set<T> getResults();
}
