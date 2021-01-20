package com.commercetools.sync.integration.ctpprojectsource.shoppinglists;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createSampleShoppingListCarrotCake;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListSyncIT {
  private List<String> errorMessages;
  private List<String> warningMessages;
  private List<Throwable> exceptions;
  private List<UpdateAction<ShoppingList>> updateActionList;
  private ShoppingListSync shoppingListSync;

  @BeforeEach
  void setup() {
    deleteShoppingListSyncTestDataFromProjects();

    createSampleShoppingListCarrotCake(CTP_SOURCE_CLIENT);
    createShoppingList(CTP_SOURCE_CLIENT, "second-shopping-list", "second-shopping-list-key");

    createSampleShoppingListCarrotCake(CTP_TARGET_CLIENT);

    setUpShoppingListSync();
  }

  @AfterAll
  static void tearDown() {
    deleteShoppingListSyncTestDataFromProjects();
  }

  private static void deleteShoppingListSyncTestDataFromProjects() {
    deleteShoppingListTestData(CTP_SOURCE_CLIENT);
    deleteShoppingListTestData(CTP_TARGET_CLIENT);
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

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {

    final List<ShoppingList> shoppingLists =
        CTP_SOURCE_CLIENT
            .execute(buildShoppingListQuery())
            .toCompletableFuture()
            .join()
            .getResults();

    final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(shoppingLists);

    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(shoppingListDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionList).isEmpty();

    assertThat(shoppingListSyncStatistics).hasValues(2, 1, 0, 0);
    assertThat(shoppingListSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 shopping lists were processed in total "
                + "(1 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdatedCustomerOnShoppingList_ShouldReturnProperStatistics() {
    final List<ShoppingList> shoppingLists =
        CTP_SOURCE_CLIENT
            .execute(buildShoppingListQuery())
            .toCompletableFuture()
            .join()
            .getResults();

    createSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);
    final Customer sampleCustomerJaneDoe = createSampleCustomerJaneDoe(CTP_TARGET_CLIENT);

    final List<ShoppingListDraft> updatedShoppingListDrafts =
        mapToShoppingListDrafts(shoppingLists).stream()
            .map(
                shoppingListDraft ->
                    ShoppingListDraftBuilder.of(shoppingListDraft)
                        .name(LocalizedString.ofEnglish("second-shopping-list"))
                        .anonymousId(null)
                        .customer(ResourceIdentifier.ofKey(sampleCustomerJaneDoe.getKey()))
                        .build())
            .collect(Collectors.toList());

    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(updatedShoppingListDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    // order is important, otherwise the error below could occur:
    // "message" : "The resource was already claimed by a customer..
    // "action" : {
    //  "action" : "setAnonymousId"
    // }
    assertThat(updateActionList)
        .containsExactly(
            ChangeName.of(LocalizedString.ofEnglish("second-shopping-list")),
            SetAnonymousId.of(null),
            SetCustomer.of(
                Reference.of(Customer.referenceTypeId(), sampleCustomerJaneDoe.getId())));

    AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(shoppingListSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 shopping lists were processed in total "
                + "(1 created, 1 updated and 0 failed to sync).");
  }
}
