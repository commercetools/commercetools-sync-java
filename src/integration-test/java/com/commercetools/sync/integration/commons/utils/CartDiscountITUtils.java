package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountTarget;
import com.commercetools.api.models.cart_discount.CartDiscountValue;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraftBuilder;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;

public final class CartDiscountITUtils {

  public static final String CART_DISCOUNT_KEY_1 = "key_1";
  public static final String CART_DISCOUNT_KEY_2 = "key_2";

  public static final LocalizedString CART_DISCOUNT_NAME_1 = LocalizedString.ofEnglish("name_1");
  public static final LocalizedString CART_DISCOUNT_NAME_2 = LocalizedString.ofEnglish("name_2");

  public static final LocalizedString CART_DISCOUNT_DESC_1 =
      LocalizedString.ofEnglish("discount- get 10 percent");
  public static final LocalizedString CART_DISCOUNT_DESC_2 =
      LocalizedString.ofEnglish("discount- get 20 EUR for special items");

  public static final String CART_DISCOUNT_CART_PREDICATE_1 = "1 = 1";
  public static final String CART_DISCOUNT_CART_PREDICATE_2 =
      "lineItemExists(sku = \"0123456789\" or sku = \"0246891213\") = true";

  public static final CartDiscountValueDraft CART_DISCOUNT_VALUE_DRAFT_1 =
      CartDiscountValueRelativeDraftBuilder.of().permyriad(1000L).build();
  public static final CartDiscountValueDraft CART_DISCOUNT_VALUE_DRAFT_2 =
      CartDiscountValueAbsoluteDraftBuilder.of()
          .money(
              CentPrecisionMoneyBuilder.of()
                  .centAmount(20L)
                  .fractionDigits(0)
                  .currencyCode(EUR.getCurrencyCode())
                  .build())
          .build();

  public static final CartDiscountValue CART_DISCOUNT_VALUE_2 =
      CartDiscountValueAbsoluteBuilder.of()
          .money(
              CentPrecisionMoneyBuilder.of()
                  .centAmount(20L)
                  .fractionDigits(2)
                  .currencyCode(EUR.getCurrencyCode())
                  .build())
          .build();

  public static final CartDiscountTarget CART_DISCOUNT_TARGET_1 =
      CartDiscountLineItemsTargetBuilder.of().predicate("1 = 1").build();
  public static final CartDiscountTarget CART_DISCOUNT_TARGET_2 =
      CartDiscountLineItemsTargetBuilder.of()
          .predicate("sku = \"0123456789\" or sku = \"0246891213\"")
          .build();

  public static final ZonedDateTime JANUARY_FROM = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
  public static final ZonedDateTime JANUARY_UNTIL = ZonedDateTime.parse("2019-01-31T00:00:00.000Z");
  public static final ZonedDateTime FEBRUARY_FROM = ZonedDateTime.parse("2019-02-01T00:00:00.000Z");
  public static final ZonedDateTime FEBRUARY_UNTIL =
      ZonedDateTime.parse("2019-02-28T00:00:00.000Z");

  public static final String SORT_ORDER_1 = "0.1";
  public static final String SORT_ORDER_2 = "0.2";

  public static final String OLD_CART_DISCOUNT_TYPE_KEY = "oldCartDiscountCustomTypeKey";
  public static final String OLD_CART_DISCOUNT_TYPE_NAME = "oldCartDiscountCustomTypeName";

  public static final CartDiscountDraft CART_DISCOUNT_DRAFT_1 =
      CartDiscountDraftBuilder.of()
          .name(CART_DISCOUNT_NAME_1)
          .cartPredicate(CART_DISCOUNT_CART_PREDICATE_1)
          .value(CART_DISCOUNT_VALUE_DRAFT_1)
          .target(CART_DISCOUNT_TARGET_1)
          .sortOrder(SORT_ORDER_1)
          .requiresDiscountCode(false)
          .key(CART_DISCOUNT_KEY_1)
          .isActive(false)
          .description(CART_DISCOUNT_DESC_1)
          .validFrom(JANUARY_FROM)
          .validUntil(JANUARY_UNTIL)
          .custom(createCustomFieldsDraft())
          .build();

  public static final CartDiscountDraft CART_DISCOUNT_DRAFT_2 =
      CartDiscountDraftBuilder.of()
          .name(CART_DISCOUNT_NAME_2)
          .cartPredicate(CART_DISCOUNT_CART_PREDICATE_2)
          .value(CART_DISCOUNT_VALUE_DRAFT_2)
          .target(CART_DISCOUNT_TARGET_2)
          .sortOrder(SORT_ORDER_2)
          .requiresDiscountCode(false)
          .key(CART_DISCOUNT_KEY_2)
          .isActive(false)
          .description(CART_DISCOUNT_DESC_2)
          .validFrom(FEBRUARY_FROM)
          .validUntil(FEBRUARY_UNTIL)
          .custom(createCustomFieldsDraft())
          .build();

