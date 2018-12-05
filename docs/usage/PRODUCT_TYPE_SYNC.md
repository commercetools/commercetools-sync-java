# ProductType Sync

Module used for importing/syncing ProductTypes into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ProductType](https://docs.commercetools.com/http-api-projects-productTypes.html#producttype) 
against a [ProductTypeDraft](https://docs.commercetools.com/http-api-projects-productTypes.html#producttypedraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of product type drafts](#sync-list-of-product-type-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of product type drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites

1. The sync expects a list of `ProductTypeDraft`s that have their `key` fields set to be matched with
product types  in the target CTP project. Also, the product types in the target project are expected to have the `key`
fields set, otherwise they won't be matched.

2. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

3. After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be be built as follows:
````java
// instantiating a ProductTypeSyncOptions
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a product type sync
final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

// execute the sync on your list of product types
CompletionStage<ProductTypeSyncStatistics> syncStatisticsStage = productTypeSync.sync(productTypeDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `ProductTypeSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed product types and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final ProductTypeSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 2000 products types were processed in total (1000 created, 995 updated, 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:
 
 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.

##### More examples of how to use the sync
 
 1. [Sync from another CTP project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/producttypes/ProductTypeSyncIT.java).
 2. [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/producttypes/ProductTypeSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a ProductType with a new ProductTypeDraft and results in a list of product type update actions.
```java
List<UpdateAction<ProductType>> updateActions = ProductTypeSyncUtils.buildActions(productType, productTypeDraft, productTypeSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a ProductType and a new ProductTypeDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<ProductType>> updateAction = ProductTypeUpdateActionUtils.buildChangeNameAction(oldProductType, productTypeDraft);
````
More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtilsTest.java).


## Caveats

1. Product types are either created or updated. Currently the tool does not support product type deletion.
2. The sync library is not meant to be executed in a parallel fashion. Check the example in [point #2 here](/docs/usage/PRODUCT_SYNC.md#caveats).
    By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
     - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
     - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch contain.
     
    The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
    is the recommended good balance for stability and performance for the sync process.
    
    In order to exploit the number of `max parallel requests`, the `batch size` should have a value set which is equal or higher.
    
3. Syncing product types with an attribute of type [NestedType](https://docs.commercetools.com/http-api-projects-productTypes.html#nestedtype) is not supported yet.
