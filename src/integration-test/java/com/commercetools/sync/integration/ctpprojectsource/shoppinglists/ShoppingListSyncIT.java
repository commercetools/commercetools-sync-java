package com.commercetools.sync.integration.ctpprojectsource.shoppinglists;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.ensureSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponse;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomerActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.shoppinglists.utils.ShoppingListTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
  private List<ShoppingListUpdateAction> updateActionList;
  private ShoppingListSync shoppingListSync;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeEach
  void setup() {
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
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
            .shoppingLists()
            .get()
            .addExpand("lineItems[*].variant")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ShoppingListPagedQueryResponse::getResults)
            .join();

    final List<ShoppingListDraft> shoppingListDrafts =
        ShoppingListTransformUtils.toShoppingListDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, shoppingLists)
            .join();

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
            .shoppingLists()
            .get()
            .addExpand("lineItems[*].variant")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ShoppingListPagedQueryResponse::getResults)
            .join();

    ensureSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);
    final Customer sampleCustomerJaneDoe = ensureSampleCustomerJaneDoe(CTP_TARGET_CLIENT);

    final List<ShoppingListDraft> updatedShoppingListDrafts =
        ShoppingListTransformUtils.toShoppingListDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, shoppingLists)
            .join()
            .stream()
            .map(
                shoppingListDraft ->
                    ShoppingListDraftBuilder.of(shoppingListDraft)
                        .name(ofEnglish("second-shopping-list"))
                        .anonymousId(null)
                        .customer(
                            CustomerResourceIdentifierBuilder.of()
                                .key(sampleCustomerJaneDoe.getKey())
                                .build())
                        .build())
            .collect(Collectors.toList());

    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(updatedShoppingListDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    // order is important, otherwise the error below could occur:
    // "message" : "The resource was already claimed by a customer.."
    // "action" : {
    //  "action" : "setAnonymousId"
    // }
    assertThat(updateActionList)
        .containsExactly(
            ShoppingListChangeNameActionBuilder.of()
                .name(ofEnglish("second-shopping-list"))
                .build(),
            ShoppingListSetAnonymousIdActionBuilder.of().anonymousId(null).build(),
            ShoppingListSetCustomerActionBuilder.of()
                .customer(
                    CustomerResourceIdentifierBuilder.of()
                        .id(sampleCustomerJaneDoe.getId())
                        .build())
                .build());

    assertThat(shoppingListSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(shoppingListSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 shopping lists were processed in total "
                + "(1 created, 1 updated and 0 failed to sync).");
  }
}
