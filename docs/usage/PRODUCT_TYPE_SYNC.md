# commercetools product type sync

Utility which provides API for building CTP product type update actions and product type synchronisation.

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

#### Prerequisites
1. The sync expects a list of non-null `ProductTypeDrafts` objects that have their `key` fields set to match the
product types from the source to the target. Also the target project is expected to have the `key` fields set, otherwise they won't be
matched.
2. It is an important responsibility of the user of the library to instantiate a `sphereClient` that has the following properties:
    - Limits the amount of concurrent requests done to CTP. This can be done by decorating the `sphereClient` with
   [QueueSphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/QueueSphereClientDecorator.html)
    - Retries on 5xx errors with a retry strategy. This can be achieved by decorating the `sphereClient` with the
   [RetrySphereClientDecorator](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/client/RetrySphereClientDecorator.html)

   You can use the same client instantiating used in the integration tests for this library found
   [here](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45).

4. After the `sphereClient` is setup, a `ProductTypeSyncOptions` should be be built as follows:
````java
// instantiating a ProductTypeSyncOptions
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient).build();
````

The options can be used to provide additional optional configuration for the sync as well:
- `errorCallBack`
a callback that is called whenever an event occurs during the sync process that represents an error. Currently, these
events.

- `warningCallBack`
a callback that is called whenever an event occurs during the sync process that represents a warning. Currently, these
events.

- `beforeUpdateCallback`
a filter function which can be applied on a generated list of update actions. It allows the user to intercept product type
update and modify (add/remove) update actions just before they are send to CTP API.

- `beforeCreateCallback`
a filter function which can be applied on a product type draft before a request to create it on CTP is issued. It allows the
user to intercept product type create requests modify the draft before the create request is sent to CTP API.

- `allowUuid`
a flag, if set to `true`, enables the user to use keys with UUID format for references. By default, it is set to `false`.

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
human readable format.

````java
final ProductTypeSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 2000 products types were processed in total (1000 created, 995 updated, 5 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:
 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not not known by the sync which batch is going to be the last one supplied.

More examples of how to use the sync can be found [here](/src/integration-test/java/com/commercetools/sync/integration/producttypes/ProductTypeSyncIT.java).

### Build all update actions

A utility method provided by the library to compare a ProductType with a new ProductTypeDraft and results in a list of product type update actions.
```java
List<UpdateAction<ProductType>> updateActions = ProductTypeSyncUtils.buildActions(productType, productTypeDraft, productTypeSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a ProductType and a new ProductTypeDraft, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<ProductType>> updateAction = ProductTypeUpdateActionUtils.buildChangeNameAction(oldProductType, productTypeDraft);
````
More examples of those utils for different fields can be found [here](/src/test/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtilsTest.java).


## Caveats

1. Product types are either created or updated. Currently the tool does not support product type deletion.
