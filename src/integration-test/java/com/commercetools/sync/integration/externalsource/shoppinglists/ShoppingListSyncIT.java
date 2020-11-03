package com.commercetools.sync.integration.externalsource.shoppinglists;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.commands.updateactions.AddBillingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.AddShippingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultBillingAddressWithKey;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.*;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.CUSTOMER_NUMBER_EXISTS_WARNING;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListSyncIT {

    private List<String> errorMessages;
    private List<String> warningMessages;
    private List<Throwable> exceptions;
    private List<UpdateAction<ShoppingList>> updateActionList;

    private ShoppingList defaultShoppingList;
    private ShoppingListSync shoppingListSync;

    @BeforeEach
    void setup() {
        deleteShoppingListTestData(CTP_TARGET_CLIENT);
        defaultShoppingList = createShoppingList(CTP_TARGET_CLIENT, "dummy-name-1" , "dummy-key-1");
        setUpCustomerSync();
    }

    private void setUpCustomerSync() {
        errorMessages = new ArrayList<>();;
        warningMessages = new ArrayList<>();;
        exceptions = new ArrayList<>();
        updateActionList = new ArrayList<>();

        ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
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
    void sync_WithSameShoppingList_ShouldNotUpdateCustomer() {
        List<ShoppingListDraft> newShoppingListDrafts =
                ShoppingListReferenceResolutionUtils
                    .mapToShoppingListDrafts(singletonList(defaultShoppingList));

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(newShoppingListDrafts)
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 0);
        assertThat(shoppingListSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 shopping lists were processed in total (0 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithNewShoppingList_ShouldCreateShoppingList() {

        final ShoppingListDraft newShoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("dummy-name-2"))
                    .key("dummy-key-2")
                    .description(LocalizedString.ofEnglish("new-shoppinglist-description"))
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

    }

    @Test
    void sync_WithModifiedShoppingList_ShouldUpdateShoppingList() {
        final ShoppingListDraft defaultShoppingListDraft =
                ShoppingListReferenceResolutionUtils
                        .mapToShoppingListDrafts(singletonList(defaultShoppingList)).get(0);

        final ShoppingListDraft newShoppingListDraft = ShoppingListDraftBuilder.of(defaultShoppingListDraft)
                        .description(LocalizedString.ofEnglish("new-shoppinglist-description"))
                        .build();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(newShoppingListDraft))
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionList).isEmpty();

        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    }

}
