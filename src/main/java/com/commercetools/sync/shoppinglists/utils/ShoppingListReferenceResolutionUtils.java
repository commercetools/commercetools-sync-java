package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.expansion.ShoppingListExpansionModel;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

/**
 * Util class which provides utilities that can be used when syncing shopping lists from a source commercetools project
 * to a target one.
 */
public final class ShoppingListReferenceResolutionUtils {

    /**
     * Returns a {@link List}&lt;{@link ShoppingListDraft}&gt; consisting of the results of applying the
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
     * @return a {@link List} of {@link ShoppingListDraft} built from the supplied {@link List} of {@link ShoppingList}.
     */
    @Nonnull
    public static List<ShoppingListDraft> mapToShoppingListDrafts(
        @Nonnull final List<ShoppingList> shoppingLists) {

        return shoppingLists
            .stream()
            .filter(Objects::nonNull)
            .map(ShoppingListReferenceResolutionUtils::mapToShoppingListDraft)
            .collect(toList());
    }

    /**
     * Returns a @link ShoppingListDraft} consisting of the result of applying the
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
     * @param shoppingList the shopping list with expanded references.
     * @return a {@link ShoppingListDraft} built from the supplied {@link ShoppingList}.
     */
    @Nonnull
    public static ShoppingListDraft mapToShoppingListDraft(@Nonnull final ShoppingList shoppingList) {

        return ShoppingListDraftBuilder
            .of(shoppingList.getName())
            .description(shoppingList.getDescription())
            .key(shoppingList.getKey())
            .customer(getResourceIdentifierWithKey(shoppingList.getCustomer()))
            .slug(shoppingList.getSlug())
            .lineItems(mapToLineItemDrafts(shoppingList.getLineItems()))
            .textLineItems(mapToTextLineItemDrafts(shoppingList.getTextLineItems()))
            .custom(mapToCustomFieldsDraft(shoppingList))
            .deleteDaysAfterLastModification(shoppingList.getDeleteDaysAfterLastModification())
            .anonymousId(shoppingList.getAnonymousId())
            .build();
    }

    @Nullable
    private static List<LineItemDraft> mapToLineItemDrafts(
        @Nullable final List<LineItem> lineItems) {

        if (lineItems == null) {
            return null;
        }

        return lineItems.stream()
                        .filter(Objects::nonNull)
                        .map(ShoppingListReferenceResolutionUtils::mapToLineItemDraft)
                        .filter(Objects::nonNull)
                        .collect(toList());
    }

    @Nullable
    private static LineItemDraft mapToLineItemDraft(@Nonnull final LineItem lineItem) {

        if (lineItem.getVariant() != null) {
            return LineItemDraftBuilder
                .ofSku(lineItem.getVariant().getSku(), lineItem.getQuantity())
                .addedAt(lineItem.getAddedAt())
                .custom(mapToCustomFieldsDraft(lineItem.getCustom()))
                .build();
        }

        return null;
    }

    @Nullable
    private static List<TextLineItemDraft> mapToTextLineItemDrafts(
        @Nullable final List<TextLineItem> textLineItems) {

        if (textLineItems == null) {
            return null;
        }

        return textLineItems.stream()
                            .filter(Objects::nonNull)
                            .map(ShoppingListReferenceResolutionUtils::mapToTextLineItemDraft)
                            .collect(toList());
    }

    @Nonnull
    private static TextLineItemDraft mapToTextLineItemDraft(@Nonnull final TextLineItem textLineItem) {

        return TextLineItemDraftBuilder.of(textLineItem.getName(), textLineItem.getQuantity())
                                       .description(textLineItem.getDescription())
                                       .addedAt(textLineItem.getAddedAt())
                                       .custom(mapToCustomFieldsDraft(textLineItem.getCustom()))
                                       .build();
    }

    /**
     * Builds a {@link ShoppingListQuery} for fetching shopping lists from a source CTP project with all the
     * needed references expanded for the sync:
     * <ul>
     *     <li>Customer</li>
     *     <li>Custom Type of the Shopping List</li>
     *     <li>Variants of the LineItems</li>
     *     <li>Custom Types of the LineItems</li>
     *     <li>Custom Types of the TextLineItems</li>
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
                                .withExpansionPaths(ShoppingListExpansionModel::customer)
                                .plusExpansionPaths(ExpansionPath.of("custom.type"))
                                .plusExpansionPaths(ExpansionPath.of("lineItems[*].variant"))
                                .plusExpansionPaths(ExpansionPath.of("lineItems[*].custom.type"))
                                .plusExpansionPaths(ExpansionPath.of("textLineItems[*].custom.type"));
    }

    private ShoppingListReferenceResolutionUtils() {
    }
}
