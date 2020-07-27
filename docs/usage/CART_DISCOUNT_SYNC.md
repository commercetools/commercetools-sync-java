# CartDiscount Sync

Module used for importing/syncing CartDiscounts into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [CartDiscount](https://docs.commercetools.com/http-api-projects-cartDiscounts#cartdiscount) 
against a [CartDiscountDraft](https://docs.commercetools.com/http-api-projects-cartDiscounts#cartdiscountdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of cart discount drafts](#sync-list-of-cart-discount-drafts)
    - [Prerequisites](#prerequisites)
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

2. Every cartDiscount may have a reference to the `Type` of its custom fields. Types are matched by their `key`s. 
Therefore, in order for the sync to resolve the actual ids of those references in the target project, the `key` of the 
`Type` has to be supplied in the following way:
    
    - Provide the `key` **value** on the `id` field of the reference. This means that calling `getId()` on the
    reference should return its `key`.

   **Note**: When syncing from a source commercetools project, you can use this util which this library provides:
    [`replaceCartDiscountsReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/1.9.0/com/commercetools/sync/cartdiscounts/utils/CartDiscountReferenceReplacementUtils.html#replaceCartDiscountsReferenceIdsWithKeys-java.util.List-)
    that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
    ````java
    // Puts the keys in the reference id fields to prepare for reference resolution
    final List<CartDiscountDraft> cartDiscountDrafts = replaceCartDiscountsReferenceIdsWithKeys(cartDiscounts);
    ````

3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is set up, a `CartDiscountSyncOptions` should be be built as follows:
````java
// instantiating a CartDiscountSyncOptions
final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

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
