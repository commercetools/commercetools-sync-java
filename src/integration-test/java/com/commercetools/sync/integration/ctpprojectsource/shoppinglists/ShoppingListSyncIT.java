package com.commercetools.sync.integration.ctpprojectsource.shoppinglists;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;

import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;

import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingListWithCustomer;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingListWithLineItems;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingListWithTextLineItems;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListSyncIT {
    private List<String> errorMessages;
    private List<Throwable> exceptions;
    private ShoppingListSync shoppingListSync;

    @BeforeEach
    void setup() {
        deleteShoppingListTestData(CTP_SOURCE_CLIENT);
        deleteShoppingListTestData(CTP_TARGET_CLIENT);
        createShoppingList(CTP_SOURCE_CLIENT, "name-1", "key-1" );
        setUpShoppingListSync();
    }

    @AfterAll
    static void tearDown() {
        deleteShoppingListTestData(CTP_SOURCE_CLIENT);
        deleteShoppingListTestData(CTP_TARGET_CLIENT);
    }

    private void setUpShoppingListSync() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();
        shoppingListSync = new ShoppingListSync(shoppingListSyncOptions);
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {

        final List<ShoppingList> shoppingLists = CTP_SOURCE_CLIENT
            .execute(buildShoppingListQuery())
            .toCompletableFuture()
            .join()
            .getResults();

        final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(shoppingLists);

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(shoppingListDrafts)
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(shoppingListSyncStatistics).hasValues(1, 1, 0, 0);
        assertThat(shoppingListSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 shopping lists were processed in total "
                    +  "(1 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithUpdatedShoppingList_ShouldReturnProperStatistics() {

        createShoppingList(CTP_SOURCE_CLIENT, "name-2", "key-2", "desc-2",
                "anonymousId-2", "slug-2", 180);
        createShoppingList(CTP_TARGET_CLIENT, "name-2", "key-2");

        final List<ShoppingList> shoppingLists = CTP_SOURCE_CLIENT
                .execute(buildShoppingListQuery())
                .toCompletableFuture()
                .join()
                .getResults();

        final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(shoppingLists);

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(shoppingListDrafts)
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(shoppingListSyncStatistics
                .getReportMessage())
                .isEqualTo("Summary: 2 shopping lists were processed in total "
                        + "(1 created, 1 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithUpdatedCustomerReference_ShouldReturnProperStatistics() {

        final CustomerDraft customerDraft =
                CustomerDraftBuilder.of("dummy-email", "dummy-password")
                                    .key("dummy-customer-key")
                                    .build();

        final Customer sourceCustomer =
                createCustomer(CTP_SOURCE_CLIENT, customerDraft);

        createCustomer(CTP_TARGET_CLIENT, customerDraft);
        createShoppingListWithCustomer(CTP_SOURCE_CLIENT, "name-2", "key-2", sourceCustomer);
        createShoppingList(CTP_TARGET_CLIENT, "name-2", "key-2");

        final List<ShoppingList> shoppingLists = CTP_SOURCE_CLIENT
                .execute(buildShoppingListQuery())
                .toCompletableFuture()
                .join()
                .getResults();

        final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(shoppingLists);

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(shoppingListDrafts)
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(shoppingListSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 shopping lists were processed in total "
                     + "(1 created, 1 updated and 0 failed to sync).");
    }

    // TODO - Enable test case when Textlineitem is available in UpdateActionUtils.
    @Disabled
    @Test
    void sync_WithUpdatedTextLineItem_ShouldReturnProperStatistics() {

        // prepare shoppinglist with textlineitem in source/target project
        List<TextLineItemDraft> sourceTextLineItemDrafts =
                singletonList(
                    TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("dummy-name"), 20L).build());

        createShoppingListWithTextLineItems(CTP_SOURCE_CLIENT, "name-2", "key-2", sourceTextLineItemDrafts);

        List<TextLineItemDraft> targetTextLineItemDrafts =
                singletonList(
                        TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("dummy-name"), 10L).build());

        createShoppingListWithTextLineItems(CTP_TARGET_CLIENT, "name-2", "key-2", targetTextLineItemDrafts);

        // prepare list of shoppinglist for sync
        final List<ShoppingList> shoppingLists = CTP_SOURCE_CLIENT
                .execute(buildShoppingListQuery())
                .toCompletableFuture()
                .join()
                .getResults();

        final List<ShoppingListDraft> updatedShoppingListDraft = mapToShoppingListDrafts(shoppingLists);

        // test and assertion
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(updatedShoppingListDraft)
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(shoppingListSyncStatistics
                .getReportMessage())
                .isEqualTo("Summary: 2 shopping lists were processed in total "
                        + "(1 created, 1 updated and 0 failed to sync).");
    }

    // TODO - Enable test case when Lineitem is available in UpdateActionUtils.
    @Disabled
    @Test
    void sync_WithUpdatedLineItem_ShouldReturnProperStatistics() {

        // prepare shoppinglist with textlineitem in source/target project
        List<LineItemDraft> sourceLineItemDrafts =
                singletonList(
                        LineItemDraftBuilder.ofSku("dummy-sku", 20L).build());

        createShoppingListWithLineItems(CTP_SOURCE_CLIENT, "name-2", "key-2", sourceLineItemDrafts);

        List<LineItemDraft> lineItemDrafts =
                singletonList(
                        LineItemDraftBuilder.ofSku("dummy-sku", 10L).build());

        createShoppingListWithLineItems(CTP_TARGET_CLIENT, "name-2", "key-2", lineItemDrafts);

        // prepare list of shoppinglist for sync
        final List<ShoppingList> shoppingLists = CTP_SOURCE_CLIENT
                .execute(buildShoppingListQuery())
                .toCompletableFuture()
                .join()
                .getResults();

        final List<ShoppingListDraft> updatedShoppingListDraft = mapToShoppingListDrafts(shoppingLists);

        // test and assertion
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(updatedShoppingListDraft)
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
        assertThat(shoppingListSyncStatistics
                .getReportMessage())
                .isEqualTo("Summary: 2 shopping lists were processed in total "
                        + "(1 created, 1 updated and 0 failed to sync).");
    }

}
