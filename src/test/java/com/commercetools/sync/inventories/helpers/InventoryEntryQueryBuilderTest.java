package com.commercetools.sync.inventories.helpers;

import static com.commercetools.sync.inventories.helpers.InventoryEntryQueryBuilder.buildQueries;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class InventoryEntryQueryBuilderTest {

  @Test
  void buildQueries_WithoutIdentifiers_ShouldReturnEmptyList() {
    final List<InventoryEntryQuery> inventoryEntryQueries = buildQueries(Collections.emptySet());
    assertThat(inventoryEntryQueries).isEmpty();
  }

  @Test
  void buildQueries_WithOneIdentifierWithoutSupplyChannel_ShouldReturnOneQuery() {
    final InventoryEntryIdentifier identifier =
        InventoryEntryIdentifier.of(InventoryEntryDraft.of("sku", 0L));

    final List<InventoryEntryQuery> inventoryEntryQueries =
        buildQueries(Collections.singleton(identifier));

    assertThat(inventoryEntryQueries)
        .contains(
            InventoryEntryQuery.of()
                .plusPredicates(QueryPredicate.of("(sku=\"sku\" and supplyChannel is not defined)"))
                .withLimit(1)
                .withFetchTotal(false));
  }

  @Test
  void buildQueries_WithOneIdentifierWithSupplyChannel_ShouldReturnOneQuery() {
    final InventoryEntryIdentifier identifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraft.of("sku", 0L)
                .withSupplyChannel(ResourceIdentifier.ofId("channel-id")));

    final List<InventoryEntryQuery> inventoryEntryQueries =
        buildQueries(Collections.singleton(identifier));

    assertThat(inventoryEntryQueries)
        .contains(
            InventoryEntryQuery.of()
                .plusPredicates(
                    QueryPredicate.of("(sku=\"sku\" and supplyChannel(id=\"channel-id\"))"))
                .withLimit(1)
                .withFetchTotal(false));
  }

  @Test
  void buildQueries_With500Identifiers_ShouldReturnOneQuery() {
    // prepare
    final String channelId = UUID.randomUUID().toString();
    final Set<InventoryEntryIdentifier> identifiers = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      final InventoryEntryIdentifier identifier =
          InventoryEntryIdentifier.of(
              InventoryEntryDraft.of("sku_" + i, i)
                  .withSupplyChannel(
                      ThreadLocalRandom.current().nextBoolean()
                          ? null
                          : ResourceIdentifier.ofId(channelId)));
      identifiers.add(identifier);
    }

    final List<InventoryEntryQuery> inventoryEntryQueries = buildQueries(identifiers);
    assertThat(inventoryEntryQueries).isNotEmpty();

    final int totalIdentifiers =
        inventoryEntryQueries.stream()
            .mapToInt(value -> Objects.requireNonNull(value.limit()).intValue())
            .sum();
    assertThat(totalIdentifiers).isEqualTo(500);
  }
}
