package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.inventories.utils.InventorySyncUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.inventories.utils.InventoryDraftTransformerUtils.transformToDrafts;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Default implementation of inventories sync process.
 */
public final class InventorySync extends BaseSync<InventoryEntryDraft, InventoryEntry, InventorySyncStatistics,
        InventorySyncOptions> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySync.class);

    //Cache that maps supply channel key to supply channel Id for supply channels existing in CT platform.
    private Map<String, String> supplyChannelKeyToId;

    @Nonnull
    private final InventoryService inventoryService;

    @Nonnull
    private final TypeService typeService;

    public InventorySync(@Nonnull final InventorySyncOptions syncOptions) {
        this(syncOptions, new InventoryServiceImpl(syncOptions.getCtpClient().getClient()),
                new TypeServiceImpl(syncOptions.getCtpClient().getClient()));
    }

    InventorySync(final InventorySyncOptions syncOptions, final InventoryService inventoryService,
                  final TypeService typeService) {
        super(new InventorySyncStatistics(), syncOptions, LOGGER);
        this.inventoryService = inventoryService;
        this.typeService = typeService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     *     <strong>Note:</strong> resource drafts are compared with existing resources by {@code sku} and
     *     {@code supplyChannel} key. Every {@link InventoryEntryDraft} that contains {@code supplyChannel} should
     *     either:
     *     <ul>
     *         <li>have {@code supplyChannel} expanded, that means
     *         {@code inventoryEntryDraft.getSupplyChannel().getObj()} should return {@link Channel} object,
     *         that contains channel key</li>
     *         <li>or have {@code supplyChannel} not expanded and {@code supplyChannel} key should be provided in
     *         place of reference id, that means {@code inventoryEntryDraft.getSupplyChannel().getObj()} should
     *         return {@code null} and {@code inventoryEntryDraft.getSupplyChannel().getId()} should
     *         return {@code supplyChannel} key</li>
     *     </ul>
     *     This is important for proper resources comparision.
     * </p>
     * @param resourceDrafts the list of new resources as drafts.
     */
    @Override
    public void syncDrafts(@Nonnull final List<InventoryEntryDraft> resourceDrafts) {
        super.syncDrafts(resourceDrafts);
    }

    /**
     * <p>Performs full process of synchronisation between inventory entries present in a system
     * and passed {@code inventories}. This is accomplished by:
     * <ul>
     *     <li>Comparing entries and drafts by {@code sku} and {@code supplyChannel} key</li>
     *     <li>Calculating of necessary updates and creation commands</li>
     *     <li>Actually <strong>performing</strong> changes in a target CTP project</li>
     * </ul>
     * The process is customized according to {@link InventorySyncOptions} passed to constructor of this object.
     * After the process finishes you can obtain its summary by calling {@link InventorySync#getStatistics()}.</p>
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} containing data that would be synced into
     *             CTP project.
     */
    @Override
    protected void processDrafts(@Nonnull List<InventoryEntryDraft> inventories) {
        buildChannelMap(inventories);
        List<InventoryEntryDraft> accumulator = new ArrayList<>(syncOptions.getBatchSize());
        for (InventoryEntryDraft entry : inventories) {
            if (entry != null) {
                if (isNotEmpty(entry.getSku())) {
                    accumulator.add(entry);
                    if (accumulator.size() == syncOptions.getBatchSize()) {
                        processBatch(accumulator);
                        accumulator.clear();
                    }
                } else {
                    statistics.incrementProcessed();
                    statistics.incrementFailed();
                    failSync("Failed to process inventory entry without sku", null);
                }
            } else {
                statistics.incrementProcessed();
                statistics.incrementFailed();
                failSync("Failed to process null object", null);
            }
        }
        if (!accumulator.isEmpty()) {
            processBatch(accumulator);
        }
    }

    /**
     * Converts {@code inventories} to {@link InventoryEntryDraft} objects and perform full synchronisation process
     * as described in {@link InventorySync#syncDrafts(List)}.
     *
     * @param inventories  {@link List} of {@link InventoryEntry} that you would like to sync into your CTP project.
     * @see InventorySync#syncDrafts(List)
     */
    @Override
    protected void process(@Nonnull List<InventoryEntry> inventories) {
        final List<InventoryEntryDraft> drafts = transformToDrafts(inventories);
        processDrafts(drafts);
    }

    @Override
    @Nonnull
    public InventorySyncStatistics getStatistics() {
        return statistics;
    }

    /**
     * Instantiate and fill {@code supplyChannelKeyToId} with values fetched from supply channels in CTP.
     * If {@code ensureChannels} flag from {@code syncOptions} is set to true, method checks if {@code drafts} reference
     * any supply channels that are missing in CTP. If such channels exist, method attempt to create missing supply
     * channels and update {@code supplyChannelKeyToId} with newly created values.
     *
     * @param drafts {@link List} of {@link InventoryEntryDraft} containing data that would be synced into CTP project.
     */
    private void buildChannelMap(@Nonnull final List<InventoryEntryDraft> drafts) {
        supplyChannelKeyToId = inventoryService.fetchAllSupplyChannels()
                .stream()
                .collect(toMap(Channel::getKey, Channel::getId));
        if (syncOptions.isEnsureChannels()) {
            final List<String> missingChannelsKeys = drafts.stream()
                    .map(SkuKeyTuple::of)
                    .map(SkuKeyTuple::getKey)
                    .distinct()
                    .filter(key -> key != null)
                    .filter(key -> !supplyChannelKeyToId.containsKey(key))
                    .collect(toList());
            missingChannelsKeys.stream()
                    .map(this::createMissingSupplyChannel)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(channel -> supplyChannelKeyToId.put(channel.getKey(), channel.getId()));
        }
    }


    /**
     * Method process batch of {@link InventoryEntryDraft}. The processing means:
     * <ol>
     *     <li>Fetching existing {@link InventoryEntry} that correspond to passed {@code drafts}</li>
     *     <li>Deciding if each draft should be used for creating new entry or updating existing one</li>
     *     <li>Attempting to create new entries or update existing ones</li>
     *     <li>Updates the {@code statistics} object backed for this instance</li>
     * </ol>
     * @param drafts batch of drafts that need to be synced
     */
    private void processBatch(final List<InventoryEntryDraft> drafts) {
        final Map<SkuKeyTuple, InventoryEntry> existingInventories = fetchExistingInventories(drafts);
        drafts.forEach(draft -> {
            final SkuKeyTuple skuKeyOfDraft = SkuKeyTuple.of(draft);
            if (existingInventories.containsKey(skuKeyOfDraft)) {
                final InventoryEntry existingEntry = existingInventories.get(skuKeyOfDraft);
                attemptUpdate(existingEntry, draft);
            } else {
                attemptCreate(draft);
            }
            statistics.incrementProcessed();
        });
    }

    /**
     * Returns mapping of {@link SkuKeyTuple} to {@link InventoryEntry} of instances existing in a CT platform.
     * Instances are fetched from API by skus, that corresponds to skus present in {@code drafts}.
     *
     * @param drafts {@link List} of drafts
     * @return {@link SkuKeyTuple} to {@link InventoryEntry} of existing entries that correspond to passed
     * {@code drafts} by sku comparision.
     */
    private Map<SkuKeyTuple, InventoryEntry> fetchExistingInventories(final List<InventoryEntryDraft> drafts) {
        final Set<String> skus = extractSkus(drafts);
        final List<InventoryEntry> existingEntries = inventoryService.fetchInventoryEntriesBySkus(skus);
        return existingEntries.stream()
                .collect(toMap(SkuKeyTuple::of, entry -> entry));
    }

    /**
     *
     * @param inventories {@link List} of {@link InventoryEntryDraft} where each draft contains its sku
     * @return {@link Set} of distinct skus found in {@code inventories}
     */
    private Set<String> extractSkus(final List<InventoryEntryDraft> inventories) {
        return inventories.stream()
                .map(InventoryEntryDraft::getSku)
                .collect(Collectors.toSet());
    }

    /**
     * Tries to update {@code entry} in CT platform with data from {@code draft}.
     * It calculates list of {@link UpdateAction} and calls API only when there is a need.
     * Before calculate differences, the channel reference from {@code draft} is replaced, so it points to
     * proper channel ID in target system.
     * It either updates inventory entry and increments "updated" counter in statistics, or increments "failed" counter
     * and log error in case of any exception.
     *
     * @param entry entry from existing system that could be updated.
     * @param draft draft containing data that could differ from data in {@code entry}.
     *              <strong>Sku isn't compared</strong>
     * @return {@link CompletionStage} that contains either updated {@link InventoryEntry} when succeeded or
     * {@code null} otherwise
     */
    private CompletionStage<InventoryEntry> attemptUpdate(final InventoryEntry entry, final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            final List<UpdateAction<InventoryEntry>> updateActions =
                    InventorySyncUtils.buildActions(entry, fixedDraft.get(), syncOptions, typeService);
            if (!updateActions.isEmpty()) {
                return inventoryService.updateInventoryEntry(entry, updateActions)
                        .handle((updatedEntry, exception) -> {
                            if (exception != null) {
                                statistics.incrementFailed();
                                failSync(format("Failed to update inventory entry of sku '%s' and supply channel key '%s'",
                                        draft.getSku(), SkuKeyTuple.of(draft).getKey()), exception);
                            } else {
                                statistics.incrementUpdated();
                            }
                            return updatedEntry;
                        });
            }
        } else {
            statistics.incrementFailed();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Tries to create Inventory Entry in CT platform, using {@code draft}.
     * Before calling API, the channel reference from {@code draft} is replaced, so it points to proper channel ID
     * in target system.
     * It either creates inventory entry and increments "created" counter in statistics, or increments "failed" counter
     * and log error in case of any exception.
     *
     * @param draft draft of new inventory entry.
     * @return {@link CompletionStage} that contains either created {@link InventoryEntry} when succeeded or
     * {@code null} otherwise
     */
    private CompletionStage<InventoryEntry> attemptCreate(final InventoryEntryDraft draft) {
        final Optional<InventoryEntryDraft> fixedDraft = replaceChannelReference(draft);
        if (fixedDraft.isPresent()) {
            return inventoryService.createInventoryEntry(fixedDraft.get())
                    .handle((createdInventory, exception) -> {
                        if (exception != null) {
                            statistics.incrementFailed();
                            failSync(format("Failed to create inventory entry of sku '%s' and supply channel key '%s'",
                                    draft.getSku(), SkuKeyTuple.of(draft).getKey()), exception);
                        } else {
                            statistics.incrementCreated();
                        }
                        return createdInventory;
                    });
        } else {
            statistics.incrementFailed();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns {@link Optional} that may contain {@link InventoryEntryDraft}. The payload of optional would be:
     * <ul>
     *     <li>Same {@code draft} instance if it has no reference to supply channel</li>
     *     <li>New {@link InventoryEntryDraft} instance if {@code draft} contains reference to supply channel.
     *     New instance would have same values as {@code draft} except for supply channel reference. Reference
     *     will be replaced with reference that points to ID of existing channel for key given in draft.</li>
     *     <li>Empty if supply channel for key wasn't found in {@code supplyChannelKeyToId}</li>
     * </ul>
     *
     * @param draft inventory entry draft from processed list
     * @return {@link Optional} with draft that is prepared to being created or compared with existing InventoryEntry,
     * or empty optional if errors occured.
     */
    private Optional<InventoryEntryDraft> replaceChannelReference(final InventoryEntryDraft draft) {
        final String supplyChannelKey = SkuKeyTuple.of(draft).getKey();
        if (supplyChannelKey != null) {
            if (supplyChannelKeyToId.containsKey(supplyChannelKey)) {
                return Optional.of(
                        ofEntryDraftPlusRefToSupplyChannel(draft, supplyChannelKeyToId.get(supplyChannelKey)));
            } else {
                failSync(format("Failed to find supply channel of key '%s'", supplyChannelKey), new Exception());
                return Optional.empty();
            }
        }
        return Optional.of(draft);
    }

    /**
     * Returns new {@link InventoryEntryDraft} containing same data as {@code draft} except for
     * supply channel reference that is replaced by reference pointing to {@code supplyChannelId}
     * @param draft draft where reference should be replaced
     * @param supplyChannelId ID of supply channel existing in target project
     * @return {@link InventoryEntryDraft} with supply channel reference pointing to {@code supplyChannelId}
     * and other data same as in {@code draft}
     */
    private InventoryEntryDraft ofEntryDraftPlusRefToSupplyChannel(@Nonnull final InventoryEntryDraft draft,
                                                                   @Nonnull final String supplyChannelId) {
        final Reference<Channel> supplyChannelRef = Channel.referenceOfId(supplyChannelId);
        return InventoryEntryDraftBuilder.of(draft)
                .supplyChannel(supplyChannelRef)
                .build();
    }

    /**
     * Method tries to create supply channel of given {@code key}.
     *
     * @param key key of supply channel that seems to not exists in a system.
     * @return {@link Optional} containing newly created supply {@link Channel} of given {@code key},
     * or empty {@link Optional} when operation failed.
     */
    private Optional<Channel> createMissingSupplyChannel(@Nonnull final String key) {
        try {
            final Channel newChannel = inventoryService.createSupplyChannel(key);
            return Optional.ofNullable(newChannel);
        } catch (Exception ex) {
            failSync(format("Failed to create new supply channel of key '%s'", key), ex);
        }
        return Optional.empty();
    }

    /**
     * Given a reason message as {@link String} and {@link Throwable} exception, this method calls the optional error
     * callback specified in the {@code syncOptions}. <br/>
     * <strong>Note</strong> to the end of {@code message} following phrase will be joined:
     * <pre> " in CTP project with key '_KEY_'"</pre>
     * where _KEY_ is replaced by CTP project key, taken from CTP client configuration.
     *
     * @param message the reason of failure
     * @param exception the exception that occurred, if any
     */
    private void failSync(@Nonnull final String message, @Nullable final Throwable exception) {
        final String finalMessage = message + format(" in CTP project with key '%s.'",
                this.syncOptions.getCtpClient().getClientConfig().getProjectKey());
        syncOptions.applyErrorCallback(finalMessage, exception);
    }
}
