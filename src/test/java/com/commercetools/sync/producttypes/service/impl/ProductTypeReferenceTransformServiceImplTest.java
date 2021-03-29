package com.commercetools.sync.producttypes.service.impl;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.producttypes.service.ProductTypeReferenceTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ProductTypeReferenceTransformServiceImplTest {

  @Test
  void transform_ShouldReplaceProductTypeNestedAttributeReferenceIdsWithKeys() {
    // preparation
    Map<String, String> idToKeyValueMap = new HashMap<>();
    final SphereClient sourceClient = mock(SphereClient.class);
    ProductTypeReferenceTransformService productTypeReferenceTransformService =
        new ProductTypeReferenceTransformServiceImpl(sourceClient, idToKeyValueMap);

    final ProductType withNestedProductTypeReferenceAttributes =
        readObjectFromResource("product-type-with-references.json", ProductType.class);
  }
}
