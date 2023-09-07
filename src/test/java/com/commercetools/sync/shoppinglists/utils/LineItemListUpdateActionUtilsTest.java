package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListAddLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListRemoveLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomerActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.CompletableFutureUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.LineItemReferenceResolver;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class LineItemListUpdateActionUtilsTest {

  private static final String RES_ROOT = "com/commercetools/sync/shoppinglists/utils/lineitems/";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-123.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123_WITH_CHANGES =
      RES_ROOT + "shoppinglist-with-lineitems-sku-123-with-changes.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_12 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-12.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1234 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-1234.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_124 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-124.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_312 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-312.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_32 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-32.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1324 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-1324.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1423 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-1423.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_324 =
      RES_ROOT + "shoppinglist-with-lineitems-sku-324.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES =
      RES_ROOT + "shoppinglist-with-lineitems-sku-132-with-changes.json";
  private static final String SHOPPING_LIST_WITH_LINE_ITEMS_NOT_EXPANDED =
      RES_ROOT + "shoppinglist-with-lineitems-not-expanded.json";

  private static final ShoppingListSyncOptions SYNC_OPTIONS =
      ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  private static final LineItemReferenceResolver lineItemReferenceResolver =
      new LineItemReferenceResolver(SYNC_OPTIONS, getMockTypeService());

  private static final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @Test
  void buildLineItemsUpdateActions_WithoutNewAndWithNullOldLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(emptyList());

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithNullNewAndNullOldLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithNullOldAndEmptyNewLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).lineItems(emptyList()).build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithNullOldAndNewLineItemsWithNullElement_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .lineItems(singletonList(null))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithNullNewLineItemsAndExistingLineItems_ShouldBuild3RemoveActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_1").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build());
  }

  @Test
  void buildLineItemsUpdateActions_WithNewLineItemsAndNoOldLineItems_ShouldBuild3AddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(newShoppingList.getLineItems()).isNotNull();

    assertThat(updateActions)
        .hasSize(3)
        .extracting("sku")
        .asString()
        .isEqualTo("[SKU-1, SKU-2, SKU-3]");
    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(0);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(2);
    assertThat(updateActions)
        .containsExactly(
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateActions_WithOnlyNewLineItemsWithoutValidQuantity_ShouldNotBuildAddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .lineItems(
                asList(
                    ShoppingListLineItemDraftBuilder.of().sku("sku1").quantity(null).build(),
                    ShoppingListLineItemDraftBuilder.of().sku("sku2").quantity(0L).build(),
                    ShoppingListLineItemDraftBuilder.of().sku("sku3").quantity(-1L).build()))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithIdenticalLineItems_ShouldNotBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithSameLineItemPositionButChangesWithin_ShouldBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123_WITH_CHANGES);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .lineItemId("line_item_id_1")
                .build(),
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_2")
                .quantity(4L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText2")
                .lineItemId("line_item_id_2")
                .build(),
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_3")
                .quantity(6L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText3")
                .lineItemId("line_item_id_3")
                .build());
  }

  @Test
  void buildLineItemsUpdateActions_WithOneMissingLineItem_ShouldBuildOneRemoveLineItemAction() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_12);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build());
  }

  @Test
  void buildLineItemsUpdateActions_WithOneExtraLineItem_ShouldBuildAddLineItemAction() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1234);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft expectedLineItemDraft =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-4")
            .quantity(4L)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.id("custom_type_id"))
                    .fields(
                        fieldContainerBuilder ->
                            fieldContainerBuilder.addValue("textField", "text4"))
                    .build())
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    assertThat(updateActions)
        .containsExactly(
            ShoppingListAddLineItemActionBuilder.of()
                .sku(expectedLineItemDraft.getSku())
                .addedAt(expectedLineItemDraft.getAddedAt())
                .custom(expectedLineItemDraft.getCustom())
                .quantity(expectedLineItemDraft.getQuantity())
                .build());
  }

  @Test
  void buildLineItemsUpdateAction_WithOneLineItemSwitch_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_124);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft expectedLineItemDraft =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-4")
            .quantity(4L)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.id("custom_type_id"))
                    .fields(
                        fieldContainerBuilder ->
                            fieldContainerBuilder.addValue("textField", "text4"))
                    .build())
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .sku(expectedLineItemDraft.getSku())
                .addedAt(expectedLineItemDraft.getAddedAt())
                .custom(expectedLineItemDraft.getCustom())
                .quantity(expectedLineItemDraft.getQuantity())
                .build());
  }

  @Test
  void buildLineItemsUpdateAction_WithDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_312);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(0);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(2);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_1").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateAction_WithRemovedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_32);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(0);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(1);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_1").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1324);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(2);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(3);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedLineItemInBetween_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1423);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(2);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(3);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedRemovedAndDifOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_324);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft1 =
        newShoppingList.getLineItems().get(0);
    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(2);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_1").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft1.getAddedAt())
                .custom(shoppingListLineItemDraft1.getCustom())
                .sku(shoppingListLineItemDraft1.getSku())
                .quantity(shoppingListLineItemDraft1.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedRemovedAndDifOrderAndChangedLineItem_ShouldBuildAllDiffLineItemActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final ShoppingListLineItemDraft shoppingListLineItemDraft2 =
        newShoppingList.getLineItems().get(1);
    final ShoppingListLineItemDraft shoppingListLineItemDraft3 =
        newShoppingList.getLineItems().get(2);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .lineItemId("line_item_id_1")
                .build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft2.getAddedAt())
                .custom(shoppingListLineItemDraft2.getCustom())
                .sku(shoppingListLineItemDraft2.getSku())
                .quantity(shoppingListLineItemDraft2.getQuantity())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .addedAt(shoppingListLineItemDraft3.getAddedAt())
                .custom(shoppingListLineItemDraft3.getCustom())
                .sku(shoppingListLineItemDraft3.getSku())
                .quantity(shoppingListLineItemDraft3.getQuantity())
                .build());
  }

  @Test
  void buildLineItemsUpdateAction_WithNotExpandedLineItem_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_LINE_ITEMS_NOT_EXPANDED, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<String> errors = new ArrayList<>();
    final List<ShoppingListUpdateAction> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback)
        .containsExactly(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .lineItemId("line_item_id_1")
                .build());

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            "LineItem at position '1' of the ShoppingList with key "
                + "'shoppinglist-with-lineitems-not-expanded' has no SKU set. "
                + "Please make sure all line items have SKUs.");
  }

  @Test
  void buildLineItemsUpdateAction_WithOldLineItemWithoutSku_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getKey()).thenReturn("shoppinglist-with-lineitems-not-expanded");

    final ProductVariant mockProductVariant = mock(ProductVariant.class);
    when(mockProductVariant.getSku()).thenReturn(null);

    final ShoppingListLineItem oldLineItem = mock(ShoppingListLineItem.class);
    when(oldLineItem.getVariant()).thenReturn(mockProductVariant);

    when(oldShoppingList.getLineItems()).thenReturn(singletonList(oldLineItem));

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<String> errors = new ArrayList<>();
    final List<ShoppingListUpdateAction> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback).isEmpty();

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            "LineItem at position '0' of the ShoppingList with key "
                + "'shoppinglist-with-lineitems-not-expanded' has no SKU set. "
                + "Please make sure all line items have SKUs.");
  }

  @Test
  void buildLineItemsUpdateAction_WithNewLineItemWithoutSku_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_NOT_EXPANDED);

    final List<String> errors = new ArrayList<>();
    final List<ShoppingListUpdateAction> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback)
        .containsExactly(
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_1")
                .quantity(1L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("text1")
                .lineItemId("line_item_id_1")
                .build());

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            "LineItemDraft at position '1' of the ShoppingListDraft with key "
                + "'shoppinglist-with-lineitems-not-expanded' has no SKU set. "
                + "Please make sure all line item drafts have SKUs.");
  }

  @Test
  void buildLineItemsUpdateActions_WithNewLineItemsWithoutValidQuantity_ShouldNotBuildAddActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    Objects.requireNonNull(newShoppingList.getLineItems())
        .addAll(
            Arrays.asList(
                ShoppingListLineItemDraftBuilder.of().sku("sku1").quantity(null).build(),
                ShoppingListLineItemDraftBuilder.of().sku("sku2").quantity(0L).build(),
                ShoppingListLineItemDraftBuilder.of().sku("sku3").quantity(-1L).build()));

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValuesWithLineItems_ShouldReturnActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<ShoppingListUpdateAction> updateActions =
        ShoppingListSyncUtils.buildActions(
            oldShoppingList, newShoppingList, mock(ShoppingListSyncOptions.class));

    assertThat(updateActions)
        .containsExactly(
            ShoppingListSetSlugActionBuilder.of().slug(ofEnglish("newSlug")).build(),
            ShoppingListChangeNameActionBuilder.of().name(ofEnglish("newName")).build(),
            ShoppingListSetDescriptionActionBuilder.of()
                .description(ofEnglish("newDescription"))
                .build(),
            ShoppingListSetAnonymousIdActionBuilder.of().anonymousId("newAnonymousId").build(),
            ShoppingListSetCustomerActionBuilder.of()
                .customer(CustomerResourceIdentifierBuilder.of().id("customer_id_2").build())
                .build(),
            ShoppingListSetDeleteDaysAfterLastModificationActionBuilder.of()
                .deleteDaysAfterLastModification(45L)
                .build(),
            ShoppingListSetCustomFieldActionBuilder.of()
                .name("textField")
                .value("newTextValue")
                .build(),
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId("line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .lineItemId("line_item_id_1")
                .build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_2").build(),
            ShoppingListRemoveLineItemActionBuilder.of().lineItemId("line_item_id_3").build(),
            ShoppingListAddLineItemActionBuilder.of()
                .sku("SKU-3")
                .quantity(6L)
                .custom(
                    CustomFieldsDraftBuilder.of()
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.id("custom_type_id"))
                        .fields(
                            fieldContainerBuilder ->
                                fieldContainerBuilder.addValue("textField", "newText3"))
                        .build())
                .addedAt(ZonedDateTime.parse("2020-11-04T10:00:00.000Z"))
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .sku("SKU-2")
                .quantity(4L)
                .custom(
                    CustomFieldsDraftBuilder.of()
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.id("custom_type_id"))
                        .fields(
                            fieldContainerBuilder ->
                                fieldContainerBuilder.addValue("textField", "newText2"))
                        .build())
                .addedAt(ZonedDateTime.parse("2020-11-03T10:00:00.000Z"))
                .build());
  }

  @Nonnull
  private static ShoppingListDraft mapToShoppingListDraftWithResolvedLineItemReferences(
      @Nonnull final String resourcePath) {

    final ShoppingListDraft template =
        ShoppingListReferenceResolutionUtils.mapToShoppingListDraft(
            TestUtils.readObjectFromResource(resourcePath, ShoppingList.class),
            referenceIdToKeyCache);

    final ShoppingListDraftBuilder builder = ShoppingListDraftBuilder.of(template);

    CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
            Objects.requireNonNull(builder.getLineItems()),
            lineItemReferenceResolver::resolveReferences,
            toList())
        .thenApply(builder::lineItems)
        .join();

    return builder.build();
  }

  @Nonnull
  private static TypeService getMockTypeService() {
    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(completedFuture(Optional.of("custom_type_id")));
    return typeService;
  }
}
