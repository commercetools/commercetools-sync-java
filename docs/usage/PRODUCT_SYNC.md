# Product Sync

The module used for importing/syncing Products into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Product](https://docs.commercetools.com/http-api-projects-products.html#product) 
against a [ProductDraft](https://docs.commercetools.com/http-api-projects-products.html#productdraft).

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
      - [syncFilter](#syncfilter)
      - [ensureChannels](#ensurechannels)
  - [Running the sync](#running-the-sync)
      - [Persistence of ProductDrafts with Irresolvable References](#persistence-of-productdrafts-with-irresolvable-references)
      - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites

#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/3.2.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| `ProductDraft` | `key` |  Also, the products in the target project are expected to have the `key` fields set. | 
| `ProductVariantDraft`  | `key`, `sku` |  Also, all the variants in the target project are expected to have the `key` and `sku` fields set. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `productType` - **Required** | ResourceIdentifier to a ProductType | 
| `categories` | Set of ResourceIdentifier for a Category |
| `taxCategory` | ResourceIdentifier to a TaxCategory | 
| `state` | ResourceIdentifier for a State | Optional |
| `variants.prices.channel` | ResourceIdentifier to a Channel | 
| `variants.prices.customerGroup` | ResourceIdentifier to a CustomerGroup | 
| `variants.prices.custom.type` | ResourceIdentifier to a Type | 
| `variants.assets.custom.type` | ResourceIdentifier to a Type | 
| `variants.attributes` * | Only the attributes with type [ReferenceType](https://docs.commercetools.com/api/projects/productTypes#referencetype), [SetType](https://docs.commercetools.com/api/projects/productTypes#settype) with `elementType` as [ReferenceType](https://docs.commercetools.com/api/projects/productTypes#referencetype) and [NestedType](https://docs.commercetools.com/api/projects/productTypes#nestedtype) requires `key` on the `id` field of the `ReferenceType`. | 

> Note that a reference without the key field will be considered as existing 
resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`mapToProductDrafts`](https://commercetools.github.io/commercetools-sync-java/v/3.2.0/com/commercetools/sync/products/utils/ProductReferenceResolutionUtils.html#mapToProductDrafts-java.util.List-)
the method that maps from a `Product` to `ProductDraft` in order to make them ready for reference resolution by the sync, for example: 

````java
// Build a ProductQuery for fetching products from a source CTP project with all the needed references expanded for the sync:
final ProductQuery productQueryWithReferenceExpanded = ProductReferenceResolutionUtils.buildProductQuery();

// Query all products (NOTE this is only for example, please adjust your logic)
final List<Product> products =
    CtpQueryUtils
        .queryAll(sphereClient, productQueryWithReferenceExpanded, Function.identity())
        .thenApply(fetchedResources -> fetchedResources
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()))
        .toCompletableFuture()
         .join();

// Mapping from Product to ProductDraft with considering reference resolution.
final List<ProductDraft> productDrafts = ProductReferenceResolutionUtils.mapToProductDrafts(products);
````
##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

```` java
final ProductDraft productDraft =
    ProductDraftBuilder
        .of(ResourceIdentifier.ofKey("product-type-key"), ofEnglish("name"), ofEnglish("slug"),
            singletonList(masterVariant))
        .key("product-key")
        .categories(asSet(ResourceIdentifier.ofKey("category1-key"), ResourceIdentifier.ofKey("category2-key")))
        .taxCategory(ResourceIdentifier.ofKey("tax-category-key")) 
        .state(ResourceIdentifier.ofKey("tax-category-key"))
        .build();
````
 
 ````java
 final PriceDraft priceDraft = PriceDraftBuilder
    of(MoneyImpl.of("20", "EUR"))
    .channel(ResourceIdentifier.ofKey("channel-key"))
    .customerGroup(ResourceIdentifier.ofKey("customer-group-key"))
    .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap()))
    .build();
````

-  The product variant attributes with a type `ReferenceType` do not support the `ResourceIdentifier` yet, for those 
references you have to provide the `key`  value on the `id` field of the reference. This means that calling `getId()` on the reference should return its `key`.

````java
final ObjectNode productReference = JsonNodeFactory.instance.objectNode();
productReference.put("typeId", Product.referenceTypeId());
productReference.put("id", "product-key"); // note that reference key provided in the id field 

final AttributeDraft productAttributeDraft = AttributeDraft.of("productAttrName", productReference);
````

-  For resolving `key-value-document` (custom object) references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`, The `id` field of the reference in the attribute draft should be defined in the correct format. 
The correct format must have a vertical bar `|` character between the values of the container and key.
For example, if the custom object has a container value `container` and key value `key`, the `id` field should be `container|key"`,  
also, the key and container value should match the pattern `[-_~.a-zA-Z0-9]+`.

````java
final ObjectNode customObjectReference = JsonNodeFactory.instance.objectNode();
productReference.put("typeId", CustomObject.referenceTypeId());
productReference.put("id", "co-container|co-key");

final AttributeDraft customObjectAttributeDraft = AttributeDraft.of("customObjectAttrName", productReference);
````

#### SyncOptions

After the `sphereClient` is set up, a `ProductSyncOptions` should be built as follows: 

````java
// instantiating a ProductSyncOptions
final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* product draft from the source
* product of the target project (only provided if an existing product could be found)
* the update-actions, which failed (only provided if an existing product could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductSync.class);
 final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, product, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* product draft from the source 
* product of the target project (only provided if an existing product could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductSync.class);
 final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, product, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target product and a product draft are matched, this callback can be used to intercept 
the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify the update 
actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * product draft from the source
 * product from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>> beforeUpdateProductCallback =
            (updateActions, newProductDraft, oldProduct) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveVariant))
                    .collect(Collectors.toList());
                        
final ProductSyncOptions productSyncOptions = 
        ProductSyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateProductCallback).build();
````

##### beforeCreateCallback
During the sync process, if a product draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * product draft that should be created

````java
// Example (Set publish stage if category references of given product draft exist)

final Function<ProductDraft, ProductDraft> beforeCreateProductCallback =
        (callbackDraft) -> {
            Set<ResourceIdentifier<Category>> categoryResourceIdentifier = callbackDraft.getCategories();
            if (categoryResourceIdentifier!=null && !categoryResourceIdentifier.isEmpty()) {
                return ProductDraftBuilder.of(callbackDraft).isPublish(true).build();
            }
            return callbackDraft;
        };
                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(sphereClient).beforeCreateCallback(beforeCreateProductCallback).build();
````

##### batchSize
A number that could be used to set the batch size with which products are fetched and processed,
as products are obtained from the target project on commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding products from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 30 for product sync.

````java                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(sphereClient).batchSize(50).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for product sync.

````java
final ProductSyncOptions productSyncOptions =
    ProductSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build(); 
````
     
##### syncFilter
It represents either a blacklist or a whitelist for filtering certain update action groups. 
  
  - __Blacklisting__ an update action group means that everything in products will be synced except for any group in the blacklist. A typical use case is to blacklist prices when syncing products. In other words, syncing everything 
  in products except for prices.
  
    ````java                         
    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofBlackList(ActionGroup.PRICES)).build();
    ````
  
  - __Whitelisting__ an update action group means that the groups in this whitelist will be the *only* group synced in products. One use case could be to whitelist prices when syncing products. In other words, syncing prices only in 
  products and nothing else.
  
    ````java                         
    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofWhiteList(ActionGroup.PRICES)).build();
    ````
  
  - The list of action groups allowed to be blacklisted or whitelisted on products can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/products/ActionGroup.java). 

