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
objects given an old inventory entry, represented by an [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java),
and a new inventory entry, represented by an [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java):
    - ChangeQuantity
    - SetRestockableInDays
    - SetExpectedDelivery
    - SetSupplyChannel
    - inventory custom fields update actions:
        - SetCustomType
        - SetCustomField

## How to use it?

### How to use `InventorySyncOptions` and `InventorySyncOptionsBuilder`

In order to use the inventory sync an instance of
[InventorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptions.java)
have to be injected. It can be created by using a dedicated builder instance of
[InventorySyncOptionsBuilder](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptionsBuilder.java).
 
In order to instantiate a `InventorySyncOptions`, a `ctpClient` is required:

#### `ctpClient` [Required]
Defines the configuration of the commercetools project that inventory entries are going to be synced to.
````java
// instantiating a ctpClient
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");
final CtpClient ctpClient = new CtpClient(clientConfig);
  
// instantiating an InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(ctpClient).build();
````

Additional configuration for the sync can be configured on the `InventorySyncOptions` instance, according to the need
of the user of the sync:

#### `ensureChannels` [Optional]

Defines an optional field which represents a strategy to handle syncing inventory entries with missing supply channels.
Having an inventory entry, with a missing supply channel reference, could be processed in either of the following ways:
 - If `ensureChannels` is set to `false` such inventory entry won't sync and `errorCallback` will be triggered.
 - If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key.
 If it fails to create the supply channel, the inventory entry won't sync and `errorCallback` will be triggered.

If not provided, it is set to `false` by default.

````java
// instantiating an InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
                                                    .of(ctpClient)
                                                    .ensureChannels(true)
                                                    .build();
````

#### `batchSize` [Optional]

Inventory entries are fetched and processed from the target CTP project in batches for better performance.
This `batchSize` field is an optional field that could be used to set the batch size with which inventory entries are
fetched and processed with. The algorithm accumulates up to `batchSize` inventory entries from input list, then fetches
corresponding inventory entries from target CTP project in a single request, and then performs sync actions on them.
Playing with this option can slightly improve or reduce processing speed.

If not provided, it is set to `30` by default.

````java
 // instantiating an InventorySyncOptions
 final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
                                                      .of(ctpClient)
                                                      .setBatchSize(10)
                                                      .build();
 ````


### How to use `InventorySync`

Having `InventorySyncOptions` instance the [InventorySync](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySync.java)
instance can be created. It can then do any of the following:

