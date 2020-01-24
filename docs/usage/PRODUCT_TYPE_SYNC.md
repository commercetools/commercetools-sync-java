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
    - [Important to Note](#important-to-note)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of product type drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites

1. The sync expects a list of `ProductTypeDraft`s that have their `key` fields set to be matched with
product types in the target CTP project. Also, the product types in the target project are expected to have the `key`
fields set, otherwise they won't be matched.

2. Every productType may have `product type` references if it contains attributeDrafts of type `NestedType`. These 
referenced are matched by their `key`s. Therefore, in order for the sync to resolve the actual ids of those 
references, those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`. 
     
        **Note**: When syncing from a source commercetools project, you can use this util which this library provides: 
         [`replaceProductTypesReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/1.8.0/com/commercetools/sync/producttypes/utils/ProductTypeReferenceReplacementUtils.html#replaceProductTypesReferenceIdsWithKeys-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<ProductTypeDraft> productTypeDrafts = replaceProductTypesReferenceIdsWithKeys(productTypes);
         ````

3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be be built as follows:
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
 
#### Important to Note

1. If two matching `attributeDefinition`s (old and new) on the matching `productType`s (old and new) have a different `AttributeType`, the sync will
**remove** the existing `attributeDefinition` and then **add** a new `attributeDefinition` with the new `AttributeType`.

2. The `attributeDefinition` for which the `AttributeType` is not defined (`null`) will not be synced. 

#### More examples of how to use the sync
 
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
1. The order of attribute definitions in the synced product types is not guaranteed.
2. Syncing product types with an attribute of type `Set` of `NestedType` attribute is supported. However, `Set` of (`Set` of `Set` of..) of `NestedType` is not yet supported.
