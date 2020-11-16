package com.commercetools.sync.integration.externalsource.shoppinglists;

import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
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
        errorMessages = new ArrayList<>();;
        warningMessages = new ArrayList<>();;
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
        // WHEN SKU-5 IS ADDED IT'S INCREMENTING THE SKU-5 INSTEAD OF ADDING NEW.


        List<LineItemDraft> lineItemDrafts = new ArrayList<>();
        lineItemDrafts.add(LineItemDraftBuilder.ofSku("SKU-5", 1L).build());
        lineItemDrafts.addAll(shoppingListDraftSampleCarrotCake.getLineItems());

        final ShoppingListDraft newShoppingListDraft =
            ShoppingListDraftBuilder.of(shoppingListDraftSampleCarrotCake)
                                    .lineItems(lineItemDrafts)
                                    .build();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
            .sync(singletonList(newShoppingListDraft))
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
    }

    private Customer prepareCustomer() {
        final CustomerDraft existingCustomerDraft =
                CustomerDraftBuilder.of("dummy-customer-email", "dummy-customer-password").build();

        return createCustomer(CTP_TARGET_CLIENT, existingCustomerDraft);
    }
}
