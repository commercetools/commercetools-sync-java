# Category Sync

A utility which provides an API for building CTP category update actions and category synchronisation.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of category drafts](#sync-list-of-category-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of category drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites
1. The sync expects a list of non-null `CategoryDraft` objects that have their `key` fields set to match the
categories from the source to the target. Also, the target project is expected to have the `key` fields set, otherwise they won't be
matched.
2. Every category may have a reference to a `parent category` and a reference to the `Type` of its custom fields. Categories 
   and Types are matched by their `key` Therefore, in order for the sync to resolve the 
    actual ids of those references, the `key` of the `Type`/parent `Category` has to be supplied in the following way:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`.  

   **Note**: This library provides you with a utility method 
    [`replaceCategoriesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/categories/utils/CategoryReferenceReplacementUtils.html#replaceCategoriesReferenceIdsWithKeys-java.util.List-)
    that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
    ````java
    // Puts the keys in the reference id fields to prepare for reference resolution
    final List<CategoryDraft> categoryDrafts = replaceCategoriesReferenceIdsWithKeys(categories);
    ````
     
   Example of its usage can be found [here](/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/categories/CategorySyncIT.java#L130).
3. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the number of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with 
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html) 
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the 
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)
   
   If you have no special requirements on sphere client creation then you can use `ClientConfigurationUtils#createClient` 
   util which applies best practices already.

4. After the `sphereClient` is set up, a `CategorySyncOptions` should be built as follows: 
````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](/docs/usage/SYNC_OPTIONS.md).

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a category sync
final CategorySync categorySync = new CategorySync(categorySyncOptions);

// execute the sync on your list of categories
CompletionStage<CategorySyncStatistics> syncStatisticsStage = categorySync.sync(categoryDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `CategorySyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed categories and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CategorySyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*"Summary: 2000 categories were processed in total (1000 created, 995 updated, 5 failed to sync and 0 categories with a missing parent)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:
 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.

More examples of how to use the sync 
1. From another CTP project as a source can be found [here](/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/categories/CategorySyncIT.java).
2. From an external source can be found [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/CategorySyncIT.java). 

##### Usage Tip
The sync library is not meant to be executed in a parallel fashion. Check the example in [point #2 here](/docs/usage/PRODUCT_SYNC.md#caveats). 
By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
  - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
  - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch contain.
 
The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
is the recommended good balance for stability and performance for the sync process.

In order to exploit the number of `max parallel requests`, the `batch size` should have a value set which is equal or higher.


### Build all update actions

A utility method provided by the library to compare a Category with a new CategoryDraft and results in a list of category update actions. 
```java
List<UpdateAction<Category>> updateActions = CategorySyncUtils.buildActions(category, categoryDraft, categorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](/src/test/java/com/commercetools/sync/categories/utils/CategorySyncUtilsTest.java).


### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a Category and a new CategoryDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<Category>> updateAction = buildChangeNameUpdateAction(oldCategory, categoryDraft);
````
More examples of those utils for different fields can be found [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/updateactionutils).


## Caveats   
1. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/87). 
