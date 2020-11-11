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
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetAnonymousId;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetCustomer;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDeleteDaysAfterLastModification;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProduct;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListSyncIT {

    private List<String> errorMessages;
    private List<String> warningMessages;
    private List<Throwable> exceptions;
    private List<UpdateAction<ShoppingList>> updateActionList;

    private ShoppingList existingShoppingList;
    private ShoppingListSync shoppingListSync;

    @BeforeEach
    void setup() {
        deleteShoppingListTestData(CTP_TARGET_CLIENT);
        existingShoppingList = createShoppingList(CTP_TARGET_CLIENT, "dummy-name-1" , "dummy-key-1");
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
        List<ShoppingListDraft> newShoppingListDrafts = mapToShoppingListDrafts(singletonList(existingShoppingList));

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
            .isEqualTo("Summary: 1 shopping lists were processed in total "
                        + "(0 created, 0 updated and 0 failed to sync).");
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
    void sync_WithNewShoppingListPlusCustomerReference_ShouldCreateShoppingList() {

        final ShoppingListDraft newShoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("dummy-name-2"))
                        .key("dummy-key-2")
                        .customer(prepareCustomer().toResourceIdentifier())
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

    @Disabled
    @Test
    void sync_WithNewShoppingListPlusLineItems_ShouldCreateShoppingList() {

        final ShoppingListDraft newShoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("dummy-name-2"))
                        .key("dummy-key-2")
                        .lineItems(prepareLineItemDrafts())
                        .textLineItems(prepareTextLineItemDrafts())
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

    @Disabled
    @Test
    void sync_WithNewShoppingListPlusTextLineItems_ShouldCreateShoppingList() {

        final ShoppingListDraft newShoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("dummy-name-2"))
                        .key("dummy-key-2")
                        .textLineItems(prepareTextLineItemDrafts())
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
        final ShoppingListDraft existingShoppingListDraft =
                mapToShoppingListDrafts(singletonList(existingShoppingList)).get(0);

        final ShoppingListDraft updatedShoppingListDraft = ShoppingListDraftBuilder.of(existingShoppingListDraft)
                        .description(LocalizedString.ofEnglish("new-shoppinglist-description"))
                        .slug(LocalizedString.ofEnglish("new-shoppinglist-slug"))
                        .anonymousId("new-shoppinglist-anonymousId")
                        .deleteDaysAfterLastModification(180)
                        .name(LocalizedString.ofEnglish("new-shoppinglist-name"))
                        .build();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(updatedShoppingListDraft))
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionList).contains(
                SetDescription.of(LocalizedString.ofEnglish("new-shoppinglist-description")),
                SetSlug.of(LocalizedString.ofEnglish("new-shoppinglist-slug")),
                SetAnonymousId.of("new-shoppinglist-anonymousId"),
                SetDeleteDaysAfterLastModification.of(180),
                ChangeName.of(LocalizedString.ofEnglish("new-shoppinglist-name"))
        );
        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    }

    @Test
    void sync_WithModifieCustomerReferenceInShoppingList_ShouldUpdateShoppingList() {

        final ShoppingListDraft existingShoppingListDraft =
                mapToShoppingListDrafts(singletonList(existingShoppingList)).get(0);

        final Customer customer = prepareCustomer();
        final ResourceIdentifier<Customer> customerResourceIdentifier = customer.toResourceIdentifier();
        final ShoppingListDraft updatedShoppingListDraft = ShoppingListDraftBuilder.of(existingShoppingListDraft)
                .customer(customerResourceIdentifier)
                .build();

        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(updatedShoppingListDraft))
                .toCompletableFuture()
                .join();

        assertThat(errorMessages).isEmpty();
        assertThat(warningMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionList).containsExactly(
                SetCustomer.of(customer)
        );

        assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    }

    private Customer prepareCustomer() {
        final CustomerDraft existingCustomerDraft =
                CustomerDraftBuilder.of("dummy-customer-email", "dummy-customer-password").build();

        return createCustomer(CTP_TARGET_CLIENT, existingCustomerDraft);

    }

    private List<LineItemDraft> prepareLineItemDrafts() {
        final ProductType productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
                .of()
                .sku("dummy-lineitem-name")
                .key("dummy-product-master-variant")
                .build();

        final ProductDraft productDraft =
                ProductDraftBuilder.of(
                        productType,
                        ofEnglish("dummy-product-name"),
                        ofEnglish("dummy-product-slug")  , masterVariant).build();

        createProduct(CTP_TARGET_CLIENT, productDraft );

        final LineItemDraft newLineItemDraft =
                LineItemDraftBuilder.ofSku("dummy-lineitem-name", 10L).build();

        return singletonList(newLineItemDraft);

    }

    private List<TextLineItemDraft> prepareTextLineItemDrafts() {
        final TextLineItemDraft newTextLineItemDraft =
                TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("dummy-textlineitem-name"), 10L).build();
        return singletonList(newTextLineItemDraft);
    }
}
