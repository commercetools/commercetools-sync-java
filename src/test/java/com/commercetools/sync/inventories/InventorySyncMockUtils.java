package com.commercetools.sync.inventories;

import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncMockUtils {

    /**
     * Returns mock {@link Channel} instance. Returned instance represents channel of passed {@code id}, {@code key}
     * and of role {@link ChannelRole#INVENTORY_SUPPLY}.
     *
     * @param id result of calling {@link Channel#getId()}
     * @param key result of calling {@link Channel#getKey()}
     * @return mock instance of {@link Channel}
     */
    public static Channel getMockSupplyChannel(final String id, final String key) {
        final Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        when(channel.getRoles()).thenReturn(singleton(ChannelRole.INVENTORY_SUPPLY));
        when(channel.toReference()).thenReturn(Channel.referenceOfId(id));
        return channel;
    }

    /**
     * Returns mock {@link InventoryEntry} instance. Executing getters on returned instance will return values passed
     * in parameters.
     *
     * @param sku result of calling {@link InventoryEntry#getSku()}
     * @param quantityOnStock result of calling {@link InventoryEntry#getQuantityOnStock()}
     * @param restockableInDays result of calling {@link InventoryEntry#getRestockableInDays()}
     * @param expectedDelivery result of calling {@link InventoryEntry#getExpectedDelivery()}
     * @param supplyChannel result of calling {@link InventoryEntry#getSupplyChannel()}
     * @param customFields result of calling {@link InventoryEntry#getCustom()}
     * @return mock instance of {@link InventoryEntry}
     */
    public static InventoryEntry getMockInventoryEntry(final String sku,
                                                       final Long quantityOnStock,
                                                       final Integer restockableInDays,
                                                       final ZonedDateTime expectedDelivery,
                                                       final Reference<Channel> supplyChannel,
                                                       final CustomFields customFields) {
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);
        when(inventoryEntry.getSku()).thenReturn(sku);
        when(inventoryEntry.getQuantityOnStock()).thenReturn(quantityOnStock);
        when(inventoryEntry.getRestockableInDays()).thenReturn(restockableInDays);
        when(inventoryEntry.getExpectedDelivery()).thenReturn(expectedDelivery);
        when(inventoryEntry.getSupplyChannel()).thenReturn(supplyChannel);
        when(inventoryEntry.getCustom()).thenReturn(customFields);
        return inventoryEntry;
    }



    /**
     * Returns mock instance of {@link InventoryService}. Executing any method with any parameter on this instance
     * returns values passed in parameters, wrapped in {@link CompletionStage}.
     *
     * @param inventoryEntries result of calling {@link InventoryService#fetchInventoryEntriesBySkus(Set)}
     * @param createdInventoryEntry result of calling {@link InventoryService#createInventoryEntry(InventoryEntryDraft)}
     * @param updatedInventoryEntry result of calling
     *      {@link InventoryService#updateInventoryEntry(InventoryEntry, List)}
     * @return mock instance of {@link InventoryService}
     */
    static InventoryService getMockInventoryService(final List<InventoryEntry> inventoryEntries,
                                                    final InventoryEntry createdInventoryEntry,
                                                    final InventoryEntry updatedInventoryEntry) {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(completedFuture(inventoryEntries));
        when(inventoryService.createInventoryEntry(any())).thenReturn(completedFuture(createdInventoryEntry));
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(completedFuture(updatedInventoryEntry));
        return inventoryService;
    }

    /**
     * Returns mock instance of {@link InventoryService} with the specified parameters as mock results of calling the
     *   {@link ChannelService#createAndCacheChannel(String)} and
     *   {@link ChannelService#fetchCachedChannelId(String)} (String)} of the mock channel service.
     *
     * @param createdSupplyChannel result of future resulting from calling
     *      {@link ChannelService#createAndCacheChannel(String)}
     *
     * @return mock instance of {@link InventoryService}.
     */
    public static ChannelService getMockChannelService(@Nonnull final Channel createdSupplyChannel) {
        final String createdSupplyChannelId = createdSupplyChannel.getId();

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(completedFuture(Optional.of(createdSupplyChannelId)));
        when(channelService.createAndCacheChannel(any()))
            .thenReturn(completedFuture(createdSupplyChannel));
        return channelService;
    }

    /**
     * Returns {@link CompletionStage} completed exceptionally.
     *
     * @param <T> type of result that is supposed to be inside {@link CompletionStage}
     * @return {@link CompletionStage} instance that is completed exceptionally with {@link RuntimeException}
     */
    static <T> CompletionStage<T> getCompletionStageWithException() {
        final CompletableFuture<T> exceptionalStage = new CompletableFuture<>();
        exceptionalStage.completeExceptionally(new RuntimeException());
        return exceptionalStage;
    }
}
