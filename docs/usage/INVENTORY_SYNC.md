# InventoryEntry Sync

Module used for importing/syncing InventoryEntries into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [InventoryEntry](https://docs.commercetools.com/http-api-projects-inventory.html#inventoryentry) 
against a [InventoryEntryDraft](https://docs.commercetools.com/http-api-projects-inventory.html#inventoryentrydraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of inventory entry drafts](#sync-list-of-inventory-entry-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of inventory entry drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites
1. The sync expects a list of `InventoryEntryDraft`s that have their `sku` fields set,
   otherwise the sync will trigger an `errorCallback` function set by the user (more on it can be found down below in the options explanations).

2. Every inventory entry may have a reference to a supply `Channel` and a reference to the `Type` of its custom fields. These
   references are matched by their `key`s. Therefore, in order for the sync to resolve the actual ids of those references,
   their `key`s has to be supplied in the following way:
   - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
   reference would return its `key`. 
     
        **Note**: This library provides you with a utility method 
         [`replaceInventoriesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/1.1.0/com/commercetools/sync/inventories/utils/InventoryReferenceReplacementUtils.html#replaceInventoriesReferenceIdsWithKeys-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<InventoryEntryDraft> inventoryEntryDrafts = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);
         ````
     
3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is setup, a `InventorySyncOptions` should be be built as follows: 
````java
// instantiating a InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(sphereClient).build();
````
[More information about Sync Options](SYNC_OPTIONS.md).

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating an inventory sync
final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

// execute the sync on your list of inventories
CompletionStage<InventorySyncStatistics> syncStatisticsStage = inventorySync.sync(inventoryEntryDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `InventorySyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed inventories and the processing time of the sync in different time units and in a
human readable format.
````java
final InventorySyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*"Summary: 25 inventory entries were processed in total (9 created, 5 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.


More examples of how to use the sync [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/inventories/InventorySyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*


### Build all update actions

A utility method provided by the library to compare an InventoryEntry with a new InventoryEntryDraft and results in a list of InventoryEntry
 update actions. 
```java
List<UpdateAction<InventoryEntry>> updateActions = InventorySyncUtils.buildActions(oldEntry, newEntry, inventorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/inventories/utils/InventorySyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of an InventoryEntry and a new InventoryEntryDraft, and in turn builds
 the update action. One example is the `buildChangeQuantityAction` which compares quantities:
  
````java
Optional<UpdateAction<InventoryEntry>> updateAction = buildChangeQuantityAction(oldEntry, newEntry);
````

## Caveats    
1. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/87). 