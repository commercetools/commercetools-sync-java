# commercetools category sync

Utility which provides API for building CTP category update actions and category synchronisation.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

  - [Usage](#usage)
    - [Build all update actions](#build-all-update-actions)
    - [Build particular update action(s)](#build-particular-update-actions)
    - [Sync list of category drafts](#sync-list-of-category-drafts)
  - [Under the hood](#under-the-hood)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Build all update actions

Compares Category with a new CategoryDraft and results in a list of category update actions. 
```java
List<UpdateAction<Category>> updateActions = CategorySyncUtils.buildActions(category, categoryDraft, categorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/categories/utils/CategorySyncUtilsTest.java).


### Build particular update action(s)

To build the update action for category name for example:
````java
Optional<UpdateAction<Category>> updateAction = buildChangeNameUpdateAction(oldCategory, categoryDraft);
````
More examples of how to use those update action utils can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/updateactionutils).

### Sync list of category drafts

In order to use the category sync an instance of
[CategorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/CategorySyncOptions.java) have to be injected.

In order to instantiate a `CategorySyncOptions`, a `sphereClient` is required.

It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
1. Limits the amount of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with 
[QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html) 
2. Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the 
[RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)

You can use the same client instantiating used in the integration tests for this library found 
[here](https://github.com/commercetools/commercetools-sync-java/blob/documentation/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45).


````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient).build();
````

then to start the sync:
````java
// instantiating a category sync
final CategorySync categorySync = new CategorySync(categorySyncOptions);

// execute the sync on your list of categories
CompletionStage<CategorySyncStatistics> syncStatisticsStage = CategorySynccategorySync.sync(categoryDrafts);
````
More examples of how to use the sync 
1. From another CTP project as source can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/categories/CategorySyncIT.java).
2. From an external source can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/CategorySyncIT.java).

**Preconditions:** The sync expects a list of non-null `CategoryDraft` objects that have their `key` fields set, otherwise
 the sync will trigger an `errorCallback` function which is set by the user. More on this option can be found down below
 in the additional `options` explanations.
 
 Every category may have a reference to a `parent category` and a reference to the `Type` of its custom fields. Categories 
and Types are matched by their `key` Therefore, in order for the sync to resolve the 
 actual ids of those references, the `key` of the `Type`/parent `Category` has to be supplied in one of two ways:
 - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
 reference would return its `key`. Note that the library will check that this `key` is not 
 provided in `UUID` format by default. However, if you want to provide the `key` in `UUID` format, you can
  set it through the sync options. <!--TODO Different example of sync performed that way can be found [here]().-->
 - Provide the reference expanded. This means that calling `getObj()` on the reference should not return `null`,
  but return the `Type` object, from which the its `key` can be directly accessible.  

**Note**: This library provides you with a utility method 
 [`replaceCategoriesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/0.0.2/com/commercetools/sync/commons/utils/SyncUtils.html#replaceCategoriesReferenceIdsWithKeys-java.util.List-)
 that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync
  
Example of its usage can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/categories/CategorySyncIT.java#L115).


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
<!--
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
object properties. -->
- `updateActionsFilter`
a filter function which can be applied on generated list of update actions to produce a resultant list after the filter 
function has been applied.
- `allowUuid`
a flag, if set to `true`, enables the user to use keys with UUID format for references. By default, it is set to `false`.

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning 
 streams, can be found [here](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/CategorySyncIT.java#L79-L82).

## Under the hood

The tool matches categories by their `key`. Based on that categories are created or 
updated. Currently the tool does not support category deletion.