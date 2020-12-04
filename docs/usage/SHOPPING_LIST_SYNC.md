# Shopping List Sync

The module used for importing/syncing Shopping Lists into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ShoppingList](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglist) 
against a [ShoppingListDraft](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglistdraft).


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  

- [Usage](#usage)
  - [Sync list of Shopping List Drafts](#sync-list-of-shopping-list-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Usage

### Sync list of Shopping List Drafts

#### Prerequisites
1. Create a `sphereClient`:
Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

2. The sync expects a list of `ShoppingList`s that have their `key` fields set to be matched with shopping lists in the 
target CTP project. The shopping lists in the target project need to have the `key` fields set, otherwise they won't be 
matched.

3. The sync expects all variants of the supplied list of `LineItemDraft`s to have their `sku` fields set. Also,
all the variants in the target project are expected to have the `sku` fields set.

4. Every shopping list may have several references including `customer` and their shopping list `type`, line item `type` and text line item `type` with custom fields etc.
All these referenced resources are matched by their `key`s. 
Any reference that is not expanded will have its id in place and not replaced by the key will be considered as existing 
resources on the target commercetools project and the library will issue an update/create an API request without reference
resolution. Therefore, in order for the sync to resolve the actual ids of those references, those `key`s have to be supplied in the following way:

     - When syncing from a source commercetools project, you can use [`mapToShoppingListDraft`](https://commercetools.github.io/commercetools-sync-java/v/3.0.1/com/commercetools/sync/shoppinglists/utils/ShoppingListReferenceResolutionUtils.html#mapToShoppingListDrafts-java.util.List-)
    method that maps from a `ShoppingList` to `ShoppingListDraft` to make them ready for reference resolution by the shopping list sync:
    
    ````java
    final List<ShoppingListDraft> shoppingListDrafts = ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(shoppingLists);
    ````

5. After the `sphereClient` is set up, a `ShoppingListSyncOptions` should be built as follows:
````java
// instantiating a ShoppingListSyncOptions
final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder.of(sphereClient).build();
````

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* shopping list draft from the source
* shopping list of the target project (only provided if an existing shopping list could be found)
* the update-actions, which failed (only provided if an existing shopping list could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(ShoppingListSync.class);
 final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, shoppingList, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* shopping list draft from the source 
* shopping list of the target project (only provided if an existing shopping list could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(ShoppingListSync.class);
 final ShoppingListSyncOptions shoppingListSyncOptions = ShoppingListSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, shoppingList, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target customer and a customer draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * shopping list draft from the source
 * shopping list from the target project
 * update actions that were calculated after comparing both

##### Example
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

##### 4. `beforeCreateCallback`
During the sync process if a shopping list draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * shopping list that should be created
 ##### Example
 Please refer to the [example in the product sync document](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/usage/PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### 5. `batchSize`
A number that could be used to set the batch size with which shopping lists are fetched and processed,
as shopping lists are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding shopping lists
from the target project on commercetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for shopping list sync.
##### Example
````java                         
final ShoppingListSyncOptions shoppingListSyncOptions = 
         ShoppingListSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

#### Running the sync
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
