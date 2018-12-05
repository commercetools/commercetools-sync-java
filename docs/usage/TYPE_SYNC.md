# Type Sync

Module used for importing/syncing Types into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Type](https://docs.commercetools.com/http-api-projects-types.html#type) 
against a [TypeDraft](https://docs.commercetools.com/http-api-projects-types.html#typedraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of type drafts](#sync-list-of-type-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Sync list of type drafts

#### Prerequisites
1. The sync expects a list of `TypeDraft`s that have their `key` fields set to be matched with
types in the target CTP project. Also, the types in the target project are expected to have the `key`
fields set, otherwise they won't be matched.

2. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

3. After the `sphereClient` is set up, a `TypeSyncOptions` should be be built as follows:
````java
// instantiating a TypeSyncOptions
final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a type sync
final TypeSync typeSync = new TypeSync(typeSyncOptions);

// execute the sync on your list of types
CompletionStage<TypeSyncStatistics> syncStatisticsStage = typeSync.sync(typeDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `TypeSyncStatistics`
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
 
##### More examples of how to use the sync
 
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

1. Updating the label of enum values and localized enum values of field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
2. Removing the enum values and localized enum values from the field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
3. Updating the input hint of a field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
4. Currently the sync would handle changes to Enum or LocalizedEnum Types but not [Set](https://docs.commercetools.com/http-api-projects-types.html#settype) of either. [#313](https://github.com/commercetools/commercetools-sync-java/issues/313)