#### `sync`

Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
[InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
objects.

````java
// instantiating an inventory sync
final InventorySync inventorySync = new InventorySync(inventorySyncOptions);
  
// execute the sync on your list of inventory entries
inventorySync.sync(inventoryEntries);
````

**Important!**
Before using `sync`, you should make sure that every [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
in your input list which contains a [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
to a supply channel has that reference expanded, that means when calling [getObj()](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L52)
function on a reference's object a [Channel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/channels/Channel.jav)
object that contains its key would be obtained. More information about why it is important can be found in the
[FAQ](#why-is-it-important-to-provide-extended-supply-channel-references-in-entries-from-input-lists-of-sync-process)
section.

#### `syncDrafts`

Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
[InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
objects.

````java
// instantiating an inventory sync
final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

// execute the sync on your list of inventory entries
inventorySync.syncDrafts(inventoryEntriesDrafts);
````

**Important!**
Before using `syncDrafts`, you should make sure that every [InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
in your input list which contains a [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
to a supply channel has either that reference expanded, or has that reference not expanded but instead has supply channel
key provided in place of reference `id` (that means calling [getId()](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L26)
function on a reference instance the `String` that represents supply channel key would be returned). By expanded reference
we mean that calling [getObj()](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java#L52)
function on a reference's instance a [Channel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/channels/Channel.jav)
object that contains its key would be returned. More information about why it is important can be found in the
[FAQ](#why-is-it-important-to-provide-extended-supply-channel-references-in-entries-from-input-lists-of-sync-process)
section.

#### `getStatistics`

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

### How to use `InventorySyncUtils` and `InventoryUpdateActionUtils`

For users who want to perform sync processor by themselves the
[InventorySyncUtils](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/utils/InventorySyncUtils.java)
and [InventoryUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/utils/InventoryUpdateActionUtils.java)
utility classes are provided. Mentioned classes allow to match differences between `InventoryEntry` containing legacy
data and `InventoryEntryDraft` containing current data and return update actions that should be executed on legacy
entry to keep it up to date. To receive such
[UpdateActions](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/commands/UpdateAction.java)
you can use following methods:

#### `buildChangeQuantityAction`
Used for comparision of `quantityOnStock` values from provided entries. `Optional` that may contain updating
[ChangeQuantity](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/commands/updateactions/ChangeQuantity.java)
action is returned.

````java
//Having an old InventoryEntry attempts to build "change quantity" update action
final Optional<UpdateAction<InventoryEntry>> updateAction =
    InventoryUpdateActionUtils.buildChangeQuantityAction(inventoryEntry, inventoryEntrydraft);

// If update action is present, the old entry can be updated by the SphereClient instance.
if (updateAction.isPresent()) {
    sphereClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateAction.get()));
}
````

#### `buildSetRestockableInDaysAction`
Used for comparision of `restockableInDays` values from provided entries. `Optional` that may contain updating
[SetRestockableInDays](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/commands/updateactions/SetRestockableInDays.java)
action is returned.

````java
/*
 * Having InventoryEntry instance with legacy data and InventoryEntryDraft with current data
 * attempt to build "set restockable in days" update action
 */
final Optional<UpdateAction<InventoryEntry>> updateAction =
    InventoryUpdateActionUtils.buildSetRestockableInDaysAction(inventoryEntry, inventoryEntrydraft);

// If update action is present, the legacy entry can be updated by SphereClient instance.
if (updateAction.isPresent()) {
    sphereClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateAction.get()));
}
````

#### `buildSetExpectedDeliveryAction`
Used for comparision of `expectedDelivery` values from provided entries. `Optional` that may contain updating
[SetExpectedDelivery](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/commands/updateactions/SetExpectedDelivery.java)
action is returned.

````java
/*
 * Having InventoryEntry instance with legacy data and InventoryEntryDraft with current data
 * attempt to build "set expected delivery" update action
 */
final Optional<UpdateAction<InventoryEntry>> updateAction =
    InventoryUpdateActionUtils.buildSetExpectedDeliveryAction(inventoryEntry, inventoryEntrydraft);

// If update action is present, the legacy entry can be updated by SphereClient instance.
if (updateAction.isPresent()) {
    sphereClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateAction.get()));
}
````

#### `buildSetSupplyChannelAction`
Used for comparision of `expectedDelivery` values from provided entries. `Optional` that may contain updating
[SetSupplyChannel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/commands/updateactions/SetSupplyChannel.java)
action is returned.

````java
/*
 * Having InventoryEntry instance with legacy data and InventoryEntryDraft with current data
 * attempt to build "set supply channel" update action
 */
final Optional<UpdateAction<InventoryEntry>> updateAction =
    InventoryUpdateActionUtils.buildSetSupplyChannelAction(inventoryEntry, inventoryEntrydraft);

// If update action is present, the legacy entry can be updated by SphereClient instance.
if (updateAction.isPresent()) {
    sphereClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateAction.get()));
}
````

#### `buildActions`
Used for comparision of all field values from provided entries. `List` that may contain all necessary updating
actions is returned. This method takes additional parameters of [InventorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptions.java)
and [TypeService](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/services/TypeService.java)
because they are needed for custom fields comparision.


````java
/*
 * Having InventoryEntry instance with legacy data and InventoryEntryDraft with current data
 * attempt to build update actions. Also instances of InventorySyncOptions and TypeService
 * have to be injected.
 */
final List<UpdateAction<InventoryEntry>> updateActions =
    InventorySyncUtils.buildActions(inventoryEntry, inventoryEntrydraft, inventorySyncOptions, typeService);

// After check if any update action is present, the legacy entry can be updated using SphereClient instance.
if (!updateActions.isEmpty()) {
    sphereClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateActions));
}
````

## How does it work?

The inventory sync uses the `sku` and `supplyChannel` key to match new inventory entries to existing ones.
1. If an inventory entry exists with the same `sku` and a reference to a `supplyChannel` with the same key, it means that
this inventory entry already exists on the CTP project. Therefore, the tool calculates update actions that should be
done to update the old inventory entry with the new inventory entry's fields. Only if there are update actions needed,
they will be issued to the CTP platform.
2. If no matching inventory entry with the same `sku` and a reference to a `supplyChannel` with the same key is found,
the tool will create a new one.

The sync, however, will never delete an inventory entry.
 
## FAQ
#### What does the number of processed inventories actually refer to in the statistics of the sync process?
It refers to the total number of inventories input to the sync. Under all the following cases an inventory is to be
counted as processed:
- new inventory caused the old one to be updated.
- new inventory was created.
- new inventory was the same as the old one and requires no action.
- new inventory failed to process.

The only case where an inventory would not be processed is if the entire sync process fails and stops, before reaching
this inventory in the input list.

#### Why is it important to provide extended supply channel references in entries from input lists of sync process?
In CTP, an [inventory](http://dev.commercetools.com/http-api-projects-inventory.html) allows you to track stock quantity per SKU and optionally per supply channel, what makes that
the sync uniquely identifies an inventory entry by its `sku` and supply channel `key`. It uses them for matching old and new inventory entries. In both the [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
[InventoryEntryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntryDraft.java)
and the [InventoryEntry](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/inventory/InventoryEntry.java)
`sku` is provided directly as a `String`, but they just have a
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk)
[Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
to the channel, not the `key` of the channel (unless [reference expanded](https://dev.commercetools.com/http-api.html#reference-expansion)).
Therefore, make sure that you comply to the preconditions of syncing as mentioned in [How to use it?](#how-to-use-it) section. This is
important for proper comparision of new and old inventories that reference supply channels.

 