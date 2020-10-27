package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.json.SphereJsonUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @param <T> Subclass of {@link BaseGraphQlResult} used to deserialize the graphql query results
 */
public abstract class BaseGraphQlRequest<U extends BaseGraphQlRequest<U, T>, T extends BaseGraphQlResult>
    implements SphereRequest<BaseGraphQlResult> {
    protected final Set<String> keysToSearch;
    protected final String endpoint;
    protected final Class<T> resultClazz;
    private long limit = 500;
    private String queryPredicate = null;

    protected BaseGraphQlRequest(@Nonnull final Set<String> keysToSearch, @Nonnull final String endpoint,
                                 @Nonnull final Class<T> resultClazz) {

        this.keysToSearch = keysToSearch;
        this.endpoint = endpoint;
        this.resultClazz = resultClazz;
    }

    @Nonnull
    public U withPredicate(final String predicate) {

        this.queryPredicate = predicate;
        return getThis();
    }

    @Nonnull
    public U withLimit(final long limit) {

        this.limit = limit;
        return getThis();
    }

    @Nullable
    @Override
    public T deserialize(final HttpResponse httpResponse) {

        final JsonNode rootJsonNode = SphereJsonUtils.parse(httpResponse.getResponseBody());
        if (rootJsonNode.isNull()) {
            return null;
        }
        JsonNode result = rootJsonNode.get("data");
        return SphereJsonUtils.readObject(result, resultClazz);
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

        if (isBlank(this.endpoint)) {
            return StringUtils.EMPTY;
        }
        return format(
            "%s(limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
            this.endpoint, this.limit, createWhereQuery(keysToSearch));
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
                .collect(
                    joining(
                        format("%s, %s", backslashQuote, backslashQuote),
                        backslashQuote,
                        backslashQuote));

        String whereQuery = createWhereQuery(commaSeparatedKeys);
        return this.queryPredicate == null ? whereQuery : format("%s AND %s", whereQuery, queryPredicate);
    }

    @Nonnull
    private static String createWhereQuery(@Nonnull final String commaSeparatedKeys) {

        return format("key in (%s)", commaSeparatedKeys);
    }

    /**
     * Returns {@code this} instance of {@code U}, which extends {@link BaseGraphQlRequest}. The purpose of this
     * method is to make sure that {@code this} is an instance of a class which extends {@link BaseGraphQlRequest}
     * in order to be used in the generic methods of the class. Otherwise, without this method, the methods above would
     * need to cast {@code this to U} which could lead to a runtime error of the class was extended in a wrong way.
     *
     * @return an instance of the class that overrides this method.
     */
    protected abstract U getThis();

}

