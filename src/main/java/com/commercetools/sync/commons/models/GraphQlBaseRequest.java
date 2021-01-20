package com.commercetools.sync.commons.models;

import io.sphere.sdk.client.SphereRequest;

public interface GraphQlBaseRequest<T extends GraphQlBaseResult<? extends GraphQlBaseResource>>
    extends SphereRequest<T> {
  /**
   * This method adds a predicate string to the request.
   *
   * @param predicate - a string representing a query predicate.
   * @return - an instance of this class.
   */
  GraphQlBaseRequest<T> withPredicate(final String predicate);

  /**
   * This method adds a limit to the request.
   *
   * @param limit - a number representing the query limit parameter
   * @return - an instance of this class
   */
  GraphQlBaseRequest<T> withLimit(final long limit);
}
