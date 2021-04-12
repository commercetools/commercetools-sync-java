package com.commercetools.sync.cartdiscounts.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.cartdiscounts.service.CartDiscountTransformService;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CartDiscountTransformServiceImplTest {

  @Test
  void
      transform_CartDiscountReferences_ShouldResolveReferencesUsingCacheAndMapToCartDiscountDraft() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    final CartDiscountTransformService cartDiscountTransformService =
        new CartDiscountTransformServiceImpl(sourceClient, referenceIdToKeyCache);

    final String cartDiscountKey = "cartDiscountKey";
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";

    final List<CartDiscount> mockCartDiscountsPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final CartDiscount mockCartDiscount = mock(CartDiscount.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCartDiscount.getCustom()).thenReturn(mockCustomFields);
      when(mockCartDiscount.getKey()).thenReturn(cartDiscountKey);
      mockCartDiscountsPage.add(mockCartDiscount);
    }

    final String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\"" + customTypeId + "\"," + "\"key\":\"" + customTypeKey + "\"}]}";
    final ResourceKeyIdGraphQlResult customTypesResult =
        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(customTypesResult));

    // test
    final List<CartDiscountDraft> cartDiscountsResolved =
        cartDiscountTransformService
            .toCartDiscountDrafts(mockCartDiscountsPage)
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<CartDiscountDraft> cartDiscountKey1 =
        cartDiscountsResolved.stream()
            .filter(cartDiscountDraft -> cartDiscountKey.equals(cartDiscountDraft.getKey()))
            .findFirst();

    assertThat(cartDiscountKey1)
        .hasValueSatisfying(
            cartDiscountDraft ->
                assertThat(cartDiscountDraft.getCustom().getType().getKey())
                    .isEqualTo(customTypeKey));
  }
}
