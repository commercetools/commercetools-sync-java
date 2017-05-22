# commercetools category sync

Utility which provides API for building CTP category update actions and category synchronisation.

- [How does it work?](#how-does-it-work)
- [FAQ](#faq)
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
 
In order to instantiate a `CategorySyncOptions`, a `ctpClient` is required:
````java
// instantiating a ctpClient
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");
final CtpClient ctpClient = new CtpClient(clientConfig);

// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient).build();
````

The category sync uses the `externalId` to match new categories to existing ones. 
1. If a category exists with the same `externalId`, it means that this category already exists on the CTP project. Therefore, 
the tool calculates update actions that should be done to update the old category with the new category's fields.
Only if there are update actions needed, they will be issued to the CTP platform.
2. If no matching category with the same `externalId` is found, the tool will create a new one. 
then to start the sync:
````java
// instantiating a category sync
final CategorySync categorySync = new CategorySync(categorySyncOptions);

The sync, however, will never delete a category.
 
## FAQ
#### What does the number of processed categories actually refer to in the statistics of the sync process?
It refers to the total number of categories input to the sync. Under all the following cases a category is to be counted
as processed:
- new category causes the old one to be updated.
- new category should be created.
- new category is the same as the old one and requires no action.
- new category failed to process.

The only case where a category would not be processed is if the entire sync process fails and stops, before reaching this
category in the input list.

#### Why is the `externalId` used for matching categories instead of another field e.g. `slug`?
Even though the `externalId` is an `optional` field on a category on CTP, it is still used as the main identifier for a
category in the sync library. The main reason why an `externalId` is an `optional` field is due to the fact that a category
can be created through the CTP [Admin Center](admin.sphere.io) or the [Merchant Center](mc.commercetools.com) and thus
an `externalId` is insignificant in such case. However, external PIM systems must have some kind of identifier
that they use to uniquely identify their categories. This unique identifier is exactly what is used for an `externalId` 
for the CTP Categories and is what is used for matching new categories with old categories. Therefore, it made sense to 
use it for it's purpose of identifying categories as opposed to using fields like `slug`, which is a localised field 
that has another purpose of storing the part of URL for accessing the category on a shopfront. However, this could be 
changed due to [Issue #36](https://github.com/commercetools/commercetools-sync-java/issues/36).
// execute the sync on your list of categories
categorySync.syncDrafts(categoryDrafts);
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


 