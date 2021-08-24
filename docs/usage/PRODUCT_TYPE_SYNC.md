# ProductType Sync

The module used for importing/syncing ProductTypes into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ProductType](https://docs.commercetools.com/http-api-projects-productTypes.html#producttype) 
against a [ProductTypeDraft](https://docs.commercetools.com/http-api-projects-productTypes.html#producttypedraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


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
    - [Important to Note](#important-to-note)
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
| [ProductTypeDraft](https://docs.commercetools.com/http-api-projects-productTypes.html#producttypedraft) | `key` |  Also, the product types in the target project are expected to have the `key` fields set. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `attributes` | Only the attributes with type [NestedType](https://docs.commercetools.com/api/projects/productTypes#nestedtype) and [SetType](https://docs.commercetools.com/api/projects/productTypes#settype) with `elementType` as [NestedType](https://docs.commercetools.com/api/projects/productTypes#nestedtype) requires `key` on the `id` field of the `ReferenceType`. | 

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toProductTypeDrafts`](https://commercetools.github.io/commercetools-sync-java/v/7.0.0/com/commercetools/sync/producttypes/utils/ProductTypeTransformUtils.html#toProductTypeDrafts-java.util.List-)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `ProductType` to `ProductTypeDraft`. It can be configured to use a cache that will speed up the reference resolution performed during the sync, for example: 

````java
// Build a ProductTypeQuery for fetching product types from a source CTP project without any references expanded for the sync.
final ProductTypeQuery productTypeQuery = ProductTypeQuery.of();

// Query all productTypes (NOTE this is just for example, please adjust your logic)
final List<ProductType> productTypes =
    CtpQueryUtils
        .queryAll(sphereClient, productTypeQuery, Function.identity())
        .thenApply(fetchedResources -> fetchedResources
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()))
        .toCompletableFuture()
        .join();
````

In order to transform and map the `ProductType` to `ProductTypeDraft`, 
Utils method `toProductTypeDrafts` requires `sphereClient`, implementation of [`ReferenceIdToKeyCache`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `productTypes` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from ProductType to ProductTypeDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<ProductTypeDraft>> productTypeDrafts = ProductTransformUtils.toProductTypeDrafts(client, referenceIdToKeyCache, productTypes);
````

##### Syncing from an external resource

-  Variant attributes with type `NestedType` do not support the `ResourceIdentifier` yet, 
for those references you have to provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the reference should return its `key`. 

````java
final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
    .of(AttributeDefinitionBuilder
        .of("nestedattr", ofEnglish("nestedattr"),
            NestedAttributeType.of(ProductType.referenceOfId("product-type-key"))) // note that key is provided in the id field of reference
        .build())
    .build();

final AttributeDefinitionDraft setOfNestedTypeAttr = AttributeDefinitionDraftBuilder
    .of(AttributeDefinitionBuilder
        .of("setofNestedAttr", ofEnglish("setofNestedAttr"),
            SetAttributeType.of(NestedAttributeType.of(ProductType.referenceOfId("product-type-key"))))
        .build())
    .searchable(false)
    .build();

final ProductTypeDraft productTypeDraft =
    ProductTypeDraftBuilder.of("key", "foo", "description",
        Arrays.asList(nestedTypeAttr, setOfNestedTypeAttr))
                           .build();
````

#### SyncOptions

After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be built as follows:
````java
// instantiating a ProductTypeSyncOptions
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* product type draft from the source
* product type of the target project (only provided if an existing product type could be found)
* the update-actions, which failed (only provided if an existing product type could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductTypeSync.class);
 final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, productType, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* product type draft from the source 
* product type of the target project (only provided if an existing product type could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductTypeSync.class);
 final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, productType, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target product type and a product type draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * product type draft from the source
 * product type from the target project
 * update actions that were calculated after comparing both

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

##### beforeCreateCallback
During the sync process, if a product type draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * product type draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).
 
##### batchSize
A number that could be used to set the batch size with which product types are fetched and processed,
as product types are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding product types from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for product type sync.

````java                         
final ProductTypeSyncOptions productTypeSyncOptions = 
         ProductTypeSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for product type sync.

````java
final ProductTypeSyncOptions productTypeSyncOptions = 
         ProductTypeSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a product type sync
final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions);

// execute the sync on your list of product types
CompletionStage<ProductTypeSyncStatistics> syncStatisticsStage = productTypeSync.sync(productTypeDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `ProductTypeSyncStatistics`
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
