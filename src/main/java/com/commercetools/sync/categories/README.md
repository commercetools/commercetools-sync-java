# commercetools category sync

Utility which provides API for building CTP category update actions and category synchronisation.

- [Usage](#usage)
- [Under the hood](#under-the-hood)

## Usage

### Build all update actions

<!-- TODO: A code snippet will be added once #14 is resolved -->

### Build particular update action(s)

To build the update action for category name:
````java
Optional<UpdateAction<Category>> updateAction = buildChangeNameUpdateAction(oldCategory, categoryDraft);
````
For other examples of update actions, please check [here](). <!-- TODO: Add link to Integration tests.-->

### Sync list of category drafts

In order to use the category sync an instance of
[CategorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/CategorySyncOptions.java) have to be injected.

In order to instantiate a `CategorySyncOptions`, a `sphereClient` is required:
````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient).build();
````

then to start the sync:
````java
// instantiating a category sync
final CategorySync categorySync = new CategorySync(categorySyncOptions);

// execute the sync on your list of categories
categorySync.sync(categoryDrafts);
````
**Preconditions:** The sync expects a list of non-null `CategoryDraft` objects that have their `key` fields set, otherwise
 the sync will trigger an `errorCallback` function which is set by the user. More on this option can be found down below
 in the additional `options` explanations.
 
 Every category may have a reference to a `parent category` and a reference to the `Type` of its custom fields. Categories 
and Types are matched by their `key` Therefore, in order for the sync to resolve the 
 actual ids of those references, the `key` of the `Type`/parent `Category` has to be supplied in one of two ways:
 - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
 reference would return its `key`. Note that the library will check that this `key` is not 
 provided in `UUID` format by default. However, if you want to provide the `key` in `UUID` format, you can
  set it through the sync options. Different example of sync performed that way can be found [here]().
 - Provide the reference expanded. This means that calling `getObj()` on the reference should not return `null`,
  but return the `Type` object, from which the its `key` can be directly accessible. Example of sync performed that 
  way can be found [here]().


In order to get the statistics of the sync process, use the `getStatistics()` method on the sync instance. It is used 
to get an object containing all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed categories and the processing time of the sync in different time units and in a
human readable format.
````java
categorySync.getStatistics();
categorySync.getStatistics().getReportMessage(); 
/*"Summary: 2000 categories were processed in total (1000 created, 995 updated and 5 categories failed to sync)."*/
````

Additional optional configuration for the sync can be set on the `CategorySyncOptions` instance, according to your 
need:
- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error. Currently, these 
events.

- `warningCallBack` 
a callback that is called whenever an event occurs during the sync process that represents a warning. Currently, these 
events.
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
- `updateActionsFilter`
a filter function which can be applied on generated list of update actions to produce a resultant list after the filter 
function has been applied.
- `allowUuid`
a flag, if set to `true`, enables the user to use keys with UUID format for references. By default, it is set to `false`.

## Under the hood

The tool matches categories by their `key`. Based on that categories are created or 
updated. Currently the tool does not support category deletion.