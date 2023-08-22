package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.sync.sdk2.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class ShoppingListTransformUtils {

  /**
   * Transforms ShoppingLists by resolving the references and map them to ShoppingListDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the ShoppingList{@link ShoppingList} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the ShoppingList to ShoppingListDraft by performing reference resolution
   * considering idToKey value from the cache.
   *
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param shoppingLists the shoppingLists to resolve the references.
   * @return a new list which contains ShoppingListDrafts which have all their references resolved.
   */
  @Nonnull
  public static CompletableFuture<List<ShoppingListDraft>> toShoppingListDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<ShoppingList> shoppingLists) {

    final ShoppingListTransformServiceImpl shoppingListTransformService =
        new ShoppingListTransformServiceImpl(client, referenceIdToKeyCache);
    return shoppingListTransformService.toShoppingListDrafts(shoppingLists);
  }
}

class ShoppingListTransformServiceImpl extends BaseTransformServiceImpl {

  public ShoppingListTransformServiceImpl(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  public CompletableFuture<List<ShoppingListDraft>> toShoppingListDrafts(
      @Nonnull final List<ShoppingList> shoppingLists) {

    final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
    transformReferencesToRunParallel.add(this.transformCustomTypeReference(shoppingLists));
    transformReferencesToRunParallel.add(this.transformCustomerReference(shoppingLists));

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
        .thenApply(ignore -> mapToShoppingListDrafts(shoppingLists, referenceIdToKeyCache));
  }

  @Nonnull
  private CompletableFuture<Void> transformCustomTypeReference(
      @Nonnull final List<ShoppingList> shoppingLists) {

    final Set<String> setOfTypeIds = new HashSet<>();

    final Set<String> customTypeIds =
        shoppingLists.stream()
            .map(ShoppingList::getCustom)
            .filter(Objects::nonNull)
            .map(CustomFields::getType)
            .map(TypeReference::getId)
            .collect(toSet());

    setOfTypeIds.addAll(customTypeIds);
    setOfTypeIds.addAll(collectLineItemCustomTypeIds(shoppingLists));
    setOfTypeIds.addAll(collectTextLineItemCustomTypeIds(shoppingLists));

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
  }

  private Set<String> collectTextLineItemCustomTypeIds(List<ShoppingList> shoppingLists) {

    return shoppingLists.stream()
        .map(ShoppingList::getTextLineItems)
        .map(
            textLineItems ->
                textLineItems.stream()
                    .filter(Objects::nonNull)
                    .map(TextLineItem::getCustom)
                    .filter(Objects::nonNull)
                    .map(CustomFields::getType)
                    .map(TypeReference::getId)
                    .collect(toList()))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private Set<String> collectLineItemCustomTypeIds(List<ShoppingList> shoppingLists) {

    return shoppingLists.stream()
        .map(ShoppingList::getLineItems)
        .map(
            lineItems ->
                lineItems.stream()
                    .filter(Objects::nonNull)
                    .map(ShoppingListLineItem::getCustom)
                    .filter(Objects::nonNull)
                    .map(CustomFields::getType)
                    .map(TypeReference::getId)
                    .collect(toList()))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  @Nonnull
  private CompletableFuture<Void> transformCustomerReference(
      @Nonnull final List<ShoppingList> shoppingLists) {

    final Set<String> customerIds =
        shoppingLists.stream()
            .map(ShoppingList::getCustomer)
            .filter(Objects::nonNull)
            .map(CustomerReference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(customerIds, GraphQlQueryResource.CUSTOMERS);
  }
}
