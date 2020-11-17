package com.commercetools.sync.integration.externalsource.shoppinglists;

import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.commands.updateactions.AddLineItemWithSku;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.RemoveLineItem;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetLineItemCustomField;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.buildIngredientCustomType;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createSampleShoppingListCarrotCake;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListSyncIT {

    private List<String> errorMessages;
    private List<String> warningMessages;
    private List<Throwable> exceptions;
    private List<UpdateAction<ShoppingList>> updateActionList;

    private ShoppingList shoppingListSampleCarrotCake;
    private ShoppingListDraft shoppingListDraftSampleCarrotCake;
    private ShoppingListSync shoppingListSync;

    @BeforeEach
    void setup() {
        deleteShoppingListTestData(CTP_TARGET_CLIENT);
        setUpShoppingListSync();

        final ImmutablePair<ShoppingList, ShoppingListDraft> sampleShoppingListCarrotCake
            = createSampleShoppingListCarrotCake(CTP_TARGET_CLIENT);

        shoppingListSampleCarrotCake = sampleShoppingListCarrotCake.getLeft();
        shoppingListDraftSampleCarrotCake = sampleShoppingListCarrotCake.getRight();
    }

    private void setUpShoppingListSync() {
        errorMessages = new ArrayList<>();
        warningMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        updateActionList = new ArrayList<>();

        final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .warningCallback((exception, oldResource, newResource)
                -> warningMessages.add(exception.getMessage()))
            .beforeUpdateCallback((updateActions, customerDraft, customer) -> {
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
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(singletonList(shoppingListDraftSampleCarrotCake))
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 0);
        assertThat(shoppingListSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 shopping lists were processed in total "
                + "(0 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithNewShoppingList_ShouldCreateShoppingList() {
        final ShoppingListDraft newShoppingListDraft =
            ShoppingListDraftBuilder.of(shoppingListDraftSampleCarrotCake)
                                    .key("new-key")
                                    .slug(LocalizedString.ofEnglish("new-slug-carrot-cake"))
                                    .anonymousId(null)
                                    .customer(prepareCustomer().toResourceIdentifier())
                                    .build();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(singletonList(newShoppingListDraft))
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionList).isEmpty();

        assertThat(shoppingListSyncStatistics).hasValues(1, 1, 0, 0);
        assertThat(shoppingListSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 shopping lists were processed in total "
                + "(1 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithModifiedShoppingList_ShouldUpdateShoppingList() {
        final ShoppingListDraft modifiedShoppingListDraft = prepareUpdatedDraft();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(singletonList(modifiedShoppingListDraft))
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertUpdateActions();

        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Nonnull
    private ShoppingListDraft prepareUpdatedDraft() {

        final List<LineItemDraft> newLineItemDrafts = new ArrayList<>();
        final LineItemDraft updatedLineItemDraft = LineItemDraftBuilder
            .ofSku("SKU-3", 2L) // was 1
            .custom(buildIngredientCustomType("sugar", "150g")) // was 100g
            .addedAt(ZonedDateTime.now())
            .build();

        final LineItemDraft newLineItemDraft = LineItemDraftBuilder
            .ofSku("SKU-5", 1L)
            .custom(buildIngredientCustomType("vanilla extract", "1.5 tsp"))
            .addedAt(ZonedDateTime.parse("2020-11-05T10:00:00.000Z"))
            .build();

        for (int i = 0; i < Objects.requireNonNull(shoppingListDraftSampleCarrotCake.getLineItems()).size(); i++) {
            if (i == 2) {
                newLineItemDrafts.add(updatedLineItemDraft);
                continue;
            }

            if (i == 4) {
                newLineItemDrafts.add(newLineItemDraft);
            }

            newLineItemDrafts.add(shoppingListDraftSampleCarrotCake.getLineItems().get(i));
        }

        final Map<String, JsonNode> servingsFields = new HashMap<>();
        servingsFields.put("nutrition",
            JsonNodeFactory.instance.textNode("Per servings: 600 cal, 11g protein, 30g fat, 56g carb"));
        servingsFields.put("servings", JsonNodeFactory.instance.numberNode(16));

        return ShoppingListDraftBuilder
            .of(shoppingListDraftSampleCarrotCake)
            .name(LocalizedString.ofEnglish("Carrot Cake - (for xmas)"))
            .slug(LocalizedString.ofEnglish("carrot-cake-for-xmas"))
            .description(LocalizedString.ofEnglish("Carrot cake recipe - ingredients (for xmas)"))
            .anonymousId("public-carrot-cake-shopping-list-xmas")
            .deleteDaysAfterLastModification(15)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson("custom-type-shopping-list", servingsFields))
            .lineItems(newLineItemDrafts)
            .build();
    }

    private void assertUpdateActions() {
        // copy references
        final String lineItemId_Sku3Sugar = shoppingListSampleCarrotCake.getLineItems().get(2).getId();
        final String lineItemId_Sku5BakingPowder = shoppingListSampleCarrotCake.getLineItems().get(4).getId();
        final String lineItemId_Sku6Cinnamon = shoppingListSampleCarrotCake.getLineItems().get(5).getId();
        final String lineItemTypeId = shoppingListSampleCarrotCake.getLineItems().get(2).getCustom().getType().getId();

        assertThat(updateActionList).contains(
            SetSlug.of(LocalizedString.ofEnglish("carrot-cake-for-xmas")),
            ChangeName.of(LocalizedString.ofEnglish("Carrot Cake - (for xmas)")),
            SetDescription.of(LocalizedString.ofEnglish("Carrot cake recipe - ingredients (for xmas)")),
            SetAnonymousId.of("public-carrot-cake-shopping-list-xmas"),
            SetDeleteDaysAfterLastModification.of(15),
            SetCustomField.ofJson("nutrition",
                JsonNodeFactory.instance.textNode("Per servings: 600 cal, 11g protein, 30g fat, 56g carb")),
            SetCustomField.ofJson("servings", JsonNodeFactory.instance.numberNode(16)),

            ChangeLineItemQuantity.of(lineItemId_Sku3Sugar, 2L),
            SetLineItemCustomField.ofJson("amount",
                JsonNodeFactory.instance.textNode("150g"), lineItemId_Sku3Sugar),

            SetLineItemCustomField.ofJson("amount",
                JsonNodeFactory.instance.textNode("1.5 tsp"), lineItemId_Sku5BakingPowder),
            SetLineItemCustomField.ofJson("ingredient",
                JsonNodeFactory.instance.textNode("vanilla extract"), lineItemId_Sku5BakingPowder),

            RemoveLineItem.of(lineItemId_Sku6Cinnamon),
            AddLineItemWithSku.of(LineItemDraftBuilder
                .ofSku("SKU-5", 1L)
                .custom(CustomFieldsDraft.ofTypeIdAndJson(lineItemTypeId,
                    buildIngredientCustomType("baking powder", "1 tsp").getFields()))
                .build()),
            AddLineItemWithSku.of(LineItemDraftBuilder
                .ofSku("SKU-6", 1L)
                .custom(CustomFieldsDraft.ofTypeIdAndJson(lineItemTypeId,
                    buildIngredientCustomType("cinnamon", "2 tsp").getFields()))
                .build())
        );
    }

    private Customer prepareCustomer() {
        final CustomerDraft existingCustomerDraft =
            CustomerDraftBuilder.of("dummy-customer-email", "dummy-customer-password").build();

        return createCustomer(CTP_TARGET_CLIENT, existingCustomerDraft);
    }
}
