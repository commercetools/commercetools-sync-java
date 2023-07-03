package com.commercetools.sync.sdk2.cartdiscounts.utils;

import static com.commercetools.api.models.common.DefaultCurrencyUnits.EUR;
import static com.commercetools.sync.sdk2.cartdiscounts.utils.CartDiscountSyncUtils.buildActions;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountChangeCartPredicateActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeIsActiveActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeNameActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeRequiresDiscountCodeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeSortOrderActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeStackingModeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeTargetActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountChangeValueActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountLineItemsTargetBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomFieldActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomTypeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetDescriptionActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetValidFromAndUntilActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountTarget;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountValue;
import com.commercetools.api.models.cart_discount.CartDiscountValueAbsoluteDraftBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelativeDraftBuilder;
import com.commercetools.api.models.cart_discount.StackingMode;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.sdk2.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CartDiscountSyncUtilsTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);

  private static final String CUSTOM_TYPE_ID = "id";
  private static final String CUSTOM_FIELD_NAME = "field";
  private static final String CUSTOM_FIELD_VALUE = "value";

  private static CartDiscount cartDiscount;
  private static CartDiscountDraft cartDiscountDraft;

  /** Creates a {@code cartDiscount} and a {@code cartDiscountDraft} with identical values. */
  @BeforeAll
  static void setup() {
    final String key = "key";
    final LocalizedString name = LocalizedString.ofEnglish("name");
    final LocalizedString desc = LocalizedString.ofEnglish("discount- get 10 percent");
    final String predicate = "lineItemExists(sku = \"0123456789\" or sku = \"0246891213\") = true";
    final CartDiscountValue value = CartDiscountValueRelativeBuilder.of().permyriad(1000L).build();

    final CartDiscountValueDraft valueDraft =
        CartDiscountValueRelativeDraftBuilder.of().permyriad(1000L).build();
    final CartDiscountTarget target =
        CartDiscountLineItemsTargetBuilder.of().predicate("1=1").build();
    final ZonedDateTime januaryFrom = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
    final ZonedDateTime januaryUntil = ZonedDateTime.parse("2019-01-31T00:00:00.000Z");
    final String sortOrder = "0.1";
    final Boolean isActive = false;
    final Boolean isRequireDiscCode = false;
    final StackingMode stackingMode = StackingMode.STACKING;

    cartDiscount = mock(CartDiscount.class);
    when(cartDiscount.getKey()).thenReturn(key);
    when(cartDiscount.getName()).thenReturn(name);
    when(cartDiscount.getDescription()).thenReturn(desc);
    when(cartDiscount.getCartPredicate()).thenReturn(predicate);
    when(cartDiscount.getValue()).thenReturn(value);
    when(cartDiscount.getTarget()).thenReturn(target);
    when(cartDiscount.getValidFrom()).thenReturn(januaryFrom);
    when(cartDiscount.getValidUntil()).thenReturn(januaryUntil);
    when(cartDiscount.getSortOrder()).thenReturn(sortOrder);
    when(cartDiscount.getIsActive()).thenReturn(isActive);
    when(cartDiscount.getRequiresDiscountCode()).thenReturn(isRequireDiscCode);
    when(cartDiscount.getStackingMode()).thenReturn(stackingMode);

    final CustomFields customFields = mock(CustomFields.class);
    when(customFields.getType()).thenReturn(TypeReferenceBuilder.of().id(CUSTOM_TYPE_ID).build());

    final FieldContainer fieldContainerMock = mock();
    final Map<String, Object> customFieldsJsonMapMock = new HashMap<>();
    customFieldsJsonMapMock.put(
        CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE));
    when(customFields.getFields()).thenReturn(fieldContainerMock);
    when(fieldContainerMock.values()).thenReturn(customFieldsJsonMapMock);

    when(cartDiscount.getCustom()).thenReturn(customFields);

    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder.addValue(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE))
            .build();

    cartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(name)
            .cartPredicate(predicate)
            .value(valueDraft)
            .target(target)
            .sortOrder(sortOrder)
            .requiresDiscountCode(isRequireDiscCode)
            .key(key)
            .description(desc)
            .sortOrder(sortOrder)
            .isActive(isActive)
            .validFrom(januaryFrom)
            .validUntil(januaryUntil)
            .custom(customFieldsDraft)
            .build();
  }

  @Test
  void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<CartDiscountUpdateAction> updateActions =
        buildActions(cartDiscount, cartDiscountDraft, cartDiscountSyncOptions);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_FromDraftsWithDifferentNameValues_ShouldBuildUpdateActions() {
    final LocalizedString newName =
        LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");

    final CartDiscountDraft newCartDiscount =
        CartDiscountDraftBuilder.of(cartDiscountDraft).name(newName).build();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<CartDiscountUpdateAction> updateActions =
        buildActions(cartDiscount, newCartDiscount, cartDiscountSyncOptions);

    assertThat(updateActions).isNotEmpty();
    assertThat(updateActions)
        .containsExactly(CartDiscountChangeNameActionBuilder.of().name(newName).build());
  }

  @Test
  void buildActions_FromDraftsWithAllDifferentValues_ShouldBuildAllUpdateActions() {
    final LocalizedString newName =
        LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");
    final String cartPredicate = "1 = 1";
    final CartDiscountValueDraft newCartDiscountValue =
        CartDiscountValueAbsoluteDraftBuilder.of()
            .money(
                CentPrecisionMoneyBuilder.of()
                    .fractionDigits(0)
                    .centAmount(10L)
                    .currencyCode(EUR.getCurrencyCode())
                    .build())
            .build();
    final CartDiscountTarget newCartDiscountTarget =
        CartDiscountLineItemsTargetBuilder.of().predicate("quantity > 1").build();
    final String newSortOrder = "0.3";
    final LocalizedString newDesc =
        LocalizedString.of(Locale.GERMAN, "Neu Beschreibung", Locale.ENGLISH, "new description");
    final ZonedDateTime newValidFrom = ZonedDateTime.parse("2019-11-01T00:00:00.000Z");
    final ZonedDateTime newValidUntil = ZonedDateTime.parse("2019-11-15T00:00:00.000Z");
    final boolean newIsActive = true;
    final boolean newRequireDiscountCode = true;
    final StackingMode newStackingMode = StackingMode.STOP_AFTER_THIS_DISCOUNT;
    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder ->
                    typeResourceIdentifierBuilder.id(UUID.randomUUID().toString()))
            .build();

    final CartDiscountDraft newCartDiscount =
        CartDiscountDraftBuilder.of()
            .name(newName)
            .cartPredicate(cartPredicate)
            .value(newCartDiscountValue)
            .target(newCartDiscountTarget)
            .sortOrder(newSortOrder)
            .requiresDiscountCode(newRequireDiscountCode)
            .isActive(newIsActive)
            .description(newDesc)
            .validFrom(newValidFrom)
            .validUntil(newValidUntil)
            .stackingMode(newStackingMode)
            .custom(newCustomFieldsDraft)
            .build();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<CartDiscountUpdateAction> updateActions =
        buildActions(cartDiscount, newCartDiscount, cartDiscountSyncOptions);

    assertThat(updateActions)
        .containsExactly(
            CartDiscountChangeValueActionBuilder.of().value(newCartDiscountValue).build(),
            CartDiscountChangeCartPredicateActionBuilder.of().cartPredicate(cartPredicate).build(),
            CartDiscountChangeTargetActionBuilder.of().target(newCartDiscountTarget).build(),
            CartDiscountChangeIsActiveActionBuilder.of().isActive(newIsActive).build(),
            CartDiscountChangeNameActionBuilder.of().name(newName).build(),
            CartDiscountSetDescriptionActionBuilder.of().description(newDesc).build(),
            CartDiscountChangeSortOrderActionBuilder.of().sortOrder(newSortOrder).build(),
            CartDiscountChangeRequiresDiscountCodeActionBuilder.of()
                .requiresDiscountCode(newRequireDiscountCode)
                .build(),
            CartDiscountSetValidFromAndUntilActionBuilder.of()
                .validFrom(newValidFrom)
                .validUntil(newValidUntil)
                .build(),
            CartDiscountChangeStackingModeActionBuilder.of().stackingMode(newStackingMode).build(),
            CartDiscountSetCustomTypeActionBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(newCustomFieldsDraft.getType().getId()))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
  }

  @Test
  void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id("newId"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.addValue("newField", "newValue"))
            .build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft).custom(customFieldsDraft).build();

    final List<CartDiscountUpdateAction> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CartDiscountSetCustomTypeActionBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.id(customFieldsDraft.getType().getId()))
                .fields(customFieldsDraft.getFields())
                .build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder
                        .addValue(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
                        .addValue("name_2", "value_2"))
            .build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(sameCustomFieldDraftWithNewCustomField)
            .build();

    final List<CartDiscountUpdateAction> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CartDiscountSetCustomFieldActionBuilder.of().name("name_2").value("value_2").build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(CUSTOM_TYPE_ID))
            .fields(
                fieldContainerBuilder ->
                    fieldContainerBuilder.addValue(CUSTOM_FIELD_NAME, "newValue"))
            .build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(sameCustomFieldDraftWithNewValue)
            .build();

    final List<CartDiscountUpdateAction> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CartDiscountSetCustomFieldActionBuilder.of()
                .name(CUSTOM_FIELD_NAME)
                .value("newValue")
                .build());
  }

  @Test
  void buildActions_WithJustNewCartDiscountHasNullCustomType_ShouldBuildUpdateAction() {
    final CartDiscountDraft cartDiscountDraftWithNullCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft).custom((CustomFieldsDraft) null).build();

    final List<CartDiscountUpdateAction> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithNullCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).containsExactly(CartDiscountSetCustomTypeActionBuilder.of().build());
  }
}
