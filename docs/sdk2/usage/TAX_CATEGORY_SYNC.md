# Tax Category Sync

Allows importing/syncing TaxCategory into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [TaxCategory](https://docs.commercetools.com/api/projects/taxCategories#taxcategory) 
against a [TaxCategoryDraft](https://docs.commercetools.com/api/projects/taxCategories#taxcategorydraft).

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
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Migration Guide](#migration-guide)
  - [Client configuration and creation](#client-configuration-and-creation)
  - [Signature of TaxCategorySyncOptions](#signature-of-taxcategorysyncoptions)
  - [Build TaxCategoryDraft (syncing from external project)](#build-taxcategorydraft-syncing-from-external-project)
  - [Query for TaxCategories (syncing from CTP project)](#query-for-taxcategories-syncing-from-ctp-project)
  - [JVM-SDK-V2 migration guide](#jvm-sdk-v2-migration-guide)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Prerequisites

#### ProjectApiRoot

Use the [ClientConfigurationUtils](#todo) which apply the best practices for `ProjectApiRoot` creation.
To create `ClientCredentials` which are required for creating a client please use the `ClientCredentialsBuilder` provided in java-sdk-v2 [Client OAUTH2 package](#todo)
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
| [TaxCategoryDraft](https://docs.commercetools.com/api/projects/taxCategories#taxcategorydraft) | `key` |  Also, the tax categories in the target project are expected to have the `key` fields set. | 

#### SyncOptions

After the `projectApiRoot` is set up, a `TaxCategorySyncOptions` should be built as follows:
````java
// instantiating a TaxCategorySyncOptions
final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder.of(projectApiRoot).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* tax category draft from the source
* tax category of the target project (only provided if an existing tax category could be found)
* the update-actions, which failed (only provided if an existing tax category could be found)

````java
 final Logger logger = LoggerFactory.getLogger(TaxCategorySync.class);
 final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
         .of(projectApiRoot)
         .errorCallback((syncException, draft, taxCategory, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* tax category draft from the source 
* tax category of the target project (only provided if an existing tax category could be found)

````java
 final Logger logger = LoggerFactory.getLogger(TaxCategorySync.class);
 final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, taxCategory) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target tax category and a tax category draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * tax category draft from the source
 * tax category from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<TaxCategoryUpdateAction>, TaxCategoryDraft, TaxCategory, List<TaxCategoryUpdateAction>> 
            beforeUpdateTaxCategoryCallback =
            (updateActions, newTaxCategoryDraft, oldTaxCategory) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof TaxCategoryRemoveTaxRateAction))
                    .collect(Collectors.toList());
                        
final TaxCategorySyncOptions taxCategorySyncOptions = 
        TaxCategorySyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateTaxCategoryCallback).build();
````

##### beforeCreateCallback
During the sync process, if a tax category draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * tax category draft that should be created

Please refer to [example in product sync document](PRODUCT_SYNC.md#beforecreatecallback).

##### batchSize
A number that could be used to set the batch size with which tax categories are fetched and processed,
as tax categories are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding tax categories from the target project on commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for tax category sync.

````java                         
final TaxCategorySyncOptions taxCategorySyncOptions = 
         TaxCategorySyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for tax category sync.

````java                         
final TaxCategorySyncOptions taxCategorySyncOptions = 
         TaxCategorySyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build();
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a TaxCategorySync
final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

// execute the sync on your list of tax categories
CompletionStage<TaxCategorySyncStatistics> syncStatisticsStage = taxCategorySync.sync(taxCategoryDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `TaxCategorySyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed tax categories and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final TaxCategorySyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 2000 tax categories were processed in total (1000 created, 995 updated, 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.
  
#### More examples of how to use the sync
 
- [Sync from an external source](#todo).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a `TaxCategory` with a new `TaxCategoryDraft` and results in a list of tax category update actions.
```java
List<TaxCategoryUpdateAction> updateActions = TaxCategorySyncUtils.buildActions(taxCategory, taxCategoryDraft, taxCategorySyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `TaxCategory` and a new `TaxCategoryDraft`, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<TaxCategoryUpdateAction> updateAction = TaxCategoryUpdateActionUtils.buildChangeNameAction(oldTaxCategory, taxCategoryDraft);
````
More examples of those utils for different tax categories can be found [here](#todo).

## Migration Guide

The tax-category-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
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

For client creation use [ClientConfigurationUtils](#todo) which apply the best practices for `ProjectApiRoot` creation.
If you have custom requirements for the client creation make sure to replace `SphereClientFactory` with `ApiRootBuilder` as described in this [Migration Document](https://docs.commercetools.com/sdk/java-sdk-migrate#client-configuration-and-creation).

### Signature of TaxCategorySyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `TaxCategoryDraft`, `TaxCategory` and `TaxCategoryUpdateAction` from `package com.commercetools.api.models.tax_category.*`

> Note: Type `UpdateAction<TaxCategory>` has changed to `TaxCategoryUpdateAction`. Make sure you create and supply a specific TaxCategoryUpdateAction in `beforeUpdateCallback`. For that you can use the [library-utilities](#todo) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a tax-category update action to change name taking the 'newName' of the taxCategoryDraft
    final Function<String, TaxCategoryUpdateAction> changeNameBeforeUpdateAction =
        (newName) -> TaxCategoryChangeNameAction.builder().name(newName).build();

// Add the change name action to the list of update actions before update is executed
    final TriFunction<
            List<TaxCategoryUpdateAction>, TaxCategoryDraft, TaxCategory, List<TaxCategoryUpdateAction>>
        beforeUpdateTypeCallback =
            (updateActions, newTaxCategoryDraft, oldTaxCategory) -> {
              final TaxCategoryUpdateAction beforeUpdateAction =
                  changeNameBeforeUpdateAction.apply(newTaxCategoryDraft.getName());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build TaxCategoryDraft (syncing from external project)

The tax-category-sync expects a list of `TaxCategoryDraft`s to process. If you use java-sync-library to sync your types from any external system into a commercetools platform project you have to convert your data into CTP compatible `TaxCategoryDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// TaxCategoryDraftBuilder in v1 takes parameters 'name', 'taxRates' and 'description'
final TaxCategoryDraft taxCategoryDraft =
              TaxCategoryDraftBuilder
                      .of("name", emptyList(), "description")
                      .key("key")
                      .build();

// TaxCategoryDraftBuilder in v2.
final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .key("key")
            .name("name")
            .rates(emptyList())
            .description("description")
            .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for TaxCategories (syncing from CTP project)

If you sync types between different commercetools projects you have to transform `TaxCategory` into `TaxCategoryDraft`.
However, if you need to query `TaxCategory` from a commercetools project instead of passing `TaxCategoryQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: TaxCategoryQuery to fetch all taxCategories
final TaxCategoryQuery query = TaxCategoryQuery.of();

final PagedQueryResult<TaxCategory> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all tax-categories in one line
final TaxCategoryPagedQueryResponse result = apiRoot.taxCategories().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 
