package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl.KEY_IS_NOT_SET_PLACE_HOLDER;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.graph_ql.GraphQLResponseBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.futures.CompletableFutures;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class ProductTransformUtilsTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void transform_WithAttributeReferences_ShouldReplaceAttributeReferenceIdsWithKeys()
      throws Exception {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<ProductProjection> productPage = asList(createProductFromJson("product-key-4.json"));

    final String jsonStringProducts =
        "{ \"products\": {\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d2\",\"key\":\"prod1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d6\",\"key\":\"prod2\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productsResponse =
        mockGraphQLResponse(jsonStringProducts);

    final String jsonStringProductTypes =
        "{ \"productTypes\": {\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
            + "\"key\":\"prodType1\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productTypesResponse =
        mockGraphQLResponse(jsonStringProductTypes);

    final String jsonStringCategories =
        "{ \"categories\": {\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d4\",\"key\":\"cat1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d5\",\"key\":\"cat2\"}]}}";
    final ApiHttpResponse<GraphQLResponse> categoriesResponse =
        mockGraphQLResponse(jsonStringCategories);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(productsResponse))
        .thenReturn(CompletableFuture.completedFuture(productTypesResponse))
        .thenReturn(CompletableFuture.completedFuture(categoriesResponse))
        .thenReturn(CompletableFuture.completedFuture(mock(ApiHttpResponse.class)));

    // test
    final List<ProductDraft> productsResolved =
        ProductTransformUtils.toProductDrafts(sourceClient, referenceIdToKeyCache, productPage)
            .join();

    // assertions
    final Optional<ProductDraft> productKey1 =
        productsResolved.stream()
            .filter(productDraft -> "productKey4".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("productReference");
                          final List<ProductReference> referenceSet = (List) attribute.getValue();
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference -> assertThat(reference.getId()).isEqualTo("prod1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference -> assertThat(reference.getId()).isEqualTo("prod2"));
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("categoryReference");
                          final List<CategoryReference> referenceSet = (List) attribute.getValue();
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference -> assertThat(reference.getId()).isEqualTo("cat1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference -> assertThat(reference.getId()).isEqualTo("cat2"));
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("productTypeReference");
                          assertThat(((ProductTypeReference) attribute.getValue()).getId())
                              .isEqualTo("prodType1");
                        }));
  }

  @NotNull
  private ApiHttpResponse<GraphQLResponse> mockGraphQLResponse(final String jsonResponseString)
      throws JsonProcessingException {
    ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final Map responseMap = objectMapper.readValue(jsonResponseString, Map.class);
    final ApiHttpResponse<GraphQLResponse> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody())
        .thenReturn(GraphQLResponseBuilder.of().data(responseMap).build());
    return apiHttpResponse;
  }

  @Test
  void transform_WithNonCachedCustomObjectAttributeReference_ShouldFetchAndTransformProduct()
      throws Exception {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<ProductProjection> productPage =
        asList(createProductFromJson("product-with-unresolved-references.json"));

    String jsonStringProductTypes =
        "{ \"productTypes\": {\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productTypesResponse =
        mockGraphQLResponse(jsonStringProductTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(productTypesResponse))
        .thenReturn(CompletableFuture.completedFuture(mock(ApiHttpResponse.class)));

    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        ProductTransformUtils.toProductDrafts(sourceClient, referenceIdToKeyCache, productPage)
            .toCompletableFuture()
            .join();

    final Optional<ProductDraft> productKey1 =
        productsResolved.stream()
            .filter(productDraft -> "productKeyResolved".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("customObjectReference");
                        }));

    verify(byProjectKeyCustomObjectsGet, times(1)).execute();
  }

  private ByProjectKeyCustomObjectsGet mockAttributeCustomObjectReference(
      ProjectApiRoot sourceClient) {
    final CustomObject mockCustomObject = mock(CustomObject.class);
    when(mockCustomObject.getId()).thenReturn("customObjectId1");
    when(mockCustomObject.getKey()).thenReturn("customObjectKey1");
    when(mockCustomObject.getContainer()).thenReturn("customObjectContainer");
    final CustomObjectPagedQueryResponse result = mock(CustomObjectPagedQueryResponse.class);
    when(result.getResults()).thenReturn(singletonList(mockCustomObject));
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);

    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mock(ByProjectKeyCustomObjectsGet.class);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withPredicateVar(anyString(), any()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCustomObjectsGet);

    when(sourceClient.customObjects()).thenReturn(mock());
    when(sourceClient.customObjects().get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    return byProjectKeyCustomObjectsGet;
  }

  @Test
  void transform_ProductReferences_ShouldReplaceReferencesIdsWithKeysAndMapToProductDraft()
      throws Exception {
    // preparation
    final List<ProductProjection> productPage =
        asList(createProductFromJson("product-with-unresolved-references.json"));

    String jsonStringProductTypes =
        "{ \"productTypes\": {\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}}";

    String jsonStringState =
        "{ \"states\": {\"results\":[{\"id\":\"ste95fb-2282-4f9a-8747-fbe440e02dcs0\","
            + "\"key\":\"stateKey\"}]}}";

    String jsonStringTaxCategory =
        "{ \"taxCategories\": {\"results\":[{\"id\":\"ebbe95fb-2282-4f9a-8747-fbe440e02dc0\","
            + "\"key\":\"taxCategoryKey\"}]}}";

    String jsonStringCustomerGroup =
        "{ \"customerGroups\": {\"results\":[{\"id\":\"d1229e6f-2b79-441e-b419-180311e52754\","
            + "\"key\":\"customerGroupKey\"}]}}";

    String jsonStringChannel =
        "{ \"channels\": {\"results\":[{\"id\":\"cdcf8bea-48f2-54bc-b3c2-cdc94bf94f2c\","
            + "\"key\":\"channelKey\"}]}}";

    String jsonStringCategories =
        "{ \"categories\": {\"results\":[{\"id\":\"1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f\",\"key\":\"categoryKey1\"},"
            + "{\"id\":\"2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f\",\"key\":\"categoryKey2\"}]}}";

    String jsonStringCustomTypes =
        "{ \"typeDefinitions\": {\"results\":[{\"id\":\"custom_type_id\","
            + "\"key\":\"customTypeId\"}]}}";

    String jsonStringCustomObjects =
        "{\"results\":[{\"id\":\"customObjectId1\","
            + "\"key\":\"customObjectKey1\", \"container\": \"customObjectContainer\"}]}";

    ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  final Charset charsetUTF8 = Charset.forName(StandardCharsets.UTF_8.name());
                  if (uri.contains("graphql") && ApiHttpMethod.POST.equals(method)) {
                    final String encodedRequestBody =
                        new String(request.getBody(), StandardCharsets.UTF_8);
                    ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                    GraphQLRequest graphQLRequest;
                    try {
                      graphQLRequest =
                          objectMapper.readValue(encodedRequestBody, GraphQLRequest.class);
                    } catch (JsonProcessingException e) {
                      graphQLRequest = GraphQLRequestBuilder.of().build();
                    }
                    final String graphQLRequestQuery = graphQLRequest.getQuery();
                    final String bodyData = "{\"data\": %s}";
                    String result = String.format(bodyData, "{}");
                    if (graphQLRequestQuery != null
                        && graphQLRequestQuery.contains("fetchKeyToIdPairs")) {
                      if (graphQLRequestQuery.contains("productTypes")) {
                        result = String.format(bodyData, jsonStringProductTypes);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("states")) {
                        result = String.format(bodyData, jsonStringState);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("taxCategories")) {
                        result = String.format(bodyData, jsonStringTaxCategory);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("customerGroups")) {
                        result = String.format(bodyData, jsonStringCustomerGroup);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("channels")) {
                        result = String.format(bodyData, jsonStringChannel);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("categories")) {
                        result = String.format(bodyData, jsonStringCategories);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else if (graphQLRequestQuery.contains("typeDefinitions")) {
                        result = String.format(bodyData, jsonStringCustomTypes);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else {
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      }
                    }
                  }
                  if (uri.contains("custom-objects") && ApiHttpMethod.GET.equals(method)) {
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(
                            200, null, jsonStringCustomObjects.getBytes(charsetUTF8)));
                  }
                  return null;
                })
            .withApiBaseUrl("testBaseUrl")
            .build("testClient");

    // test
    final List<ProductDraft> productsResolved =
        ProductTransformUtils.toProductDrafts(testClient, referenceIdToKeyCache, productPage)
            .toCompletableFuture()
            .join();

    // assertions

    final Optional<ProductDraft> productKey1 =
        productsResolved.stream()
            .filter(productDraft -> "productKeyResolved".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getPrices())
                    .anySatisfy(
                        priceDraft -> {
                          assertThat(priceDraft.getChannel().getKey()).isEqualTo("channelKey");
                          assertThat(priceDraft.getCustomerGroup().getKey())
                              .isEqualTo("customerGroupKey");
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft -> {
              assertThat(productDraft.getProductType().getKey()).isEqualTo("productTypeKey");
              assertThat(productDraft.getState().getKey()).isEqualTo("stateKey");
              assertThat(productDraft.getTaxCategory().getKey()).isEqualTo("taxCategoryKey");
              assertThat(productDraft.getCategories())
                  .anySatisfy(
                      categoryDraft -> {
                        assertThat(categoryDraft.getKey()).isEqualTo("categoryKey1");
                      });
            });
  }

  @Test
  void
      transform_ProductWithProductTypeReferencesWithNullKey_ShouldReplaceReferencesKeyValueWithPlaceHolder()
          throws Exception {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<ProductProjection> productPage =
        asList(createProductFromJson("product-with-unresolved-references.json"));

    String jsonStringProductTypes =
        "{ \"productTypes\": {\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":"
            + null
            + "}]}}";
    final ApiHttpResponse<GraphQLResponse> productTypesResponse =
        mockGraphQLResponse(jsonStringProductTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(productTypesResponse))
        .thenReturn(CompletableFuture.completedFuture(mock(ApiHttpResponse.class)));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        ProductTransformUtils.toProductDrafts(sourceClient, referenceIdToKeyCache, productPage)
            .toCompletableFuture()
            .join();

    // assertions

    final Optional<ProductDraft> productKey1 =
        productsResolved.stream()
            .filter(productDraft -> "productKeyResolved".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getProductType().getKey())
                    .isEqualTo(KEY_IS_NOT_SET_PLACE_HOLDER));
  }

  @Test
  void
      transform_ProductWithProductTypeReferencesWithNullKeyAlreadyInCache_ShouldFetchAndReplaceReferencesKeyValue()
          throws Exception {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    referenceIdToKeyCache.add("cda0dbf7-b42e-40bf-8453-241d5b587f93", KEY_IS_NOT_SET_PLACE_HOLDER);
    final List<ProductProjection> productPage =
        asList(createProductFromJson("product-with-unresolved-references.json"));

    String jsonStringProductTypes =
        "{ \"productTypes\": {\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}}";
    final ApiHttpResponse<GraphQLResponse> productTypesResponse =
        mockGraphQLResponse(jsonStringProductTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(productTypesResponse))
        .thenReturn(CompletableFuture.completedFuture(mock(ApiHttpResponse.class)));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        ProductTransformUtils.toProductDrafts(sourceClient, referenceIdToKeyCache, productPage)
            .toCompletableFuture()
            .join();

    // assertions

    final Optional<ProductDraft> productKey1 =
        productsResolved.stream()
            .filter(productDraft -> "productKeyResolved".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getProductType().getKey()).isEqualTo("productTypeKey"));
  }

  @Test
  void transform_WithErrorOnGraphQlRequest_ShouldThrowReferenceTransformException() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<ProductProjection> productPage =
        asList(
            createProductFromJson("product-key-5.json"),
            createProductFromJson("product-key-6.json"));

    final BadGatewayException badGatewayException =
        new BadGatewayException(500, "", null, "Failed Graphql request", null);
    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);
    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFutures.exceptionallyCompletedFuture(badGatewayException));

    // test
    final CompletionStage<List<ProductDraft>> productDraftsFromPageStage =
        ProductTransformUtils.toProductDrafts(sourceClient, referenceIdToKeyCache, productPage);

    // assertions
    assertThat(productDraftsFromPageStage)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceTransformException.class)
        .withRootCauseExactlyInstanceOf(BadGatewayException.class);
    ;
  }
}
