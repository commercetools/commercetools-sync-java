# commercetools product type sync

A utility which provides an API for building CTP product type update actions and product type synchronisation.

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
1. The sync expects a list of non-null `ProductTypeDrafts` objects that have their `key` fields set to match the
product types from the source to the target. Also, the target project is expected to have the `key` fields set, otherwise they won't be
matched.
2. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the number of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html)
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)

   If you have no special requirements on sphere client creation then you can use `ClientConfigurationUtils#createClient`
   util which applies best practices already.

4. After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be be built as follows:
````java
// instantiating a ProductTypeSyncOptions
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient).build();
````

Additional optional configuration for the sync can be configured on the `ProductTypeSyncOptionsBuilder` instance, according to your need:
- `errorCallBack`
a callback that is called whenever an error event occurs during the sync process.

- `warningCallBack` 
a callback that is called whenever a warning event occurs during the sync process.

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept product type
 **_update_** actions just before they are sent to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a product type draft before a request to create it on CTP is issued. It allows the
user to intercept product type **_create_** requests to modify the draft before the create request is sent to CTP API.

- `allowUuid`
a flag, if set to `true`, enables the user to use keys with UUID format for references. By default, it is set to `false`.

- `batchSize`
a number that could be used to set the batch size with which product types are fetched and processed with,
as product types are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to
`batchSize` product types from the input list, then fetches the corresponding product types from the target CTP project
in a single request. Playing with this option can slightly improve or reduce processing speed. (The default value is `50`).

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning
streams, would look as follows:
```java
final Logger logger = LoggerFactory.getLogger(MySync.class);
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient)
                                                                                   .errorCallBack(logger::error)
                                                                                   .warningCallBack(logger::warn)
                                                                                   .build();
```


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

More examples of how to use the sync can be found [here](/src/integration-test/java/com/commercetools/sync/integration/producttypes/ProductTypeSyncIT.java).

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
More examples of those utils for different fields can be found [here](/src/test/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtilsTest.java).


## Caveats

1. Product types are either created or updated. Currently the tool does not support product type deletion.
2. The sync library is not meant to be executed in a parallel fashion. Check the example in [point #2 here](/docs/usage/PRODUCT_SYNC.md#caveats).
    By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
     - Changing the number of [max parallel requests](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
     - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch constitute.
     
    The current overridable default [configuration](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
    is the recommended good balance for stability and performance for the sync process.
3. Syncing product types with an attribute of type [NestedType](https://docs.commercetools.com/http-api-projects-productTypes.html#nestedtype) is not supported yet.
