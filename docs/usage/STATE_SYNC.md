# State Sync

The module used for importing/syncing States into a commercetools project. 
It also provides utilities for generating update actions based on the comparison a [State](https://docs.commercetools.com/http-api-projects-states#states) (which basically represents what commercetools already has)
against a [StateDraft](https://docs.commercetools.com/http-api-projects-states#statedraft) (which represents a new version of the state supplied by the user).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
    - [Prerequisites](#prerequisites)
    - [SphereClient](#sphereclient)
    - [Required Fields](#required-fields)
    - [Reference Resolution](#reference-resolution)
      - [Syncing from a commercetools project](#syncing-from-a-commercetools-project)
      - [Syncing from an external resource](#syncing-from-an-external-resource)
    - [SyncOptions](#syncoptions)
      - [errorCallback](#errorcallback)
      - [warningCallback](#warningcallback)
      - [beforeUpdateCallback](#beforeupdatecallback)
      - [beforeCreateCallback](#beforecreatecallback)
      - [batchSize](#batchsize)
      - [cacheSize](#cachesize)
  - [Running the sync](#running-the-sync)
      - [Persistence of StateDrafts with missing references](#persistence-of-statedrafts-with-missing-references)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

#### Prerequisites
#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/3.1.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [StateDraft](https://docs.commercetools.com/http-api-projects-states#statedraft)| `key` |  Also, the states in the target project are expected to have the `key` fields set. | 

#### Reference Resolution 

`Transitions` are a way to describe possible transformations of the current state to other states of the same type (for example Initial -> Shipped). When performing a [SetTransitions](https://docs.commercetools.com/api/projects/states#set-transitions), an array of [Reference](https://docs.commercetools.com/api/types#reference) to State is needed.
In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier). 
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference. 
Currently, commercetools API does not support the [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier) for the `transitions`,  for those `transition` references you have to provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the 
reference should return its `key`.

|Reference Field|Type|
|:---|:---|
| `transitions` | Array of Reference to State | 

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use this utility which this library provides:  [`mapToStateDrafts`](https://commercetools.github.io/commercetools-sync-java/v/3.1.0/com/commercetools/sync/states/utils/StateReferenceResolutionUtils.html#mapToStateDrafts-java.util.List-)
that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:

````java
// Build a StateQuery for fetching states from a source CTP project with all the needed references expanded for the sync
final StateQuery stateQueryWithReferenceExpanded = StateReferenceResolutionUtils.buildStateQuery();

// Query all states (NOTE this is just for example, please adjust your logic)
final List<State> states =
    CtpQueryUtils
        .queryAll(sphereClient, stateQueryWithReferenceExpanded, Function.identity())
        .thenApply(fetchedResources -> fetchedResources
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()))
        .toCompletableFuture()
        .join();

// Mapping from State to StateDraft with considering reference resolution.
final List<StateDraft> stateDrafts = StateReferenceResolutionUtils.mapToStateDrafts(states);
````
##### Syncing from an external resource

````java
final StateDraft stateDraft = StateDraftBuilder
    .of("state-key", StateType.LINE_ITEM_STATE)
    .transitions(asSet(State.referenceOfId("another-state-key"))) // note that state transition key is provided in the id field of reference.
    .build();
````


#### SyncOptions

After the `sphereClient` is set up, a `StateSyncOptions` should be built as follows: 

````java
// instantiating a StateSyncOptions
   final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* state draft from the source
* state of the target project (only provided if an existing state could be found)
* the update-actions, which failed (only provided if an existing state could be found)

````java
 final Logger logger = LoggerFactory.getLogger(StateSync.class);
 final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, state, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback

A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* state draft from the source 
* state of the target project (only provided if an existing state could be found)

````java
 final Logger logger = LoggerFactory.getLogger(StateSync.class);
 final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, state, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target state and a state draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * state draft from the source
 * state from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>> 
            beforeUpdateStateCallback =
            (updateActions, newStateDraft, oldState) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveRoles))
                    .collect(Collectors.toList());
                        
final StateSyncOptions stateSyncOptions = 
        StateSyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateStateCallback).build();
````

##### beforeCreateCallback
During the sync process, if a state draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * state draft that should be created

Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### batchSize
A number that could be used to set the batch size with which states are fetched and processed,
as states are obtained from the target project on commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding states
from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for state sync.

````java                         
final StateSyncOptions stateSyncOptions = 
         StateSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for state sync.

````java
final StateSyncOptions stateSyncOptions = 
         StateSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````


### Running the sync
After all the aforementioned points in the previous section have been fulfilled, run the sync as follows:
````java
// instantiating a State sync
   final StateSync stateSync = new StateSync(stateSyncOptions);

// execute the sync on your list of StateDraft
  final CompletionStage<StateSyncStatistics> stateSyncStatisticsStage = stateSync.sync(stateDrafts);
````
The result of the completing the `StateSyncStatistics` in the previous code snippet contains a `StateSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed states, the missing parent of transitions and the processing time of the sync in different time units and in a
human-readable format.
````java
final StateSyncStatistics stats = stateSyncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
// Summary: 3 state(s) were processed in total (3 created, 0 updated, 0 failed to sync and 0 state(s) with missing transition(s).
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.

##### Persistence of StateDrafts with missing references

A StateDraft (state-A) could be supplied with a transition referencing StateDraft (state-B). 
It could be that (state-B) is not supplied before (state-A), which means the sync could fail to create/update (state-A). 
It could also be that (state-B) is not supplied at all in this batch but at a later batch.
 
The library keeps tracks of such "referencing" states like (state-A) and persists them in storage 
(**commercetools `customObjects` in the target project** , in this case) 
to keep them and create/update them accordingly whenever the referenced state has been provided at some point.

The `customObject` will have a `container:` **`"commercetools-sync-java.UnresolvedTransitionsService.stateDrafts"`**
and a `key` representing a hash value of the StateDraft key that is waiting to be created/updated.


Here is an example of a `CustomObject` in the target project that represents a StateDraft with key `state-A`.  
It being persisted as `CustomObject` means that the referenced StateDrafts with keys `state-B`  do not exist yet.

```json
{
  "container": "commercetools-sync-java.UnresolvedTransitionsService.stateDrafts",
  "key": "518ea82bb78755c0cdd67909dd3206d56186f7e5",
  "value": {
    "missingTransitionStateKeys": [
      "state-B"
    ],
    "stateDraft": {
      "type": "ReviewState",
      "transitions": [
        {
          "id": "state-B",
          "typeId": "state"
        }
      ],
      "roles": [
        "ReviewIncludedInStatistics"
      ],
      "key": "state-A",
      "initial": true
    }
  }
}
```

As soon, as the referenced StateDrafts are supplied to the sync, the draft will be created/updated and the 
`CustomObject` will be removed from the target project.

Keeping the old custom objects around forever can negatively influence the performance of your project and the time it takes to restore it from a backup.  Deleting unused data ensures the best performance for your project. Please have a look into the [Cleanup guide](CLEANUP_GUIDE.md) to cleanup old unresolved custom objects.

#### More examples of how to use the sync
 
 1. [Sync usages](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/states/StateSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*


### Build all update actions

A utility method provided by the library to compare a `State` with a new `StateDraft` and results in a list of state update actions.
 update actions. 
```java
List<UpdateAction<State>> updateActions = StateSyncUtils.buildActions(state, stateDraft, stateSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `State` and a new `StateDraft`, and in turn builds
 the update action. One example is the `buildSetNameAction` which compares names:
````java
Optional<UpdateAction<State>> updateAction = StateUpdateActionUtils.buildSetNameAction(oldState, stateDraft);
````
More examples of those utils for different types can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/states/utils/StateUpdateActionUtilsTest.java).
