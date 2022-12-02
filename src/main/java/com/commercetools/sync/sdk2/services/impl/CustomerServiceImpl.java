package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyCustomersGet;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerPagedQueryResponse;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer.CustomerUpdateBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptions;
import com.commercetools.sync.sdk2.services.CustomerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.ApiMethod;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

public final class CustomerServiceImpl extends BaseService<CustomerSyncOptions>
    implements CustomerService {

  public CustomerServiceImpl(@Nonnull final CustomerSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keysToCache) {
    final Set<String> keysNotCached = getKeysNotCached(keysToCache);

    if (keysNotCached.isEmpty()) {
      return CompletableFuture.completedFuture(keyToIdCache.asMap());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keysNotCached, CHUNK_SIZE);

    String query =
        "query fetchIdKeyPairs($where: String, $limit: Int) {\n"
            + "  customers(limit: $limit, where: $where) {\n"
            + "    results {\n"
            + "      id\n"
            + "      key\n"
            + "    }\n"
            + "  }\n"
            + "}";

    final List<GraphQLRequest> graphQLRequests =
        chunkedKeys.stream()
            .map(
                keys ->
                    keys.stream()
                        .filter(key -> !isBlank(key))
                        .map(StringEscapeUtils::escapeJava)
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")))
            .map(commaSeparatedKeys -> format("key in (%s)", commaSeparatedKeys))
            .map(
                whereQuery ->
                    GraphQLVariablesMapBuilder.of()
                        .addValue("where", whereQuery)
                        .addValue("limit", CHUNK_SIZE)
                        .build())
            .map(variables -> GraphQLRequestBuilder.of().query(query).variables(variables).build())
            .collect(Collectors.toList());

    return collectionOfFuturesToFutureOfCollection(
            graphQLRequests.stream()
                .map(
                    graphQLRequest ->
                        syncOptions.getCtpClient().graphql().post(graphQLRequest).execute())
                .collect(Collectors.toList()),
            Collectors.toList())
        .thenApply(
            graphQlResults -> {
              graphQlResults.stream()
                  .map(r -> r.getBody().getData())
                  // todo: set limit to -1, the payload will have errors object but what to do with
                  // it ?
                  //                  .filter(Objects::nonNull)
                  .forEach(
                      data -> {
                        ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                        final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
                        final Iterator<JsonNode> elements =
                            jsonNode.get("customers").get("results").elements();
                        while (elements.hasNext()) {
                          JsonNode idAndKey = elements.next();
                          keyToIdCache.put(
                              idAndKey.get("key").asText(), idAndKey.get("id").asText());
                        }
                      });
              return keyToIdCache.asMap();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Customer>> fetchMatchingCustomersByKeys(
      @Nonnull final Set<String> customerKeys) {

    if (customerKeys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(customerKeys, CHUNK_SIZE);

    final List<ByProjectKeyCustomersGet> fetchByKeysRequests =
        chunkedKeys.stream()
            .map(
                keys ->
                    keys.stream()
                        .filter(key -> !isBlank(key))
                        .map(StringEscapeUtils::escapeJava)
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")))
            .map(commaSeparatedKeys -> format("key in (%s)", commaSeparatedKeys))
            .map(
                whereQuery ->
                    syncOptions
                        .getCtpClient()
                        .customers()
                        .get()
                        .addWhere(whereQuery)
                        .withLimit(CHUNK_SIZE)
                        .withWithTotal(false))
            .collect(toList());

    // todo: what happens on error ?
    return collectionOfFuturesToFutureOfCollection(
            fetchByKeysRequests.stream().map(ApiMethod::execute).collect(Collectors.toList()),
            Collectors.toList())
        .thenApply(
            pagedCustomerResponses ->
                pagedCustomerResponses.stream()
                    .map(ApiHttpResponse::getBody)
                    .map(CustomerPagedQueryResponse::getResults)
                    .flatMap(Collection::stream)
                    .peek(customer -> keyToIdCache.put(customer.getKey(), customer.getId()))
                    .collect(Collectors.toSet()));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Customer>> fetchCustomerByKey(@Nullable final String key) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .customers()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            customer -> {
              keyToIdCache.put(customer.getKey(), customer.getId());
              return Optional.of(customer);
            })
        .exceptionally(
            throwable -> {
              if (throwable.getCause() instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo - to check with the team: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerId(@Nonnull String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }

    return fetchCustomerByKey(key).thenApply(customer -> customer.map(Customer::getId));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Customer>> createCustomer(
      @Nonnull final CustomerDraft customerDraft) {

    final String draftKey = customerDraft.getKey();

    if (isBlank(draftKey)) {
      syncOptions.applyErrorCallback(
          new SyncException(format(CREATE_FAILED, draftKey, "Draft key is blank!")),
          null,
          customerDraft,
          null);
      return CompletableFuture.completedFuture(Optional.empty());
    } else {
      return syncOptions
          .getCtpClient()
          .customers()
          .post(customerDraft)
          .execute()
          .handle(
              ((resource, exception) -> {
                if (exception == null
                    && resource.getBody() != null
                    && resource.getBody().getCustomer() != null) {
                  keyToIdCache.put(draftKey, resource.getBody().getCustomer().getId());
                  return Optional.of(resource.getBody().getCustomer());
                } else if (exception != null) {
                  syncOptions.applyErrorCallback(
                      new SyncException(
                          format(CREATE_FAILED, draftKey, exception.getMessage()), exception),
                      null,
                      customerDraft,
                      null);
                  return Optional.empty();
                } else {
                  return Optional.empty();
                }
              }));
    }
  }

  @Nonnull
  @Override
  public CompletionStage<Customer> updateCustomer(
      @Nonnull final Customer customer, @Nonnull final List<CustomerUpdateAction> updateActions) {

    final List<List<CustomerUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<Customer>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, customer));

    for (final List<CustomerUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedCustomer ->
                      syncOptions
                          .getCtpClient()
                          .customers()
                          .withId(updatedCustomer.getId())
                          .post(
                              CustomerUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedCustomer.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}