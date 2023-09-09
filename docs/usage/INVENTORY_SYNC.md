# InventoryEntry Sync

The module used for importing/syncing InventoryEntries into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [InventoryEntry](https://docs.commercetools.com/api/projects/inventory#inventoryentry) 
against a [InventoryEntryDraft](https://docs.commercetools.com/api/projects/inventory#inventoryentrydraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Prerequisites](#prerequisites)
    - [ProjectApiRoot](#projectapiroot)
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
      - [ensureChannels](#ensurechannels)
  - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Migration Guide](#migration-guide)
  - [Client configuration and creation](#client-configuration-and-creation)
  - [Signature of InventorySyncOptions](#signature-of-inventorysyncoptions)
  - [Build InventoryEntryDraft (syncing from external project)](#build-inventoryentrydraft-syncing-from-external-project)
  - [Query for Inventories (syncing from CTP project)](#query-for-inventories-syncing-from-ctp-project)
  - [JVM-SDK-V2 migration guide](#jvm-sdk-v2-migration-guide)

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
| [InventoryEntryDraft](https://docs.commercetools.com/api/projects/inventory#inventoryentrydraft) | `sku` |  Also, the inventory entries in the target project are expected to have the `sku` fields set. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#ctp:api:type:ResourceIdentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `supplyChannel` | [ChannelResourceIdentifier](https://docs.commercetools.com/api/projects/channels#ctp:api:type:ChannelResourceIdentifier)| 
| `custom.type` | [TypeResourceIdentifier](https://docs.commercetools.com/api/projects/types#ctp:api:type:TypeResourceIdentifier) |  

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toInventoryEntryDrafts`](/src/main/java/com/commercetools/sync/inventories/utils/InventoryTransformUtils.java)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `InventoryEntry` to `InventoryEntryDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build an ByProjectKeyInventoryGet for fetching inventories from a source CTP project without any references expanded for the sync:
final ByProjectKeyInventoryEntriesGet inventoryEntryQuery = client.inventory().get();

// Query all inventories (NOTE this is just for example, please adjust your logic)
final List<InventoryEntry> categories = QueryUtils.queryAll(inventoryEntryQuery,
            (inventories) -> inventories)
            .thenApply(lists -> lists.stream().flatMap(List::stream).collect(Collectors.toList()))
            .toCompletableFuture()
            .join();
````

In order to transform and map the `InventoryEntry` to `InventoryEntryDraft`, 
Utils method `toInventoryEntryDrafts` requires `projectApiRoot`, implementation of [`ReferenceIdToKeyCache`](/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `inventoryEntries` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from InventoryEntry to InventoryEntryDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<InventoryEntryDraft>> inventoryEntryDrafts = InventoryTransformUtils.toInventoryEntryDrafts(client, referenceIdToKeyCache, inventoryEntries);
````

##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

````java
final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraftBuilder
        .of()
        .sku("sku1")
        .quantityOnStock(1L)
        .custom(CustomFieldsDraftBuilder.of().type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("type-key")).fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap())).build())) // note that custom type provided with key
        .supplyChannel(ChannelResourceIdentifierBuilder.of().key("channel-key").build()) // note that channel reference provided with key
        .build();
````

#### SyncOptions
After the `projectApiRoot` is set up, an `InventorySyncOptions` should be built as follows: 
````java
// instantiating a InventorySyncOptions
final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder.of(projectApiRoot).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* inventory entry draft from the source
* inventory entry of the target project (only provided if an existing inventory entry could be found)
* the update-actions, which failed (only provided if an existing inventory entry could be found)

````java
 final Logger logger = LoggerFactory.getLogger(InventorySync.class);
 final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
         .of(projectApiRoot)
         .errorCallback((syncException, draft, inventoryEntry, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* inventory entry draft from the source 
* inventory entry of the target project (only provided if an existing inventory entry could be found)

````java
 final Logger logger = LoggerFactory.getLogger(InventorySync.class);
 final InventorySyncOptions inventorySyncOptions = InventorySyncOptionsBuilder
         .of(projectApiRoot)
         .warningCallback((syncException, draft, inventoryEntry) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target inventory entry and an inventory entry draft are matched, this callback can be used 
to intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * inventory entry draft from the source
 * inventory from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<InventoryEntryUpdateAction>, 
        InventoryEntryDraft, 
        InventoryEntry, 
        List<InventoryEntryUpdateAction>> beforeUpdateInventoryCallback =
            (updateActions, newInventoryEntryDraft, oldInventoryEntry) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof InventoryEntryRemoveQuantityAction))
                    .collect(Collectors.toList());
                        
final InventorySyncOptions inventorySyncOptions = 
        InventorySyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateInventoryCallback).build();
````

##### beforeCreateCallback
During the sync process, if an inventory entry draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * inventory entry draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).
                         
##### batchSize
A number that could be used to set the batch size with which inventories are fetched and processed,
as inventories are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding inventories from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 150 for inventory sync.

````java                         
final InventorySyncOptions inventorySyncOptions = 
         InventorySyncOptionsBuilder.of(projectApiRoot).batchSize(100).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for inventory sync.

````java
final InventorySyncOptions inventorySyncOptions = 
         InventorySyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build();
````


##### ensureChannels
A flag to indicate whether the sync process should create a supply channel of the given key when it doesn't exist in a 
target project yet.
- If `ensureChannels` is set to `false` this inventory won't be synced and the `errorCallback` will be triggered.
- If `ensureChannels` is set to `true` the sync will attempt to create the missing supply channel with the given key. 
If it fails to create the supply channel, the inventory won't sync and `errorCallback` will be triggered.
- If not provided, it is set to `false` by default.

````java                         
final InventorySyncOptions inventorySyncOptions = 
         InventorySyncOptionsBuilder.of(projectApiRoot).ensureChannels(true).build();
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating an inventory sync
final InventorySync inventorySync = new InventorySync(inventorySyncOptions);

// execute the sync on your list of inventories
CompletionStage<InventorySyncStatistics> syncStatisticsStage = inventorySync.sync(inventoryEntryDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `InventorySyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed inventories and the processing time of the sync in different time units and in a
human-readable format.
````java
final InventorySyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*"Summary: 25 inventory entries were processed in total (9 created, 5 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.


More examples of how to use the sync can be found [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/inventories/InventorySyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*


### Build all update actions

A utility method provided by the library to compare an InventoryEntry with a new InventoryEntryDraft and results in a list of InventoryEntry
 update actions. 
```java
List<InventoryEntryUpdateAction> updateActions = InventorySyncUtils.buildActions(oldEntry, newEntry, inventorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/inventories/utils/InventorySyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of an InventoryEntry and a new InventoryEntryDraft, and in turn builds
 the update action. One example is the `buildChangeQuantityAction` which compares quantities:
  
````java
Optional<InventoryEntryUpdateAction> updateAction = buildChangeQuantityAction(oldEntry, newEntry);
````

## Migration Guide

The inventory-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
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

### Signature of InventorySyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `InventoryEntryDraft`, `InventoryEntry` and `InventoryEntryUpdateAction` from `package com.commercetools.api.models.inventory.*`

> Note: Type `UpdateAction<InventoryEntry>` has changed to `InventoryEntryUpdateAction`. Make sure you create and supply a specific InventoryEntryUpdateAction in `beforeUpdateCallback`. For that you can use the [library-utilities](/src/main/java/com/commercetools/sync/inventories/utils/InventorySyncUtils.java) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a inventory update action to change quantity taking the 'newQuantity' of the InventoryEntryDraft
    final Function<Long, InventoryEntryUpdateAction> createBeforeUpdateAction =
        (newQuantity) -> InventoryEntryChangeQuantityAction.builder().quantity(newQuantity).build();

// Add the change quantity action to the list of update actions before update is executed
    final TriFunction<
            List<InventoryEntryUpdateAction>, InventoryEntryDraft, InventoryEntry, List<InventoryEntryUpdateAction>>
        beforeUpdateInventoryCallback =
            (updateActions, newInventoryEntryDraft, oldInventoryEntry) -> {
              final InventoryEntryUpdateAction beforeUpdateAction =
                  createBeforeUpdateAction.apply(newInventoryDraft.getQuantity());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build InventoryEntryDraft (syncing from external project)

The inventory-sync expects a list of `InventoryEntryDraft`s to process. If you use java-sync-library to sync your inventories from any external system into a commercetools platform project you have to convert your data into CTP compatible `InventoryEntryDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// InventoryEntryDraftBuilder in v1 takes parameters 'sku' and 'quantityOnStock'
final InventoryEntryDraft inventoryEntryDraft =
              InventoryEntryDraftBuilder
                      .of("sku", 10L)
                      .restockableInDays(10) //Note: Field 'restockableInDays' is of type Integer
                      .build();

// InventoryEntryDraftBuilder in v2
final InventoryEntryDraft inventoryEntryDraft =
              InventoryEntryDraftBuilder
                      .of()
                      .sku("sku")
                      .quantityOnStock(10L)
                      .restockableInDays(10L) //Note: Field 'restockableInDays' is of type Long
                      .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for Inventories (syncing from CTP project)

If you sync inventories between different commercetools projects you probably use [InventoryTransformUtils#toInventoryEntryDrafts](/src/main/java/com/commercetools/sync/inventories/utils/InventoryTransformUtils.java) to transform `InventoryEntry` into `InventoryEntryDraft` which can be used by the inventory-sync.
However, if you need to query `Inventories` from a commercetools project instead of passing `InventoryEntryQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: InventoryEntryQuery to fetch all inventories
final InventoryEntryQuery query = InventoryEntryQuery.of();

final PagedQueryResult<InventoryEntry> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all inventories in one line
final InventoryEntryPagedQueryResponse result = apiRoot.inventory().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

> Note: If you use predicates to query resources please keep in mind the URI length is limited to ~8kB. Therefore, consider limiting the query-predicates (max. 10.000 characters), because above this size it could return - Error 414 (Request-URI Too Large).

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 

