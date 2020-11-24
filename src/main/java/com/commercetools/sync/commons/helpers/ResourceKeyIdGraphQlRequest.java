package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ResourceKeyIdGraphQlRequest implements SphereRequest<ResourceKeyIdGraphQlResult> {
    protected final Set<String> keysToSearch;
    protected final GraphQlQueryResources resource;
    private long limit = 500;
    private String queryPredicate = null;

    /**
     * A SphereRequest implementation to allow {@link SphereClient} to execute graphQL queries on CTP. It provides a
     * POST request to the CTP graphql API containing body to fetch a set of ids matching given keys of a resource
     * defined in endpoint parameter.
     *
     * @param keysToSearch - a set of keys to fetch matching ids for.
     * @param resource - a string representing the name of the resource endpoint.
     */
    public ResourceKeyIdGraphQlRequest(@Nonnull final Set<String> keysToSearch,
                                       @Nonnull final GraphQlQueryResources resource) {

        this.keysToSearch = requireNonNull(keysToSearch);
        this.resource = resource;
    }

    /**
     * This method adds a predicate string to the request.
     *
     * @param predicate - a string representing a query predicate.
     * @return - an instance of this class.
     */
    @Nonnull
    public ResourceKeyIdGraphQlRequest withPredicate(final String predicate) {

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
    public ResourceKeyIdGraphQlRequest withLimit(final long limit) {

        this.limit = limit;
        return this;
    }

    @Nullable
    @Override
    public ResourceKeyIdGraphQlResult deserialize(final HttpResponse httpResponse) {

        final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
        if (rootJsonNode.isNull()) {
            return null;
        }
        JsonNode result = rootJsonNode.get("data").get(resource.getName());
        return SphereJsonUtils.readObject(result, ResourceKeyIdGraphQlResult.class);
    }

    @Override
    public HttpRequestIntent httpRequestIntent() {

        final String body = format("{\"query\": \"{%s}\"}", buildQueryString());
        return HttpRequestIntent.of(HttpMethod.POST, "/graphql", body);
    }

    /**
     * This method builds a string matching the required format to query a set of ids matching given keys of a
     * resource using the CTP graphql API
     *
     * @return a string representing a graphql query
     */
    @Nonnull
    String buildQueryString() {

        return format(
            "%s(limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
            this.resource.getName(), this.limit, createWhereQuery(keysToSearch));
    }

    @Nonnull
    private String createWhereQuery(@Nonnull final Set<String> keys) {
        // The where in the graphql query should look like this in the end =>  `where: "key in (\"key1\",
        // \"key2\")"`
        // So we need an escaping backslash before the quote. So to add this:
        // We need 1 backslash (2 in java) to escape the quote in the graphql query.
        // We need 2 backslashes (4 in java) to escape the backslash in the JSON payload string.
        // We need 1 extra backslash to escape the quote in the java string
        // hence: 7 backslashes:
        final String backslashQuote = "\\\\\\\"";
        final String commaSeparatedKeys =
            keys.stream()
                .filter(key -> !isBlank(key))
                .collect(
                    joining(
                        format("%s, %s", backslashQuote, backslashQuote),
                        backslashQuote,
                        backslashQuote));

        String whereQuery = createWhereQuery(commaSeparatedKeys);
        return isBlank(this.queryPredicate) ? whereQuery : format("%s AND %s", whereQuery, queryPredicate);
    }

    @Nonnull
    private static String createWhereQuery(@Nonnull final String commaSeparatedKeys) {

        return format("key in (%s)", commaSeparatedKeys);
    }

}

