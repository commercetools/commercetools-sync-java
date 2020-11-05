package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;

import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.commands.ShoppingListCreateCommand;
import io.sphere.sdk.shoppinglists.commands.ShoppingListDeleteCommand;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomers;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;

import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;


public final class ShoppingListITUtils {

    /**
     * Deletes all shopping lists, products and product types from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete test data from.
     */
    public static void deleteShoppingListTestData(@Nonnull final SphereClient ctpClient) {
        deleteShoppingLists(ctpClient);
        deleteCustomers(ctpClient);
        deleteAllProducts(ctpClient);
        deleteProductTypes(ctpClient);
    }

    /**
     * Deletes all ShoppingLists from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the ShoppingLists from.
     */
    public static void deleteShoppingLists(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, ShoppingListQuery.of(), ShoppingListDeleteCommand::of);
    }


    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingList(@Nonnull final SphereClient ctpClient, @Nonnull final String name,
                                                    @Nonnull final String key) {

        return createShoppingList(ctpClient, name, key, null, null, null, null);
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient     defines the CTP project to create the ShoppingList in.
     * @param name          the name of the ShoppingList to create.
     * @param key           the key of the ShoppingList to create.
     * @param desc          the description of the ShoppingList to create.
     * @param anonymousId   the anonymous ID of the ShoppingList to create.
     * @param slug          the slug of the ShoppingList to create.
     * @param deleteDaysAfterLastModification  the deleteDaysAfterLastModification of the ShoppingList to create.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingList(@Nonnull final SphereClient ctpClient, @Nonnull final String name,
                                                  @Nonnull final String key, @Nullable final String desc,
                                                  @Nullable final String anonymousId, @Nullable final String slug,
                                                  @Nullable final Integer deleteDaysAfterLastModification) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                .key(key)
                .description(desc == null ? null : LocalizedString.ofEnglish(desc))
                .anonymousId(anonymousId)
                .slug(slug == null ? null : LocalizedString.ofEnglish(slug))
                .deleteDaysAfterLastModification(deleteDaysAfterLastModification)
                .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @param customer  the Customer which ShoppingList refers to.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithCustomer(
            @Nonnull final SphereClient ctpClient,
            @Nonnull final String name,
            @Nonnull final String key,
            @Nonnull final Customer customer) {

        final ResourceIdentifier<Customer> customerResourceIdentifier = customer.toResourceIdentifier();
        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                .key(key)
                .customer(customerResourceIdentifier)
                .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @param textLineItems     the list of TextLineItemDraft which ShoppingList contains.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithTextLineItems(
            @Nonnull final SphereClient ctpClient,
            @Nonnull final String name,
            @Nonnull final String key,
            @Nonnull final List<TextLineItemDraft> textLineItems) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                .key(key)
                .textLineItems(textLineItems)
                .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @param lineItems the list of LineItemDraft which ShoppingList contains.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithLineItems(
            @Nonnull final SphereClient ctpClient,
            @Nonnull final String name,
            @Nonnull final String key,
            @Nonnull final List<LineItemDraft> lineItems) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                .key(key)
                .lineItems(lineItems)
                .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    private ShoppingListITUtils() {
    }
}
