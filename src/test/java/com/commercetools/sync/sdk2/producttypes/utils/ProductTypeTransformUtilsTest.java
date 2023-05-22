package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeMockUtils.getAttributeDefinitionBuilder;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeMockUtils.getProductTypeBuilder;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ByProjectKeyGraphqlRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductTypeTransformUtilsTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void setup() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNoAttributeDefs_ShouldReturnProductType() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);

    final ProductType productTypeFoo =
        getProductTypeBuilder().key("foo").attributes(emptyList()).build();

    final List<ProductType> productTypes = singletonList(productTypeFoo);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                sourceClient, referenceIdToKeyCache, productTypes)
            .join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(ResourceToDraftConverters.toProductTypeDraft(productTypeFoo));
  }

  @Test
  void mapToProductDrafts_WithNoReferences_ShouldReturnCorrectProductTypeDrafts() {
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);

    final AttributeDefinition stringAttr =
        getAttributeDefinitionBuilder().type(AttributeTypeBuilder::textBuilder).build();

    final AttributeDefinition numberAttr =
        getAttributeDefinitionBuilder()
            .name("a")
            .label(ofEnglish("b"))
            .type(AttributeTypeBuilder::numberBuilder)
            .build();

    final ProductType productTypeFoo =
        getProductTypeBuilder().key("ProductTypeFoo").attributes(singletonList(stringAttr)).build();

    final ProductType productTypeBar =
        getProductTypeBuilder().key("ProductTypeBar").attributes(singletonList(numberAttr)).build();

    final List<ProductType> productTypes = asList(productTypeFoo, productTypeBar);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                sourceClient, referenceIdToKeyCache, productTypes)
            .join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(
            ResourceToDraftConverters.toProductTypeDraft(productTypeFoo),
            ResourceToDraftConverters.toProductTypeDraft(productTypeBar));
  }



  @Test
  void mapToProductDrafts_whenProductTypeContainsNestedAttribute_ShouldReplaceProductTypeAttributeNestedTypeIdsWithKeys() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductTypeReference productTypeReference =
            ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
            getAttributeDefinitionBuilder()
                    .name("nestedattr")
                    .type(attributeTypeBuilder -> attributeTypeBuilder.nestedBuilder()
                            .typeReference(productTypeReference))
                    .build();

    final String jsonStringProductTypes =
            "{\"data\":{\"productTypes\":{\"results\":[{\"id\":\""
                    + referencedProductTypeId
                    + "\","
                    + "\"key\":\""
                    + referencedProductTypeKey
                    + "\"}]}}}";
    final GraphQLResponse productTypesResult =
            fromJsonString(jsonStringProductTypes, GraphQLResponse.class);

    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(sourceClient.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
            .thenReturn(byProjectKeyGraphqlPost);
    final CompletableFuture<ApiHttpResponse<GraphQLResponse>> apiHttpResponseCompletableFuture =
            CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productTypesResult));
    when(byProjectKeyGraphqlPost.execute()).thenReturn(apiHttpResponseCompletableFuture);

    final ProductType productType =
            getProductTypeBuilder()
                    .key("withNestedTypeAttr")
                    .attributes(singletonList(nestedTypeAttr))
                    .build();

    // test
    final List<ProductTypeDraft> productTypeDrafts =
            ProductTypeTransformUtils.toProductTypeDrafts(
                            sourceClient, referenceIdToKeyCache, singletonList(productType))
                    .join();


    // assertion
    assertThat(productTypeDrafts)
            .satisfies(
                    productTypeDraft -> {
                      final AttributeNestedType nestedAttributeType =
                              (AttributeNestedType) productTypeDraft.get(0).getAttributes().get(0).getType();
                      assertThat(nestedAttributeType.getTypeReference().getId())
                              .isEqualTo(referencedProductTypeKey);
                    });
  }

  @Test
  void mapToProductDrafts_whenProductTypeContainsSetOfNestedAttributes_ShouldReplaceProductTypeNestedAttributeReferenceIdsWithKeys() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        getAttributeDefinitionBuilder()
            .name("nestedattr")
            .label(ofEnglish("nestedattr"))
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(productTypeReference)))
            .build();

    final ProductType productType =
        getProductTypeBuilder()
            .key("withNestedTypeAttr")
            .attributes(singletonList(nestedTypeAttr))
            .build();

    final List<ProductType> productTypes = singletonList(productType);

    final String jsonStringProductTypes =
        "{\"data\":{\"productTypes\":{\"results\":[{\"id\":\""
            + referencedProductTypeId
            + "\","
            + "\"key\":\""
            + referencedProductTypeKey
            + "\"}]}}}";
    final GraphQLResponse productTypesResult =
        fromJsonString(jsonStringProductTypes, GraphQLResponse.class);

    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(sourceClient.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    final CompletableFuture<ApiHttpResponse<GraphQLResponse>> apiHttpResponseCompletableFuture =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productTypesResult));
    when(byProjectKeyGraphqlPost.execute()).thenReturn(apiHttpResponseCompletableFuture);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                sourceClient, referenceIdToKeyCache, productTypes)
            .join();

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final AttributeSetType setAttributeType =
                  (AttributeSetType) productTypeDraft.get(0).getAttributes().get(0).getType();
              final AttributeNestedType nestedAttributeType =
                  (AttributeNestedType) setAttributeType.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductTypeKey);
            });
  }
}
