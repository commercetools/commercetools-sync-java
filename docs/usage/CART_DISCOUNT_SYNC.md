# CartDiscount Sync

Module used for importing/syncing CartDiscounts into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [CartDiscount](https://docs.commercetools.com/http-api-projects-cartDiscounts#cartdiscount) 
against a [CartDiscountDraft](https://docs.commercetools.com/http-api-projects-cartDiscounts#cartdiscountdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of cart discount drafts](#sync-list-of-cart-discount-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Sync list of cart discount drafts

#### Prerequisites
1. The sync expects a list of `CartDiscountDraft`s that have their `key` fields set to be matched with
cart discounts in the target CTP project. Also, the cart discounts in the target project are expected to have the `key`
fields set, otherwise they won't be matched.

2. Every cartDiscount may have a reference to the `Type` of its custom fields. 
Types are matched by their `key`s. Therefore, in order for the sync to resolve the 
actual ids of the type reference, the `key` of the `Type` has to be supplied.

   - When syncing from a source commercetools project, you can use [`mapToCartDiscountDrafts`](https://commercetools.github.io/commercetools-sync-java/v/2.2.1/com/commercetools/sync/cartdiscounts/utils/CartDiscountReferenceResolutionUtils.html#mapToCartDiscountDrafts-java.util.List-)
    method that maps from a `CartDiscount` to `CartDiscountDraft` in order to make them ready for reference resolution by the sync:

    ````java
    final List<CartDiscountDraft> cartDiscountDrafts = CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(cartDiscounts);
    ````

3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is set up, a `CartDiscountSyncOptions` should be built as follows:
````java
// instantiating a CartDiscountSyncOptions
final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(sphereClient).build();
````

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* cart discount draft from the source
* cart discount of the target project (only provided if an existing cart discount could be found)
* the update-actions, which failed (only provided if an existing cart discount could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CartDiscountSync.class);
 final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, cartDiscount, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* cart discount draft from the source 
* cart discount of the target project (only provided if an existing cart discount could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CartDiscountSync.class);
 final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, cartDiscount, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target cart discount and a cart discount draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * cart discount draft from the source
 * cart discount from the target project
 * update actions that were calculated after comparing both

##### Example
````java
final TriFunction<
        List<UpdateAction<CartDiscount>>, CartDiscountDraft, CartDiscount, List<UpdateAction<CartDiscount>>> 
            beforeUpdateCartDiscountCallback =
            (updateActions, newCartDiscountDraft, oldCartDiscount) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof ChangeCartPredicate))
                    .collect(Collectors.toList());
                        
final CartDiscountSyncOptions cartDiscountSyncOptions = 
        CartDiscountSyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateCartDiscountCallback).build();
````

##### 4. `beforeCreateCallback`
During the sync process if a cart discount draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * cart discount draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### 5. `batchSize`
A number that could be used to set the batch size with which cart discounts are fetched and processed,
as cart discounts are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding cart discounts 
from the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for cart discount sync.
##### Example
````java                         
final CartDiscountSyncOptions cartDiscountSyncOptions = 
         CartDiscountSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a cart discount sync
final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

// execute the sync on your list of cart discounts
CompletionStage<CartDiscountSyncStatistics> syncStatisticsStage = cartDiscountSync.sync(cartDiscountDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `CartDiscountSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed cart discounts and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CartDiscountSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 100 cart discounts were processed in total (11 created, 87 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.

 
#### More examples of how to use the sync
 
 1. [Sync from another CTP project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/cartDiscounts/CartDiscountSyncIT.java).
 2. [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/cartDiscounts/CartDiscountSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a `CartDiscount` with a new `CartDiscountDraft` and results in a list of cart discount update actions.
```java
List<UpdateAction<CartDiscount>> updateActions = CartDiscountSyncUtils.buildActions(cartDiscount, cartDiscountDraft, cartDiscountSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `CartDiscount` and a new `CartDiscountDraft`, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<CartDiscount>> updateAction = CartDiscountUpdateActionUtils.buildChangeNameAction(oldCartDiscount, cartDiscountDraft);
````
More examples of those utils for different cart discounts can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtilsTest.java).

## Caveats   
1. Syncing cart discounts with a `CartDiscountValue` of type `giftLineItem` is not supported yet. [#411](https://github.com/commercetools/commercetools-sync-java/issues/411).
