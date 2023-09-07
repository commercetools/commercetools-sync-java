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
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListAddTextLineItemAction;
import com.commercetools.api.models.shopping_list.ShoppingListAddTextLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListRemoveTextLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomerActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.CompletableFutureUtils;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TestUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.TextLineItemReferenceResolver;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
      ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

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
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemsUpdateActions_WithNullNewAndNullOldTextLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildTextLineItemsUpdateActions_WithNullOldAndEmptyNewTextLineItems_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).textLineItems(emptyList()).build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNullOldAndNewTextLineItemsWithNullElement_ShouldNotBuildActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .textLineItems(singletonList(null))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNullNewLineItemsAndExistingLineItems_ShouldBuild3RemoveActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of().name(ofEnglish("name")).build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveTextLineItemActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .build(),
            ShoppingListRemoveTextLineItemActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .build(),
            ShoppingListRemoveTextLineItemActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .build());
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithNewTextLineItemsAndNoOldTextLineItems_ShouldBuild3AddActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

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
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("name"))
            .textLineItems(
                asList(
                    TextLineItemDraftBuilder.of().name(ofEnglish("name1")).quantity(null).build(),
                    TextLineItemDraftBuilder.of().name(ofEnglish("name2")).quantity(0L).build(),
                    TextLineItemDraftBuilder.of().name(ofEnglish("name3")).quantity(-1L).build()))
            .build();

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithIdenticalTextLineItems_ShouldNotBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    final List<ShoppingListUpdateAction> updateActions =
        LineItemUpdateActionUtils.buildLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildLineItemsUpdateActions_WithAllDifferentFields_ShouldBuildUpdateActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123_WITH_CHANGES);

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .name(ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .description(ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .textLineItemId("text_line_item_id_1")
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .name(ofEnglish("newName2-EN").plus(Locale.GERMAN, "newName2-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .description(ofEnglish("newDesc2-EN").plus(Locale.GERMAN, "newDesc2-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .quantity(4L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText2")
                .textLineItemId("text_line_item_id_2")
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .name(ofEnglish("newName3-EN").plus(Locale.GERMAN, "newName3-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .description(ofEnglish("newDesc3-EN").plus(Locale.GERMAN, "newDesc3-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .quantity(6L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText3")
                .textLineItemId("text_line_item_id_3")
                .build());
  }

  @Test
  void
      buildTextLineItemsUpdateActions_WithOneMissingTextLineItem_ShouldBuildOneRemoveTextLineItemAction() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_12);

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ShoppingListRemoveTextLineItemActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .build());
  }

  @Test
  void buildTextLineItemsUpdateActions_WithOneExtraTextLineItem_ShouldBuildAddTextLineItemAction() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_1234);

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    final TextLineItemDraft expectedLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("name4-EN").plus(Locale.GERMAN, "name4-DE"))
            .quantity(4L)
            .description(ofEnglish("desc4-EN").plus(Locale.GERMAN, "desc4-DE"))
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.id("custom_type_id"))
                    .fields(
                        fieldContainerBuilder ->
                            fieldContainerBuilder.addValue("textField", "text4"))
                    .build())
            .addedAt(ZonedDateTime.parse("2020-11-06T10:00:00.000Z"))
            .build();

    assertThat(updateActions).containsExactly(mapToAddTextLineItemAction(expectedLineItemDraft));
  }

  @Test
  void buildTextLineItemsUpdateAction_WithTextLineItemWithoutName_ShouldTriggerErrorCallback() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppinglist"))
            .key("key")
            .textLineItems(
                singletonList(
                    TextLineItemDraftBuilder.of().name(LocalizedString.of()).quantity(1L).build()))
            .build();

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
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, syncOptions);

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
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123);

    Objects.requireNonNull(newShoppingList.getTextLineItems())
        .addAll(
            List.of(
                TextLineItemDraftBuilder.of().name(ofEnglish("name1")).quantity(null).build(),
                TextLineItemDraftBuilder.of().name(ofEnglish("name2")).quantity(0L).build(),
                TextLineItemDraftBuilder.of().name(ofEnglish("name3")).quantity(-1L).build()));

    final List<ShoppingListUpdateAction> updateActions =
        TextLineItemUpdateActionUtils.buildTextLineItemsUpdateActions(
            oldShoppingList, newShoppingList, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildActions_WithDifferentValuesWithTextLineItems_ShouldReturnActions() {
    final ShoppingList oldShoppingList =
        TestUtils.readObjectFromResource(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123, ShoppingList.class);

    final ShoppingListDraft newShoppingList =
        mapToShoppingListDraftWithResolvedTextLineItemReferences(
            SHOPPING_LIST_WITH_TEXT_LINE_ITEMS_NAME_123_WITH_CHANGES);

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
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .name(ofEnglish("newName1-EN").plus(Locale.GERMAN, "newName1-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .description(ofEnglish("newDesc1-EN").plus(Locale.GERMAN, "newDesc1-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_1")
                .quantity(2L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText1")
                .textLineItemId("text_line_item_id_1")
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .name(ofEnglish("newName2-EN").plus(Locale.GERMAN, "newName2-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .description(ofEnglish("newDesc2-EN").plus(Locale.GERMAN, "newDesc2-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_2")
                .quantity(4L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText2")
                .textLineItemId("text_line_item_id_2")
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .name(ofEnglish("newName3-EN").plus(Locale.GERMAN, "newName3-DE"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .description(ofEnglish("newDesc3-EN").plus(Locale.GERMAN, "newDesc3-DE"))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId("text_line_item_id_3")
                .quantity(6L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("textField")
                .value("newText3")
                .textLineItemId("text_line_item_id_3")
                .build());
  }

  @Nonnull
  private static ShoppingListDraft mapToShoppingListDraftWithResolvedTextLineItemReferences(
      @Nonnull final String resourcePath) {

    final ShoppingListDraft template =
        ShoppingListReferenceResolutionUtils.mapToShoppingListDraft(
            TestUtils.readObjectFromResource(resourcePath, ShoppingList.class),
            referenceIdToKeyCache);

    final ShoppingListDraftBuilder builder = ShoppingListDraftBuilder.of(template);

    CompletableFutureUtils.mapValuesToFutureOfCompletedValues(
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
  private static ShoppingListAddTextLineItemAction mapToAddTextLineItemAction(
      @Nonnull final TextLineItemDraft textLineItemDraft) {

    return ShoppingListAddTextLineItemActionBuilder.of()
        .name(textLineItemDraft.getName())
        .description(textLineItemDraft.getDescription())
        .quantity(textLineItemDraft.getQuantity())
        .addedAt(textLineItemDraft.getAddedAt())
        .custom(textLineItemDraft.getCustom())
        .build();
  }
}
