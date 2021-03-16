# Custom Object Sync

Module used for importing/syncing CustomObject into a commercetools project. 
It also provides utilities for correlating a custom object to a given custom object draft based on the 
comparison of a [CustomObject](https://docs.commercetools.com/http-api-projects-custom-objects#customobject) 
against a [CustomObjectDraft](https://docs.commercetools.com/http-api-projects-custom-objects#customobjectdraft).

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
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
       
### Prerequisites

#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/4.0.1/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [CustomObjectDraft](https://docs.commercetools.com/http-api-projects-custom-objects#customobjectdraft) | `key` |  Also, the custom objects in the target project are expected to have the `key` fields set. | 
| [CustomObjectDraft](https://docs.commercetools.com/http-api-projects-custom-objects#customobjectdraft) | `container` |  Also, the custom objects in the target project are expected to have the `container` fields set. | 

####  SyncOptions

After the `sphereClient` is set up, a `CustomObjectSyncOptions` should be built as follows:
````java
// instantiating a CustomObjectSyncOptions
final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* custom object draft from the source
* custom object of the target project (only provided if an existing custom object could be found)
* the update-actions, which failed (only provided if an existing custom object could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CustomObjectSync.class);
 final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, customObject, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* custom object draft from the source 
* custom object of the target project (only provided if an existing custom object could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CustomObjectSync.class);
 final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, customObject, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
In theory, `CustomObjectSyncOptions` provides callback before update operation. User can customize their own callback and inject
into sync options. However, in the actual case, `beforeUpdateCallback`is not triggered in the custom object sync process. When
the new custom object draft has the same key and container as an existing custom object but different in custom object values, 
the sync process automatically performs the update operation. The value of a corresponding custom object in the target project is overwritten. This approach is different from other resources and no update action is involved.

No example is applicable.

##### beforeCreateCallback
During the sync process, if a custom object draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * custom object draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### batchSize
A number that could be used to set the batch size with which custom objects are fetched and processed,
as custom objects are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding custom objects from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for custom object sync.

````java                         
final CustomObjectSyncOptions customObjectSyncOptions = 
         CustomObjectSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for custom object sync.

````java
final CustomObjectSyncOptions customObjectSyncOptions = 
         CustomObjectSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a CustomObjectSync
final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

// execute the sync on your list of custom object drafts
CompletionStage<CustomObjectSyncStatistics> syncStatisticsStage = customObjectSync.sync(customObjectDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `CustomObjectSyncStatistics`
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
