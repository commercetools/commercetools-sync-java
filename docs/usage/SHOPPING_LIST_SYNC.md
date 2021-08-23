# Shopping List Sync

The module used for importing/syncing Shopping Lists into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ShoppingList](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglist) 
against a [ShoppingListDraft](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglistdraft).


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  

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
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Usage

### Prerequisites
#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/7.0.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [ShoppingListDraft](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglistdraft) | `key` |  Also, the shopping lists in the target project are expected to have the `key` fields set. | 
| [LineItemDraft](https://docs.commercetools.com/api/projects/shoppingLists#lineitemdraft) | `sku` |  Also, all the line items in the target project are expected to have the `sku` fields set. | 
| [TextLineItemDraft](https://docs.commercetools.com/api/projects/shoppingLists#textlineitem) | `name` |   | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `customer` | ResourceIdentifier to a Customer | 
| `custom.type` | ResourceIdentifier to a Type |  
| `lineItems.custom.type` |  ResourceIdentifier to a Type | 
| `textLineItems.custom.type ` | ResourceIdentifier to a Type | 

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toShoppingListDrafts`](https://commercetools.github.io/commercetools-sync-java/v/7.0.0/com/commercetools/sync/shoppinglists/utils/ShoppingListTransformUtils.html#toShoppingListDrafts-java.util.List-)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `ShoppingList` to `ShoppingListDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build a ShoppingListQuery for fetching shopping lists from a source CTP project without any references expanded for the sync:
final ShoppingListQuery shoppingListQuery = ShoppingListReferenceResolutionUtils.buildShoppingListQuery();

// Query all shopping lists (NOTE this is just for example, please adjust your logic)
final List<ShoppingList> shoppingLists =
    CtpQueryUtils
        .queryAll(sphereClient, shoppingListQuery, Function.identity())
        .thenApply(fetchedResources -> fetchedResources
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()))
        .toCompletableFuture()
        .join();
````

In order to transform and map the `ShoppingList` to `ShoppingListDraft`, 
Utils method `toShoppingListDrafts` requires `sphereClient`, implementation of [`ReferenceIdToKeyCache`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `shoppingLists` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from ShoppingList to ShoppingListDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<ShoppingListDraft>> shoppingListDrafts = ShoppingListTransformUtils.toShoppingListDrafts(client, referenceIdToKeyCache, shoppingLists);
````

##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

````java
final ShoppingListDraft shoppingListDraft =
    ShoppingListDraftBuilder
        .of(LocalizedString.ofEnglish("name"))
        .key("shopping-list-key")
        .customer(ResourceIdentifier.ofKey("customer-key")) // note that customer provided with key
        .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
        .lineItems(singletonList(LineItemDraftBuilder
            .ofSku("SKU-1", 1L) // note that sku field is set.
            .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
            .build()))
        .textLineItems(singletonList(
            TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("name"), 1L) // note that name field is set for text line item.
              .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
              .build()))
        .build();
````

#### SyncOptions

After the `sphereClient` is set up, a `ShoppingListSyncOptions` should be built as follows:
````java
// instantiating a ShoppingListSyncOptions
final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* shopping list draft from the source
* shopping list of the target project (only provided if an existing shopping list could be found)
* the update-actions, which failed (only provided if an existing shopping list could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ShoppingListSync.class);
 final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, shoppingList, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* shopping list draft from the source 
* shopping list of the target project (only provided if an existing shopping list could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ShoppingListSync.class);
 final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, shoppingList, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target customer and a customer draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * shopping list draft from the source
 * shopping list from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<List<UpdateAction<ShoppingList>>, ShoppingListDraft, ShoppingList,
            List<UpdateAction<ShoppingList>>> beforeUpdateCallback =
                (updateActions, newShoppingList, oldShoppingList) ->  updateActions
                    .stream()
                    .filter(updateAction -> !(updateAction instanceof SetSlug))
                    .collect(Collectors.toList());
                        
final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
                    .of(CTP_CLIENT)
                    .beforeUpdateCallback(beforeUpdateCallback)
                    .build();
````

##### beforeCreateCallback
During the sync process, if a shopping list draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * shopping list that should be created

 Please refer to the [example in the product sync document](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/usage/PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### batchSize
A number that could be used to set the batch size with which shopping lists are fetched and processed,
as shopping lists are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding shopping lists
from the target project on the commercetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for shopping list sync.

````java                         
final ShoppingListSyncOptions shoppingListSyncOptions = 
         ShoppingListSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for shopping list sync.

````java
final ShoppingListSyncOptions shoppingListSyncOptions = 
         ShoppingListSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````

### Running the sync
When all prerequisites are fulfilled, follow these steps to run the sync:

````java
// instantiating a shopping list sync
final ShoppingListSync shoppingListSync = new ShoppingListSync(shoppingListSyncOptions);

// execute the sync on your list of shopping lists
CompletionStage<ShoppingListSyncStatistics> syncStatisticsStage = shoppingListSync.sync(shoppingListDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `ShoppingListSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed shopping lists, and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final ShoppingListSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 100 shopping lists were processed in total (11 created, 87 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.
 
#### More examples of how to use the sync
 
  [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/blob/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/shoppinglists/ShoppingListSyncIT.java).
 
 *Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*
 
### Build all update actions
 A utility method provided by the library to compare a `ShoppingList` to a new `ShoppingListDraft`. The results are collected in a list of shopping list update actions.
 ```java
 List<UpdateAction<ShoppingList>> updateActions = ShoppingListSyncUtils.buildActions(shoppingList, shoppingListDraft, shoppingListSyncOptions);
 ```
 
### Build particular update action(s)
 The library provides utility methods to compare specific fields of a `ShoppingList` and a new `ShoppingListDraft`, and builds the update action(s) as a result.
 One example is the `buildChangeNameUpdateAction` which compare shopping list names:
 ````java
 Optional<UpdateAction<ShoppingList>> updateAction = ShoppingListUpdateActionUtils.buildChangeNameAction(shoppingList, shoppingListDraft);
 ````
 
 More examples for particular update actions can be found in the test scenarios for [ShoppingListUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListUpdateActionUtils.java).
 
 
## Caveats

In commercetools shopping lists API, there is no update action to change the `addedAt` field of the `LineItem` and `TextLineItem`, 
hereby commercetools-java-sync library will not update the `addedAt` value. 
> For the new LineItem and TextLineItem the `addedAt` values will be added, if the draft has the value set.
