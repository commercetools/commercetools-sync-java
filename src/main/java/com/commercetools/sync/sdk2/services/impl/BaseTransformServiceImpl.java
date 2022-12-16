package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.collectionOfFuturesToFutureOfCollection;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public abstract class BaseTransformServiceImpl {
  /*
   * An id is a 36 characters long string. (i.e: 53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3) We
   * chunk them in 300 ids, we will have a query around 11.000 characters. Above this size it
   * could return - Error 413 (Request Entity Too Large)
   */
  public static final int CHUNK_SIZE = 300;
  public static final String KEY_IS_NOT_SET_PLACE_HOLDER = "KEY_IS_NOT_SET";
  protected final ReferenceIdToKeyCache referenceIdToKeyCache;

  private final ProjectApiRoot ctpClient;

  protected BaseTransformServiceImpl(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    this.ctpClient = ctpClient;
    this.referenceIdToKeyCache = referenceIdToKeyCache;
  }

  protected ProjectApiRoot getCtpClient() {
    return ctpClient;
  }

  protected CompletableFuture<Void> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final Set<String> ids, @Nonnull final GraphQlQueryResources requestType) {

    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedReferenceIds, CHUNK_SIZE);

    String query =
        "query fetchKeyToIdPairs($where: String, $limit: Int) {\n"
            + "  "
            + requestType.getName()
            + "(limit: $limit, where: $where) {\n"
            + "    results {\n"
            + "      id\n"
            + "      key\n"
            + "    }\n"
            + "  }\n"
            + "}";

    final List<GraphQLRequest> graphQLRequests =
        chunkedIds.stream()
            .map(
                keys ->
                    keys.stream()
                        .filter(id -> !isBlank(id))
                        .map(StringEscapeUtils::escapeJava)
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")))
            .map(commaSeparatedIds -> format("id in (%s)", commaSeparatedIds))
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
                .map(graphQLRequest -> getCtpClient().graphql().post(graphQLRequest).execute())
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
                            jsonNode.get(requestType.getName()).get("results").elements();
                        while (elements.hasNext()) {
                          JsonNode idAndKey = elements.next();
                          fillReferenceIdToKeyCache(
                              idAndKey.get("id").asText(), idAndKey.get("key").asText());
                        }
                      });
              return null;
            });
  }

  @Nonnull
  protected Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds.stream()
        .filter(
            id ->
                (!referenceIdToKeyCache.containsKey(id)
                    || KEY_IS_NOT_SET_PLACE_HOLDER.equals(referenceIdToKeyCache.get(id))))
        .collect(toSet());
  }

  private void fillReferenceIdToKeyCache(String id, String key) {
    final String keyValue = StringUtils.isBlank(key) ? KEY_IS_NOT_SET_PLACE_HOLDER : key;
    referenceIdToKeyCache.add(id, keyValue);
  }
}
