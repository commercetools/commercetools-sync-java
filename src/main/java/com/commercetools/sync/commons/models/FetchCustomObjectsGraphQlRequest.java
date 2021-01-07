package com.commercetools.sync.commons.models;

import com.commercetools.sync.commons.helpers.CommonGraphQlRequestImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.http.HttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class FetchCustomObjectsGraphQlRequest extends CommonGraphQlRequestImpl<ResourceKeyIdGraphQlResult> {

    private final String container;
    private final Instant lastModifiedAt;
    private final String resourceName = GraphQlQueryResources.CUSTOM_OBJECTS.getName();

    /**
     * A SphereRequest implementation to allow {@link SphereClient} to execute graphQL queries on CTP. It provides a
     * POST request to the CTP graphql API containing body to fetch a set of keys matching given container of a resource
     * defined in endpoint parameter.
     *
     * @param container - A namespace to group custom objects.
     * @param lastModifiedAt - lastModifiedAt will be used in where param.
     */
    public FetchCustomObjectsGraphQlRequest(
        @Nonnull final String container,
        @Nonnull final Instant lastModifiedAt) {

        this.container = container;
        this.lastModifiedAt = lastModifiedAt;
    }

    @Nullable
    @Override
    public ResourceKeyIdGraphQlResult deserialize(final HttpResponse httpResponse) {

        return deserializeWithResourceName(httpResponse, resourceName, ResourceKeyIdGraphQlResult.class);
    }

    @Nonnull
    @Override
    protected String buildQueryString() {

        return format(
            "%s(container: \\\"%s\\\", limit: %d, where: \\\"%s\\\", sort: [\\\"id asc\\\"]) { results { id key } }",
            this.resourceName, this.container, this.limit, createWhereQuery());
    }

    @Nonnull
    private String createWhereQuery() {
        final String whereQuery = format("lastModifiedAt < \\\\\\\"%s\\\\\\\"\\\"", lastModifiedAt);
        return isBlank(this.queryPredicate) ? whereQuery : format("%s AND %s", whereQuery, queryPredicate);
    }
}

