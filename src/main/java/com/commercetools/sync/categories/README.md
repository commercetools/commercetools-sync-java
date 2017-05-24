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
- `warningCallBack`
- `removeOtherLocales`
- `removeOtherSetEntries`
- `removeOtherCollectionEntries`
- `removeOtherProperties`
- `updateActionsFilter`

## Under the hood

The tool matches categories by their `externalId`. Based on that categories are created or 
updated. Currently the tool does not support category deletion.