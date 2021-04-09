package com.commercetools.sync.shoppinglists.service.impl;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import com.commercetools.sync.shoppinglists.service.ShoppingListTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.types.CustomFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class ShoppingListTransformServiceImpl extends BaseTransformServiceImpl
    implements ShoppingListTransformService {

  public ShoppingListTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
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
            .map(Reference::getId)
            .collect(toSet());

    setOfTypeIds.addAll(customTypeIds);
    setOfTypeIds.addAll(collectLineItemCustomTypeIds(shoppingLists));
    setOfTypeIds.addAll(collectTextLineItemCustomTypeIds(shoppingLists));

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
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
                    .map(Reference::getId)
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
                    .map(LineItem::getCustom)
                    .filter(Objects::nonNull)
                    .map(CustomFields::getType)
                    .map(Reference::getId)
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
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(customerIds, GraphQlQueryResources.CUSTOMERS);
  }
}
