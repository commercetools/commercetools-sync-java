package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.expansion.ShoppingListExpansionModel;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static java.util.stream.Collectors.toList;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class ShoppingListReferenceResolutionUtils {

    /**
     * Returns an {@link List}&lt;{@link ShoppingListDraft}&gt; consisting of the results of applying the
     * mapping from {@link ShoppingList} to {@link ShoppingListDraft} with considering reference resolution.
     *
     * <table summary="Mapping of Reference fields for the reference resolution">
     *   <thead>
     *     <tr>
     *       <th>Reference field</th>
     *       <th>from</th>
     *       <th>to</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *        <td>customer</td>
     *        <td>{@link Reference}&lt;{@link Customer}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link Customer}&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>custom.type</td>
     *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>lineItems.custom.type</td>
     *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
     *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>textLineItems.custom.type</td>
     *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Type}&gt;&gt;</td>
     *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Type}&gt;&gt;</td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * <p><b>Note:</b> The aforementioned references should be expanded with a key.
     * Any reference that is not expanded will have its id in place and not replaced by the key will be
     * considered as existing resources on the target commercetools project and
     * the library will issues an update/create API request without reference resolution.
     *
     * @param shoppingLists the shopping lists with expanded references.
     * @return a {@link List} of {@link ShoppingListDraft} built from the
     *         supplied {@link List} of {@link ShoppingList}.
     */
    @Nonnull
    public static List<ShoppingListDraft> mapToShoppingListDrafts(
        @Nonnull final List<ShoppingList> shoppingLists) {

        final Set<Throwable> errors = new HashSet<>();

        final List<ShoppingListDraft> referenceReplacedDrafts = shoppingLists
            .stream()
            .filter(Objects::nonNull)
            .map(shoppingList -> buildShoppingListDraft(shoppingList))
            .collect(toList());

        if (!errors.isEmpty()) {
            throw new ReferenceReplacementException("Some errors occurred during reference replacement.", errors);
        }

        return referenceReplacedDrafts;
    }


    @Nonnull
    private static List<LineItemDraft> mapToLineItemDrafts(@Nonnull final List<LineItem> lineItems) {
        return lineItems.stream()
                .filter(Objects::nonNull)
                .map(lineItem -> mapToLineItemDraft(lineItem))
                .collect(Collectors.toList());
    }

    @Nonnull
    private static LineItemDraft mapToLineItemDraft(@Nonnull final LineItem lineItem) {
        final ProductVariant productVariant = lineItem.getVariant();
        LineItemDraftBuilder builder;
        if (productVariant!=null) {
            builder = LineItemDraftBuilder
                    .ofSku(productVariant.getSku(), lineItem.getQuantity())
                    .variantId(productVariant.getId());
        } else {
            builder = LineItemDraftBuilder
                    .of(lineItem.getProductId())
                    .quantity(lineItem.getQuantity());
        }

        return builder
                .custom(mapToCustomFieldsDraft(lineItem.getCustom()))
                .build();
    }

    @Nonnull
    private static List<TextLineItemDraft> mapToTextLineItemDrafts(@Nonnull final List<TextLineItem> textLineItems) {
        return textLineItems.stream()
                .filter(Objects::nonNull)
                .map(textLineItem -> mapToTextLineItemDraft(textLineItem))
                .collect(Collectors.toList());
    }

    @Nonnull
    private static TextLineItemDraft mapToTextLineItemDraft(@Nonnull final TextLineItem textLineItem) {
        return  TextLineItemDraftBuilder.of(textLineItem.getName(), textLineItem.getQuantity())
                .description(textLineItem.getDescription())
                .custom(mapToCustomFieldsDraft(textLineItem.getCustom())).build();
    }

    @Nonnull
    private static ShoppingListDraft buildShoppingListDraft(@Nonnull final ShoppingList shoppingList) {
        shoppingList.getCustomer();

        return ShoppingListDraftBuilder.of(shoppingList.getName())
                .customer(replaceCustomerIdWithKey(shoppingList.getCustomer()))
                .custom(CustomFieldsDraft.ofCustomFields(shoppingList.getCustom()))
                .plusLineItems(mapToLineItemDrafts(shoppingList.getLineItems()))
                .plusTextLineItems(mapToTextLineItemDrafts(shoppingList.getTextLineItems()))
                .build();
    }

    @Nonnull
    private static Reference<Customer> replaceCustomerIdWithKey(@Nonnull final Reference<Customer> customerReference) {

        return getReferenceWithKeyReplaced(customerReference,
                    () -> Customer.referenceOfId(customerReference.getKey()));

    }

    /**
     * Builds a {@link ShoppingListQuery} for fetching shopping lists from a source CTP project with all the
     * needed references expanded for the sync:
     * <ul>
     *     <li>Customer </li>
     *     <li>Custom Type</li>
     *     <li>Lists of LineItems</li>
     *     <li>Lists of TextLineItems</li>
     * </ul>
     *
     * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
     * a source commercetools project. Otherwise, it is more efficient to build the query without expansions, if they
     * are not needed, to avoid unnecessarily bigger payloads fetched from the source project.
     *
     * @return the query for fetching shopping lists from the source CTP project with all the aforementioned references
     *         expanded.
     */
    public static ShoppingListQuery buildShoppingListQuery() {
        return ShoppingListQuery.of()
                .withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE)
                .withExpansionPaths(ExpansionPath.of("custom.type"))
                .plusExpansionPaths(ExpansionPath.of("lineItems[*].custom.type"))
                .plusExpansionPaths(ExpansionPath.of("textLineItems[*].custom.type"))
                .plusExpansionPaths(ShoppingListExpansionModel::customer);
    }

    private ShoppingListReferenceResolutionUtils() {
    }
}