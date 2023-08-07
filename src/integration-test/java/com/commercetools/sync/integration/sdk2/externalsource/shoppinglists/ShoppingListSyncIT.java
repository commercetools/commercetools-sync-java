package com.commercetools.sync.integration.sdk2.externalsource.shoppinglists;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ShoppingListITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListAddLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListAddTextLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListChangeTextLineItemQuantityActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponse;
import com.commercetools.api.models.shopping_list.ShoppingListRemoveLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemDescriptionActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSync;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.sdk2.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.sdk2.shoppinglists.utils.ShoppingListTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
  private List<ShoppingListUpdateAction> updateActionList;

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
    final List<ShoppingListLineItemDraft> newLineItemDrafts = new ArrayList<>();
    final ShoppingListLineItemDraft updatedLineItemDraft =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-3")
            .quantity(2L) // was 1
            .custom(buildIngredientCustomType("sugar", "150g")) // was 100g
            .addedAt(ZonedDateTime.now())
            .build();

    final ShoppingListLineItemDraft newLineItemDraft =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-5")
            .quantity(1L)
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

    final TextLineItemDraft updatedTextLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 1 - updated"))
            .quantity(2L)
            .description(
                ofEnglish(
                    "Peel carrots and set aside, crack the nuts, separate eggs into small balls."))
            .custom(buildUtensilsCustomType("Peeler, nuts cracker, 2 small bowls"))
            .build();

    final TextLineItemDraft newTextLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("before step 5"))
            .quantity(1L)
            .description(ofEnglish("Pre-heat oven to 180 C degree."))
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

    final Map<String, Object> servingsFields = new HashMap<>();
    servingsFields.put("nutrition", "Per servings: 600 cal, 11g protein, 30g fat, 56g carb");
    servingsFields.put("servings", 14L);

    return ShoppingListDraftBuilder.of(shoppingListDraftSampleCarrotCake)
        .name(ofEnglish("Carrot Cake - (for xmas)"))
        .slug(ofEnglish("carrot-cake-for-xmas"))
        .description(ofEnglish("Carrot cake recipe - ingredients (for xmas)"))
        .anonymousId("public-carrot-cake-shopping-list-xmas")
        .deleteDaysAfterLastModification(15L)
        .custom(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.key("custom-type-shopping-list"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(servingsFields))
                .build())
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

    updateActionList.contains(
        ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
            .name("amount")
            .value("150g")
            .textLineItemId(lineItemId_Sku3Sugar)
            .build());

    assertThat(updateActionList)
        .contains(
            ShoppingListSetSlugActionBuilder.of().slug(ofEnglish("carrot-cake-for-xmas")).build(),
            ShoppingListChangeNameActionBuilder.of()
                .name(ofEnglish("Carrot Cake - (for xmas)"))
                .build(),
            ShoppingListSetDescriptionActionBuilder.of()
                .description(ofEnglish("Carrot cake recipe - ingredients (for xmas)"))
                .build(),
            ShoppingListSetAnonymousIdActionBuilder.of()
                .anonymousId("public-carrot-cake-shopping-list-xmas")
                .build(),
            ShoppingListSetDeleteDaysAfterLastModificationActionBuilder.of()
                .deleteDaysAfterLastModification(15L)
                .build(),
            ShoppingListSetCustomFieldActionBuilder.of()
                .name("nutrition")
                .value("Per servings: 600 cal, 11g protein, 30g fat, 56g carb")
                .build(),
            ShoppingListSetCustomFieldActionBuilder.of().name("servings").value(14L).build(),
            ShoppingListChangeLineItemQuantityActionBuilder.of()
                .lineItemId(lineItemId_Sku3Sugar)
                .quantity(2L)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("amount")
                .value("150g")
                .lineItemId(lineItemId_Sku3Sugar)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("amount")
                .value("100g")
                .lineItemId(lineItemId_Sku5BakingPowder)
                .build(),
            ShoppingListSetLineItemCustomFieldActionBuilder.of()
                .name("ingredient")
                .value("nuts")
                .lineItemId(lineItemId_Sku5BakingPowder)
                .build(),
            ShoppingListRemoveLineItemActionBuilder.of()
                .lineItemId(lineItemId_Sku6Cinnamon)
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .sku("SKU-5")
                .quantity(1L)
                .custom(
                    CustomFieldsDraftBuilder.of()
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.id(lineItemTypeId))
                        .fields(buildIngredientCustomType("baking powder", "1 tsp").getFields())
                        .build())
                .build(),
            ShoppingListAddLineItemActionBuilder.of()
                .sku("SKU-6")
                .quantity(1L)
                .custom(
                    CustomFieldsDraftBuilder.of()
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.id(lineItemTypeId))
                        .fields(buildIngredientCustomType("cinnamon", "2 tsp").getFields())
                        .build())
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId(textLineItemId_Step1)
                .name(ofEnglish("step 1 - updated"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId(textLineItemId_Step1)
                .description(
                    ofEnglish(
                        "Peel carrots and set aside, crack the nuts, "
                            + "separate eggs into small balls."))
                .build(),
            ShoppingListChangeTextLineItemQuantityActionBuilder.of()
                .textLineItemId(textLineItemId_Step1)
                .quantity(2L)
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("utensils")
                .value("Peeler, nuts cracker, 2 small bowls")
                .textLineItemId(textLineItemId_Step1)
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId(textLineItemId_Step5)
                .name(ofEnglish("before step 5"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId(textLineItemId_Step5)
                .description(ofEnglish("Pre-heat oven to 180 C degree."))
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("utensils")
                .value("Oven")
                .textLineItemId(textLineItemId_Step5)
                .build(),
            ShoppingListChangeTextLineItemNameActionBuilder.of()
                .textLineItemId(textLineItemId_Step6)
                .name(ofEnglish("step 5"))
                .build(),
            ShoppingListSetTextLineItemDescriptionActionBuilder.of()
                .textLineItemId(textLineItemId_Step6)
                .description(
                    ofEnglish("Put cake mixture into cake pan, bake appr 40 min with 180 C degree"))
                .build(),
            ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
                .name("utensils")
                .value("Cake pan, oven")
                .textLineItemId(textLineItemId_Step6)
                .build(),
            ShoppingListAddTextLineItemActionBuilder.of()
                .name(ofEnglish("step 6"))
                .description(ofEnglish("Decorate as you wish and serve, enjoy!"))
                .quantity(1L)
                .addedAt(ZonedDateTime.parse("2020-11-06T10:00:00.000Z"))
                .custom(
                    CustomFieldsDraftBuilder.of()
                        .type(
                            typeResourceIdentifierBuilder ->
                                typeResourceIdentifierBuilder.id(textLineItemId))
                        .fields(buildUtensilsCustomType("Knife, cake plate.").getFields())
                        .build())
                .build());
  }

  private void assertShoppingListUpdatedCorrectly(
      @Nonnull final ShoppingListDraft expectedShoppingListDraft) {

    final List<ShoppingList> shoppingLists =
        CTP_TARGET_CLIENT
            .shoppingLists()
            .get()
            .addExpand("lineItems[*].variant")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ShoppingListPagedQueryResponse::getResults)
            .join();

    // We are using updated draft for the sync without creating the types in project,
    // So manually adding the types into cache.
    prepareCache(shoppingLists);

    final List<ShoppingListDraft> shoppingListDrafts =
        ShoppingListTransformUtils.toShoppingListDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, shoppingLists)
            .join();

    ShoppingListDraftBuilder.of(shoppingListDrafts.get(0))
        .lineItems(setNullToAddedAtValuesForLineItems(shoppingListDrafts.get(0).getLineItems()))
        .textLineItems(
            setNullToAddedAtValuesForTextLineItems(shoppingListDrafts.get(0).getTextLineItems()))
        .build();

    ShoppingListDraftBuilder.of(expectedShoppingListDraft)
        .lineItems(setNullToAddedAtValuesForLineItems(expectedShoppingListDraft.getLineItems()))
        .textLineItems(
            setNullToAddedAtValuesForTextLineItems(expectedShoppingListDraft.getTextLineItems()))
        .build();

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
    final Set<String> customTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getCustom)
            .filter(Objects::nonNull)
            .map(customFields -> customFields.getType().getId())
            .collect(Collectors.toSet());
    customTypeIds.forEach(id -> referenceIdToKeyCache.add(id, "custom-type-shopping-list"));

    final Set<String> lineItemsCustomTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getLineItems)
            .filter(Objects::nonNull)
            .map(
                lineItems ->
                    lineItems.stream()
                        .filter(Objects::nonNull)
                        .map(ShoppingListLineItem::getCustom)
                        .filter(Objects::nonNull)
                        .map(CustomFields::getType)
                        .map(TypeReference::getId)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    lineItemsCustomTypeIds.stream()
        .forEach(id -> referenceIdToKeyCache.add(id, "custom-type-line-items"));

    final Set<String> textLineItemsCustomTypeIds =
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
                        .map(TypeReference::getId)
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
  private List<ShoppingListLineItemDraft> setNullToAddedAtValuesForLineItems(
      @Nonnull final List<ShoppingListLineItemDraft> lineItemDrafts) {

    return lineItemDrafts.stream()
        .map(
            lineItemDraft ->
                ShoppingListLineItemDraftBuilder.of(lineItemDraft).addedAt(null).build())
        .collect(toList());
  }
}
