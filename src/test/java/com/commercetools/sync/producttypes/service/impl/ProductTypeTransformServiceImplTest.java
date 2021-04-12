package com.commercetools.sync.producttypes.service.impl;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.producttypes.service.ProductTypeTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.NumberAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProductTypeTransformServiceImplTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void setup() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void mapToProductDrafts_WithProductTypeWithNoAttributeDefs_ShouldReturnProductType() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTypeTransformService productTypeTransformService =
        new ProductTypeTransformServiceImpl(sourceClient, referenceIdToKeyCache);

    final ProductType productTypeFoo = mock(ProductType.class);
    when(productTypeFoo.getKey()).thenReturn("foo");
    when(productTypeFoo.getAttributes()).thenReturn(emptyList());

    final List<ProductType> productTypes = singletonList(productTypeFoo);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        productTypeTransformService.toProductTypeDrafts(productTypes).join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(ProductTypeDraftBuilder.of(productTypeFoo).build());
  }

  @Test
  void mapToProductDrafts_WithNoReferences_ShouldReturnCorrectProductTypeDrafts() {
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTypeTransformService productTypeTransformService =
        new ProductTypeTransformServiceImpl(sourceClient, referenceIdToKeyCache);

    final AttributeDefinition stringAttr =
        AttributeDefinitionBuilder.of("a", ofEnglish("a"), StringAttributeType.of()).build();

    final AttributeDefinition numberAttr =
        AttributeDefinitionBuilder.of("b", ofEnglish("b"), NumberAttributeType.of()).build();

    final ProductType productTypeFoo = mock(ProductType.class);
    when(productTypeFoo.getKey()).thenReturn("ProductTypeFoo");
    when(productTypeFoo.getAttributes()).thenReturn(singletonList(stringAttr));

    final ProductType productTypeBar = mock(ProductType.class);
    when(productTypeBar.getKey()).thenReturn("ProductTypeBar");
    when(productTypeBar.getAttributes()).thenReturn(singletonList(numberAttr));

    final List<ProductType> productTypes = asList(productTypeFoo, productTypeBar);

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        productTypeTransformService.toProductTypeDrafts(productTypes).join();

    // assertion
    assertThat(productTypeDrafts)
        .containsExactly(
            ProductTypeDraftBuilder.of(productTypeFoo).build(),
            ProductTypeDraftBuilder.of(productTypeBar).build());
  }

  @Test
  void transform_ShouldReplaceProductTypeNestedAttributeReferenceIdsWithKeys() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductTypeTransformService productTypeTransformService =
        new ProductTypeTransformServiceImpl(sourceClient, referenceIdToKeyCache);

    final String referencedProductTypeId = UUID.randomUUID().toString();
    final String referencedProductTypeKey = "referencedProductTypeKey";

    final ProductType referencedProductType = mock(ProductType.class);
    when(referencedProductType.getKey()).thenReturn(referencedProductTypeKey);

    final Reference<ProductType> productTypeReference =
        spy(ProductType.reference(referencedProductType));
    when(productTypeReference.getId()).thenReturn(referencedProductTypeId);

    final AttributeDefinition nestedTypeAttr =
        AttributeDefinitionBuilder.of(
                "nestedattr",
                ofEnglish("nestedattr"),
                SetAttributeType.of(NestedAttributeType.of(productTypeReference)))
            .build();

    final ProductType productType = mock(ProductType.class);
    when(productType.getKey()).thenReturn("withNestedTypeAttr");
    when(productType.getAttributes()).thenReturn(singletonList(nestedTypeAttr));

    final List<ProductType> productTypes = singletonList(productType);

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\""
            + referencedProductTypeId
            + "\","
            + "\"key\":\""
            + referencedProductTypeKey
            + "\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    // test
    final List<ProductTypeDraft> productTypeDrafts =
        productTypeTransformService.toProductTypeDrafts(productTypes).join();

    // assertion
    assertThat(productTypeDrafts)
        .satisfies(
            productTypeDraft -> {
              final SetAttributeType setAttributeType =
                  (SetAttributeType)
                      productTypeDraft.get(0).getAttributes().get(0).getAttributeType();
              final NestedAttributeType nestedAttributeType =
                  (NestedAttributeType) setAttributeType.getElementType();
              assertThat(nestedAttributeType.getTypeReference().getId())
                  .isEqualTo(referencedProductType.getKey());
            });
  }
}
