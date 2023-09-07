package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.customer.CustomerResourceIdentifier;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing shopping lists from a source
 * commercetools project to a target one.
 */
public final class ShoppingListReferenceResolutionUtils {

  /**
   * Returns a {@link java.util.List}&lt;{@link ShoppingListDraft}&gt; consisting of the results of
   * applying the mapping from {@link ShoppingList} to {@link ShoppingListDraft} with considering
   * reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
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
   *        <td>{@link com.commercetools.api.models.customer.CustomerReference}</td>
   *        <td>{@link com.commercetools.api.models.customer.CustomerResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>lineItems.custom.type</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeResourceIdentifier}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>textLineItems.custom.type</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeResourceIdentifier}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should contain ID in the map(cache) with a key
   * value. Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issue an update/create API request without
   * reference resolution.
   *
   * @param shoppingLists the shopping lists without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link ShoppingListDraft} built from the supplied {@link
   *     java.util.List} of {@link ShoppingList}.
   */
  @Nonnull
  public static List<ShoppingListDraft> mapToShoppingListDrafts(
      @Nonnull final List<ShoppingList> shoppingLists,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return shoppingLists.stream()
        .filter(Objects::nonNull)
        .map(shoppingList -> mapToShoppingListDraft(shoppingList, referenceIdToKeyCache))
        .collect(toList());
  }

  /**
   * Returns a @link ShoppingListDraft} consisting of the result of applying the mapping from {@link
   * ShoppingList} to {@link ShoppingListDraft} with considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
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
   *        <td>{@link com.commercetools.api.models.customer.CustomerReference}</td>
   *        <td>{@link com.commercetools.api.models.customer.CustomerResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>lineItems.custom.type</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeResourceIdentifier}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>textLineItems.custom.type</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.type.TypeResourceIdentifier}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should contain ID in the map(cache) with a key
   * value. Any reference, which have its id in place and not replaced by the key, it would not be
   * found in the map. In this case, this reference will be considered as existing resources on the
   * target commercetools project and the library will issue an update/create API request without
   * reference resolution.
   *
   * @param shoppingList the shopping list without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link ShoppingListDraft} built from the supplied {@link ShoppingList}.
   */
  @Nonnull
  public static ShoppingListDraft mapToShoppingListDraft(
      @Nonnull final ShoppingList shoppingList,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    final CustomerResourceIdentifier resourceIdentifierWithKey =
        getResourceIdentifierWithKey(
            shoppingList.getCustomer(),
            referenceIdToKeyCache,
            (id, key) -> CustomerResourceIdentifierBuilder.of().id(id).key(key).build());
    return ShoppingListDraftBuilder.of()
        .name(shoppingList.getName())
        .description(shoppingList.getDescription())
        .key(shoppingList.getKey())
        .customer(resourceIdentifierWithKey)
        .slug(shoppingList.getSlug())
        .lineItems(mapToLineItemDrafts(shoppingList.getLineItems(), referenceIdToKeyCache))
        .textLineItems(
            mapToTextLineItemDrafts(shoppingList.getTextLineItems(), referenceIdToKeyCache))
        .custom(mapToCustomFieldsDraft(shoppingList, referenceIdToKeyCache))
        .deleteDaysAfterLastModification(shoppingList.getDeleteDaysAfterLastModification())
        .anonymousId(shoppingList.getAnonymousId())
        .build();
  }

  private static List<ShoppingListLineItemDraft> mapToLineItemDrafts(
      @Nullable final List<ShoppingListLineItem> lineItems,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (lineItems == null) {
      return List.of();
    }

    return lineItems.stream()
        .filter(Objects::nonNull)
        .map(lineItem -> mapToLineItemDraft(lineItem, referenceIdToKeyCache))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nullable
  private static ShoppingListLineItemDraft mapToLineItemDraft(
      @Nonnull final ShoppingListLineItem lineItem,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    if (lineItem.getVariant() != null) {
      return ShoppingListLineItemDraftBuilder.of()
          .sku(lineItem.getVariant().getSku())
          .quantity(lineItem.getQuantity())
          .addedAt(lineItem.getAddedAt())
          .custom(mapToCustomFieldsDraft(lineItem.getCustom(), referenceIdToKeyCache))
          .build();
    }

    return null;
  }

  private static List<TextLineItemDraft> mapToTextLineItemDrafts(
      @Nullable final List<TextLineItem> textLineItems,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (textLineItems == null) {
      return List.of();
    }

    return textLineItems.stream()
        .filter(Objects::nonNull)
        .map(textLineItem -> mapToTextLineItemDraft(textLineItem, referenceIdToKeyCache))
        .collect(toList());
  }

  @Nonnull
  private static TextLineItemDraft mapToTextLineItemDraft(
      @Nonnull final TextLineItem textLineItem,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return TextLineItemDraftBuilder.of()
        .name(textLineItem.getName())
        .quantity(textLineItem.getQuantity())
        .description(textLineItem.getDescription())
        .addedAt(textLineItem.getAddedAt())
        .custom(mapToCustomFieldsDraft(textLineItem.getCustom(), referenceIdToKeyCache))
        .build();
  }

  private ShoppingListReferenceResolutionUtils() {}
}
