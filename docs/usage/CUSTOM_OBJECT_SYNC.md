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
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Sync list of CustomObjectDrafts

#### Prerequisites
1. The sync expects a list of `CustomObjectDraft`s that have their `key` and `container` fields set to be matched with
custom objects in the target CTP project. Therefore, the custom objects in the target project are expected to have the 
same `key` and `container` fields set, otherwise they won't be matched.

2. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

3. After the `sphereClient` is set up, a `CustomObjectSyncOptions` should be be built as follows:
````java
// instantiating a CustomObjectSyncOptions
final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

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
