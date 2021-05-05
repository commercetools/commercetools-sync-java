package com.commercetools.sync.commons.helpers;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpResponse;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * A SphereRequest implementation to allow {@link SphereClient} to execute graphQL queries on CTP.
 * It provides a POST request to the CTP graphql API containing body to fetch a set of ids matching
 * given keys of a resource defined in endpoint parameter.
 */
public class ResourceKeyIdGraphQlRequest
    extends GraphQlBaseRequestImpl<ResourceKeyIdGraphQlResult> {
  protected final Set<String> keysToSearch;
  protected final GraphQlQueryResources resource;

  /**
   * Takes {@code keysToSearch} and query resource name {@link GraphQlQueryResources} to instantiate
   * a new {@link ResourceKeyIdGraphQlRequest} instance, which is an implementation of the {@link
   * SphereRequest}.
   *
   * @param keysToSearch - a set of keys to fetch matching ids for.
   * @param resource - a string representing the name of the resource endpoint.
   */
  public ResourceKeyIdGraphQlRequest(
      @Nonnull final Set<String> keysToSearch, @Nonnull final GraphQlQueryResources resource) {

    this.keysToSearch = requireNonNull(keysToSearch);
    this.resource = resource;
  }

  @Nullable
  @Override
  public ResourceKeyIdGraphQlResult deserialize(final HttpResponse httpResponse) {
    return deserializeWithResourceName(
        httpResponse, resource.getName(), ResourceKeyIdGraphQlResult.class);
  }

  /**
   * This method builds a string matching the required format to query a set of ids matching given
   * keys of a resource using the CTP graphql API
   *
   * @return a string representing a graphql query
   */
  @Nonnull
  @Override
  protected String buildQueryString() {

    return format(
        "%s(limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
        this.resource.getName(), this.limit, createWhereQuery(keysToSearch));
  }

  @Nonnull
  private String createWhereQuery(@Nonnull final Set<String> keys) {
    // The where in the graphql query should look like this in the end =>  `where: "key in
    // (\"key1\",
    // \"key2\")"`
    // So we need an escaping backslash before the quote. So to add this:
    // We need 1 backslash (2 in java) to escape the quote in the graphql query.
    // We need 2 backslashes (4 in java) to escape the backslash in the JSON payload string.
    // We need 1 extra backslash to escape the quote in the java string
    // hence: 7 backslashes:
    // And If any special character in the key, We will prefix escape character to it.
    final String backslashQuote = "\\\\\\\"";
    final String commaSeparatedKeys =
        keys.stream()
            .filter(key -> !isBlank(key))
            .map(key -> StringEscapeUtils.escapeJava(key))
            .collect(
                joining(
                    format("%s, %s", backslashQuote, backslashQuote),
                    backslashQuote,
                    backslashQuote));

    String whereQuery = createWhereQuery(commaSeparatedKeys);
    return isBlank(this.queryPredicate)
        ? whereQuery
        : format("%s AND %s", whereQuery, queryPredicate);
  }

  @Nonnull
  private static String createWhereQuery(@Nonnull final String commaSeparatedKeys) {

    return format("key in (%s)", commaSeparatedKeys);
  }
}
