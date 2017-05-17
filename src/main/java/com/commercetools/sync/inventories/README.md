# commercetools inventory sync

Java module that can be used to synchronise your new commercetools inventory entries to your existing
commercetools project.

- [What it offers?](#what-it-offers)
- [How to use it?](#how-to-use-it)
- [How does it work?](#how-does-it-work)
- [FAQ](#faq)


## What it offers?

1. Synchronise inventory entries coming from an external system, in any form (CSV, XML, etc..), that has been already mapped to
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
[InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
objects to the desired commercetools project.
2. Synchronise inventory entries coming from an already-existing commercetools project in the form of
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
[InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
objects to another commercetools project.

3. Build any of the following commercetools [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) update action
objects given an old inventory entry, represented by a [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java),
and a new inventory entry, represented by a [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java):
    - ChangeQuantity
    - SetRestockableInDays
    - SetExpectedDelivery
    - SetSupplyChannel
    - inventory custom fields update actions:
        - SetCustomType
        - SetCustomField

## How to use it?
In order to use the inventory sync an instance of
 [InventorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptions.java) have to be injected.
 
 In order to instantiate a `InventorySyncOptions`, a `ctpClient` is required:
  #### `ctpClient` [Required]
  Defines the configuration of the commercetools project that inventory entries are going to be synced to.
  ````java
  // instantiating a ctpClient
  final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");
  final CtpClient ctpClient = new CtpClient(clientConfig);
  
  // instantiating a InventorySyncOptions
  final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(ctpClient).build();
  ````
  
  The inventory sync can then do any of the following:
  ##### sync
  Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
  [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
  objects.
  ````java
  // instantiating a inventory sync
  final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
  
  // execute the sync on your list of inventory entries
  inventorySync.sync(inventory entries);
  ````
  
  ##### syncDrafts
  Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
  [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
  objects.
  ````java
  // instantiating a inventory sync
  final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
  
  // execute the sync on your list of inventory entries
  inventorySync.syncDrafts(inventoryDrafts);
  ````
  ##### getStatistics
  Used to get an object  containing all the stats of the sync process; which includes a report message, the total number
  of updated, created, failed, processed inventory entries and the processing time of the sync in different time units and in a
  human readable format.
  ````java
  inventorySync.syncDrafts(inventoryDrafts);
  inventorySync.getStatistics().getCreated(); // 1000
  inventorySync.getStatistics().getFailed(); // 5
  inventorySync.getStatistics().getUpdated(); // 995
  inventorySync.getStatistics().getProcessed(); // 2000
  inventorySync.getStatistics().getReportMessage();
  /*
   * Summary of inventory synchronisation:
   * 2000 inventory entries were processed in total (1000 created, 995 updated and 5 failed to sync)."
   */
   ````
    
  <!--- TODO Also add code snippets for building update actions utils! -->
  
  Additional configuration for the sync can be configured on the `InventorySyncOptions` instance, according to the need
  of the user of the sync:
  #### `ensureChannels` [Optional]
  Defines an optional field which represents a strategy for handling with inventory entries of missing supply channels.
  By missing supply channels you could consider supply channels of keys that are referenced in provided inventory
  entries list but do not exists in a target CTP project. Having a inventory entry with missing supply channel
  referenced it could be processed in either ways:
   - If `ensureChannels` is set to `false` such inventory entry will fail to sync.
   - If `ensureChannels` is set to `true` there will be attempt to create supply channel of given key. If such attempt
   succeed then inventory entry would be created either, otherwise it fails to sync.

  If not provided, it is set to `false` by default.
  
  An example of setting a callback would be for example logging the error as follows:
  ````java
  // instantiating a InventorySyncOptions
  final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
                                                      .of(ctpClient)
                                                      .ensureChannels(true)
                                                      .build();
  ````
  
  #### `batchSize` [Optional]
  Defines an optional field which represents a size of batch of processed inventory entries. The purpose of this option
  is to limit requests send to CTP. During sync process there is a need to fetch existing inventory entries so they can
  be compared with newly provided inventory entries. It is achieved by accumulating up to `batchSize` inventory entries
  from input list, then fetching corresponding inventory entries from target CTP project in a one call, and then
  performing sync actions on them. Playing with this option can slightly improve or reduce processing speed.

  If not provided, it is set to `30` by default.
      
  ````java
   // instantiate a logger
   final Logger LOGGER = LoggerFactory.getLogger(myclass.class);
    
   // instantiating a InventorySyncOptions
   final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .setBatchSize(10)
                                                        .build();
   ````

   #### Important note about input data of `sync` and `syncDraft` methods
   In CTP inventory allows you to track stock quantity per SKU and optionally per supply channel, what makes that the
   both `sku` and `supplyChannel` key are used to inventory entry distinction. Distinction of inventory entries is then
   necessary for comparision of new and old resources during sync process. In both the [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
   [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
   and the [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
   SKU is provided directly as a 'String' field but supply channel key is not. Supply channel is kept as a
   [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
   [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
   what makes the supply channel key [couldn't be simply extracted from this reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L98).
   Because of that for proper comparision of new and old inventories that reference supply channels you should assert
   the following conditions are fulfilled:

   1. Before using `syncDrafts` you should assert that every [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
      from input list which contains [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
      to a supply channel has either that reference expanded, or has that reference not expanded but has supply channel
      key provided in place of reference `id` (that means calling [getId function](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L26)
      on a reference instance the `String` that represents supply channel key would be returned). By expanded reference
      we mean when calling [getObj function](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L52)
      on a reference instance the [Channel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/channels/Channel.jav)
      object that contains its key would be returned.

   2. Before using `sync` you should assert that every [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
      from input list which contains [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
      to a supply channel have that reference expanded, that means when calling [getObj function](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L52)
      on a reference object you should obtain a [Channel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/channels/Channel.jav)
      object that contains its key.



## How does it work?

The inventory sync uses the `sku` and `supplyChannel` key to match new inventory entries to existing ones.
1. If a inventory entry exists with the same `sku` and a reference to a `supplyChannel` with the same key, it means that
this inventory entry already exists on the CTP project. Therefore, the tool calculates update actions that should be
done to update the old inventory entry with the new inventory entry's fields. Only if there are update actions needed,
they will be issued to the CTP platform.
2. If no matching inventory entry with the same `sku` and a reference to a `supplyChannel` with the same key is found,
the tool will create a new one.

The sync, however, will never delete a inventory entry.
 
## FAQ
#### What does the number of processed inventories actually refer to in the statistics of the sync process?
It refers to the total number of inventories input to the sync. Under all the following cases a inventory is to be
counted as processed:
- new inventory caused the old one to be updated.
- new inventory was created.
- new inventory was the same as the old one and requires no action.
- new inventory failed to process.

The only case where a inventory would not be processed is if the entire sync process fails and stops, before reaching
this inventory in the input list.


 