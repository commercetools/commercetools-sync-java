# InventoryEntry Sync

A utility which provides an API for building CTP inventory update actions and inventory synchronisation.

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
1. The sync expects a list of `InventoryEntryDraft` objects that have their `sku` fields set,
   otherwise the sync will trigger an `errorCallback` function set by the user (more on it can be found down below in the options explanations).

2. Every inventory entry may have a reference to a supply `Channel` and a reference to the `Type` of its custom fields. These
   references are matched by their `key`. Therefore, in order for the sync to resolve the actual ids of those references,
   their `key`s has to be supplied in the following way:
   - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
   reference would return its `key`. 
     
        **Note**: This library provides you with a utility method 
         [`replaceInventoriesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/inventories/utils/InventoryReferenceReplacementUtils.html#replaceInventoriesReferenceIdsWithKeys-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<InventoryEntryDraft> inventoryEntryDrafts = replaceInventoriesReferenceIdsWithKeys(inventoryEntries);
         ````
     
3. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the amount of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with 
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html) 
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the 
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)
   
   If you have no special requirements on sphere client creation then you can use `ClientConfigurationUtils#createClient`
   util which applies best practices already.

4. After the `sphereClient` is setup, a `InventorySyncOptions` should be be built as follows: 
````java
// instantiating a InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(sphereClient).build();
````
[More information about Sync Options](/docs/usage/SYNC_OPTIONS.md).

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


More examples of how to use the sync [here](/src/integration-test/java/com/commercetools/sync/integration/inventories/InventorySyncIT.java).

##### Usage Tip
The sync library is not meant to be executed in a parallel fashion. Check the example in [point #2 here](/docs/usage/PRODUCT_SYNC.md#caveats).
By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
  - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
  - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch contain.
 
The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
is the recommended good balance for stability and performance for the sync process.

In order to exploit the number of `max parallel requests`, the `batch size` should have a value set which is equal or higher.


### Build all update actions

A utility method provided by the library to compare an InventoryEntry with a new InventoryEntryDraft and results in a list of InventoryEntry
 update actions. 
```java
List<UpdateAction<InventoryEntry>> updateActions = InventorySyncUtils.buildActions(oldEntry, newEntry, inventorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](/src/test/java/com/commercetools/sync/inventories/utils/InventorySyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of an InventoryEntry and a new InventoryEntryDraft, and in turn builds
 the update action. One example is the `buildChangeQuantityAction` which compares quantities:
  
````java
Optional<UpdateAction<InventoryEntry>> updateAction = buildChangeQuantityAction(oldEntry, newEntry);
````

## Caveats    
1. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/87). 