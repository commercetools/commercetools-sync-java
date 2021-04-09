package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.commons.utils.CompletableFutureUtils.mapValuesToFutureOfCompletedValues;
import static com.commercetools.sync.shoppinglists.utils.LineItemUpdateActionUtils.buildLineItemsUpdateActions;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListSyncUtils.buildActions;
import static com.commercetools.sync.shoppinglists.utils.TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions;
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
import com.commercetools.sync.shoppinglists.helpers.TextLineItemReferenceResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.AddTextLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveTextLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemDescription;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class TextLineItemListUpdateActionUtilsTest {

  private static final String RES_ROOT =
      "com/commercetools/sync/shoppinglists/utils/textlineitems/";
  private static final String SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123 =
      RES_ROOT + "shoppinglist-with-textlineitems-name-123.json";
  private static final String SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123_WITH_CHANGES =
      RES_ROOT + "shoppinglist-with-textlineitems-name-123-with-changes.json";
  private static final String SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_12 =
      RES_ROOT + "shoppinglist-with-textlineitems-name-12.json";
  private static final String SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_1234 =
      RES_ROOT + "shoppinglist-with-textlineitems-name-1234.json";

  private static final ShoppingListSyncOptions SYNC_OPTIONS =
      ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();

  private static final TextLineItemReferenceResolver textLineItemReferenceResolver =
      new TextLineItemReferenceResolver(SYNC_OPTIONS, getMockTypeService());

  private static final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @Test
  void
      buildTextLineItemsUpdateActions_WithoutNewAndWithNullOldTextLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(emptyList());

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemsUpdateActions_WithNullNewAndNullOldTextLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemsUpdateActions_WithNullOldAndEmptyNewTextLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .textLineItems(emptyList())
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNullOldAndNewTextLineItemsWithNullElement_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .textLineItems(singletonList(null))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNullNewLineItemsAndExistingLineItems_ShouldBuild3RemoveActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name")).build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            RemoveTextLineItem.of("text_line_item_id_1"),
            RemoveTextLineItem.of("text_line_item_id_2"),
            RemoveTextLineItem.of("text_line_item_id_3"));
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNewTextLineItemsAndNoOldTextLineItems_ShouldBuild3AddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(newShoppingList.getTextLineItems()).isNotNull();
    assertThat(updateActions)
        .containsExactly(
            mapToAddTextLineItemAction(newShoppingList.getTextLineItems().get(0)),
            mapToAddTextLineItemAction(newShoppingList.getTextLineItems().get(1)),
            mapToAddTextLineItemAction(newShoppingList.getTextLineItems().get(2)));
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithOnlyNewTextLineItemsWithoutValidQuantity_ShouldNotBuildAddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
            .textLineItems(
                asList(
                    TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name1"), null).build(),
                    TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name2"), 0L).build(),
                    TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name3"), -1L).build()))
            .build();

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithIdenticalTextLineItems_ShouldNotBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithAllDifferentFields_ShouldBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123_WITH_CHANGES);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ChangeTextLineItemName.of(
                "text_line_item_id_1",
                LocalizedString.ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE")),
            SetTextLineItemDescription.of("text_line_item_id_1")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_1", 2L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "text_line_item_id_1"),
            ChangeTextLineItemName.of(
                "text_line_item_id_2",
                LocalizedString.ofEnglish("newName2-EN").plus(Locale.GERMAN, "newName2-DE")),
            SetTextLineItemDescription.of("text_line_item_id_2")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc2-EN").plus(Locale.GERMAN, "newDesc2-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_2", 4L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText2"), "text_line_item_id_2"),
            ChangeTextLineItemName.of(
                "text_line_item_id_3",
                LocalizedString.ofEnglish("newName3-EN").plus(Locale.GERMAN, "newName3-DE")),
            SetTextLineItemDescription.of("text_line_item_id_3")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc3-EN").plus(Locale.GERMAN, "newDesc3-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_3", 6L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText3"), "text_line_item_id_3"));
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithOneMissingTextLineItem_ShouldBuildOneRemoveTextLineItemAction() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_12);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).containsExactly(RemoveTextLineItem.of("text_line_item_id_3"));
  }

  @Test
  void buildTextLineItemsUpdateActions_WithOneExtraTextLineItem_ShouldBuildAddTextLineItemAction() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_1234);

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final TextLineItemDraft expectedLineItemDraft =
        TextLineItemDraftBuilder.of(
                LocalizedString.ofEnglish("name4-EN").plus(Locale.GERMAN, "name4-DE"), 4L)
            .description(LocalizedString.ofEnglish("desc4-EN").plus(Locale.GERMAN, "desc4-DE"))
            .custom(
                CustomFieldsDraft.ofTypeIdAndJson(
                    "custom_type_id",
                    singletonMap("textField", JsonNodeFactory.instance.textNode("text4"))))
            .addedAt(ZonedDateTime.parse("2020-11-06T10:00:00.000Z"))
            .build();

    assertThat(updateActions).containsExactly(mapToAddTextLineItemAction(expectedLineItemDraft));
  }

  @Test
  void buildTextLineItemsUpdateAction_WithTextLineItemWithNullName_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final TextLineItemDraft updatedTextLineItemDraft1 =
        TextLineItemDraftBuilder.of(
                LocalizedString.ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE"), 2L)
            .description(
                LocalizedString.ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE"))
            .custom(
                CustomFieldsDraft.ofTypeIdAndJson(
                    "custom_type_id",
                    singletonMap("textField", JsonNodeFactory.instance.textNode("newText1"))))
            .build();

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shoppinglist"))
            .key("key")
            .textLineItems(
                asList(updatedTextLineItemDraft1, TextLineItemDraftBuilder.of(null, 1L).build()))
            .build();

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
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback)
        .containsExactly(
            ChangeTextLineItemName.of(
                "text_line_item_id_1",
                LocalizedString.ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE")),
            SetTextLineItemDescription.of("text_line_item_id_1")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_1", 2L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "text_line_item_id_1"));

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            "TextLineItemDraft at position '1' of the ShoppingListDraft with key "
                + "'key' has no name set. Please make sure all text line items have names.");
  }

  @Test
  void buildTextLineItemsUpdateAction_WithTextLineItemWithoutName_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shoppinglist"))
            .key("key")
            .textLineItems(
                singletonList(TextLineItemDraftBuilder.of(LocalizedString.of(), 1L).build()))
            .build();

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
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(updateActionsBeforeCallback).isEmpty();

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0))
        .isEqualTo(
            "TextLineItemDraft at position '0' of the ShoppingListDraft with key "
                + "'key' has no name set. Please make sure all text line items have names.");
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNewTextLineItemsWithoutValidQuantity_ShouldNotBuildAddActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    Objects.requireNonNull(newShoppingList.getTextLineItems())
        .addAll(
            Arrays.asList(
                TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name1"), null).build(),
                TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name2"), 0L).build(),
                TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name3"), -1L).build()));

    final List<UpdateAction<ShoppingList>> updateActions =
        buildTextLineItemsUpdateActions(oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValuesWithTextLineItems_ShouldReturnActions() {
    final ShoppingList oldShoppingList =
        readObjectFromResource(SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123_WITH_CHANGES);

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
            ChangeTextLineItemName.of(
                "text_line_item_id_1",
                LocalizedString.ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE")),
            SetTextLineItemDescription.of("text_line_item_id_1")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_1", 2L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText1"), "text_line_item_id_1"),
            ChangeTextLineItemName.of(
                "text_line_item_id_2",
                LocalizedString.ofEnglish("newName2-EN").plus(Locale.GERMAN, "newName2-DE")),
            SetTextLineItemDescription.of("text_line_item_id_2")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc2-EN").plus(Locale.GERMAN, "newDesc2-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_2", 4L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText2"), "text_line_item_id_2"),
            ChangeTextLineItemName.of(
                "text_line_item_id_3",
                LocalizedString.ofEnglish("newName3-EN").plus(Locale.GERMAN, "newName3-DE")),
            SetTextLineItemDescription.of("text_line_item_id_3")
                .withDescription(
                    LocalizedString.ofEnglish("newDesc3-EN").plus(Locale.GERMAN, "newDesc3-DE")),
            ChangeTextLineItemQuantity.of("text_line_item_id_3", 6L),
            SetTextLineItemCustomField.ofJson(
                "textField", JsonNodeFactory.instance.textNode("newText3"), "text_line_item_id_3"));
  }

  @Nonnull
  private static ShoppingListDraft mapToShoppingListDraftWithResolvedTextLineItemReferences(
      @Nonnull final String resourcePath) {

    final ShoppingListDraft template =
        ShoppingListReferenceResolutionUtils.mapToShoppingListDraft(
            readObjectFromResource(resourcePath, ShoppingList.class), referenceIdToKeyCache);

    final ShoppingListDraftBuilder builder = ShoppingListDraftBuilder.of(template);

    mapValuesToFutureOfCompletedValues(
            Objects.requireNonNull(builder.getTextLineItems()),
            textLineItemReferenceResolver::resolveReferences,
            toList())
        .thenApply(builder::textLineItems)
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

  @Nonnull
  private static AddTextLineItem mapToAddTextLineItemAction(
      @Nonnull final TextLineItemDraft textLineItemDraft) {

    return AddTextLineItem.of(textLineItemDraft.getName())
        .withDescription(textLineItemDraft.getDescription())
        .withQuantity(textLineItemDraft.getQuantity())
        .withAddedAt(textLineItemDraft.getAddedAt())
        .withCustom(textLineItemDraft.getCustom());
  }
}
