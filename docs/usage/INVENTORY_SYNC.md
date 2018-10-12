# commercetools inventory sync

Utility which provides API for building CTP inventory update actions and inventory synchronisation.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
  - [Sync list of inventory entry drafts](#sync-list-of-inventory-entry-drafts)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Build all update actions

<!-- TODO: Probably #14 affects inventory sync as well. Ensure before providing the code snippet. -->

### Build particular update action(s)

To build the update action for changing inventory quantity:

````java
final Optional<UpdateAction<InventoryEntry>> updateAction = buildChangeQuantityAction(oldInventory, inventoryDraft);
````

For other examples of update actions, please check [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/utils/InventoryUpdateActionUtilsItTest.java).

### Sync list of inventory entry drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

In order to use the inventory sync an instance of
[InventorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptions.java)
has to be injected.

In order to instantiate a `InventorySyncOptions`, a `sphereClient` is required:

````java
// instantiating an InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(sphereClient).build();
````

then to start the sync:

````java
// instantiating an inventory sync
final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

// execute the sync on your list of inventories
inventorySync.sync(inventoryEntryDrafts);
````

**Note:** We encourage you to use `QueueSphereClientDecorator` as `sphereClient` implementation. It will reduce amount
of concurrent requests to CTP, thus will improve its performance. An example of use can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncItTest.java#L345).

**Preconditions:** The sync expects a list of `InventoryEntryDraft` objects that have their `sku` fields set,
otherwise the sync will trigger an `errorCallback` function set by the user (more on it can be found down below in the options explanations).

Every inventory entry may have a reference to a supply `Channel` and a reference to the `Type` of its custom fields. These
references are matched by their `key`. Therefore, in order for the sync to resolve the actual ids of those references,
their `key`s has to be supplied in the following way:
- Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
reference would return its `key`.

The sync results in a `CompletionStage` that contains an `InventorySyncStatistics` object. This object contains all
the stats of the sync process: a report message, the total number of updated, created, failed, processed inventory entries
and the processing time of the sync in different time units and in a human readable format. An example of how it looks like can be found
[here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncItTest.java#L366).

<!-- TODO: Update above after resolving #23 -->
<!-- TODO: Consider if getStatistics() is needed. Express your doubts in a #23 -->
````java
inventorySync.sync(inventoryEntryDrafts)
            .thenAccept(inventorySyncStatistics -> inventorySyncStatistics.getReportMessage());
//"Summary: 2000 inventory entries were processed in total (1000 created, 995 updated, 5 failed to sync)"
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:
 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not not known by the sync which batch is going to be the last one supplied.

Additional optional configuration for the sync can be configured on the `InventorySyncOptionsBuilder` instance, according to your need:

- `ensureChannels`
a flag which represents a strategy to handle syncing inventory entries with missing supply channels.
Having an inventory entry, with a missing supply channel reference, could be processed in either of the following ways:
    - If `ensureChannels` is set to `false` this inventory entry won't be synced and the `errorCallback` will be triggered.
    An example of use can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncItTest.java#L301).
    - If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key.
      If it fails to create the supply channel, the inventory entry won't sync and `errorCallback` will be triggered.
      An example of use can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncItTest.java#L284).
    - If not provided, it is set to `false` by default.

- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error.
An example of use can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncItTest.java#L391).

- `warningCallBack`
a callback that is called whenever an event occurs during the sync process that represents a warning.

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept inventory 
entry updates and modify (add/remove) update actions just before they are sent to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a inventoryEntry draft before a request to create it on CTP is issued. It allows the 
user to intercept inventoryEntry create requests modify the draft before the create request is sent to CTP API.

- `batchSize`
a number that could be used to set the batch size with which inventory entries are fetched and processed with,
as inventory entries are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to
`batchSize` inventory entries from the input list, then fetches the corresponding inventory entries from the target CTP project
in a single request. Playing with this option can slightly improve or reduce processing speed. (The default value is `150`).

<!-- TODO Update above options with links to tests. Tests should be written when inventory sync could actually use them (when custom update actions would use them).  -->

## Caveats

1. The tool matches inventories by their `sku` and `supplyChannel` key. Inventories are either created or updated. 
Currently the tool does not support inventory deletion.
2. The sync library is not meant to be executed in a parallel fashion. For example:
    ````java
    final InventorySync inventorySync = new InventorySync(syncOptions);
    final CompletableFuture<InventorySyncStatistics> syncFuture1 = inventorySync.sync(batch1).toCompletableFuture();
    final CompletableFuture<InventorySyncStatistics> syncFuture2 = inventorySync.sync(batch2).toCompletableFuture();
    CompletableFuture.allOf(syncFuture1, syncFuture2).join;
    ````
    The aforementioned example demonstrates how the library should **not** be used. The library, however, should be instead
    used in a sequential fashion:
    ````java
    final InventorySync inventorySync = new InventorySync(syncOptions);
    inventorySync.sync(batch1)
                .thenCompose(result -> inventorySync.sync(batch2))
                .toCompletableFuture()
                .join();
    ````
    By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
      - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration, which dictate how many requests the client can execute in parallel.
      - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-), which dictate how many drafts can one batch constitute.
     
    The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
    is the recommended good balance for stability and performance for the sync process.
3. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/3). 