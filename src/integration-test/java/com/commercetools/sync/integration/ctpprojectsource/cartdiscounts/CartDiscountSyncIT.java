package com.commercetools.sync.integration.ctpprojectsource.cartdiscounts;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeTargetActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeValueActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomTypeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountShippingCostTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraftBuilder;
import com.commercetools.api.models.common.MoneyBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.sdk2.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.sdk2.cartdiscounts.utils.CartDiscountTransformUtils;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountSyncIT {

  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeEach
  void setup() {
    deleteCartDiscountsFromTargetAndSource();
    populateSourceProject();
    populateTargetProject();
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @AfterAll
  static void tearDown() {
    deleteCartDiscountsFromTargetAndSource();
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<CartDiscount> cartDiscounts =
        CTP_SOURCE_CLIENT
            .cartDiscounts()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getResults)
            .join();

    final List<CartDiscountDraft> cartDiscountDrafts =
        CartDiscountTransformUtils.toCartDiscountDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, cartDiscounts)
            .join();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(cartDiscountDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 0, 0);
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 cart discounts were processed in total"
                + " (1 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<CartDiscount> cartDiscounts =
        CTP_SOURCE_CLIENT
            .cartDiscounts()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getResults)
            .join();
    final String newTypeKey = "new-type";
    ensureCartDiscountCustomType(newTypeKey, Locale.ENGLISH, newTypeKey, CTP_SOURCE_CLIENT);
    final Type newTargetCustomType =
        ensureCartDiscountCustomType(newTypeKey, Locale.ENGLISH, newTypeKey, CTP_TARGET_CLIENT);

    final List<CartDiscountDraft> cartDiscountDrafts =
        CartDiscountTransformUtils.toCartDiscountDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, cartDiscounts)
            .join();

    // Apply some changes
    final List<CartDiscountDraft> updatedCartDiscountDrafts =
        cartDiscountDrafts.stream()
            .map(
                draft ->
                    CartDiscountDraftBuilder.of(draft)
                        .cartPredicate("totalPrice >= \"100 EUR\"")
                        .value(
                            CartDiscountValueAbsoluteDraftBuilder.of()
                                .money(
                                    MoneyBuilder.of()
                                        .currencyCode(EUR.getCurrencyCode())
                                        .centAmount(40L)
                                        .build())
                                .build())
                        .target(CartDiscountShippingCostTargetBuilder.of().build())
                        .custom(
                            CustomFieldsDraftBuilder.of()
                                .type(
                                    typeResourceIdentifierBuilder ->
                                        typeResourceIdentifierBuilder.key(newTypeKey))
                                .fields(
                                    fieldContainerBuilder ->
                                        fieldContainerBuilder.values(emptyMap()))
                                .build())
                        .build())
            .collect(Collectors.toList());

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final List<CartDiscountUpdateAction> updateActionsList = new ArrayList<>();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .beforeUpdateCallback(
                (updateActions, newCartDiscount, oldCartDiscount) -> {
                  updateActionsList.addAll(updateActions);
                  return updateActions;
                })
            .build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(updatedCartDiscountDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountChangeValueActionBuilder.of()
                .value(
                    CartDiscountValueAbsoluteDraftBuilder.of()
                        .money(
                            MoneyBuilder.of()
                                .centAmount(40L)
                                .currencyCode(EUR.getCurrencyCode())
                                .build())
                        .build())
                .build(),
            CartDiscountChangeCartPredicateActionBuilder.of()
                .cartPredicate("totalPrice >= \"100 EUR\"")
                .build(),
            CartDiscountChangeTargetActionBuilder.of()
                .target(CartDiscountShippingCostTargetBuilder.of().build())
                .build(),
            CartDiscountSetCustomTypeActionBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(newTargetCustomType.getId()))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 cart discounts were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
  }
}
