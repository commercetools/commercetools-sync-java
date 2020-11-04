# Custom Object Sync

Module used for importing/syncing CustomObject into a commercetools project. 
It also provides utilities for correlating a custom object to a given custom object draft based on the 
comparison of a [CustomObject](https://docs.commercetools.com/http-api-projects-custom-objects#customobject) 
against a [CustomObjectDraft](https://docs.commercetools.com/http-api-projects-custom-objects#customobjectdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of CustomObjectDrafts](#sync-list-of-customobjectdrafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Sync list of CustomObjectDrafts

#### Prerequisites
1. Create a `sphereClient`:
Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/2.3.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

2. The sync expects a list of `CustomObjectDraft`s that have their `key` and `container` fields set to be matched with
custom objects in the target CTP project. Therefore, the custom objects in the target project are expected to have the 
same `key` and `container` fields set, otherwise they won't be matched.

3. After the `sphereClient` is set up, a `CustomObjectSyncOptions` should be be built as follows:
````java
// instantiating a CustomObjectSyncOptions
final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(sphereClient).build();
````

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* custom object draft from the source
* custom object of the target project (only provided if an existing custom object could be found)
* the update-actions, which failed (only provided if an existing custom object could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CustomObjectSync.class);
 final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, customObject, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* custom object draft from the source 
* custom object of the target project (only provided if an existing custom object could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CustomObjectSync.class);
 final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, customObject, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
In theory, `CustomObjectSyncOptions` provides callback before update operation. User can customize own callback and inject
into sync options. However, in actual case, `beforeUpdateCallback`is not triggered in custom object sync process. When
new custom object draft has the same key and container as existing custom object but different in custom object values, 
sync process automatically perform update operation. The value of corresponding custom object in target project is 
overwritten. This approach is different from other resources and no update action is involved.

No example is applicable.

##### 4. `beforeCreateCallback`
During the sync process if a custom object draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * custom object draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### 5. `batchSize`
A number that could be used to set the batch size with which custom objects are fetched and processed,
as custom objects are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding custom objects 
from the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for custom object sync.
##### Example
````java                         
final CustomObjectSyncOptions customObjectSyncOptions = 
         CustomObjectSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a CustomObjectSync
final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

// execute the sync on your list of custom object drafts
CompletionStage<CustomObjectSyncStatistics> syncStatisticsStage = customObjectSync.sync(customObjectDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `CustomObjectSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed custom objects and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CustomObjectSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 2000 custom objects were processed in total (1000 created, 995 updated, 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.
  
#### More examples of how to use the sync
 
- [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/customobjects/CustomObjectSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

More examples of those utils for different custom objects can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/customobjects/utils/CustomObjectSyncUtilsTest.java).
