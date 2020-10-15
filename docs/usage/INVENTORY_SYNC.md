# InventoryEntry Sync

Module used for importing/syncing InventoryEntries into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [InventoryEntry](https://docs.commercetools.com/http-api-projects-inventory.html#inventoryentry) 
against a [InventoryEntryDraft](https://docs.commercetools.com/http-api-projects-inventory.html#inventoryentrydraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of inventory entry drafts](#sync-list-of-inventory-entry-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
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
   their `key`s has to be supplied.
   
   - When syncing from a source commercetools project, you can use [`mapToInventoryEntryDrafts`](https://commercetools.github.io/commercetools-sync-java/v/2.3.0/com/commercetools/sync/inventories/utils/InventoryReferenceResolutionUtils.html#mapToInventoryEntryDrafts-java.util.List-)
     method that that maps from a `InventoryEntry` to `InventoryEntryDraft` in order to make them ready for reference resolution by the sync:
     ````java
     final List<InventoryEntryDraft> inventoryEntryDrafts = InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts(inventoryEntries);
     ````
     
3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is setup, a `InventorySyncOptions` should be built as follows: 
````java
// instantiating a InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(sphereClient).build();
````


#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* inventory entry draft from the source
* inventory entry of the target project (only provided if an existing inventory entry could be found)
* the update-actions, which failed (only provided if an existing inventory entry could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(InventorySync.class);
 final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, inventoryEntry, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* inventory entry draft from the source 
* inventory entry of the target project (only provided if an existing inventory entry could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(InventorySync.class);
 final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, inventoryEntry, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target inventory entry and a inventory entry draft are matched, this callback can be used 
to intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * inventory entry draft from the source
 * inventory from the target project
 * update actions that were calculated after comparing both

##### Example
````java
final TriFunction<
        List<UpdateAction<InventoryEntry>>, 
        InventoryEntryDraft, 
        InventoryEntry, 
        List<UpdateAction<InventoryEntry>>> beforeUpdateInventoryCallback =
            (updateActions, newInventoryEntryDraft, oldInventoryEntry) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveQuantity))
                    .collect(Collectors.toList());
                        
final InventorySyncOptions inventorySyncOptions = 
        InventorySyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateInventoryCallback).build();
````

##### 4. `beforeCreateCallback`
During the sync process if a inventory entry draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * inventory entry draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).
                         
##### 5. `batchSize`
A number that could be used to set the batch size with which inventories are fetched and processed,
as inventories are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding inventories from 
the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 150 for inventory sync.

##### Example
````java                         
final InventorySyncOptions inventorySyncOptions = 
         InventorySyncOptionsBuilder.of(sphereClient).batchSize(100).build();
````

##### 6. `ensureChannels` 
A flag to indicate whether the sync process should create supply channel of the given key when it doesn't exist in a 
target project yet.
- If `ensureChannels` is set to `false` this inventory won't be synced and the `errorCallback` will be triggered.
- If `ensureChannels` is set to `true` the sync will attempt to create the missing supply channel with the given key. 
If it fails to create the supply channel, the inventory won't sync and `errorCallback` will be triggered.
- If not provided, it is set to `false` by default.

##### Example
````java                         
final InventorySyncOptions inventorySyncOptions = 
         InventorySyncOptionsBuilder.of(sphereClient).ensureChannels(true).build();
````

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