##### ensureChannels
A flag to indicate whether the sync process should create a price channel of the given key when it doesn't exist in a 
target project yet.
- If `ensureChannels` is set to `false` this product won't be synced and the `errorCallback` will be triggered.
- If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key. 
If it fails to create the price channel, the product won't sync and `errorCallback` will be triggered.
- If not provided, it is set to `false` by default.

````java                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(sphereClient).ensureChannels(true).build();
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a product sync
final ProductSync productSync = new ProductSync(productSyncOptions);

// execute the sync on your list of products
CompletionStage<ProductSyncStatistics> syncStatisticsStage = productSync.sync(productDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `ProductSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed products and the processing time of the sync in different time units and in a
human-readable format.
````java
final ProductSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*Summary: 2000 product(s) were processed in total (1000 created, 995 updated, 5 failed to sync and 0 product(s) with missing reference(s)).*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.

##### Persistence of ProductDrafts with Irresolvable References

A productDraft X could be supplied in with an attribute referencing productDraft Y. 
It could be that Y is not supplied before X, which means the sync could fail to create/updating X. 
It could also be that Y is not supplied at all in this batch but at a later batch.
 
The library keeps tracks of such "referencing" drafts like X and persists them in storage 
(**Commercetools platform `customObjects` in the target project** , in this case) 
to keep them and create/update them accordingly whenever the referenced drafts exist in the target project.

The `customObject` will have a `container:` **`"commercetools-sync-java.UnresolvedReferencesService.productDrafts"`**
and a `key` representing the key of the productDraft that is waiting to be created/updated.


Here is an example of a `CustomObject` in the target project that represents a productDraft with `productKey1`.  
It being persisted as `CustomObject` means that the referenced productDrafts with keys `foo` and `bar` do not exist yet.

```json
{
  "id": "d0fbb69e-76e7-4ec0-893e-3aaab6f4f6b6",
  "version": 1,
  "container": "commercetools-sync-java.UnresolvedReferencesService.productDrafts",
  "key": "productKey1",
  "value": {
    "dependantProductKeys": [
      "foo",
      "bar"
    ],
    "productDraft": {
      "productType": {
        "typeId": "product-type",
        "id": "main-product-type"
      },
      "masterVariant": {
          "id": 1,
          "sku": "white-shirt-1",
          "key": "white-shirt-1",
          "prices": [],
          "images": [],
          "attributes": [
            {
              "name": "product-reference-set",
              "value": [
                {
                  "typeId": "product",
                  "id": "foo"
                },
                {
                  "typeId": "product",
                  "id": "bar"
                }
              ]
            }
          ]
        },
      "key": "productKey1"
    }
  },
  "createdAt": "2019-09-27T13:45:35.495Z",
  "lastModifiedAt": "2019-09-27T13:45:35.495Z",
  "lastModifiedBy": {
    "clientId": "8bV3XSW-taCpi873-GQTa8lf",
    "isPlatformClient": false
  },
  "createdBy": {
    "clientId": "8bV3XSW-taCpi873-GQTa8lf",
    "isPlatformClient": false
  }
}
```

As soon, as the referenced productDrafts are supplied to the sync, the draft will be created/updated and the 
`CustomObject` will be removed from the target project.

Keeping the old custom objects around forever can negatively influence the performance of your project and the time it takes to restore it from a backup.  Deleting unused data ensures the best performance for your project. Please have a look into the [Cleanup guide](CLEANUP_GUIDE.md) to cleanup old unresolved custom objects.

##### More examples of how to use the sync

1. [Sync from another commercetools project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/ProductSyncIT.java).
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
More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/products/utils).

## Caveats

The commercetools-java-sync library has some exceptions to the data it can sync, particularly around product variant 
attributes.

1. List of supported variant attributes, with a  `AttributeType`: `ReferenceType`, 
 that can be synced (See more: [#87](https://github.com/commercetools/commercetools-sync-java/issues/87)):
 
    | `referenceTypeId`  |  supported |
    |---|---|
    | `“cart”` | ❌ |
    | `“category”`  | ✅ |
    | `“channel”`  | ❌ |
    | `“customer”`  | ❌ |
    | `“key-value-document”`  | ✅ |
    | `“order”`  | ❌ |
    | `“product”` | ✅ |
    | `“product-type”` | ✅ |
    | `“review”`  | ❌ |
    | `“state”`  | ❌ |
    | `“shipping-method”`  | ❌ |
    | `“zone”`  | ❌ |

2. Support for syncing variant attributes with an `AttributeType` of `SetType` of `ReferenceType` 
(of `elementType: ReferenceType`) with any of the aforementioned `referenceTypeId`, accordingly applies.
3. Support for syncing variant attributes with an `AttributeType` of `NestedType` which has an attribute inside of it of 
`ReferenceType`  with any of the aforementioned `referenceTypeId`, accordingly applies.
4. Syncing products with cyclic dependencies are not supported yet. An example of a cyclic dependency is a product `a` which references a product `b` and at the same time product `b` references product `a`. Cycles can contain more than 2 products. For example: `a` -> `b` -> `c` -> `a`. If there are such cycles, the sync will consider all the products in the cycle as products with missing parents. They will be persisted as custom objects in the target project.
