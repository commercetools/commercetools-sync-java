# State Sync

Module used for importing/syncing States into a commercetools project. 
It also provides utilities for generating update actions based on the comparison a [State](https://docs.commercetools.com/http-api-projects-states#states) (which basically represents what commercetools already has)
against a [StateDraft](https://docs.commercetools.com/http-api-projects-states#statedraft) (which represents a new version of the state supplied by the user).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of State drafts](#sync-list-of-state-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
      - [Persistence of StateDrafts with missing references](#persistence-of-statedrafts-with-irresolvable-references)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)  
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of State drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites
1. The sync expects a list of `StateDraft`s that have their `key` fields set to be matched with
states in the target commercetools project. Also, the states in the target project are expected to have the `key` fields set,
otherwise they won't be matched.

2. Every state may have several `transitions` to other states. Therefore, in order for the sync to resolve the actual ids of those transitions,
 those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the transition. This means that calling `getId()` on the
      transition would return its `key`. 
     
        **Note**: When syncing from a source commercetools project, you can use this util which this library provides: 
         [`mapToStateDrafts`](https://commercetools.github.io/commercetools-sync-java/v/2.0.0/com/commercetools/sync/states/utils/StateReferenceResolutionUtils.html#mapToStateDrafts-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<StateDraft> stateDrafts = StateReferenceResolutionUtils.mapToStateDrafts(states);
         ````
     
4. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

5. After the `sphereClient` is set up, a `StateSyncOptions` should be built as follows: 
````java
// instantiating a StateSyncOptions
   final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(sphereClient).build();
````
[More information about Sync Options](SYNC_OPTIONS.md). 

#### Running the sync
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

A StateDraft (state-A) could be supplied in with a transition referencing StateDraft (state-B). 
It could be that (state-B) is not supplied before (state-A), which means the sync could fail creating/updating (state-A). 
It could also be that (state-B) is not supplied at all in this batch but at a later batch.
 
The library keep tracks of such "referencing" states like (state-A) and persists them in storage 
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
