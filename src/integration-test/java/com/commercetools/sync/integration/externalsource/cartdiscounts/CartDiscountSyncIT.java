package com.commercetools.sync.integration.externalsource.cartdiscounts;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.as;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeTargetActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeValueActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomFieldActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomTypeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueGiftLineItemDraftBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductResourceIdentifierBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountSyncIT {

  private CartDiscount oldCartDiscount;

  private final Random random = new Random();

  /**
   * Deletes cart discounts from the target CTP projects. Populates the target CTP project with test
   * data.
   */
  @BeforeEach
  void setup() {
    deleteCartDiscounts(CTP_TARGET_CLIENT);
    oldCartDiscount = populateTargetProject();
  }

  @AfterAll
  static void tearDown() {
    deleteCartDiscounts(CTP_TARGET_CLIENT);
  }

  @Test
  void
      sync_WithUpdatedCartDiscount_WithNewCartPredicate_ShouldUpdateCartDiscountWithNewCartPredicate() {
    // preparation
    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
            .cartPredicate(CART_DISCOUNT_CART_PREDICATE_2)
            .build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountChangeCartPredicateActionBuilder.of()
                .cartPredicate(CART_DISCOUNT_CART_PREDICATE_2)
                .build());
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithUpdatedCartDiscount_WithNewValue_ShouldUpdateCartDiscountWithNewValue() {
    // preparation
    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
            .value(CART_DISCOUNT_VALUE_DRAFT_2)
            .build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountChangeValueActionBuilder.of().value(CART_DISCOUNT_VALUE_DRAFT_2).build());
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithUpdatedCartDiscount_WithNewTarget_ShouldUpdateCartDiscountWithNewTarget() {
    // preparation
    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1).target(CART_DISCOUNT_TARGET_2).build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountChangeTargetActionBuilder.of().target(CART_DISCOUNT_TARGET_2).build());
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithUpdatedCartDiscount_WithNewCustomType_ShouldUpdateCartDiscountWithNewCustomType() {
    // preparation
    final Type newCustomType =
        ensureCartDiscountCustomType("new-type", Locale.ENGLISH, "new-type", CTP_TARGET_CLIENT);

    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key(newCustomType.getKey()))
                        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap())))
            .build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountSetCustomTypeActionBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(newCustomType.getId()))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithUpdatedCartDiscount_WithNonExistingResIdentifier_ShouldFailToResolveReference() {
    // preparation
    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key("not-existing-key"))
                        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap())))
            .build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorMessages)
        .containsExactly(
            "Failed to process the CartDiscountDraft with key:'key_1'. Reason: "
                + ReferenceResolutionException.class.getCanonicalName()
                + ": "
                + "Failed to resolve custom type reference on CartDiscountDraft with key:'key_1'. "
                + "Reason: Type with key 'not-existing-key' doesn't exist.");
  }

  @Test
  void
      sync_WithUpdatedCartDiscount_WithNewCustomField_ShouldUpdateCartDiscountWithNewCustomField() {
    // preparation
    final FieldContainer customFieldsJsons = createCustomFieldsJsonMap();
    customFieldsJsons.setValue(
        BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(true));

    final CartDiscountDraft newCartDiscountDraftWithExistingKey =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key(OLD_CART_DISCOUNT_TYPE_KEY))
                        .fields(customFieldsJsons))
            .build();

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
        cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionsList)
        .containsExactly(
            CartDiscountSetCustomFieldActionBuilder.of()
                .name(BOOLEAN_CUSTOM_FIELD_NAME)
                .value(JsonNodeFactory.instance.booleanNode(true))
                .build());
    assertThat(cartDiscountSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithNewCartDiscount_ShouldCreateNewDiscount() {
    // preparation
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);
    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(singletonList(CART_DISCOUNT_DRAFT_2)).toCompletableFuture().join();

    // assertions
    assertThat(cartDiscountSyncStatistics).hasValues(1, 1, 0, 0);

    final Optional<CartDiscount> cartDiscountAfterCreation =
        getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_2);

    assertThat(cartDiscountAfterCreation)
        .hasValueSatisfying(
            cartDiscount -> {
              assertThat(cartDiscount.getName()).isEqualTo(CART_DISCOUNT_DRAFT_2.getName());
              assertThat(cartDiscount.getDescription())
                  .isEqualTo(CART_DISCOUNT_DRAFT_2.getDescription());
              assertThat(cartDiscount.getCartPredicate())
                  .isEqualTo(CART_DISCOUNT_DRAFT_2.getCartPredicate());
              assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_VALUE_2);
            });
  }

  @Test
  void
      sync_WithTargetSetForGiftLineItemValue_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // prepare
    // According to docs target must not be set when the value is of type gift line item:
    // https://docs.commercetools.com/api/projects/cartDiscounts#ctp:api:type:CartDiscountDraft
    final String cartDiscountKey = "cart-discount-gift-line-item-" + UUID.randomUUID();

    final CartDiscountDraft targetCartDiscountDraft =
        createCartDiscountDraftWithGiftLineItemValue(cartDiscountKey);

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
        cartDiscountSync.sync(singletonList(targetCartDiscountDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(format("Failed to update cart discount with key: '%s'.", cartDiscountKey));

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(SyncException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(CompletionException.class);
              assertThat(throwable.getCause()).hasCauseExactlyInstanceOf(BadRequestException.class);
              assertThat(throwable.getCause())
                  .hasMessageContaining(
                      "A value can not change from type 'giftLineItem' to another type, or from another type to 'giftLineItem'.");
              return true;
            });

    assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
  }

  private CartDiscountDraft createCartDiscountDraftWithGiftLineItemValue(
      final String cartDiscountKey) {
    final ProductType sourceProductType =
        ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(sourceProductType.toResourceIdentifier())
            .name(LocalizedString.ofEnglish("test"))
            .slug(LocalizedString.ofEnglish("test" + UUID.randomUUID()))
            .build();

    final Product product =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final CartDiscountValueGiftLineItemDraft cartDiscountValueGiftLineItem =
        CartDiscountValueGiftLineItemDraftBuilder.of()
            .product(ProductResourceIdentifierBuilder.of().id(product.getId()).build())
            .variantId(1L)
            .build();

    final String sortOrder = Double.toString(random.nextDouble());

    final CartDiscountDraft sourceCartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(CART_DISCOUNT_NAME_1)
            .cartPredicate("1=1")
            .value(cartDiscountValueGiftLineItem)
            .sortOrder(sortOrder)
            .requiresDiscountCode(false)
            .key(cartDiscountKey)
            .isActive(false)
            .description(CART_DISCOUNT_DESC_1)
            .validFrom(JANUARY_FROM)
            .validUntil(JANUARY_UNTIL)
            .build();

    CTP_TARGET_CLIENT.cartDiscounts().create(sourceCartDiscountDraft).executeBlocking();

    final CartDiscountDraft targetCartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(CART_DISCOUNT_NAME_1)
            .cartPredicate("1=1")
            .value(CART_DISCOUNT_VALUE_DRAFT_1)
            .sortOrder(sortOrder)
            .requiresDiscountCode(false)
            .key(cartDiscountKey)
            .isActive(false)
            .description(CART_DISCOUNT_DESC_1)
            .validFrom(JANUARY_FROM)
            .validUntil(JANUARY_UNTIL)
            .build();

    return targetCartDiscountDraft;
  }

  @Test
  void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
    // preparation
    final List<String> sortOrders = getSortOrders(100);
    // Default batch size is 50 (check CartDiscountSyncOptionsBuilder) so we have 2 batches of 50
    final List<CartDiscountDraft> cartDiscountDrafts =
        IntStream.range(0, 100)
            .mapToObj(
                i ->
                    CartDiscountDraftBuilder.of()
                        .name(CART_DISCOUNT_NAME_2)
                        .cartPredicate(CART_DISCOUNT_CART_PREDICATE_2)
                        .value(CART_DISCOUNT_VALUE_DRAFT_2)
                        .target(CART_DISCOUNT_TARGET_2)
                        .sortOrder(sortOrders.get(i))
                        .requiresDiscountCode(false)
                        .key(format("key__%s", i))
                        .isActive(false)
                        .build())
            .collect(Collectors.toList());

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics cartDiscountSyncStatistics =
        cartDiscountSync.sync(cartDiscountDrafts).toCompletableFuture().join();

    // assertion
    assertThat(cartDiscountSyncStatistics).hasValues(100, 100, 0, 0);
  }

  @Test
  void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCartDiscountWithSuccess() {
    // Preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdate(oldCartDiscount.getId());
    final CartDiscountDraft updatedDraft =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
            .key(oldCartDiscount.getKey())
            .description(CART_DISCOUNT_DESC_1)
            .build();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(spyClient).build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics statistics =
        cartDiscountSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // assertion
    assertThat(statistics).hasValues(1, 0, 1, 0);

    // Assert CTP state.
    final List<CartDiscount> queryResult =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", CART_DISCOUNT_KEY_1)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(CartDiscountPagedQueryResponse::getResults)
            .toCompletableFuture()
            .join();

    assertThat(queryResult.get(0))
        .satisfies(
            cartDiscount -> assertThat(cartDiscount.getKey()).isEqualTo(CART_DISCOUNT_KEY_1));
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final CartDiscountDraft draft2 =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key(OLD_CART_DISCOUNT_TYPE_KEY))
                        .fields(createCustomFieldsJsonMap()))
            .build();

    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .create(draft2)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry(
            cartDiscount.getId(), cartDiscount.getKey());

    final CartDiscountDraft updatedDraft =
        CartDiscountDraftBuilder.of(draft2).description(CART_DISCOUNT_DESC_1).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics statistics =
        cartDiscountSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions).hasSize(1);

    assertThat(exceptions.get(0).getCause())
        .hasCauseExactlyInstanceOf(ConcurrentModificationException.class);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update cart discount with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                CART_DISCOUNT_KEY_2));
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final CartDiscountDraft draft2 =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
            .custom(
                customFieldsDraftBuilder ->
                    customFieldsDraftBuilder
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.key(OLD_CART_DISCOUNT_TYPE_KEY))
                        .fields(createCustomFieldsJsonMap()))
            .build();

    final CartDiscount cartDiscount =
        CTP_TARGET_CLIENT
            .cartDiscounts()
            .create(draft2)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry(
            cartDiscount.getId(), cartDiscount.getKey());

    final CartDiscountDraft updatedDraft =
        CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
            .description(CART_DISCOUNT_DESC_1)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

    // test
    final CartDiscountSyncStatistics statistics =
        cartDiscountSync.sync(singletonList(updatedDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1);
    assertThat(exceptions).hasSize(1);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update cart discount with key: '%s'. Reason: Not found when attempting to fetch while "
                    + "retrying after concurrency modification.",
                CART_DISCOUNT_KEY_2));
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry(
      final String cartDiscountId, final String cartDiscountKey) {
    final AtomicInteger getRequestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("cart-discounts/key=" + cartDiscountKey)
                      && ApiHttpMethod.GET.equals(method)) {
                    if (getRequestInvocationCounter.getAndIncrement() > 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createNotFoundException());
                    }
                  }
                  if (uri.contains("cart-discounts/" + cartDiscountId)
                      && ApiHttpMethod.POST.equals(method)) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate(
      @Nonnull final String oldCartDiscountId) {
    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger postRequestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("cart-discounts/" + oldCartDiscountId)
                      && ApiHttpMethod.POST.equals(method)
                      && postRequestInvocationCounter.getAndIncrement() == 0) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry(
      final String cartDiscountId, final String cartDiscountKey) {
    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger postRequestInvocationCounter = new AtomicInteger(0);

    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("cart-discounts/" + cartDiscountId)
                      && ApiHttpMethod.POST.equals(method)
                      && postRequestInvocationCounter.getAndIncrement() == 0) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  } else if (uri.contains("cart-discounts/key=" + cartDiscountKey)
                      && ApiHttpMethod.GET.equals(method)) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }
}
