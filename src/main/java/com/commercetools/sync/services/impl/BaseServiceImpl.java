package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPathContainer;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.MetaModelQueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.ResourceQueryModel;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The abstraction of service implementation. Extract of common operation using generics approach.
 *
 * @param <D> the resource draft
 * @param <T> the resource, implementation of the {@link Resource}
 * @param <O> the resource sync options, subclass of the {@link BaseSyncOptions}
 * @param <Q> the resource query, implementation of the {@link MetaModelQueryDsl}
 * @param <M> the resource query model, implementation of the {@link ResourceQueryModel}
 * @param <E> the resource expansion model, implementation of the {@link ExpansionPathContainer}
 */
abstract class BaseServiceImpl<D, T extends Resource<T>, O extends BaseSyncOptions,
    Q extends MetaModelQueryDsl<T, Q, M, E>, M, E extends ExpansionPathContainer<T>> extends BaseService<D, T, O> {

    BaseServiceImpl(@Nonnull final O syncOptions) {
        super(syncOptions);
    }

    /**
     * Given a {@code key}, this method first checks if a cached map of resource keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all resource keys to ids
     * in the CTP project, by querying the CTP project for all Tax categories.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no resource was
     * found in the CTP project with this key.
     *
     * @param key the key by which a resource id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         resource was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedResourceId(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.isEmpty()) {
            return fetchAndCache(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    /**
     * Given a {@code key}, this method queries a CTP project and fetches all available resource of given type.
     * Next it stores key - id pair in the {@code keyToIdCache}.
     *
     * @param key the key by which a resource id should be fetched from the CTP project
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching id if exists, otherwise empty.
     */
    abstract CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key);

    /**
     * The default implementation of the {@link #fetchAndCache(String)} abstract method.
     *
     * @param key       the key by which a resource id should be fetched from the CTP project
     * @param query     a function to get a query
     * @param keyMapper a function to get the key from the returned resource
     * @param typeName  a resource type name, f.e.: <i>ProductType</i>
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching id if exists, otherwise empty.
     */
    CompletionStage<Optional<String>> fetchAndCache(@Nullable final String key,
                                                    @Nonnull final Supplier<Q> query,
                                                    @Nonnull final Function<T, String> keyMapper,
                                                    @Nonnull final String typeName) {
        final Consumer<List<T>> resourcePageConsumer = resourcePage ->
            resourcePage.forEach(resource -> {
                final String fetchedResourceKey = keyMapper.apply(resource);
                final String id = resource.getId();
                if (StringUtils.isNotBlank(fetchedResourceKey)) {
                    keyToIdCache.put(fetchedResourceKey, id);
                } else {
                    syncOptions.applyWarningCallback(format("%s with id: '%s' has no key set. Keys are"
                        + " required for resource matching.", typeName, id));
                }
            });

        return CtpQueryUtils
            .queryAll(syncOptions.getCtpClient(), query.get(), resourcePageConsumer)
            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    /**
     * Given a {@link Set} of resource keys, this method fetches a set of all the resources, matching this given set of
     * keys in the CTP project, defined in an injected {@link SphereClient}. A mapping of the key to the id
     * of the fetched resources is persisted in an in-memory map.
     *
     * @param resourceKeys set of state keys to fetch matching states by
     * @param query        a function to get query
     * @param keyMapper    a function to get the key from the returned resource
     * @return {@link CompletionStage}&lt;{@link Set}&lt;{@code T}&gt;&gt; in which the result of it's completion
     *         contains a {@link Set} of all matching resources.
     */
    @Nonnull
    CompletionStage<Set<T>> fetchMatchingResources(@Nonnull final Set<String> resourceKeys,
                                                   @Nonnull final Supplier<Q> query,
                                                   @Nonnull final Function<T, String> keyMapper) {
        if (resourceKeys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        return CtpQueryUtils
            .queryAll(syncOptions.getCtpClient(), query.get(), Function.identity())
            .thenApply(fetchedResources -> fetchedResources
                .stream()
                .flatMap(List::stream)
                .peek(resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()))
                .collect(Collectors.toSet()));
    }

    /**
     * Given a resource key, this method fetches a resource that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching resource an empty {@link Optional} will be
     * returned in the returned future. A mapping of the key to the id of the fetched category is persisted in an in
     * -memory map.
     *
     * @param key       the key of the resource to fetch
     * @param query     a function to get query
     * @param keyMapper a function to get the key from the returned resource
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@code T} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<T>> fetchResource(@Nullable final String key,
                                               @Nonnull final Supplier<Q> query,
                                               @Nonnull final Function<T, String> keyMapper) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return syncOptions
            .getCtpClient()
            .execute(query.get())
            .thenApply(pagedQueryResult -> pagedQueryResult
                .head()
                .map(resource -> {
                    keyToIdCache.put(keyMapper.apply(resource), resource.getId());
                    return resource;
                }));
    }

    /**
     * Builds query predicate based on provided set of keys.
     *
     * @param resourceKeys set of resource keys
     * @return the query predicate with set of keys transformed into string
     */
    QueryPredicate<T> buildResourceKeysQueryPredicate(@Nonnull final Set<String> resourceKeys) {
        return buildResourceQueryPredicate(resourceKeys, "key");
    }

    /**
     * Builds query predicate based on provided set of values and property name.
     *
     * @param propertyValues set of resource property values
     * @param propertyName   name of the property to create query for
     * @return the query predicate with set of keys transformed into string
     */
    QueryPredicate<T> buildResourceQueryPredicate(@Nonnull final Set<String> propertyValues,
                                                  @Nonnull final String propertyName) {
        final String keysQueryString = propertyValues.stream()
            .filter(StringUtils::isNotBlank)
            .map(resourceKey -> format("\"%s\"", resourceKey))
            .collect(Collectors.joining(", "));
        return QueryPredicate.of(format("%s in (%s)", propertyName, keysQueryString));
    }

}
