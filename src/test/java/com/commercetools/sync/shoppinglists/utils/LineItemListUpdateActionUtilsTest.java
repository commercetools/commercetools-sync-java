package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.commands.updateactions.AddLineItemWithSku;
import com.commercetools.sync.shoppinglists.helpers.LineItemReferenceResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class LineItemListUpdateActionUtilsTest {

    private static final String RES_ROOT = "com/commercetools/sync/shoppinglists/utils/lineitems/";
    private static final String SHOPPING_LIST_WITHOUT_LINE_ITEMS = RES_ROOT + "shoppinglist-without-lineitems.json";
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

    private static final ShoppingListSyncOptions SYNC_OPTIONS =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    private static final LineItemReferenceResolver lineItemReferenceResolver =
        new LineItemReferenceResolver(SYNC_OPTIONS, getMockTypeService());

    @Test
    void buildLineItemsUpdateActions_WithNullNewAndOldLineItems_ShouldNotBuildActions() {

        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITHOUT_LINE_ITEMS, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildLineItemsUpdateActions_WithNullOldAndEmptyNewLineItems_ShouldNotBuildActions() {

        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITHOUT_LINE_ITEMS, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .lineItems(emptyList())
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildLineItemsUpdateActions_WithNullOldAndNewLineItemsWithNullElement_ShouldNotBuildActions() {

        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITHOUT_LINE_ITEMS, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .lineItems(singletonList(null))
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildLineItemsUpdateActions_WithNullNewLineItemsAndExistingLineItems_ShouldBuild3RemoveActions() {

        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                                    .build();

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"));
    }

    @Test
    void buildLineItemsUpdateActions_WithNewLineItemsAndNoOldLineItems_ShouldBuild3AddActions() {

        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITHOUT_LINE_ITEMS, ShoppingList.class);

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

        assertThat(updateActions).containsExactly(
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(0)),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2))
        );
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
    void buildLineItemsUpdateActions_WithSameLineItemPositionButChangesWithin_ShouldBuildUpdateActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123_WITH_CHANGES);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson("textField",
                JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"),

            ChangeLineItemQuantity.of("line_item_id_2", 4L),
            SetLineItemCustomField.ofJson("textField",
                JsonNodeFactory.instance.textNode("newText2"), "line_item_id_2"),

            ChangeLineItemQuantity.of("line_item_id_3", 6L),
            SetLineItemCustomField.ofJson("textField",
                JsonNodeFactory.instance.textNode("newText3"), "line_item_id_3")
        );
    }

    @Test
    void buildLineItemsUpdateActions_WithOneMissingLineItem_ShouldBuildOneRemoveLineItemAction() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_12);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_3")
        );
    }

    @Test
    void buildLineItemsUpdateActions_WithOneExtraLineItem_ShouldBuildAddLineItemAction() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1234);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        final LineItemDraft expectedLineItemDraft =
            LineItemDraftBuilder.ofSku("SKU-4", 4L)
                                .custom(CustomFieldsDraft.ofTypeIdAndJson("custom_type_id",
                                    singletonMap("textField",
                                        JsonNodeFactory.instance.textNode("text4"))))
                                .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
                                .build();

        assertThat(updateActions).containsExactly(
            AddLineItemWithSku.of(expectedLineItemDraft)
        );
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
                                .custom(CustomFieldsDraft.ofTypeIdAndJson("custom_type_id",
                                    singletonMap("textField",
                                        JsonNodeFactory.instance.textNode("text4"))))
                                .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
                                .build();

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(expectedLineItemDraft)
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_312);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)), // SKU-1
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2))  // SKU-2
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithRemovedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_32);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)) // SKU-2
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithAddedAndDifferentOrder_ShouldBuildRemoveAndAddLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1324);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)), // SKU-3
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2)), // SKU-2
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(3)) // SKU-4
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithAddedLineItemInBetween_ShouldBuildRemoveAndAddLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_1423);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)), // SKU-4
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2)), // SKU-2
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(3)) // SKU-3
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithAddedRemovedAndDifOrder_ShouldBuildRemoveAndAddLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_324);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveLineItem.of("line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(0)), // SKU-3
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)), // SKU-2
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2)) // SKU-4
        );
    }

    @Test
    void buildLineItemsUpdateAction_WithAddedRemovedAndDifOrderAndChangedLineItem_ShouldBuildAllDiffLineItemActions() {
        final ShoppingList oldShoppingList =
            readObjectFromResource(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_123, ShoppingList.class);

        final ShoppingListDraft newShoppingList =
            mapToShoppingListDraftWithResolvedLineItemReferences(SHOPPING_LIST_WITH_LINE_ITEMS_SKU_132_WITH_CHANGES);

        final List<UpdateAction<ShoppingList>> updateActions =
            buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeLineItemQuantity.of("line_item_id_1", 2L),
            SetLineItemCustomField.ofJson("textField",
                JsonNodeFactory.instance.textNode("newText1"), "line_item_id_1"),
            RemoveLineItem.of("line_item_id_2"),
            RemoveLineItem.of("line_item_id_3"),
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(1)), // SKU-3
            AddLineItemWithSku.of(newShoppingList.getLineItems().get(2)) // SKU-2
        );
    }

    @Nonnull
    private static ShoppingListDraft mapToShoppingListDraftWithResolvedLineItemReferences(
        @Nonnull final String resourcePath) {

        final ShoppingListDraft template =
            ShoppingListReferenceResolutionUtils.mapToShoppingListDraft(
                readObjectFromResource(resourcePath, ShoppingList.class));

        final ShoppingListDraftBuilder builder = ShoppingListDraftBuilder.of(template);

        mapValuesToFutureOfCompletedValues(
            Objects.requireNonNull(builder.getLineItems()), lineItemReferenceResolver::resolveReferences, toList())
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
