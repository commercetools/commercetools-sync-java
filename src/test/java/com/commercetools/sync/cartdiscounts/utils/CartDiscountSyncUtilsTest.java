package com.commercetools.sync.cartdiscounts.utils;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountSyncUtils.buildActions;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountTarget;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.LineItemsTarget;
import io.sphere.sdk.cartdiscounts.StackingMode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeCartPredicate;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeIsActive;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeName;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeRequiresDiscountCode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeSortOrder;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeStackingMode;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeTarget;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeValue;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomField;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomType;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetDescription;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetValidFromAndUntil;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartDiscountSyncUtilsTest {

  private static final SphereClient CTP_CLIENT = mock(SphereClient.class);

  private static final String KEY = "key";
  private static final LocalizedString NAME = LocalizedString.of(Locale.ENGLISH, "name");
  private static final LocalizedString DESC =
      LocalizedString.of(Locale.ENGLISH, "discount- get 10 percent");
  private static final String PREDICATE =
      "lineItemExists(sku = \"0123456789\" or sku = \"0246891213\") = true";
  private static final CartDiscountValue VALUE = CartDiscountValue.ofRelative(1000);
  private static final CartDiscountTarget TARGET = LineItemsTarget.ofAll();
  private static final ZonedDateTime JANUARY_FROM = ZonedDateTime.parse("2019-01-01T00:00:00.000Z");
  private static final ZonedDateTime JANUARY_UNTIL =
      ZonedDateTime.parse("2019-01-31T00:00:00.000Z");
  private static final String SORT_ORDER = "0.1";
  private static final Boolean IS_ACTIVE = false;
  private static final Boolean IS_REQUIRE_DISC_CODE = false;
  private static final StackingMode STACKING_MODE = StackingMode.STACKING;

  private static final String CUSTOM_TYPE_ID = "id";
  private static final String CUSTOM_FIELD_NAME = "field";
  private static final String CUSTOM_FIELD_VALUE = "value";

  private CartDiscount cartDiscount;
  private CartDiscountDraft cartDiscountDraft;

  /** Creates a {@code cartDiscount} and a {@code cartDiscountDraft} with identical values. */
  @BeforeEach
  void setup() {
    cartDiscount = mock(CartDiscount.class);
    when(cartDiscount.getKey()).thenReturn(KEY);
    when(cartDiscount.getName()).thenReturn(NAME);
    when(cartDiscount.getDescription()).thenReturn(DESC);
    when(cartDiscount.getCartPredicate()).thenReturn(PREDICATE);
    when(cartDiscount.getValue()).thenReturn(VALUE);
    when(cartDiscount.getTarget()).thenReturn(TARGET);
    when(cartDiscount.getValidFrom()).thenReturn(JANUARY_FROM);
    when(cartDiscount.getValidUntil()).thenReturn(JANUARY_UNTIL);
    when(cartDiscount.getSortOrder()).thenReturn(SORT_ORDER);
    when(cartDiscount.isActive()).thenReturn(IS_ACTIVE);
    when(cartDiscount.isRequiringDiscountCode()).thenReturn(IS_REQUIRE_DISC_CODE);
    when(cartDiscount.getStackingMode()).thenReturn(STACKING_MODE);

    final CustomFields customFields = mock(CustomFields.class);
    when(customFields.getType()).thenReturn(Type.referenceOfId(CUSTOM_TYPE_ID));

    final Map<String, JsonNode> customFieldsJsonMapMock = new HashMap<>();
    customFieldsJsonMapMock.put(
        CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE));
    when(customFields.getFieldsJsonMap()).thenReturn(customFieldsJsonMapMock);

    when(cartDiscount.getCustom()).thenReturn(customFields);

    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
            .addObject(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
            .build();

    cartDiscountDraft =
        CartDiscountDraftBuilder.of(
                NAME, PREDICATE, VALUE, TARGET, SORT_ORDER, IS_REQUIRE_DISC_CODE)
            .key(KEY)
            .description(DESC)
            .sortOrder(SORT_ORDER)
            .active(IS_ACTIVE)
            .validFrom(JANUARY_FROM)
            .validUntil(JANUARY_UNTIL)
            .custom(customFieldsDraft)
            .build();
  }

  @Test
  void buildActions_WithSameValues_ShouldNotBuildUpdateActions() {
    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<UpdateAction<CartDiscount>> updateActions =
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
    final List<UpdateAction<CartDiscount>> updateActions =
        buildActions(cartDiscount, newCartDiscount, cartDiscountSyncOptions);

    assertThat(updateActions).isNotEmpty();
    assertThat(updateActions).containsExactly(ChangeName.of(newName));
  }

  @Test
  void buildActions_FromDraftsWithAllDifferentValues_ShouldBuildAllUpdateActions() {
    final LocalizedString newName =
        LocalizedString.of(Locale.GERMAN, "Neu Name", Locale.ENGLISH, "new name");
    final CartPredicate newCartDiscountPredicate = CartPredicate.of("1 = 1");
    final CartDiscountValue newCartDiscountValue =
        CartDiscountValue.ofAbsolute(MoneyImpl.of(10, EUR));
    final CartDiscountTarget newCartDiscountTarget = LineItemsTarget.of("quantity > 1");
    final String newSortOrder = "0.3";
    final LocalizedString newDesc =
        LocalizedString.of(Locale.GERMAN, "Neu Beschreibung", Locale.ENGLISH, "new description");
    final ZonedDateTime newValidFrom = ZonedDateTime.parse("2019-11-01T00:00:00.000Z");
    final ZonedDateTime newValidUntil = ZonedDateTime.parse("2019-11-15T00:00:00.000Z");
    final boolean newIsActive = true;
    final boolean newRequireDiscountCode = true;
    final StackingMode newStackingMode = StackingMode.STOP_AFTER_THIS_DISCOUNT;
    final CustomFieldsDraft newCustomFieldsDraft =
        CustomFieldsDraftBuilder.ofTypeId(UUID.randomUUID().toString()).build();

    final CartDiscountDraft newCartDiscount =
        CartDiscountDraftBuilder.of(
                newName,
                newCartDiscountPredicate,
                newCartDiscountValue,
                newCartDiscountTarget,
                newSortOrder,
                newRequireDiscountCode)
            .active(newIsActive)
            .description(newDesc)
            .validFrom(newValidFrom)
            .validUntil(newValidUntil)
            .stackingMode(newStackingMode)
            .custom(newCustomFieldsDraft)
            .build();

    final CartDiscountSyncOptions cartDiscountSyncOptions =
        CartDiscountSyncOptionsBuilder.of(CTP_CLIENT).build();
    final List<UpdateAction<CartDiscount>> updateActions =
        buildActions(cartDiscount, newCartDiscount, cartDiscountSyncOptions);

    assertThat(updateActions)
        .containsExactly(
            ChangeValue.of(newCartDiscountValue),
            ChangeCartPredicate.of(newCartDiscountPredicate),
            ChangeTarget.of(newCartDiscountTarget),
            ChangeIsActive.of(newIsActive),
            ChangeName.of(newName),
            SetDescription.of(newDesc),
            ChangeSortOrder.of(newSortOrder),
            ChangeRequiresDiscountCode.of(newRequireDiscountCode),
            SetValidFromAndUntil.of(newValidFrom, newValidUntil),
            ChangeStackingMode.of(newStackingMode),
            SetCustomType.ofTypeIdAndJson(newCustomFieldsDraft.getType().getId(), emptyMap()));
  }

  @Test
  void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.ofTypeId("newId").addObject("newField", "newValue").build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft).custom(customFieldsDraft).build();

    final List<UpdateAction<CartDiscount>> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build());

    assertThat(actions)
        .containsExactly(
            SetCustomType.ofTypeIdAndJson(
                customFieldsDraft.getType().getId(), customFieldsDraft.getFields()));
  }

  @Test
  void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
        CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
            .addObject(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
            .addObject("name_2", "value_2")
            .build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(sameCustomFieldDraftWithNewCustomField)
            .build();

    final List<UpdateAction<CartDiscount>> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build());

    assertThat(actions)
        .containsExactly(
            SetCustomField.ofJson("name_2", JsonNodeFactory.instance.textNode("value_2")));
  }

  @Test
  void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
        CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
            .addObject(CUSTOM_FIELD_NAME, "newValue")
            .build();

    final CartDiscountDraft cartDiscountDraftWithCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft)
            .custom(sameCustomFieldDraftWithNewValue)
            .build();

    final List<UpdateAction<CartDiscount>> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build());

    assertThat(actions)
        .containsExactly(
            SetCustomField.ofJson(
                CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("newValue")));
  }

  @Test
  void buildActions_WithJustNewCartDiscountHasNullCustomType_ShouldBuildUpdateAction() {
    final CartDiscountDraft cartDiscountDraftWithNullCustomField =
        CartDiscountDraftBuilder.of(cartDiscountDraft).custom(null).build();

    final List<UpdateAction<CartDiscount>> actions =
        buildActions(
            cartDiscount,
            cartDiscountDraftWithNullCustomField,
            CartDiscountSyncOptionsBuilder.of(mock(SphereClient.class)).build());

    assertThat(actions).containsExactly(SetCustomType.ofRemoveType());
  }
}
