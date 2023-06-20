# Product Sync

The module used for importing/syncing Products into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ProductProjection](https://docs.commercetools.com/api/projects/productProjections#productprojection) 
against a [ProductDraft](https://docs.commercetools.com/api/projects/products#productdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Prerequisites](#prerequisites)
    - [ProjectApiRoot](#projectapiroot)
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
- [Migration Guide](#migration-guide)
  - [Client configuration and creation](#client-configuration-and-creation)
  - [Signature of ProductSyncOptions](#signature-of-productsyncoptions)
  - [Build ProductDraft (syncing from external project)](#build-productdraft-syncing-from-external-project)
  - [Query for Products (syncing from CTP project)](#query-for-products-syncing-from-ctp-project)
  - [JVM-SDK-V2 migration guide](#jvm-sdk-v2-migration-guide)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites

#### ProjectApiRoot 
<!-- TODO Check all github-links in this doc when this was merged to master-branch! -->
Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/sdk2/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
To create `ClientCredentials` which are required for creating a client please use the `ClientCredentialsBuilder` provided in java-sdk-v2 [Client OAUTH2 package](https://github.com/commercetools/commercetools-sdk-java-v2/blob/main/rmf/rmf-java-base/src/main/java/io/vrap/rmf/base/client/oauth2/ClientCredentialsBuilder.java)
If you have custom requirements for the client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final ClientCredentials clientCredentials =
        new ClientCredentialsBuilder()
            .withClientId("client-id")
            .withClientSecret("client-secret")
            .withScopes("scopes")
            .build();
final ProjectApiRoot apiRoot = ClientConfigurationUtils.createClient("project-key", clientCredentials, "auth-url", "api-url");
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

|Reference Field|Type|Necessity|
|:---|:---|:---|
| `productType` | ProductTypeResourceIdentifier | **Required** | 
| `categories` | List of CategoryResourceIdentifier | Optional |
| `taxCategory` | TaxCategoryResourceIdentifier | Optional |
| `state` | StateResourceIdentifier | Optional |
| `variants.prices.channel` | ChannelResourceIdentifier | Optional |
| `variants.prices.customerGroup` | CustomerGroupResourceIdentifier | Optional | 
| `variants.prices.custom.type` | TypeResourceIdentifier | **Required** for `custom` (CustomFieldsDraft) |
| `variants.assets.custom.type` | TypeResourceIdentifier | **Required** for `custom` (CustomFieldsDraft) |
| `variants.attributes` | Only the attributes with type [ReferenceType](https://docs.commercetools.com/api/projects/productTypes#referencetype), [SetType](https://docs.commercetools.com/api/projects/productTypes#settype) with `elementType` as [ReferenceType](https://docs.commercetools.com/api/projects/productTypes#referencetype) and [NestedType](https://docs.commercetools.com/api/projects/productTypes#nestedtype) requires `key` on the `id` field of the `ReferenceType`. | Optional |

> Note that a reference without the key field will be considered as existing 
resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toProductDrafts`](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/sdk2/products/utils/ProductTransformUtils.java#L59)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `ProductProjection` to `ProductDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build a ProductQuery for fetching products from a source CTP project without any references expanded for the sync:
final ByProjectKeyProductProjectionsGet byProjectKeyProductsGet = getCtpClient().productProjections().get().addStaged(true);

// Query all product projections (NOTE this is only for example, please adjust your logic)
final List<ProductProjection> products = QueryUtils.queryAll(byProjectKeyProductsGet,
            (productProjections) -> productProjections)
            .thenApply(lists -> lists.stream().flatMap(List::stream).collect(Collectors.toList()))
            .toCompletableFuture()
            .join();
````

In order to transform and map the `ProductProjections` to `ProductDraft`, 
Utils method `toProductDrafts` requires `ProjectApiRoot`, implementation of [`ReferenceIdToKeyCache`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and list of `ProductProjection` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from ProductProjection to ProductDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<ProductDraft>> productDrafts = ProductTransformUtils.toProductDrafts(client, referenceIdToKeyCache, products);
````
##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

```` java
final ProductDraft productDraft =
              ProductDraftBuilder
                      .of()
                      .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type-key").build())
                      .name(LocalizedString.ofEnglish("name"))
                      .slug(LocalizedString.ofEnglish("slug"))
                      .masterVariant(masterVariant)
                      .key("product-key")
                      .categories(CategoryResourceIdentifierBuilder.of().key("category1-key").build(),
                              CategoryResourceIdentifierBuilder.of().key("category2-key").build())
                      .taxCategory(TaxCategoryResourceIdentifierBuilder.of().key("tax-category-key").build())
                      .state(StateResourceIdentifierBuilder.of().key("tax-category-key").build())
                      .build();
````
 
 ````java
final PriceDraft priceDraft =
          PriceDraftBuilder.of()
              .value(MoneyBuilder.of().centAmount(20L).currencyCode("EUR").build())
              .channel(ChannelResourceIdentifierBuilder.of().key("channel-key").build())
              .customerGroup(
                  CustomerGroupResourceIdentifierBuilder.of().key("customer-group-key").build())
              .custom(
                  CustomFieldsDraftBuilder.of()
                      .type(TypeResourceIdentifierBuilder.of().key("type-key").build())
                      .fields(FieldContainerBuilder.of().values(Collections.emptyMap()).build()).build())
              .build();
````

-  The product projection variant attributes with a type `ReferenceType` do not support the `ResourceIdentifier` yet, for
 those references you have to provide the `key`  value on the `id` field of the reference. This means that calling `getId()` on the reference should return its `key`.

````java
final ProductReference productReference = ProductReferenceBuilder.of().id("product-key").build();
final Attribute attr = AttributeBuilder.of().name("attribute-name").value(productReference).build();

````

-  For resolving `key-value-document` (custom object) references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`, The `id` field of the reference in the attribute should be defined in the correct format. 
The correct format must have a vertical bar `|` character between the values of the container and key.
For example, if the custom object has a container value `container` and key value `key`, the `id` field should be `container|key`,  
also, the key and container value should match the pattern `[-_~.a-zA-Z0-9]+`. Please also keep in mind that length of the key is limited to 256 characters max: [CustomObject](https://docs.commercetools.com/api/projects/custom-objects#customobject)

````java
final CustomObjectReference coReference =
          CustomObjectReferenceBuilder.of().id("co-container|co-key").build();
final Attribute attr =
          AttributeBuilder.of().name("attribute-name").value(coReference).build();
````

#### SyncOptions

After the `ProjectApiRoot` is set up, a `ProductSyncOptions` should be built as follows: 

````java
// instantiating a ProductSyncOptions
final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(projectApiRoot).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* product draft from the source
* product projection of the target project (only provided if an existing product projection could be found)
* the update-actions, which failed (only provided if an existing product projection could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductSync.class);
 final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
         .of(projectApiRoot)
         .errorCallback((syncException, draft, productProjection, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* product draft from the source 
* product projection of the target project (only provided if an existing product projection could be found)

````java
 final Logger logger = LoggerFactory.getLogger(ProductSync.class);
 final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
         .of(projectApiRoot)
         .warningCallback((syncException, draft, productProjection) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target product projection and a product draft are matched, this callback can be used to
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify the update 
actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * product draft from the source
 * product projection from the target project
 * update actions that were calculated after comparing both

````java
// Example: Ignore update actions which contain deletion of variants
final TriFunction<
        List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction
>> beforeUpdateProductCallback =
            (updateActions, newProductDraft, oldProduct) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof ProductRemoveVariantActionImpl))
                    .collect(Collectors.toList());
                        
final ProductSyncOptions productSyncOptions = 
        ProductSyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateProductCallback).build();
````

##### beforeCreateCallback
During the sync process, if a product draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * product draft that should be created

````java
// Example: Set publish stage if category references of given product draft exist
final Function<ProductDraft, ProductDraft> beforeCreateProductCallback =
        (callbackDraft) -> {
            List<CategoryResourceIdentifier> categoryResourceIdentifier = callbackDraft.getCategories();
            if (categoryResourceIdentifier!=null && !categoryResourceIdentifier.isEmpty()) {
                return ProductDraftBuilder.of(callbackDraft).publish(true).build();
            }
            return callbackDraft;
        };
                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(projectApiRoot).beforeCreateCallback(beforeCreateProductCallback).build();
````

##### batchSize
A number that could be used to set the batch size with which product projections are fetched and processed,
as product projections are obtained from the target project on commercetools platform in batches for better performance.
 The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding product
projections from the target project on the commercetools platform in a single request. Playing with this option can
slightly improve or reduce processing speed. If it is not set, the default batch size is 30 for product sync.

````java                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(projectApiRoot).batchSize(50).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for product sync.

````java
final ProductSyncOptions productSyncOptions =
    ProductSyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build(); 
````
     
##### syncFilter
It represents either a blacklist or a whitelist for filtering certain update action groups. 
  
  - __Blacklisting__ an update action group means that everything in products will be synced except for any group in
   the blacklist. A typical use case is to blacklist prices when syncing product projections. In other words, syncing
    everything in product projections except for prices.
  
    ````java                         
    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofBlackList(ActionGroup.PRICES)).build();
    ````
  
  - __Whitelisting__ an update action group means that the groups in this whitelist will be the *only* group synced in products. One use case could be to whitelist prices when syncing products. In other words, syncing prices only in 
 product projections and nothing else.
  
    ````java                         
    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofWhiteList(ActionGroup.PRICES)).build();
    ````
  
  - The list of action groups allowed to be blacklisted or whitelisted on product projections can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/products/ActionGroup.java). 

##### ensureChannels
A flag to indicate whether the sync process should create a price channel of the given key when it doesn't exist in a 
target project yet.
- If `ensureChannels` is set to `false` this products won't be synced and the `errorCallback` will be triggered.
- If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key. 
If it fails to create the price channel, the products won't sync and `errorCallback` will be triggered.
- If not provided, it is set to `false` by default.

````java                         
final ProductSyncOptions productSyncOptions = 
         ProductSyncOptionsBuilder.of(projectApiRoot).ensureChannels(true).build();
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
final ProductSyncStatistics stats = syncStatisticsStage.toCompletableFuture().join();
stats.getReportMessage(); 
/*Summary: 2000 product(s) were processed in total (1000 created, 995 updated, 5 failed to sync and 0 product(s) with missing reference(s)).*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.

##### Persistence of ProductDrafts with Irresolvable References

A productDraft X could be supplied in with an attribute referencing productDraft Y. 
It could be that Y is not supplied before X, which means the sync could fail to create/update X. 
It could also be that Y is not supplied at all in this batch but at a later batch.
 
The library keeps track of such "referencing" drafts like X and persists them in storage 
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

A utility method provided by the library to compare a ProductProjection with a new ProductDraft and results in a
 list of update actions. 
```java
List<ProductUpdateAction> updateActions = ProductSyncUtils.buildActions(productProjection, productDraft, productSyncOptions, attributesMetaData);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/test/java/com/commercetools/sync/sdk2/products/utils/ProductSyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a ProductProjection and a new ProductDraft,
 build the update action. One example is the `buildChangeNameUpdateAction` which compares names:
  
````java
Optional<ProductUpdateAction> updateAction = buildChangeNameUpdateAction(oldProductProjection, productDraft);
````
More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/products/utils).

## Caveats

The commercetools-java-sync library has some exceptions to the data it can sync, particularly around product variant 
attributes.

1. List of supported variant attributes, with an  `AttributeType`: `ReferenceType`, 
 that can be synced:
 
    | `referenceTypeId`  |  supported |
    |---|---|
    | `“cart”` | ❌ |
    | `“category”`  | ✅ |
    | `“channel”`  | ❌ |
    | `“customer”`  | ✅ |
    | `“key-value-document”`  | ✅ |
    | `“order”`  | ❌ |
    | `“product”` | ✅ |
    | `“product-type”` | ✅ |
    | `“review”`  | ❌ |
    | `“state”`  | ✅ |
    | `“shipping-method”`  | ❌ |
    | `“zone”`  | ❌ |

2. Support for syncing variant attributes with an `AttributeType` of `SetType` of `ReferenceType` 
(of `elementType: ReferenceType`) with any of the aforementioned `referenceTypeId`, accordingly applies.
3. Support for syncing variant attributes with an `AttributeType` of `NestedType` which has an attribute inside of it of 
`ReferenceType`  with any of the aforementioned `referenceTypeId`, accordingly applies.
4. Syncing products with cyclic dependencies are not supported yet. An example of a cyclic dependency is a product `a` which references a product `b` and at the same time product `b` references product `a`. Cycles can contain more than 2 products. For example: `a` -> `b` -> `c` -> `a`. If there are such cycles, the sync will consider all the products in the cycle as products with missing parents. They will be persisted as custom objects in the target project.

## Migration Guide

The product-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
any HTTP client module. The default one is `commercetools-http-client`. 

```xml
 <!-- Sample maven pom.xml -->
 <properties>
     <commercetools.version>LATEST</commercetools.version>
 </properties>

 <dependencies>
     <dependency>
       <groupId>com.commercetools.sdk</groupId>
       <artifactId>commercetools-http-client</artifactId>
       <version>${commercetools.version}</version>
     </dependency>
     <dependency>
       <groupId>com.commercetools.sdk</groupId>
       <artifactId>commercetools-sdk-java-api</artifactId>
       <version>${commercetools.version}</version>
     </dependency>
 </dependencies>

```

### Client configuration and creation

For client creation use [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/sdk2/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
If you have custom requirements for the client creation make sure to replace `SphereClientFactory` with `ApiRootBuilder` as described in this [Migration Document](https://docs.commercetools.com/sdk/java-sdk-migrate#client-configuration-and-creation).

### Signature of ProductSyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `ProductDraft`, `ProductProjection` and `ProductUpdateAction` from `package com.commercetools.api.models.product.*`

> Note: Type `UpdateAction<Product>` has changed to `ProductUpdateAction`. Make sure you create and supply a specific ProductUpdateAction in `beforeUpdateCallback`. Therefore you can use the [library-utilities](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/sdk2/products/utils/ProductUpdateActionUtils.java) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a product update action to change name taking the 'newName' of the productDraft
    final Function<LocalizedString, ProductUpdateAction> createBeforeUpdateAction =
        (newName) -> ProductChangeNameAction.builder().name(newName).staged(true).build();

// Add the change name action to the list of update actions before update is executed
    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        beforeUpdateProductCallback =
            (updateActions, newProductDraft, oldProduct) -> {
              final ProductUpdateAction beforeUpdateAction =
                  createBeforeUpdateAction.apply(newProductDraft.getName());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build ProductDraft (syncing from external project)

The product-sync expects a list of `ProductDraft`s to process. If you use java-sync-library to sync your products from any external system into a commercetools platform project you have to convert your data into CTP compatible `ProductDraft` type. This was done in previous version using `DraftBuilder`s. 
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// ProductDraftBuilder in v1 takes parameters 'productType', 'name', 'slug' and optional 'masterVariant'
final ProductDraft productDraft =
              ProductDraftBuilder
                      .of(mock(ProductType.class), ofEnglish("name"), ofEnglish("slug"), emptyList())
                      .key("product-key")
                      .build();

// ProductDraftBuilder in v2
final ProductDraft productDraft =
              ProductDraftBuilder
                      .of()
                      .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type-key").build())
                      .name(LocalizedString.ofEnglish("name"))
                      .slug(LocalizedString.ofEnglish("slug"))
                      .masterVariant(masterVariant)
                      .key("product-key")
                      .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for Products (syncing from CTP project)

If you sync products between different commercetools projects you probably use [ProductTransformUtils#toProductDrafts](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/sdk2/products/utils/ProductTransformUtils.java#L59) to transform `ProductProjection` into `ProductDraft` which can be used by the product-sync.
However, if you need to query `Products` / `ProductProjections` from a commercetools project instead of passing `ProductQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: ProductProjectionQuery to fetch all staged product projections
final ProductProjectionQuery query = ProductProjectionQuery.ofStaged();

final PagedQueryResult<ProductProjection> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all staged product projections in one line
final ProductProjectionPagedQueryResponse result = apiRoot.productProjections().get().addStaged(true).executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 