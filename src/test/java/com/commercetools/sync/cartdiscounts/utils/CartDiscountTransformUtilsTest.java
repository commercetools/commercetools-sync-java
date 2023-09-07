package com.commercetools.sync.cartdiscounts.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CartDiscountTransformUtilsTest {

  @Test
  void transform_CartDiscountReferences_ShouldResolveReferencesUsingCacheAndMapToCartDiscountDraft()
      throws JsonProcessingException {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

    final String cartDiscountKey = "cartDiscountKey";
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";

    final List<CartDiscount> mockCartDiscountsPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final CartDiscount mockCartDiscount = mock(CartDiscount.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCartDiscount.getCustom()).thenReturn(mockCustomFields);
      when(mockCartDiscount.getKey()).thenReturn(cartDiscountKey);
      when(mockCartDiscount.getName()).thenReturn(LocalizedString.ofEnglish("cartDiscount" + i));
      when(mockCartDiscount.getValue())
          .thenReturn(CartDiscountValueBuilder.of().relativeBuilder().permyriad(10L).build());
      when(mockCartDiscount.getCartPredicate()).thenReturn("cartPredicate" + i);
      when(mockCartDiscount.getSortOrder()).thenReturn("sortOrder" + i);
      mockCartDiscountsPage.add(mockCartDiscount);
    }

    final String jsonStringCustomTypes =
        "{\"typeDefinitions\":{\"results\":[{\"id\":\""
            + customTypeId
            + "\","
            + "\"key\":\""
            + customTypeKey
            + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> customTypesResponse =
        TestUtils.mockGraphQLResponse(jsonStringCustomTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(customTypesResponse));

    // test
    final List<CartDiscountDraft> cartDiscountsResolved =
        CartDiscountTransformUtils.toCartDiscountDrafts(
                sourceClient, referenceIdToKeyCache, mockCartDiscountsPage)
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
