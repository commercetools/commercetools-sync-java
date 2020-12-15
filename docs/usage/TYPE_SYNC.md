# Type Sync

The module used for importing/syncing Types into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Type](https://docs.commercetools.com/http-api-projects-types.html#type) 
against a [TypeDraft](https://docs.commercetools.com/http-api-projects-types.html#typedraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Prerequisites](#prerequisites)
    - [SphereClient](#sphereclient)
    - [Required Fields](#required-fields)
    - [SyncOptions](#syncoptions)
      - [errorCallback](#errorcallback)
      - [warningCallback](#warningcallback)
      - [beforeUpdateCallback](#beforeupdatecallback)
      - [beforeCreateCallback](#beforecreatecallback)
      - [batchSize](#batchsize)
      - [cacheSize](#cachesize)
  - [Running the sync](#running-the-sync)
    - [Important to Note](#important-to-note)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites
#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/3.0.1/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [TypeDraft](https://docs.commercetools.com/http-api-projects-types.html#typedraft) | `key` |  Also, the types in the target project are expected to have the `key` fields set. | 

#### SyncOptions

After the `sphereClient` is set up, a `TypeSyncOptions` should be built as follows:
````java
// instantiating a TypeSyncOptions
final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* type draft from the source
* type of the target project (only provided if an existing type could be found)
* the update-actions, which failed (only provided if an existing type could be found)

````java
 final Logger logger = LoggerFactory.getLogger(TypeSync.class);
 final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, type, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* type draft from the source 
* type of the target project (only provided if an existing type could be found)

````java
 final Logger logger = LoggerFactory.getLogger(TypeSync.class);
 final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, type, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target type and a type draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * type draft from the source
 * type from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>> 
            beforeUpdateTypeCallback =
            (updateActions, newTypeDraft, oldType) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveFieldDefinition))
                    .collect(Collectors.toList());
                        
final TypeSyncOptions typeSyncOptions = 
        TypeSyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateTypeCallback).build();
````

##### beforeCreateCallback
During the sync process, if a type draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * type draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### batchSize
A number that could be used to set the batch size with which types are fetched and processed,
as types are obtained from the target project on commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding types from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for type sync.

````java                         
final TypeSyncOptions typeSyncOptions = 
         TypeSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for type sync.

````java
final TypeSyncOptions typeSyncOptions = 
         TypeSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````


### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a type sync
final TypeSync typeSync = new TypeSync(typeSyncOptions);

// execute the sync on your list of types
CompletionStage<TypeSyncStatistics> syncStatisticsStage = typeSync.sync(typeDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `TypeSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed types and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final TypeSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 2000 types were processed in total (1000 created, 995 updated, 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.
 
#### Important to Note
1. If two matching `fieldDefinition`s (old and new) on the matching `type`s (old and new) have a different `FieldType`, the sync will
**remove** the existing `fieldDefinition` and then **add** a new `fieldDefinition` with the new `FieldType`.

2. The `fieldDefinition` for which the `fieldType` is not defined (`null`) will not be synced.
 
#### More examples of how to use the sync
 
 1. [Sync from another CTP project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/types/TypeSyncIT.java).
 2. [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/types/TypeSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a `Type` with a new `TypeDraft` and results in a list of type update actions.
```java
List<UpdateAction<Type>> updateActions = TypeSyncUtils.buildActions(type, typeDraft, typeSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `Type` and a new `TypeDraft`, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<Type>> updateAction = TypeUpdateActionUtils.buildChangeNameAction(oldType, typeDraft);
````
More examples of those utils for different types can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/types/utils/TypeUpdateActionUtilsTest.java).

## Caveats

1. Updating the label of enum values and localized enum values of field definition are not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
2. Removing the enum values and localized enum values from the field definition are not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
3. Updating the input hint of a field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
