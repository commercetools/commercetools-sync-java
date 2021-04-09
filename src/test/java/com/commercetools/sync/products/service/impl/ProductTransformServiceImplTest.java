package com.commercetools.sync.products.service.impl;

import static com.commercetools.sync.services.impl.BaseTransformServiceImpl.KEY_IS_NOT_SET_PLACE_HOLDER;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.service.ProductTransformService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductTransformServiceImplTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void transform_WithAttributeReferences_ShouldReplaceAttributeReferenceIdsWithKeys() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(readObjectFromResource("product-key-4.json", Product.class).toProjection(STAGED));

    String jsonStringProducts =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d2\",\"key\":\"prod1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d6\",\"key\":\"prod2\"}]}";
    final ResourceKeyIdGraphQlResult productsResult =
        SphereJsonUtils.readObject(jsonStringProducts, ResourceKeyIdGraphQlResult.class);

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
            + "\"key\":\"prodType1\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d4\",\"key\":\"cat1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d5\",\"key\":\"cat2\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productsResult))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult));

    // test
    final List<ProductDraft> productsResolved =
        productTransformService.toProductDrafts(productPage).toCompletableFuture().join();

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
                          final JsonNode referenceSet = attribute.getValue();
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("prod1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("prod2"));
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("categoryReference");
                          final JsonNode referenceSet = attribute.getValue();
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("cat1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("cat2"));
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("productTypeReference");
                          assertThat(attribute.getValue().get("id").asText())
                              .isEqualTo("prodType1");
                        }));
  }

  @Test
  void transform_WithNonCachedCustomObjectAttributeReference_ShouldFetchAndTransformProduct() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-with-unresolved-references.json", Product.class)
                .toProjection(STAGED));

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        productTransformService.toProductDrafts(productPage).join();

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

    verify(sourceClient, times(1)).execute(any(CustomObjectQuery.class));
  }

  private void mockAttributeCustomObjectReference(SphereClient sourceClient) {
    final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
    when(mockCustomObject.getId()).thenReturn("customObjectId1");
    when(mockCustomObject.getKey()).thenReturn("customObjectKey1");
    when(mockCustomObject.getContainer()).thenReturn("customObjectContainer");
    final PagedQueryResult<CustomObject<JsonNode>> result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(singletonList(mockCustomObject));

    when(result.getResults()).thenReturn(asList(mockCustomObject));
    when(sourceClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(result));
  }

  @Test
  void
      transform_WithIrresolvableAttributeReferences_ShouldSkipProductsWithIrresolvableReferences() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-key-5.json", Product.class).toProjection(STAGED),
            readObjectFromResource("product-key-6.json", Product.class).toProjection(STAGED));

    String jsonStringProducts =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c1\"," + "\"key\":\"prod1\"}]}";
    final ResourceKeyIdGraphQlResult productsResult =
        SphereJsonUtils.readObject(jsonStringProducts, ResourceKeyIdGraphQlResult.class);

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c2\","
            + "\"key\":\"prodType1\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3\"," + "\"key\":\"cat1\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productsResult))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    // test
    final List<ProductDraft> productsFromPageStage =
        productTransformService.toProductDrafts(productPage).toCompletableFuture().join();

    // assertions
    assertThat(productsFromPageStage).hasSize(1);
    assertThat(productsFromPageStage)
        .anySatisfy(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("productReference");
                          assertThat(attribute.getValue().get("id").asText()).isEqualTo("prod1");
                        }));

    assertThat(productsFromPageStage)
        .anySatisfy(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("categoryReference");
                          assertThat(attribute.getValue().get("id").asText()).isEqualTo("cat1");
                        }));

    assertThat(productsFromPageStage)
        .anySatisfy(
            product ->
                assertThat(product.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attribute -> {
                          assertThat(attribute.getName()).isEqualTo("productTypeReference");
                          assertThat(attribute.getValue().get("id").asText())
                              .isEqualTo("prodType1");
                        }));
  }

  @Test
  void transform_ProductReferences_ShouldReplaceReferencesIdsWithKeysAndMapToProductDraft() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-with-unresolved-references.json", Product.class)
                .toProjection(STAGED));

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    String jsonStringState =
        "{\"results\":[{\"id\":\"ste95fb-2282-4f9a-8747-fbe440e02dcs0\","
            + "\"key\":\"stateKey\"}]}";
    final ResourceKeyIdGraphQlResult statesResult =
        SphereJsonUtils.readObject(jsonStringState, ResourceKeyIdGraphQlResult.class);

    String jsonStringTaxCategory =
        "{\"results\":[{\"id\":\"ebbe95fb-2282-4f9a-8747-fbe440e02dc0\","
            + "\"key\":\"taxCategoryKey\"}]}";
    final ResourceKeyIdGraphQlResult taxCategoryResult =
        SphereJsonUtils.readObject(jsonStringTaxCategory, ResourceKeyIdGraphQlResult.class);

    String jsonStringCustomerGroup =
        "{\"results\":[{\"id\":\"d1229e6f-2b79-441e-b419-180311e52754\","
            + "\"key\":\"customerGroupKey\"}]}";
    final ResourceKeyIdGraphQlResult customerGroupResult =
        SphereJsonUtils.readObject(jsonStringCustomerGroup, ResourceKeyIdGraphQlResult.class);

    String jsonStringChannel =
        "{\"results\":[{\"id\":\"cdcf8bea-48f2-54bc-b3c2-cdc94bf94f2c\","
            + "\"key\":\"channelKey\"}]}";
    final ResourceKeyIdGraphQlResult channelResult =
        SphereJsonUtils.readObject(jsonStringChannel, ResourceKeyIdGraphQlResult.class);

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f\",\"key\":\"categoryKey1\"},"
            + "{\"id\":\"2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f\",\"key\":\"categoryKey2\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\"custom_type_id\"," + "\"key\":\"customTypeId\"}]}";
    final ResourceKeyIdGraphQlResult customTypesResult =
        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult))
        .thenReturn(CompletableFuture.completedFuture(statesResult))
        .thenReturn(CompletableFuture.completedFuture(channelResult))
        .thenReturn(CompletableFuture.completedFuture(taxCategoryResult))
        .thenReturn(CompletableFuture.completedFuture(customerGroupResult))
        .thenReturn(CompletableFuture.completedFuture(customTypesResult));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        productTransformService.toProductDrafts(productPage).toCompletableFuture().join();

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
      transform_ProductWithProductTypeReferencesWithNullKey_ShouldReplaceReferencesKeyValueWithPlaceHolder() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-with-unresolved-references.json", Product.class)
                .toProjection(STAGED));

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":"
            + null
            + "}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        productTransformService.toProductDrafts(productPage).toCompletableFuture().join();

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
      transform_ProductWithProductTypeReferencesWithNullKeyAlreadyInCache_ShouldFetchAndReplaceReferencesKeyValue() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    referenceIdToKeyCache.add("cda0dbf7-b42e-40bf-8453-241d5b587f93", KEY_IS_NOT_SET_PLACE_HOLDER);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-with-unresolved-references.json", Product.class)
                .toProjection(STAGED));

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"cda0dbf7-b42e-40bf-8453-241d5b587f93\","
            + "\"key\":\"productTypeKey\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    mockAttributeCustomObjectReference(sourceClient);

    // test
    final List<ProductDraft> productsResolved =
        productTransformService.toProductDrafts(productPage).toCompletableFuture().join();

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
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTransformService productTransformService =
        new ProductTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<ProductProjection> productPage =
        asList(
            readObjectFromResource("product-key-5.json", Product.class).toProjection(STAGED),
            readObjectFromResource("product-key-6.json", Product.class).toProjection(STAGED));

    final BadGatewayException badGatewayException =
        new BadGatewayException("Failed Graphql request");
    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));

    // test
    final CompletionStage<List<ProductDraft>> productDraftsFromPageStage =
        productTransformService.toProductDrafts(productPage);

    // assertions
    assertThat(productDraftsFromPageStage)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceTransformException.class)
        .withRootCauseExactlyInstanceOf(BadGatewayException.class);
    ;
  }
}
