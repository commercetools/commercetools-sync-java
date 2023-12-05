package com.commercetools.sync.producttypes.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.producttypes.MockBuilderUtils.createMockAttributeDefinitionDraftBuilder;
import static com.commercetools.sync.producttypes.MockBuilderUtils.createMockProductTypeDraftBuilder;
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
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.producttypes.MockBuilderUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductTypeTransformUtilsTest {

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
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
        MockBuilderUtils.createMockProductTypeBuilder().key("foo").attributes(emptyList()).build();

    final List<ProductType> productTypes = singletonList(productTypeFoo);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                sourceClient, referenceIdToKeyCache, productTypes)
            .join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(
            createMockProductTypeDraftBuilder().key("foo").attributes(emptyList()).build());
  }

  @Test
  void mapToProductDrafts_WithNoReferences_ShouldReturnCorrectProductTypeDrafts() {
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);

    final AttributeDefinition stringAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(AttributeTypeBuilder::textBuilder)
            .build();

    final AttributeDefinition numberAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .name("a")
            .label(ofEnglish("b"))
            .type(AttributeTypeBuilder::numberBuilder)
            .build();

    final ProductType productTypeFoo =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("ProductTypeFoo")
            .attributes(singletonList(stringAttr))
            .build();

    final ProductType productTypeBar =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key("ProductTypeBar")
            .attributes(singletonList(numberAttr))
            .build();

    final List<ProductType> productTypes = asList(productTypeFoo, productTypeBar);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                sourceClient, referenceIdToKeyCache, productTypes)
            .join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactlyInAnyOrder(
            createMockProductTypeDraftBuilder()
                .key(productTypeFoo.getKey())
                .attributes(createMockAttributeDefinitionDraftBuilder().build())
                .build(),
            createMockProductTypeDraftBuilder()
                .key(productTypeBar.getKey())
                .attributes(
                    createMockAttributeDefinitionDraftBuilder()
                        .name(numberAttr.getName())
                        .label(numberAttr.getLabel())
                        .type(numberAttr.getType())
                        .build())
                .build());
  }

  @Test
  void mapToProductDrafts_WithReferencesAndSetOfReferences_ShouldResolveAttributesCorrectly() {
    // preparation
    final AttributeDefinition attributeProductReference =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .name("product-reference")
            .label(ofEnglish("product-reference"))
            .isRequired(false)
            .isSearchable(true)
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .referenceBuilder()
                        .referenceTypeId(AttributeReferenceTypeId.PRODUCT))
            .build();
    final AttributeDefinition colorProductReferenceSetAttribute =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .name("product-reference-set")
            .label(ofEnglish("product-reference-set"))
            .isRequired(false)
            .isSearchable(true)
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            typeBuilder ->
                                typeBuilder
                                    .referenceBuilder()
                                    .referenceTypeId(AttributeReferenceTypeId.PRODUCT)))
            .build();

    final ProductType mainProductType =
        MockBuilderUtils.createMockProductTypeBuilder()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a main product type")
            .attributes(attributeProductReference, colorProductReferenceSetAttribute)
            .build();

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        ProductTypeTransformUtils.toProductTypeDrafts(
                mock(ProjectApiRoot.class), referenceIdToKeyCache, singletonList(mainProductType))
            .join();

    // assertion
    final AttributeDefinitionDraft productReferenceSetAttrDraft =
        createMockAttributeDefinitionDraftBuilder()
            .name(colorProductReferenceSetAttribute.getName())
            .label(colorProductReferenceSetAttribute.getLabel())
            .isRequired(false)
            .isSearchable(true)
            .type(colorProductReferenceSetAttribute.getType())
            .build();

    final AttributeDefinitionDraft productReferenceAttrDraft =
        createMockAttributeDefinitionDraftBuilder()
            .name(attributeProductReference.getName())
            .label(attributeProductReference.getLabel())
            .isRequired(false)
            .isSearchable(true)
            .type(attributeProductReference.getType())
            .build();

    assertThat(productTypeDrafts)
        .containsExactly(
            createMockProductTypeDraftBuilder()
                .name(MAIN_PRODUCT_TYPE_KEY)
                .key(MAIN_PRODUCT_TYPE_KEY)
                .description("a main product type")
                .attributes(productReferenceAttrDraft, productReferenceSetAttrDraft)
                .build());
  }

  @Test
  void
      mapToProductDrafts_whenProductTypeContainsNestedAttribute_ShouldReplaceReferencedTypeIdsWithKeys() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .name("nestedattr")
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.nestedBuilder().typeReference(productTypeReference))
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
        MockBuilderUtils.createMockProductTypeBuilder()
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
  void
      mapToProductDrafts_whenProductTypeContainsSetOfNestedAttributes_ShouldReplaceReferenceIdsWithKeys() {
    // preparation
    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductTypeReference productTypeReference =
        ProductTypeReferenceBuilder.of().id(referencedProductTypeId).build();

    final AttributeDefinition nestedTypeAttr =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
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
        MockBuilderUtils.createMockProductTypeBuilder()
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
