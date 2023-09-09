# Custom Object Sync

Module used for importing/syncing CustomObject into a commercetools project. 
It also provides utilities for correlating a custom object to a given custom object draft based on the 
comparison of a [CustomObject](https://docs.commercetools.com/api/projects/custom-objects#customobject) 
against a [CustomObjectDraft](https://docs.commercetools.com/api/projects/custom-objects#customobjectdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Prerequisites](#prerequisites)
    - [ProjectApiRoot](#projectapiroot)
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
- [Migration Guide](#migration-guide)
  - [Client configuration and creation](#client-configuration-and-creation)
  - [Signature of CustomObjectSyncOptions](#signature-of-customobjectsyncoptions)
  - [Build CustomObjectDraft (syncing from external project)](#build-customobjectdraft-syncing-from-external-project)
  - [Query for CustomObjects](#query-for-customobjects)
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
| [CustomObjectDraft](https://docs.commercetools.com/api/projects/custom-objects#customobjectdraft) | `key` |  Also, the custom objects in the target project are expected to have the `key` fields set. | 
| [CustomObjectDraft](https://docs.commercetools.com/api/projects/custom-objects#customobjectdraft) | `container` |  Also, the custom objects in the target project are expected to have the `container` fields set. | 

####  SyncOptions

After the `projectApiRoot` is set up, a `CustomObjectSyncOptions` should be built as follows:
````java
// instantiating a CustomObjectSyncOptions
final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder.of(projectApiRoot).build();
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
* a fake list of update actions, as custom objects API does not provide update actions. [NoopResourceUpdateAction.java](/src/main/java/com/commercetools/sync/customobjects/models/NoopResourceUpdateAction.java)

````java
 final Logger logger = LoggerFactory.getLogger(CustomObjectSync.class);
 final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
         .of(projectApiRoot)
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
         .of(projectApiRoot)
         .warningCallback((syncException, draft, customObject) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
In theory, `CustomObjectSyncOptions` provides callback before update operation. User can customize their own callback and inject
into sync options. However, in the actual case, `beforeUpdateCallback`is not triggered in the custom object sync process. When
the new custom object draft has the same key and container as an existing custom object but different in custom object values, 
the sync process automatically performs the create/update operation. The value of a corresponding custom object in the target project is overwritten. This approach is different from other resources and no update action is involved.

Also see the API documentation of [Create-or-update-customobject](https://docs.commercetools.com/api/projects/custom-objects#create-or-update-customobject)

##### beforeCreateCallback
During the sync process, if a custom object draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * custom object draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#beforecreatecallback).

##### batchSize
A number that could be used to set the batch size with which custom objects are fetched and processed,
as custom objects are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding custom objects from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for custom object sync.

````java                         
final CustomObjectSyncOptions customObjectSyncOptions = 
         CustomObjectSyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for custom object sync.

````java
final CustomObjectSyncOptions customObjectSyncOptions = 
         CustomObjectSyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build();
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
 
- [Sync from an external source](/src/integration-test/java/com/commercetools/sync/integration/externalsource/customobjects/CustomObjectSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

## Migration Guide

The custom-object-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
any HTTP client module. The default one is `commercetools-http-client`.

```maven
 // Sample maven pom.xml
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

### Signature of CustomObjectSyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `CustomObjectDraft`, `CustomObject` (without type parameter as in v1) from `package com.commercetools.api.models.custom_object.*` and `NoopResourceUpdateAction` which is a fake class representing resource without update actions like custom-object.

> Note: Make sure `beforeUpdateCallback` isn't used as the sync will **not** trigger it in the process.
> Note: Further make sure on `errorCallback` to not operate on`NoopResourceUpdateAction`'s actions field as it is null.

### Build CustomObjectDraft (syncing from external project)

The custom-object-sync expects a list of `CustomObjectDraft`s to process. To sync your categories from anywhere (including other CTP project) into a commercetools platform project you have to convert your data into CTP compatible `CategoryDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

> Note: In v1 the value in CustomObjectDraft is of generic type and custom-object-sync was expecting `JsonNode` as value. This changed in V2 SDK and the sync-library, and value field is of type `Object` now.
```java
// CategoryDraft builder in v1 takes parameters 'container', 'key' and 'value'
final CustomObjectDraft cutomObjectDraft =
            CustomObjectDraft.ofUnversionedUpsert(
            "someContainer",
            "someKey",
            JsonNodeFactory.instance.objectNode().put("json-field", "json-value"));

// CustomObjectDraftBuilder in v2
final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value("someValue")
            .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for CustomObjects

If you need to query `CustomObjects` from a commercetools project instead of passing `CustomObjectQuery<T>`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: CategoryQuery to fetch all categories
final CustomObjectQuery<JsonNode> query = CustomObjectQuery.ofJsonNode();

final PagedQueryResult<CustomObject<JsonNode>> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all custom objects in one line
final CustomObjectPagedQueryResponse result = apiRoot.customObjects().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 

