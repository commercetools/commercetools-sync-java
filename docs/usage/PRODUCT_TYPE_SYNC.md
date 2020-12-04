# ProductType Sync

Module used for importing/syncing ProductTypes into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ProductType](https://docs.commercetools.com/http-api-projects-productTypes.html#producttype) 
against a [ProductTypeDraft](https://docs.commercetools.com/http-api-projects-productTypes.html#producttypedraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of product type drafts](#sync-list-of-product-type-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
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
1. Create a `sphereClient`:
Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/3.0.1/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

2. The sync expects a list of `ProductTypeDraft`s that have their `key` fields set to be matched with
product types in the target CTP project. Also, the product types in the target project are expected to have the `key`
fields set, otherwise they won't be matched.

3. Every productType may have `product type` references if it contains attributeDrafts of type `NestedType`. These 
referenced are matched by their `key`s. Therefore, in order for the sync to resolve the actual ids of those 
references, those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`. 
     
        **Note**: When syncing from a source commercetools project, you can use [`mapToProductTypeDrafts`](https://commercetools.github.io/commercetools-sync-java/v/3.0.1/com/commercetools/sync/producttypes/utils/ProductTypeReferenceResolutionUtils.html#mapToProductTypeDrafts-java.util.List-)

         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<ProductTypeDraft> productTypeDrafts = ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(productTypes);
         ````

4. After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be built as follows:
````java
// instantiating a ProductTypeSyncOptions
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient).build();
````

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* product type draft from the source
* product type of the target project (only provided if an existing product type could be found)
* the update-actions, which failed (only provided if an existing product type could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(ProductTypeSync.class);
 final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, productType, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* product type draft from the source 
* product type of the target project (only provided if an existing product type could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(ProductTypeSync.class);
 final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, productType, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target product type and a product type draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * product type draft from the source
 * product type from the target project
 * update actions that were calculated after comparing both

##### Example
````java
final TriFunction<
        List<UpdateAction<ProductType>>, ProductTypeDraft, ProductType, List<UpdateAction<ProductType>>> 
            beforeUpdateProductTypeCallback =
            (updateActions, newProductTypeDraft, oldProductType) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveAttributeDefinition))
                    .collect(Collectors.toList());
                        
final ProductTypeSyncOptions productTypeSyncOptions = 
        ProductTypeSyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateProductTypeCallback).build();
````

##### 4. `beforeCreateCallback`
During the sync process if a product type draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * product type draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).
 
##### 5. `batchSize`
A number that could be used to set the batch size with which product types are fetched and processed,
as product types are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding product types 
from the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for product type sync.
##### Example
````java                         
final ProductTypeSyncOptions productTypeSyncOptions = 
         ProductTypeSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

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
