# commercetools type sync

A utility which provides an API for building CTP type update actions and type synchronization.

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
1. The sync expects a list of non-null `TypeDrafts` objects that have their `key` fields set to match the
types from the source to the target. Also, the target project is expected to have the `key` fields set, otherwise they won't be
matched.
2. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the number of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html)
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)

   You can instantiate the client the same way it is instantiated in the integration tests for this library found
   [here](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45).

4. After the `sphereClient` is setup, a `TypeSyncOptions` should be be built as follows:
````java
// instantiating a TypeSyncOptions
final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(sphereClient).build();
````

The options can be used to provide additional optional configuration for the sync as well:
- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error. Currently, these
events.

- `warningCallBack`
a callback that is called whenever an event occurs during the sync process that represents a warning. Currently, these
events.

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept type
 **_update_** actions just before they are sent to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a type draft before a request to create it on CTP is issued. It allows the
user to intercept type **_create_** request to modify the draft before the create request is sent to CTP API.

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning
streams would look as follows:
```java
final Logger logger = LoggerFactory.getLogger(MySync.class);
final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(sphereClient)
                                                              .errorCallBack(logger::error)
                                                              .warningCallBack(logger::warn)
                                                              .build();
```

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

More examples of how to use the sync can be found [here](/src/integration-test/java/com/commercetools/sync/integration/types/TypeSyncIT.java).

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
More examples of those utils for different types can be found [here](/src/test/java/com/commercetools/sync/types/utils/TypeUpdateActionUtilsTest.java).
and field definitions can be found [here](/src/test/java/com/commercetools/sync/types/utils/FieldDefinitionUpdateActionUtilsTest.java).

## Caveats

1. Updating the label of enum values and localized enum values of field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
2. Removing the enum values and localized enum values from the field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
3. Updating the input hint of field definition is not supported yet. [#339](https://github.com/commercetools/commercetools-sync-java/issues/339)
4. Syncing types with an field of type [SetType](https://docs.commercetools.com/http-api-projects-types.html#settype) is not supported yet.
