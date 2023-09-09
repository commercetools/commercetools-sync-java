# Type Sync

The module used for importing/syncing Types into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Type](https://docs.commercetools.com/api/projects/types#type) 
against a [TypeDraft](https://docs.commercetools.com/api/projects/types#typedraft).

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

#### ProjectApiRoot

Use the [ClientConfigurationUtils](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
To create `ClientCredentials` which are required for creating a client please use the `ClientCredentialsBuilder` provided in java-sdk-v2 [Client OAUTH2 package](https://github.com/commercetools/commercetools-sdk-java-v2/blob/main/rmf/rmf-java-base/src/main/java/io/vrap/rmf/base/client/oauth2/ClientCredentialsBuilder.java)
If you have custom requirements for the client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final ClientCredentials clientCredentials =
        new ClientCredentialsBuilder()
        .withClientId("client-id")
        .withClientSecret("client-secret")
        .withScopes("scopes")
        .build();
final ProjectApiRoot apiRoot = ClientConfigurationUtils.createClient("project-key", clientCredentials, "auth-url", "api-url");
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [TypeDraft](https://docs.commercetools.com/api/projects/types#typedraft) | `key` |  Also, the types in the target project are expected to have the `key` fields set. | 

#### SyncOptions

After the `projectApiRoot` is set up, a `TypeSyncOptions` should be built as follows:
````java
// instantiating a TypeSyncOptions
final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(projectApiRoot).build();
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
         .of(projectApiRoot)
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
         .of(projectApiRoot)
         .warningCallback((syncException, draft, type) -> 
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
// Example: Ignore update actions that remove field definition
final TriFunction<
        List<TypeUpdateAction>, TypeDraft, Type, List<TypeUpdateAction>> 
            beforeUpdateTypeCallback =
            (updateActions, newTypeDraft, oldType) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof TypeRemoveFieldDefinitionAction))
                    .collect(Collectors.toList());
                        
final TypeSyncOptions typeSyncOptions = 
        TypeSyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateTypeCallback).build();
````

##### beforeCreateCallback
During the sync process, if a type draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * type draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#beforecreatecallback).

##### batchSize
A number that could be used to set the batch size with which types are fetched and processed,
as types are obtained from the target project on commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding types from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for type sync.

````java                         
final TypeSyncOptions typeSyncOptions = 
         TypeSyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for type sync.

````java
final TypeSyncOptions typeSyncOptions = 
         TypeSyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build();
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

2. The `fieldDefinition` with missing `fieldType` (is `null`) will not be synced.
 
#### More examples of how to use the sync
 
 1. [Sync from another CTP project as a source](/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/types/TypeSyncIT.java).
 2. [Sync from an external source](/src/integration-test/java/com/commercetools/sync/integration/externalsource/types/TypeSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a `Type` with a new `TypeDraft` and results in a list of type update actions.
```java
List<TypeUpdateAction> updateActions = TypeSyncUtils.buildActions(type, typeDraft, typeSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `Type` and a new `TypeDraft`, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<TypeUpdateAction> updateAction = TypeUpdateActionUtils.buildChangeNameUpdateAction(oldType, typeDraft);
````
More examples of those utils for different types can be found [here](/src/main/java/com/commercetools/sync/types/utils/TypeUpdateActionUtils.java).

## Migration Guide

The type-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
any HTTP client module. The default one is `commercetools-http-client`.

```xml
 <!-- Sample maven pom.xml -->
 <properties>
     <commercetools.version>LATEST</commercetools.version>
 </properties>

 <dependencies>
     <dependency>
       <groupId>com.commercetools.sdk</groupId>
       <artifactId>commercetools-http-client</artifactId>
       <version>${commercetools.version}</version>
     </dependency>
     <dependency>
       <groupId>com.commercetools.sdk</groupId>
       <artifactId>commercetools-sdk-java-api</artifactId>
       <version>${commercetools.version}</version>
     </dependency>
 </dependencies>

```

### Client configuration and creation

For client creation use [ClientConfigurationUtils](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
If you have custom requirements for the client creation make sure to replace `SphereClientFactory` with `ApiRootBuilder` as described in this [Migration Document](https://docs.commercetools.com/sdk/java-sdk-migrate#client-configuration-and-creation).

### Signature of TypeSyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `TypeDraft`, `Type` and `TypeUpdateAction` from `package com.commercetools.api.models.type.*`

> Note: Type `UpdateAction<Type>` has changed to `TypeUpdateAction`. Make sure you create and supply a specific TypeUpdateAction in `beforeUpdateCallback`. For that you can use the [library-utilities](/src/main/java/com/commercetools/sync/types/utils/TypeSyncUtils.java) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a type update action to change name taking the 'newName' of the typeDraft
    final Function<LocalizedString, TypeUpdateAction> changeNameBeforeUpdateAction =
        (newName) -> TypeChangeNameAction.builder().name(newName).build();

// Add the change name action to the list of update actions before update is executed
    final TriFunction<
            List<TypeUpdateAction>, TypeDraft, Type, List<TypeUpdateAction>>
        beforeUpdateTypeCallback =
            (updateActions, newTypeDraft, oldType) -> {
              final TypeUpdateAction beforeUpdateAction =
                  changeNameBeforeUpdateAction.apply(newTypeDraft.getName());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build TypeDraft (syncing from external project)

The type-sync expects a list of `TypeDraft`s to process. If you use java-sync-library to sync your types from any external system into a commercetools platform project you have to convert your data into CTP compatible `TypeDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// TypeDraftBuilder in v1 takes parameters 'key', 'name' and a set of 'resourceTypeIds'
final TypeDraft typeDraft =
              TypeDraftBuilder
                      .of("type-key", ofEnglish("name"), ResourceTypeIdsSetBuilder.of().addCategories().build())
                      .build();

// TypeDraftBuilder in v2. 'resourceTypeIds' is a list
    TypeDraftBuilder.of()
            .key("type-key")
            .name(LocalizedString.ofEnglish("name"))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for Types (syncing from CTP project)

If you sync types between different commercetools projects you have to transform `Type` into `TypeDraft`.
However, if you need to query `Types` from a commercetools project instead of passing `TypeQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: TypeQuery to fetch all types
final TypeQuery query = TypeQuery.of();

final PagedQueryResult<Type> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all types in one line
final TypePagedQueryResponse result = apiRoot.types().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 
