package com.commercetools.sync.inventories.helpers;

import static java.util.Collections.emptyList;

import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryModel;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public final class InventoryEntryQueryBuilder {

  /*
   * The query predicate will be limited to max 10.000 characters(also considered some
   * conservative space for headers). Because above this size it could return - Error 414 (Request-URI Too Large).
   * With that one inventory entry could fetch maximum 238 (10000/42 - assumed the sku will be 1 character)
   * inventory entry.
   */
  private static final int MAX_QUERY_LENGTH = 10000;

  /**
   * Builds {@link List} of {@link InventoryEntryQuery}'s to be used to query existing {@link
   * InventoryEntry}'s against set of sku and supply channels.
   *
   * <p>For instance, a query for one inventory entry will be like (sku="sku_9" and
   * supplyChannel(id="c28c5609-9766-4176-b8d3-a72edee753e8")) or when supply channel not defined
   * the query will be like (sku="sku_9" and supplyChannel is not defined)
   *
   * @param identifiers {@link Set} of unique inventory identifiers, used in search predicate
   * @return {@link List} of inventory entry queries or empty list when there was no identifiers.
   */
  public static List<InventoryEntryQuery> buildQueries(
      @Nonnull final Set<InventoryEntryIdentifier> identifiers) {

    if (identifiers.isEmpty()) {
      return emptyList();
    }

    final List<InventoryEntryQuery> inventoryEntryQueries = new ArrayList<>();
    StringBuilder queryBuilder = new StringBuilder();
    int limit = 0;
    for (InventoryEntryIdentifier identifier : identifiers) {

      String predicate = buildQueryPredicate(identifier).toSphereQuery();

      if (queryBuilder.length() + predicate.length() > MAX_QUERY_LENGTH) {
        inventoryEntryQueries.add(getInventoryEntryQuery(queryBuilder, limit));

        // clear/reset the queryBuilder and limit
        queryBuilder.setLength(0);
        limit = 0;
      }

      queryBuilder.append("(");
      queryBuilder.append(predicate);
      queryBuilder.append(") or ");
      limit++;
    }

    // add last one:
    inventoryEntryQueries.add(getInventoryEntryQuery(queryBuilder, limit));
    return inventoryEntryQueries;
  }

  private static InventoryEntryQuery getInventoryEntryQuery(
      @Nonnull final StringBuilder queryBuilder, final int limit) {
    // drop " or " keyword in the end of the predicate.
    final String queryString = queryBuilder.substring(0, queryBuilder.length() - 4);

    return io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder.of()
        .plusPredicates(QueryPredicate.of(queryString))
        .build()
        .withLimit(limit)
        .withFetchTotal(false);
  }

  private static QueryPredicate<InventoryEntry> buildQueryPredicate(
      @Nonnull final InventoryEntryIdentifier identifier) {

    final String sku = identifier.getSku();
    final String supplyChannelId = identifier.getSupplyChannelId();
    final InventoryEntryQueryModel queryModel = InventoryEntryQueryModel.of();
    return supplyChannelId == null
        ? queryModel.sku().is(sku).and(queryModel.supplyChannel().isNotPresent())
        : queryModel.sku().is(sku).and(queryModel.supplyChannel().id().is(supplyChannelId));
  }

  private InventoryEntryQueryBuilder() {}
}
