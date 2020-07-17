# State Sync

Module used for importing/syncing State into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [State](https://docs.commercetools.com/http-api-projects-states#states) 
against a [StateDraft](https://docs.commercetools.com/http-api-projects-states#states).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of State drafts](#sync-list-of-state-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
      - [Persistence of StateDrafts with Irresolvable References](#persistence-of-statedrafts-with-irresolvable-references)
  - [Build all update actions](#build-all-update-actions)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of State drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites
1. The sync expects a list of `StateDraft`s that have their `key` fields set to be matched with
states in the target CTP project. Also, the states in the target project are expected to have the `key` fields set,
otherwise they won't be matched.


2. Every state may have several `transitions` to other states. Therefore, in order for the sync to resolve the actual ids of those transitions,
 those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the transition. This means that calling `getId()` on the
      transition would return its `key`. 
     
        **Note**: When syncing from a source commercetools project, you can use this util which this library provides: 
         `replaceStateTransitionIdsWithKeys`]
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<StateDraft> StateDrafts = replaceStateTransitionIdsWithKeys(products);
         ````
     
4. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

5. After the `sphereClient` is set up, a `StateSyncOptions` should be built as follows: 
````java
// instantiating a StateSyncOptions
     final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(sphereClient).build();
````
[More information about Sync Options](SYNC_OPTIONS.md). 

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
/ / instantiating a State sync
  final StateSync stateSync = new StateSync(stateSyncOptions);

// execute the sync on your list of Statedraft
 final CompletionStage<StateSyncStatistics> stateSyncStatisticsStage = stateSync.sync(StateDrafts);
````
The result of the completing the `StateSyncStatistics` in the previous code snippet contains a `StateSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed products, the missing parent of transitions and the processing time of the sync in different time units and in a
human-readable format.
````java
final StateSyncStatistics stats = stateSyncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*Summary: 3 state(s) were processed in total (3 created, 0 updated, 0 failed to sync and 0 state(s) with missing transition(s).*/
````



##### Persistence of StateDrafts with Irresolvable References

A StateDraft X could be supplied in with an transition referencing StateDraft Y. 
It could be that Y is not supplied before X, which means the sync could fail creating/updating X. 
It could also be that Y is not supplied at all in this batch but at a later batch.
 
The library keep tracks of such "referencing" states like X and persists them in storage 
(**CTP `customObjects` in the target project** , in this case) 
to keep them and create/update them accordingly whenever the referenced state exist in the target project.

The `customObject` will have a `container:` **`"commercetools-sync-java.UnresolvedTransitionsService.stateDrafts"`**
and a `key` representing the key of the StateDraft that is waiting to be created/updated.


Here is an example of a `CustomObject` in the target project that represents a StateDraft with key `state-A`.  
It being persisted as `CustomObject` means that the referenced StateDrafts with keys `state-C`  do not exist yet.

```json
    {
            "id": "46ab3e17-2c35-41cd-9bf5-d2660c275262",
            "version": 1,
            "container": "commercetools-sync-java.UnresolvedTransitionsService.stateDrafts",
            "key": "518ea82bb78755c0cdd67909dd3206d56186f7e5",
            "value": {
              "missingTransitionStateKeys": [
                "state-C"
              ],
              "stateDraft": {
                "type": "ReviewState",
                "transitions": [
                  {
                    "id": "state-C",
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
```

As soon, as the referenced StateDrafts are supplied to the sync, the draft will be created/updated and the 
`CustomObject` will be removed from the target project.


### Build all update actions

A utility method provided by the library to compare a give state with a new StateDraft and update the given state accordently
 update actions. 
```java
 private CompletionStage<Void> buildActionsAndUpdate(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {
```