  public static CustomFieldsDraft createCustomFieldsDraft() {
    return CustomFieldsDraftBuilder.of()
        .type(
            typeResourceIdentifierBuilder ->
                typeResourceIdentifierBuilder.key(OLD_CART_DISCOUNT_TYPE_KEY))
        .fields(createCustomFieldsJsonMap())
        .build();
  }

  public static CustomFields getCustomFields() {
    return CustomFieldsBuilder.of()
        .type(typeReferenceBuilder -> typeReferenceBuilder.id(UUID.randomUUID().toString()))
        .fields(createCustomFieldsJsonMap())
        .build();
  }

  /**
   * Deletes all cart discounts from CTP project, represented by provided {@code ctpClient}.
   *
   * @param ctpClient represents the CTP project the cart discounts will be deleted from.
   */
  public static void deleteCartDiscounts(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
            ctpClient.cartDiscounts().get(),
            cartDiscounts -> {
              CompletableFuture.allOf(
                      cartDiscounts.stream()
                          .map(cartDiscount -> deleteCartDiscount(ctpClient, cartDiscount))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<CartDiscount> deleteCartDiscount(
      final ProjectApiRoot ctpClient, final CartDiscount cartDiscount) {
    return ctpClient
        .cartDiscounts()
        .delete(cartDiscount)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Deletes all cart discounts from CTP projects defined by {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteCartDiscountsFromTargetAndSource() {
    deleteCartDiscounts(CTP_SOURCE_CLIENT);
    deleteCartDiscounts(CTP_TARGET_CLIENT);
  }

  public static void populateSourceProject() {
    ensureCartDiscountCustomType(
        OLD_CART_DISCOUNT_TYPE_KEY, Locale.ENGLISH, OLD_CART_DISCOUNT_TYPE_NAME, CTP_SOURCE_CLIENT);

    final CartDiscountDraft draft1 = createCartDiscountDraft(CART_DISCOUNT_DRAFT_1);
    final CartDiscountDraft draft2 = createCartDiscountDraft(CART_DISCOUNT_DRAFT_2);

    CompletableFuture.allOf(
            CTP_SOURCE_CLIENT.cartDiscounts().create(draft1).execute(),
            CTP_SOURCE_CLIENT.cartDiscounts().create(draft2).execute())
        .join();
  }

  public static Type ensureCartDiscountCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {

    return createTypeIfNotAlreadyExisting(
        typeKey, locale, name, List.of(ResourceTypeId.CART_DISCOUNT), ctpClient);
  }

  public static CartDiscount populateTargetProject() {
    ensureCartDiscountCustomType(
        OLD_CART_DISCOUNT_TYPE_KEY, Locale.ENGLISH, OLD_CART_DISCOUNT_TYPE_NAME, CTP_TARGET_CLIENT);

    final CartDiscountDraft draft1 = createCartDiscountDraft(CART_DISCOUNT_DRAFT_1);
    return CTP_TARGET_CLIENT
        .cartDiscounts()
        .create(draft1)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }

  private static CartDiscountDraft createCartDiscountDraft(
      final CartDiscountDraft cartDiscountDraft) {
    return CartDiscountDraftBuilder.of(cartDiscountDraft).custom(createCustomFieldsDraft()).build();
  }

  /**
   * Tries to fetch cart discount of {@code key} using {@code projectApiRoot}.
   *
   * @param projectApiRoot client used to execute requests.
   * @param key key of requested cart discount.
   * @return {@link java.util.Optional} which may contain type of {@code key}.
   */
  public static Optional<CartDiscount> getCartDiscountByKey(
      @Nonnull final ProjectApiRoot projectApiRoot, @Nonnull final String key) {
    return projectApiRoot
        .cartDiscounts()
        .get()
        .withWhere("key=:key")
        .withPredicateVar("key", key)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join()
        .getResults()
        .stream()
        .findFirst();
  }

  /**
   * Builds a list of odd sortOrder strings that start with `0.01`. They are odd because because
   * sortOrder in CTP is not allowed to end with a zero and should be decimal value between 0 and 1.
   *
   * @param capacity the number of sort orders to build.
   */
  public static List<String> getSortOrders(final int capacity) {
    return IntStream.range(0, capacity * 2)
        .filter(index -> index % 2 != 0)
        .mapToObj(oddNumber -> format("0.0%s", oddNumber))
        .collect(toList());
  }

  private CartDiscountITUtils() {}
}
