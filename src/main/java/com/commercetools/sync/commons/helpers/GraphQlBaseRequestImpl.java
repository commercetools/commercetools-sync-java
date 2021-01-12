package com.commercetools.sync.commons.helpers;

import static java.lang.String.format;

import com.commercetools.sync.commons.models.GraphQlBaseRequest;
import com.commercetools.sync.commons.models.GraphQlBaseResource;
import com.commercetools.sync.commons.models.GraphQlBaseResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GraphQlBaseRequestImpl<
        T extends GraphQlBaseResult<? extends GraphQlBaseResource>>
    implements GraphQlBaseRequest<T> {

  protected String queryPredicate = null;
  protected long limit = 500;

  /**
   * This method adds a predicate string to the request.
   *
   * @param predicate - a string representing a query predicate.
   * @return - an instance of this class.
   */
  @Nonnull
  @Override
  public GraphQlBaseRequest<T> withPredicate(final String predicate) {

    this.queryPredicate = predicate;
    return this;
  }

  /**
   * This method adds a limit to the request.
   *
   * @param limit - a number representing the query limit parameter
   * @return - an instance of this class
   */
  @Nonnull
  @Override
  public GraphQlBaseRequest<T> withLimit(final long limit) {

    this.limit = limit;
    return this;
  }

  @Override
  public HttpRequestIntent httpRequestIntent() {

    final String body = format("{\"query\": \"{%s}\"}", buildQueryString());
    return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
  }

  /**
   * Deserialize the body of {@code httpResponse}.
   *
   * @param httpResponse httpResponse of the request.
   * @param resourceName resource type in the query (i.e "customers", "products", "customObjects")
   * @param clazz the type of the class to deserialize.
   * @return the deserialized body of the graphql request.
   */
  @Nullable
  public T deserializeWithResourceName(
      @Nonnull final HttpResponse httpResponse,
      @Nonnull final String resourceName,
      @Nonnull final Class<T> clazz) {

    final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
    if (rootJsonNode.isNull()) {
      return null;
    }
    JsonNode result = rootJsonNode.get("data").get(resourceName);
    return SphereJsonUtils.readObject(result, clazz);
  }

  /**
   * This method builds a string matching the required format needed in the CTP graphql API.
   *
   * @return a string representing a graphql query
   */
  protected abstract String buildQueryString();
}
