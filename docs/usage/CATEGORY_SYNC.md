# commercetools category sync

A utility which provides an API for building CTP category update actions and category synchronization.

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

#### Prerequisites
1. The sync expects a list of non-null `CategoryDraft` objects that have their `key` fields set to match the
categories from the source to the target. Also, the target project is expected to have the `key` fields set, otherwise they won't be
matched.
2. Every category may have a reference to a `parent category` and a reference to the `Type` of its custom fields. Categories 
   and Types are matched by their `key` Therefore, in order for the sync to resolve the 
    actual ids of those references, the `key` of the `Type`/parent `Category` has to be supplied in one of two ways:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`. Note that the library will check that this `key` is not 
    provided in `UUID` format by default. However, if you want to provide the `key` in `UUID` format, you can
     set it through the sync options. <!--TODO Different example of sync performed that way can be found [here]().-->
    - Provide the reference expanded. This means that calling `getObj()` on the reference should not return `null`,
     but return the `Type` object, from which its `key` can be directly accessible.  
   
   **Note**: This library provides you with a utility method 
    [`replaceCategoriesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M1/com/commercetools/sync/commons/utils/SyncUtils.html#replaceCategoriesReferenceIdsWithKeys-java.util.List-)
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
   
   You can use the same client instantiating used in the integration tests for this library found 
   [here](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45).

4. After the `sphereClient` is set up, a `CategorySyncOptions` should be built as follows: 
````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient).build();
````

The options can be used to provide additional optional configuration for the sync as well:
- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error. Currently, these 
events.

- `warningCallBack` 
a callback that is called whenever an event occurs during the sync process that represents a warning. Currently, these 
events.

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept category 
update and modify (add/remove) update actions just before they are sent to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a category draft before a request to create it on CTP is issued. It allows the 
user to intercept category creates requests to modify the draft before the create request is sent to CTP API.

- `allowUuid`
a flag, if set to `true`, enables the user to use keys with UUID format for references. By default, it is set to `false`.

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning 
streams would look as follows:
```java
final Logger logger = LoggerFactory.getLogger(MySync.class);
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient)
                                                                          .errorCallBack(logger::error)
                                                                          .warningCallBack(logger::warn)
                                                                          .build();
```


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

1. Categories are either created or updated. Currently, the tool does not support category deletion.
2. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/3). 
