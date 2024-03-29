package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.CustomValueConverter.isValidTextNode;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.custom_object.CustomObjectReference;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.shopping_list.ShoppingListReference;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
      @Nonnull final Set<String> ids, @Nonnull final GraphQlQueryResource requestType) {

    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedReferenceIds, CHUNK_SIZE);

    final List<GraphQLRequest> graphQLRequests = createGraphQLRequests(chunkedIds, requestType);

    return ChunkUtils.executeChunks(getCtpClient(), graphQLRequests)
        .thenAccept(graphQLResult -> cacheResourceReferenceKeys(graphQLResult, requestType));
  }

  protected Map<GraphQlQueryResource, Set<String>> buildMapOfRequestTypeToReferencedIds(
      final Set<JsonNode> references) {
    Map<GraphQlQueryResource, Set<String>> typedReferenceMap = new HashMap<>();
    typedReferenceMap.put(GraphQlQueryResource.CATEGORIES, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.CHANNELS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.CUSTOMERS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.CUSTOM_OBJECTS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.CUSTOMER_GROUPS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.PRODUCT_TYPES, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.PRODUCTS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.SHOPPING_LISTS, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.STATES, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.TAX_CATEGORIES, new HashSet<>());
    typedReferenceMap.put(GraphQlQueryResource.TYPES, new HashSet<>());
    references.forEach(
        ref -> {
          final String refAsText = ref.get(REFERENCE_ID_FIELD).asText();
          switch (ref.get(REFERENCE_TYPE_ID_FIELD).asText()) {
            case (ProductReference.PRODUCT):
              typedReferenceMap.get(GraphQlQueryResource.PRODUCTS).add(refAsText);
              break;
            case (CategoryReference.CATEGORY):
              typedReferenceMap.get(GraphQlQueryResource.CATEGORIES).add(refAsText);
              break;
            case (ChannelReference.CHANNEL):
              typedReferenceMap.get(GraphQlQueryResource.CHANNELS).add(refAsText);
              break;
            case (CustomerReference.CUSTOMER):
              typedReferenceMap.get(GraphQlQueryResource.CUSTOMERS).add(refAsText);
              break;
            case (CustomObjectReference.KEY_VALUE_DOCUMENT):
              typedReferenceMap.get(GraphQlQueryResource.CUSTOM_OBJECTS).add(refAsText);
              break;
            case (CustomerGroupReference.CUSTOMER_GROUP):
              typedReferenceMap.get(GraphQlQueryResource.CUSTOMER_GROUPS).add(refAsText);
              break;
            case (ProductTypeReference.PRODUCT_TYPE):
              typedReferenceMap.get(GraphQlQueryResource.PRODUCT_TYPES).add(refAsText);
              break;
            case (ShoppingListReference.SHOPPING_LIST):
              typedReferenceMap.get(GraphQlQueryResource.SHOPPING_LISTS).add(refAsText);
              break;
            case (StateReference.STATE):
              typedReferenceMap.get(GraphQlQueryResource.STATES).add(refAsText);
              break;
            case (TaxCategoryReference.TAX_CATEGORY):
              typedReferenceMap.get(GraphQlQueryResource.TAX_CATEGORIES).add(refAsText);
              break;
            case (TypeReference.TYPE):
              typedReferenceMap.get(GraphQlQueryResource.TYPES).add(refAsText);
              break;
            default:
              // Nothing to do
              break;
          }
        });
    return typedReferenceMap;
  }

  @Nonnull
  protected Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds.stream().filter(id -> filterNonCachedIds(id)).collect(toSet());
  }

  @Nonnull
  protected Set<JsonNode> getNonCachedReferences(@Nonnull final List<JsonNode> references) {
    return references.stream()
        .filter(ref -> filterNonCachedIds(ref.get(REFERENCE_ID_FIELD).asText()))
        .collect(toSet());
  }

  private boolean filterNonCachedIds(@Nonnull final String id) {
    return !referenceIdToKeyCache.containsKey(id)
        || KEY_IS_NOT_SET_PLACE_HOLDER.equals(referenceIdToKeyCache.get(id));
  }

  @Nonnull
  protected List<GraphQLRequest> createGraphQLRequests(
      @Nonnull final List<List<String>> chunkedIds,
      @Nonnull final GraphQlQueryResource requestType) {

    final String query =
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
    return graphQLRequests;
  }

  protected void cacheResourceReferenceKeys(
      @Nonnull final List<ApiHttpResponse<GraphQLResponse>> graphQLResults) {
    GraphQlQueryResource[] queryResources = GraphQlQueryResource.values();
    for (int i = 0; i < queryResources.length; i++) {
      cacheResourceReferenceKeys(graphQLResults, queryResources[i]);
    }
  }

  protected void cacheResourceReferenceKeys(
      @Nonnull final List<ApiHttpResponse<GraphQLResponse>> graphQLResults,
      @Nonnull final GraphQlQueryResource requestType) {
    graphQLResults.stream()
        .map(ApiHttpResponse::getBody)
        .filter(Objects::nonNull)
        .map(GraphQLResponse::getData)
        .filter(Objects::nonNull)
        .forEach(
            data -> {
              final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
              final JsonNode jsonNode = objectMapper.convertValue(data, JsonNode.class);
              final String requestTypeName = requestType.getName();
              if (jsonNode.get(requestTypeName) != null
                  && jsonNode.get(requestTypeName).get("results") != null) {
                final Iterator<JsonNode> elements =
                    jsonNode.get(requestTypeName).get("results").elements();
                while (elements.hasNext()) {
                  JsonNode idAndKey = elements.next();
                  fillReferenceIdToKeyCache(idAndKey.get("id"), idAndKey.get("key"));
                }
              }
            });
  }

  private void fillReferenceIdToKeyCache(@Nullable JsonNode id, @Nullable JsonNode key) {
    if (isValidTextNode(id)) {
      final String idValue = id.asText();
      final String keyValue = isValidTextNode(key) ? key.asText() : KEY_IS_NOT_SET_PLACE_HOLDER;
      referenceIdToKeyCache.add(idValue, keyValue);
    }
  }
}
