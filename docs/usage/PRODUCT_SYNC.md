# Product Sync

Module used for importing/syncing Products into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Product](https://docs.commercetools.com/http-api-projects-products.html#product) 
against a [ProductDraft](https://docs.commercetools.com/http-api-projects-products.html#productdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of product drafts](#sync-list-of-product-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of product drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites
1. The sync expects a list of `ProductDraft`s that have their `key` fields set to be matched with
products in the target CTP project. Also, the products in the target project are expected to have the `key` fields set,
otherwise they won't be matched.

2. The sync expects all variants of the supplied list of `ProductDraft`s to have their `sku` fields set. Also,
all the variants in the target project are expected to have the `sku` fields set.

3. Every product may have several references including `product type`, `categories`, `taxCategory`, etc.. Variants
of the product also have prices, where each price also has some references including a reference to the `Type` of its 
custom fields and a reference to a `channel`. All these referenced resources are matched by their `key`s. Therefore, in 
order for the sync to resolve the actual ids of those references, those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`. 
     
        **Note**: When syncing from a source commercetools project, you can use this util which this library provides: 
         [`replaceProductsReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/1.4.0/com/commercetools/sync/products/utils/ProductReferenceReplacementUtils.html#replaceProductsReferenceIdsWithKeys-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);
         ````
     
4. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

5. After the `sphereClient` is set up, a `ProductSyncOptions` should be built as follows: 
````java
// instantiating a ProductSyncOptions
final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(sphereClient).build();
````
[More information about Sync Options](SYNC_OPTIONS.md). 

#### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a product sync
final ProductSync productSync = new ProductSync(productSyncOptions);

// execute the sync on your list of products
CompletionStage<ProductSyncStatistics> syncStatisticsStage = productSync.sync(productDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `ProductSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed products and the processing time of the sync in different time units and in a
human-readable format.
````java
final ProductSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*"Summary: 2000 products were processed in total (1000 created, 995 updated and 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.


##### More examples of how to use the sync

1. [Sync from another CTP project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/ProductSyncIT.java).
2. [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncIT.java). 
3. [Sync with blacklisting/whitelisting](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a Product with a new ProductDraft and results in a list of product
 update actions. 
```java
List<UpdateAction<Product>> updateActions = ProductSyncUtils.buildActions(product, productDraft, productSyncOptions, attributesMetaData);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/products/utils/ProductSyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a Product and a new ProductDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
  
````java
Optional<UpdateAction<Product>> updateAction = buildChangeNameUpdateAction(oldProduct, productDraft);
````
More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/utils).

## Caveats
1. Syncing attribute field types with  `ReferenceType` and `SetType` (of `elementType: ReferenceType`) field definitions, except 
for Product references, is not supported yet. (See more: [#87](https://github.com/commercetools/commercetools-sync-java/issues/87) [#160](https://github.com/commercetools/commercetools-sync-java/issues/87)). This
also applies to an attribute field of type `NestedType` which has any of the aforementioned types as an inner field.


