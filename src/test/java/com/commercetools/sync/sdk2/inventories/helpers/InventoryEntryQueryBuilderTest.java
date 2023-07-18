package com.commercetools.sync.sdk2.inventories.helpers;

import static com.commercetools.sync.sdk2.inventories.helpers.InventoryEntryQueryBuilder.buildQueries;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ByProjectKeyInventoryGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryEntryQueryBuilderTest {

  private static ProjectApiRoot ctpClient;

  @BeforeAll
  static void setup() {
    ctpClient = ApiRootBuilder.of().withApiBaseUrl("testBaseUri").build("testProjectKey");
  }

  @Test
  void buildQueries_WithoutIdentifiers_ShouldReturnEmptyList() {
    final List<ByProjectKeyInventoryGet> inventoryEntryQueries =
        buildQueries(ctpClient, Collections.emptySet());
    assertThat(inventoryEntryQueries).isEmpty();
  }

  @Test
  void buildQueries_WithOneIdentifierWithoutSupplyChannel_ShouldReturnOneQuery() {
    final InventoryEntryIdentifier identifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of().sku("sku").quantityOnStock(0L).build());

    final List<ByProjectKeyInventoryGet> inventoryEntryQueries =
        buildQueries(ctpClient, Collections.singleton(identifier));
    final String queryString = "(sku=\"sku\" and supplyChannel is not defined)";
    assertThat(inventoryEntryQueries)
        .contains(
            ctpClient.inventory().get().withWhere(queryString).withLimit(1).withWithTotal(false));
  }

  @Test
  void buildQueries_WithOneIdentifierWithSupplyChannel_ShouldReturnOneQuery() {
    final InventoryEntryIdentifier identifier =
        InventoryEntryIdentifier.of(
            InventoryEntryDraftBuilder.of()
                .sku("sku")
                .quantityOnStock(0L)
                .supplyChannel(ChannelResourceIdentifierBuilder.of().id("channel-id").build())
                .build());

    final List<ByProjectKeyInventoryGet> inventoryEntryQueries =
        buildQueries(ctpClient, Collections.singleton(identifier));

    final String queryString = "(sku=\"sku\" and supplyChannel(id=\"channel-id\"))";
    assertThat(inventoryEntryQueries)
        .contains(
            ctpClient.inventory().get().withWhere(queryString).withLimit(1).withWithTotal(false));
  }

  @Test
  void buildQueries_With500Identifiers_ShouldReturnOneQuery() {
    // prepare
    final String channelId = UUID.randomUUID().toString();
    final Set<InventoryEntryIdentifier> identifiers = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      final InventoryEntryIdentifier identifier =
          InventoryEntryIdentifier.of(
              InventoryEntryDraftBuilder.of()
                  .sku("sku_" + i)
                  .quantityOnStock((long) i)
                  .supplyChannel(
                      ThreadLocalRandom.current().nextBoolean()
                          ? null
                          : ChannelResourceIdentifierBuilder.of().id(channelId).build())
                  .build());
      identifiers.add(identifier);
    }

    final List<ByProjectKeyInventoryGet> inventoryEntryQueries =
        buildQueries(ctpClient, identifiers);
    assertThat(inventoryEntryQueries).isNotEmpty();

    final int totalIdentifiers =
        inventoryEntryQueries.stream()
            .mapToInt(value -> Integer.parseInt(value.getLimit().stream().findFirst().orElse("0")))
            .sum();
    assertThat(totalIdentifiers).isEqualTo(500);
  }
}
