package com.commercetools.sync.integration.externalsource.shoppinglists;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.buildIngredientCustomType;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.buildUtensilsCustomType;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createSampleShoppingListCarrotCake;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.shoppinglists.utils.ShoppingListTransformUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraftDsl;
import io.sphere.sdk.shoppinglists.commands.updateactions.AddLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.AddTextLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemName;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeTextLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetTextLineItemDescription;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListSyncIT {

  private List<String> errorMessages;
  private List<String> warningMessages;
  private List<Throwable> exceptions;
  private List<UpdateAction<ShoppingList>> updateActionList;

  private ShoppingList shoppingListSampleCarrotCake;
  private ShoppingListDraft shoppingListDraftSampleCarrotCake;
  private ShoppingListSync shoppingListSync;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeEach
  void setup() {
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    deleteShoppingListTestData(CTP_TARGET_CLIENT);
    setUpShoppingListSync();

    final ImmutablePair<ShoppingList, ShoppingListDraft> sampleShoppingListCarrotCake =
        createSampleShoppingListCarrotCake(CTP_TARGET_CLIENT);

    shoppingListSampleCarrotCake = sampleShoppingListCarrotCake.getLeft();
    shoppingListDraftSampleCarrotCake = sampleShoppingListCarrotCake.getRight();
  }

  private void setUpShoppingListSync() {
    errorMessages = new ArrayList<>();
    warningMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    updateActionList = new ArrayList<>();

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .beforeUpdateCallback(
                (updateActions, customerDraft, customer) -> {
                  updateActionList.addAll(Objects.requireNonNull(updateActions));
                  return updateActions;
                })
            .build();

    shoppingListSync = new ShoppingListSync(shoppingListSyncOptions);
  }

  @AfterAll
  static void tearDown() {
    deleteShoppingListTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_WithSameShoppingList_ShouldNotUpdateShoppingList() {
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync
            .sync(singletonList(shoppingListDraftSampleCarrotCake))
            .toCompletableFuture()
            .join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 0);
    assertThat(shoppingListSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 shopping lists were processed in total "
                + "(0 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithModifiedShoppingList_ShouldUpdateShoppingList() {
    final ShoppingListDraft modifiedShoppingListDraft = prepareUpdatedDraft();

    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync
            .sync(singletonList(modifiedShoppingListDraft))
            .toCompletableFuture()
            .join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    assertUpdateActions();

    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
    assertThat(shoppingListSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 shopping lists were processed in total "
                + "(0 created, 1 updated and 0 failed to sync).");

    assertShoppingListUpdatedCorrectly(modifiedShoppingListDraft);
  }

  /**
   * To understand the reasoning behind the ordering changes, it would be useful to check
   * `docs/adr/0002-shopping-lists-lineitem-and-textlineitem-update-actions.md`
   */
  @Nonnull
  private ShoppingListDraft prepareUpdatedDraft() {
    final List<LineItemDraft> newLineItemDrafts = new ArrayList<>();
    final LineItemDraft updatedLineItemDraft =
        LineItemDraftBuilder.ofSku("SKU-3", 2L) // was 1
            .custom(buildIngredientCustomType("sugar", "150g")) // was 100g
            .addedAt(ZonedDateTime.now())
            .build();

    final LineItemDraft newLineItemDraft =
        LineItemDraftBuilder.ofSku("SKU-5", 1L)
            .custom(buildIngredientCustomType("nuts", "100g"))
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    for (int i = 0;
        i < Objects.requireNonNull(shoppingListDraftSampleCarrotCake.getLineItems()).size();
        i++) {
      if (i == 2) {
        newLineItemDrafts.add(updatedLineItemDraft);
        continue;
      }

      if (i == 4) {
        newLineItemDrafts.add(newLineItemDraft);
      }

      newLineItemDrafts.add(shoppingListDraftSampleCarrotCake.getLineItems().get(i));
    }

    final TextLineItemDraftDsl updatedTextLineItemDraft =
        TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("step 1 - updated"), 2L)
            .description(
                LocalizedString.ofEnglish(
                    "Peel carrots and set aside, crack the nuts, separate eggs into small balls."))
            .custom(buildUtensilsCustomType("Peeler, nuts cracker, 2 small bowls"))
            .build();

    final TextLineItemDraftDsl newTextLineItemDraft =
        TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("before step 5"), 1L)
            .description(LocalizedString.ofEnglish("Pre-heat oven to 180 C degree."))
            .custom(buildUtensilsCustomType("Oven"))
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

    final List<TextLineItemDraft> newTextLineItemDrafts = new ArrayList<>();
    for (int i = 0;
        i < Objects.requireNonNull(shoppingListDraftSampleCarrotCake.getTextLineItems()).size();
        i++) {
      if (i == 0) {
        newTextLineItemDrafts.add(updatedTextLineItemDraft);
        continue;
      }

      if (i == 4) {
        newTextLineItemDrafts.add(newTextLineItemDraft);
      }

      newTextLineItemDrafts.add(shoppingListDraftSampleCarrotCake.getTextLineItems().get(i));
    }

    final Map<String, JsonNode> servingsFields = new HashMap<>();
    servingsFields.put(
        "nutrition",
        JsonNodeFactory.instance.textNode("Per servings: 600 cal, 11g protein, 30g fat, 56g carb"));
    servingsFields.put("servings", JsonNodeFactory.instance.numberNode(14));

    return ShoppingListDraftBuilder.of(shoppingListDraftSampleCarrotCake)
        .name(LocalizedString.ofEnglish("Carrot Cake - (for xmas)"))
        .slug(LocalizedString.ofEnglish("carrot-cake-for-xmas"))
        .description(LocalizedString.ofEnglish("Carrot cake recipe - ingredients (for xmas)"))
        .anonymousId("public-carrot-cake-shopping-list-xmas")
        .deleteDaysAfterLastModification(15)
        .custom(CustomFieldsDraft.ofTypeKeyAndJson("custom-type-shopping-list", servingsFields))
        .lineItems(newLineItemDrafts)
        .textLineItems(newTextLineItemDrafts)
        .build();
  }

  private void assertUpdateActions() {
    // copy references / manual ref resolution for testing purpose
    final String lineItemId_Sku3Sugar = shoppingListSampleCarrotCake.getLineItems().get(2).getId();
    final String lineItemId_Sku5BakingPowder =
        shoppingListSampleCarrotCake.getLineItems().get(4).getId();
    final String lineItemId_Sku6Cinnamon =
        shoppingListSampleCarrotCake.getLineItems().get(5).getId();
    final String lineItemTypeId =
        shoppingListSampleCarrotCake.getLineItems().get(2).getCustom().getType().getId();
    final String textLineItemId_Step1 =
        shoppingListSampleCarrotCake.getTextLineItems().get(0).getId();
    final String textLineItemId_Step5 =
        shoppingListSampleCarrotCake.getTextLineItems().get(4).getId();
    final String textLineItemId_Step6 =
        shoppingListSampleCarrotCake.getTextLineItems().get(5).getId();
    final String textLineItemId =
        shoppingListSampleCarrotCake.getTextLineItems().get(0).getCustom().getType().getId();

    assertThat(updateActionList)
        .contains(
            SetSlug.of(LocalizedString.ofEnglish("carrot-cake-for-xmas")),
            ChangeName.of(LocalizedString.ofEnglish("Carrot Cake - (for xmas)")),
            SetDescription.of(
                LocalizedString.ofEnglish("Carrot cake recipe - ingredients (for xmas)")),
            SetAnonymousId.of("public-carrot-cake-shopping-list-xmas"),
            SetDeleteDaysAfterLastModification.of(15),
            SetCustomField.ofJson(
                "nutrition",
                JsonNodeFactory.instance.textNode(
                    "Per servings: 600 cal, 11g protein, 30g fat, 56g carb")),
            SetCustomField.ofJson("servings", JsonNodeFactory.instance.numberNode(14)),
            ChangeLineItemQuantity.of(lineItemId_Sku3Sugar, 2L),
            SetLineItemCustomField.ofJson(
                "amount", JsonNodeFactory.instance.textNode("150g"), lineItemId_Sku3Sugar),
            SetLineItemCustomField.ofJson(
                "amount", JsonNodeFactory.instance.textNode("100g"), lineItemId_Sku5BakingPowder),
            SetLineItemCustomField.ofJson(
                "ingredient",
                JsonNodeFactory.instance.textNode("nuts"),
                lineItemId_Sku5BakingPowder),
            RemoveLineItem.of(lineItemId_Sku6Cinnamon),
            AddLineItem.of(
                LineItemDraftBuilder.ofSku("SKU-5", 1L)
                    .custom(
                        CustomFieldsDraft.ofTypeIdAndJson(
                            lineItemTypeId,
                            buildIngredientCustomType("baking powder", "1 tsp").getFields()))
                    .build()),
            AddLineItem.of(
                LineItemDraftBuilder.ofSku("SKU-6", 1L)
                    .custom(
                        CustomFieldsDraft.ofTypeIdAndJson(
                            lineItemTypeId,
                            buildIngredientCustomType("cinnamon", "2 tsp").getFields()))
                    .build()),
            ChangeTextLineItemName.of(
                textLineItemId_Step1, LocalizedString.ofEnglish("step 1 - updated")),
            SetTextLineItemDescription.of(textLineItemId_Step1)
                .withDescription(
                    LocalizedString.ofEnglish(
                        "Peel carrots and set aside, crack the nuts, "
                            + "separate eggs into small balls.")),
            ChangeTextLineItemQuantity.of(textLineItemId_Step1, 2L),
            SetTextLineItemCustomField.ofJson(
                "utensils",
                JsonNodeFactory.instance.textNode("Peeler, nuts cracker, 2 small bowls"),
                textLineItemId_Step1),
            ChangeTextLineItemName.of(
                textLineItemId_Step5, LocalizedString.ofEnglish("before step 5")),
            SetTextLineItemDescription.of(textLineItemId_Step5)
                .withDescription(LocalizedString.ofEnglish("Pre-heat oven to 180 C degree.")),
            SetTextLineItemCustomField.ofJson(
                "utensils", JsonNodeFactory.instance.textNode("Oven"), textLineItemId_Step5),
            ChangeTextLineItemName.of(textLineItemId_Step6, LocalizedString.ofEnglish("step 5")),
            SetTextLineItemDescription.of(textLineItemId_Step6)
                .withDescription(
                    LocalizedString.ofEnglish(
                        "Put cake mixture into cake pan, bake appr 40 min with 180 C degree")),
            SetTextLineItemCustomField.ofJson(
                "utensils",
                JsonNodeFactory.instance.textNode("Cake pan, oven"),
                textLineItemId_Step6),
            AddTextLineItem.of(LocalizedString.ofEnglish("step 6"))
                .withDescription(
                    LocalizedString.ofEnglish("Decorate as you wish and serve, enjoy!"))
                .withQuantity(1L)
                .withAddedAt(ZonedDateTime.parse("2020-11-06T10:00:00.000Z"))
                .withCustom(
                    CustomFieldsDraft.ofTypeIdAndJson(
                        textLineItemId,
                        buildUtensilsCustomType("Knife, cake plate.").getFields())));
  }

  private void assertShoppingListUpdatedCorrectly(
      @Nonnull final ShoppingListDraft expectedShoppingListDraft) {

    final List<ShoppingList> shoppingLists =
        CTP_TARGET_CLIENT
            .execute(buildShoppingListQuery())
            .toCompletableFuture()
            .join()
            .getResults();

    // We are using updated draft for the sync without creating the types in project,
    // So manually adding the types into cache.
    prepareCache(shoppingLists);

    final List<ShoppingListDraft> shoppingListDrafts =
        ShoppingListTransformUtils.toShoppingListDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, shoppingLists)
            .join();

    assertThat(
            ShoppingListDraftBuilder.of(shoppingListDrafts.get(0))
                .lineItems(
                    setNullToAddedAtValuesForLineItems(shoppingListDrafts.get(0).getLineItems()))
                .textLineItems(
                    setNullToAddedAtValuesForTextLineItems(
                        shoppingListDrafts.get(0).getTextLineItems()))
                .build())
        .isEqualTo(
            ShoppingListDraftBuilder.of(expectedShoppingListDraft)
                .lineItems(
                    setNullToAddedAtValuesForLineItems(expectedShoppingListDraft.getLineItems()))
                .textLineItems(
                    setNullToAddedAtValuesForTextLineItems(
                        expectedShoppingListDraft.getTextLineItems()))
                .build());
  }

  private void prepareCache(List<ShoppingList> shoppingLists) {
    Set<String> customTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getCustom)
            .filter(Objects::nonNull)
            .map(customFields -> customFields.getType().getId())
            .collect(Collectors.toSet());
    customTypeIds.stream()
        .forEach(id -> referenceIdToKeyCache.add(id, "custom-type-shopping-list"));

    Set<String> lineItemsCustomTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getLineItems)
            .filter(Objects::nonNull)
            .map(
                lineItems ->
                    lineItems.stream()
                        .filter(Objects::nonNull)
                        .map(LineItem::getCustom)
                        .filter(Objects::nonNull)
                        .map(CustomFields::getType)
                        .map(Reference::getId)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    lineItemsCustomTypeIds.stream()
        .forEach(id -> referenceIdToKeyCache.add(id, "custom-type-line-items"));

    Set<String> textLineItemsCustomTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getTextLineItems)
            .filter(Objects::nonNull)
            .map(
                textLineItems ->
                    textLineItems.stream()
                        .filter(Objects::nonNull)
                        .map(TextLineItem::getCustom)
                        .filter(Objects::nonNull)
                        .map(CustomFields::getType)
                        .map(Reference::getId)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    textLineItemsCustomTypeIds.stream()
        .forEach(id -> referenceIdToKeyCache.add(id, "custom-type-text-line-items"));
  }

  @Nonnull
  private List<TextLineItemDraft> setNullToAddedAtValuesForTextLineItems(
      @Nonnull final List<TextLineItemDraft> textLineItemDrafts) {

    return textLineItemDrafts.stream()
        .map(
            textLineItemDraft ->
                TextLineItemDraftBuilder.of(textLineItemDraft).addedAt(null).build())
        .collect(toList());
  }

  @Nonnull
  private List<LineItemDraft> setNullToAddedAtValuesForLineItems(
      @Nonnull final List<LineItemDraft> lineItemDrafts) {

    return lineItemDrafts.stream()
        .map(lineItemDraft -> LineItemDraftBuilder.of(lineItemDraft).addedAt(null).build())
        .collect(toList());
  }
}
