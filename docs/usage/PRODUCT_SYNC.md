# commercetools product sync

A utility which provides an API for building CTP product update actions and product synchronisation.

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
1. The sync expects a list of non-null `ProductDraft` objects that have their `key` fields set to match the
products from the source to the target. Also, the target project is expected to have the `key` fields set,
otherwise they won't be matched.

**NOTE: PLEASE MAKE SURE THE `SKU` FIELDS OF ALL PRODUCTS ARE SET AS THE SYNC LIBRARY WILL BE MIGRATED TO MATCH PRODUCTS BY `SKU` INSTEAD OF `KEY` IN THE FUTURE.**

2. Every product may have several references including `product type`, `categories`, `taxCategory`, etc.. Variants
of the product also have prices, where each price also has some references including a reference to the `Type` of its 
custom fields and a reference to a `channel`. All these referenced resources are matched by their `key` Therefore, in 
order for the sync to resolve the actual ids of those references, those `key`s have to be supplied in the following way:
    - Provide the `key` value on the `id` field of the reference. This means that calling `getId()` on the
    reference would return its `key`. 
     
        **Note**: This library provides you with a utility method 
         [`replaceProductsReferenceIdsWithKeys`](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/products/utils/ProductReferenceReplacementUtils.html#replaceProductsReferenceIdsWithKeys-java.util.List-)
         that replaces the references id fields with keys, in order to make them ready for reference resolution by the sync:
         ````java
         // Puts the keys in the reference id fields to prepare for reference resolution
         final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);
         ````
     
3. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the number of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with 
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html) 
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the 
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)
   
  If you have no special requirements on sphere client creation then you can use `ClientConfigurationUtils#createClient`
  util which applies best practices already.

4. After the `sphereClient` is set up, a `ProductSyncOptions` should be built as follows: 
````java
// instantiating a ProductSyncOptions
final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(sphereClient).build();
````

Additional optional configuration for the sync can be configured on the `ProductSyncOptionsBuilder` instance, according to your need:
- `errorCallBack`
a callback that is called whenever an error event occurs during the sync process.

- `warningCallBack` 
a callback that is called whenever a warning event occurs during the sync process.

- `syncFilter`
 represents either a blacklist or a whitelist for filtering certain update action groups. 
  - __Blacklisting__ an update action group means that everything in products will be synced except for any group 
  in the blacklist. A typical use case it to blacklist prices when syncing products, so as to sync everything in products
  except prices. [Here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java#L142-L143)
  is an example where the sync is performed while blacklisting product categories. 
  
  - __Whitelisting__ an update action group means that the groups in this whitelist will be the *only* group synced in 
  products. One use case could be to whitelist prices when syncing products, so as to only sync prices in products and
  nothing else. [Here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java#L173)
  is an example where the sync is performed while whitelisting product names.
  
  - The list of action groups allowed to be blacklist or whitelisted on products can be found [here](/src/main/java/com/commercetools/sync/products/ActionGroup.java). 

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept product 
 **_update_** actions just before they are sent to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a product draft before a request to create it on CTP is issued. It allows the 
user to intercept product **_create_** requests modify the draft before the create request is sent to CTP API.

- `batchSize`
a number that could be used to set the batch size with which products are fetched and processed with,
as products are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to
`batchSize` products from the input list, then fetches the corresponding products from the target CTP project
in a single request. Playing with this option can slightly improve or reduce processing speed. (The default value is `30`).

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning 
streams, would look as follows:
 ```java
 final Logger logger = LoggerFactory.getLogger(MySync.class);
 final ProductSyncOptions productsyncOptions = ProductSyncOptionsBuilder.of(sphereClient)
                                                                        .errorCallBack(logger::error)
                                                                        .warningCallBack(logger::warn)
                                                                        .build();
 ```

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


More examples of how to use the sync
1. From another CTP project as a source can be found [here](/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/ProductSyncIT.java).
2. From an external source can be found [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncIT.java). 
3. Syncing with blacklisting/whitelisting [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java).

### Build all update actions

A utility method provided by the library to compare a Product with a new ProductDraft and results in a list of product
 update actions. 
```java
List<UpdateAction<Product>> updateActions = ProductSyncUtils.buildActions(product, productDraft, productSyncOptions, attributesMetaData);
```

Examples of its usage can be found in the tests 
[here](/src/test/java/com/commercetools/sync/products/utils/ProductSyncUtilsTest.java).

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a Product and a new ProductDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
  
````java
Optional<UpdateAction<Product>> updateAction = buildChangeNameUpdateAction(oldProduct, productDraft);
````
More examples of those utils for different fields can be found [here](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/utils).

## Caveats
1. Products are either created or updated. Currently the tool does not support product deletion.
2. The sync library is not meant to be executed in a parallel fashion. For example:
    ````java
    final ProductSync productSync = new ProductSync(syncOptions);
    final CompletableFuture<ProductSyncStatistics> syncFuture1 = productSync.sync(batch1).toCompletableFuture();
    final CompletableFuture<ProductSyncStatistics> syncFuture2 = productSync.sync(batch2).toCompletableFuture();
    CompletableFuture.allOf(syncFuture1, syncFuture2).join;
    ````
    The aforementioned example demonstrates how the library should **not** be used. The library, however, should be instead
    used in a sequential fashion:
    ````java
    final ProductSync productSync = new ProductSync(syncOptions);
    productSync.sync(batch1)
               .thenCompose(result -> productSync.sync(batch2))
               .toCompletableFuture()
               .join();
    ````
    By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
     - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
     - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch constitute.
     
    The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
    is the recommended good balance for stability and performance for the sync process.
3. Syncing attribute field types with  `ReferenceType` and `SetType` (of `elementType: ReferenceType`) field definitions, except 
for Product references, is not supported yet. (See more: [#87](https://github.com/commercetools/commercetools-sync-java/issues/87) [#160](https://github.com/commercetools/commercetools-sync-java/issues/87)). This
also applies to an attribute field of type `NestedType` which has any of the aforementioned types as an inner field.


