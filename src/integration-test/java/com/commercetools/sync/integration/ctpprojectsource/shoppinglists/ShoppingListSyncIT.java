package com.commercetools.sync.integration.ctpprojectsource.shoppinglists;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingListWithCustomer;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
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
}
