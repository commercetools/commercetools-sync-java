# commercetools inventory sync

Utility which provides API for building CTP inventory update actions and inventory synchronisation.

- [Usage](#usage)
- [Under the hood](#under-the-hood)


## Usage

### Build all update actions

<!-- TODO: Probably #14 affects inventory sync as well. Ensure before providing the code snippet. -->

### Build particular update action(s)

To build the update action for changing inventory quantity:

````java
final Optional<UpdateAction<InventoryEntry>> updateAction = buildChangeQuantityAction(oldInventory, inventoryDraft);
````

For other examples of update actions, please check [here]():

<!-- TODO: Provide relevant integration tests in a dedicated branch -->

### Sync list of inventory entry drafts

### How to use `InventorySyncOptions` and `InventorySyncOptionsBuilder`

In order to use the inventory sync an instance of
[InventorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/InventorySyncOptions.java)
have to be injected.

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

**Preconditions:** The sync expects a list of non-null `InventoryEntryDraft` objects that have their `sku` fields set.
Also every `InventoryEntryDraft`, that belongs to a `Channel`, in your input list must either:
- Has the [Reference](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-sdk-base/src/main/java/io/sphere/sdk/models/Reference.java)
to the `supplyChannel` expanded. This means that calling `getObj()` on the reference would not
return `null`, but returns the [Channel](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/channels/Channel.java)
object, from which the sync can access its `key`. Example of sync performed that way can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncTest.java#L128).
- If the reference is not expanded, then it is very important to provide the channel `key` in the `id` field of the
reference. This means calling `getId()` on the channel reference would return it's `key`. Example of sync performed that
way can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/inventories/InventorySyncTest.java#L181).

<!-- TODO: Ensure above paths to examples in a dedicated branch -->

The sync results in a `ComplectionStage` that contains `InventorySyncStatistics` object. From a received instance
you could check all the stats of the sync process, which included a report message, the total number of updated,
created, failed, processed inventories and the processing time of the sync.

<!-- TODO: Update above after resolving #23 -->
<!-- TODO: Consider if getStatistics() is needed. Express your doubts in a #23 -->
````java
inventorySync.sync(inventoryEntryDrafts)
            .thenAccept(inventorySyncStatistics -> inventorySyncStatistics.getReportMessage());
//"Summary: 2000 inventory entries were processed in total (1000 created, 995 updated, 5 failed to sync)"
````

Additional optional configuration for the sync can be configured on the `InventorySyncOptions` instance, according to your need:

- `ensureChannels`
a flag which represents a strategy to handle syncing inventory entries with missing supply channels.
Having an inventory entry, with a missing supply channel reference, could be processed in either of the following ways:
    - If `ensureChannels` is set to `false` this inventory entry won't be synced and the `errorCallback` will be triggered.
    - If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key.
      If it fails to create the supply channel, the inventory entry won't sync and `errorCallback` will be triggered.
    - If not provided, it is set to `false` by default.

- `batchSize`
a number that could be used to set the batch size with which inventory entries are fetched and processed with,
as inventory entries are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to
`batchSize` inventory entries from the input list, then fetches the corresponding inventory entries from the target CTP project
in a single request, and then performs the update actions needed. Playing with this option can slightly improve or reduce processing speed.
    - If not provided, it is set to `30` by default.

- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error.

- `warningCallBack`
a callback that is called whenever an event occurs during the sync process that represents a warning.

- `removeOtherLocales`
a flag which enables the sync module to add additional localizations without deleting existing ones, if set to `false`.
If set to `true`, which is the default value of the option, it deletes the existing object properties.

- `removeOtherSetEntries`
a flag which enables the sync module to add additional Set entries without deleting existing ones, if set to `false`.
If set to `true`, which is the default value of the option, it deletes the existing Set entries.

- `removeOtherCollectionEntries`
a flag which enables the sync module to add collection (e.g. Assets, Images etc.) entries without deleting existing
ones, if set to `false`. If set to `true`, which is the default value of the option, it deletes the existing collection
entries.

- `removeOtherProperties`
a flag which enables the sync module to add additional object properties (e.g. custom fields, etc..) without deleting
existing ones, if set to `false`. If set to `true`, which is the default value of the option, it deletes the existing
object properties.

## Under the hood

The inventory sync uses the `sku` and `supplyChannel` key to match new inventory entries to existing ones.
1. If an inventory entry exists with the same `sku` and a reference to a `supplyChannel` with the same key, it means that
this inventory entry already exists on the CTP project. Therefore, the tool calculates update actions that should be
done to update the old inventory entry with the new inventory entry's fields. Only if there are update actions needed,
they will be issued to the CTP platform.
2. If no matching inventory entry with the same `sku` and a reference to a `supplyChannel` with the same key is found,
the tool will create a new one.

The sync, however, will never delete an inventory entry.
