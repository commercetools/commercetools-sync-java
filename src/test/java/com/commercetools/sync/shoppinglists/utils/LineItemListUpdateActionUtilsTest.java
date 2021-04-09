package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemsUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListSyncUtils.buildActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.LineItemReferenceResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.AddLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import io.sphere.sdk.types.CustomFieldsDraft;
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
      ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

  private static final LineItemReferenceResolver lineItemReferenceResolver =
      new LineItemReferenceResolver(SYNC_OPTIONS, getMockTypeService());

  private static final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @Test
  void buildLineItemsUpdateActions_WithoutNewAndWithNullOldLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(emptyList());

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithNullNewAndNullOldLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithNullOldAndEmptyNewLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .lineItems(emptyList())
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithNullOldAndNewLineItemsWithNullElement_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .lineItems(singletonList(null))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithNullNewLineItemsAndExistingLineItems_ShouldBuild3RemoveActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"));
  }

  @Test
  void buildLineItemsUpdateActions_WithNewLineItemsAndNoOldLineItems_ShouldBuild3AddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(newShoppingList.getLineItems()).isNotNull();

    assertThat(updateActions)
        .hasSize(3)
        .extracting("sku")
        .asString()
        .isEqualTo("[SKU-1, SKU-2, SKU-3]");

    assertThat(updateActions)
        .containsExactly(
            AddLineItem.of(newShoppingList.getLineItems().get(0)),
            AddLineItem.of(newShoppingList.getLineItems().get(1)),
            AddLineItem.of(newShoppingList.getLineItems().get(2)));
  }

  @Test
  void
      buildLineItemsUpdateActions_WithOnlyNewLineItemsWithoutValidQuantity_ShouldNotBuildAddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .lineItems(
                asList(
                    LineItemDraftBuilder.ofSku("sku1", null).build(),
                    LineItemDraftBuilder.ofSku("sku2", 0L).build(),
                    LineItemDraftBuilder.ofSku("sku3", -1L).build()))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithIdenticalLineItems_ShouldNotBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildLineItemsUpdateActions_WithSameLineItemPositionButChangesWithin_ShouldBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123_WITH_CHANGES);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"),
            ChangeLineItemQuantity.of("line_item_id_2", 4L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText2"), "line_item_id_2"),
            ChangeLineItemQuantity.of("line_item_id_3", 6L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText3"), "line_item_id_3"));
  }

  @Test
  void buildLineItemsUpdateActions_WithOneMissingLineItem_ShouldBuildOneRemoveLineItemAction() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_12);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).containsExactly(RemoveLineItem.of("line_item_id_3"));
  }

  @Test
  void buildLineItemsUpdateActions_WithOneExtraLineItem_ShouldBuildAddLineItemAction() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1234);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final LineItemDraft expectedLineItemDraft =
        LineItemDraftBuilder.ofSku("SKU-4", 4L)
            .custom(
                CustomFieldsDraft.ofTypeIdAndJson(
                    "custom_type_id",
                    singletonMap("textField", JsonNodeFactory.instance.textNode("text4"))))
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    assertThat(updateActions).containsExactly(AddLineItem.of(expectedLineItemDraft));
  }

  @Test
  void buildLineItemsUpdateAction_WithOneLineItemSwitch_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_124);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final LineItemDraft expectedLineItemDraft =
        LineItemDraftBuilder.ofSku("SKU-4", 4L)
            .custom(
                CustomFieldsDraft.ofTypeIdAndJson(
                    "custom_type_id",
                    singletonMap("textField", JsonNodeFactory.instance.textNode("text4"))))
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_3"), AddLineItem.of(expectedLineItemDraft));
  }

  @Test
  void buildLineItemsUpdateAction_WithDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_312);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItem.of(newShoppingList.getLineItems().get(1)), // SKU-1
            AddLineItem.of(newShoppingList.getLineItems().get(2)) // SKU-2
            );
  }

  @Test
  void
      buildLineItemsUpdateAction_WithRemovedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_32);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItem.of(newShoppingList.getLineItems().get(1)) // SKU-2
            );
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1324);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(1)), // SKU-3
            AddLineItem.of(newShoppingList.getLineItems().get(2)), // SKU-2
            AddLineItem.of(newShoppingList.getLineItems().get(3)) // SKU-4
            );
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedLineItemInBetween_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1423);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(1)), // SKU-4
            AddLineItem.of(newShoppingList.getLineItems().get(2)), // SKU-2
            AddLineItem.of(newShoppingList.getLineItems().get(3)) // SKU-3
            );
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedRemovedAndDifOrder_ShouldBuildRemoveAndAddLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_324);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItem.of(newShoppingList.getLineItems().get(1)), // SKU-2
            AddLineItem.of(newShoppingList.getLineItems().get(2)) // SKU-4
            );
  }

  @Test
  void
      buildLineItemsUpdateAction_WithAddedRemovedAndDifOrderAndChangedLineItem_ShouldBuildAllDiffLineItemActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(newShoppingList.getLineItems().get(1)), // SKU-3
            AddLineItem.of(newShoppingList.getLineItems().get(2)) // SKU-2
            );
  }

  @Test
  void buildLineItemsUpdateAction_WithNotExpandedLineItem_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_NOT_EXPANDED, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<String> errors = new ArrayList<>();
    final List<UpdateAction<ShoppingList>> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback)
        .containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"));

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

    final LineItem oldLineItem = mock(LineItem.class);
    when(oldLineItem.getVariant()).thenReturn(mockProductVariant);

    when(oldShoppingList.getLineItems()).thenReturn(singletonList(oldLineItem));

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    final List<String> errors = new ArrayList<>();
    final List<UpdateAction<ShoppingList>> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions);

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
        readObjectFromResource(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_NOT_EXPANDED);

    final List<String> errors = new ArrayList<>();
    final List<UpdateAction<ShoppingList>> updateActionsBeforeCallback = new ArrayList<>();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errors.add(exception.getMessage());
                  updateActionsBeforeCallback.addAll(updateActions);
                })
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback)
        .containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 1L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("text1"), "line_item_id_1"));

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
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123);

    Objects.requireNonNull(newShoppingList.getLineItems())
        .addAll(
            Arrays.asList(
                LineItemDraftBuilder.ofSku("sku1", null).build(),
                LineItemDraftBuilder.ofSku("sku2", 0L).build(),
                LineItemDraftBuilder.ofSku("sku3", -1L).build()));

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValuesWithLineItems_ShouldReturnActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedLineItemReferences(
            SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildActions(oldShoppingList, newShoppingList, mock(ShoppingListSyncOptions.class));

    assertThat(updateActions)
        .containsExactly(
            SetSlug.of(LocalizedString.ofEnglish("newSlug")),
            ChangeName.of(LocalizedString.ofEnglish("newName")),
            SetDescription.of(LocalizedString.ofEnglish("newDescription")),
            SetAnonymousId.of("newAnonymousId"),
            SetCustomer.of(ResourceIdentifier.ofId("customer_id_2")),
            SetDeleteDaysAfterLastModification.of(45),
            SetCustomField.ofJson("textField", JsonNodeFactory.instance.textNode("newTextValue")),
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItem.of(
                LineItemDraftBuilder.ofSku("SKU-3", 6L)
                    .custom(
                        CustomFieldsDraft.ofTypeIdAndJson(
                            "custom_type_id",
                            singletonMap(
                                "textField", JsonNodeFactory.instance.textNode("newText3"))))
                    .addedAt(ZonedDateTime.parse("2020-11-04T10:00:00.000Z"))
                    .build()),
            AddLineItem.of(
                LineItemDraftBuilder.ofSku("SKU-2", 4L)
                    .custom(
                        CustomFieldsDraft.ofTypeIdAndJson(
                            "custom_type_id",
                            singletonMap(
                                "textField", JsonNodeFactory.instance.textNode("newText2"))))
                    .addedAt(ZonedDateTime.parse("2020-11-03T10:00:00.000Z"))
                    .build()));
  }

  @Nonnull
  private static ShoppingListDraft mapToShoppingListDraftWithResolvedLineItemReferences(
      @Nonnull final String resourcePath) {

    final ShoppingListDraft template =
        ShoppingListReferenceResolutionUtils.mapToShoppingListDraft(
            readObjectFromResource(resourcePath, ShoppingList.class), referenceIdToKeyCache);

    final ShoppingListDraftBuilder builder = ShoppingListDraftBuilder.of(template);

    mapValuesToFutureOfCompletedValues(
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
