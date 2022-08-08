# Customer Sync

The module used for importing/syncing Customers into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Customer](https://docs.commercetools.com/api/projects/customers#customer) 
against a [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft).

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
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites
#### SphereClient

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/9.0.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).

````java
final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");

final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);
````

#### Required Fields

The following fields are **required** to be set in, otherwise, they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft) | `key` |  Also, the customers in the target project are expected to have the `key` fields set. | 
| [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft) | `address.key` |  Every customer [Address](https://docs.commercetools.com/api/types#address) needs a unique key to match the existing `Address` with the new Address. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `customerGroup` | ResourceIdentifier to a CustomerGroup | 
| `stores` | Set of ResourceIdentifier to a Store | 
| `custom.type` | ResourceIdentifier to a Type |  

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toCustomerDrafts`](https://commercetools.github.io/commercetools-sync-java/v/9.0.0/com/commercetools/sync/customers/utils/CustomerTransformUtils.html#toCustomerDrafts-java.util.List-)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `Customer` to `CustomerDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build a CustomerQuery for fetching customers from a source CTP project without any references expanded for the sync:
final CustomerQuery customerQuery = CustomerQuery.of();

// Query all customers (NOTE this is just for example, please adjust your logic)
final List<Customer> customers =
    CtpQueryUtils
        .queryAll(sphereClient, customerQuery, Function.identity())
        .thenApply(fetchedResources -> fetchedResources
            .stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()))
        .toCompletableFuture()
        .join();
````

In order to transform and map the `Customer` to `CustomerDraft`, 
Utils method `toCustomerDrafts` requires `sphereClient`, implementation of [`ReferenceIdToKeyCache`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `customers` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from Customer to CustomerDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<CustomerDraft>> customerDrafts = CustomerTransformUtils.toCustomerDrafts(client, referenceIdToKeyCache, customers);
````

##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

````java
final CustomerDraftBuilder customerDraftBuilder = CustomerDraftBuilder
    .of("email@example.com", "password")
    .customerGroup(ResourceIdentifier.ofKey("customer-group-key")) // note that customer group reference provided with key
    .stores(asList(ResourceIdentifier.ofKey("store-key1"), 
        ResourceIdentifier.ofKey("store-key2"))) // note that store references provided with key
    .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
    .addresses(singletonList(Address.of(CountryCode.DE).withKey("address-key-1"))) // note that addresses has to be provided with their keys        
    .key("customer-key");
````

#### SyncOptions

After the `sphereClient` is set up, a `CustomerSyncOptions` should be built as follows:
````java
// instantiating a CustomerSyncOptions
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(sphereClient).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* customer draft from the source
* customer of the target project (only provided if an existing customer could be found)
* the update-actions, which failed (only provided if an existing customer could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CustomerSync.class);
 final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, customer, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* customer draft from the source 
* customer of the target project (only provided if an existing customer could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CustomerSync.class);
 final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, customer, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target customer and a customer draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * customer draft from the source
 * customer from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<List<UpdateAction<Customer>>, CustomerDraft, Customer,
                  List<UpdateAction<Customer>>> beforeUpdateCallback, =
            (updateActions, newCustomerDraft, oldCustomer) ->  updateActions
                    .stream()
                    .filter(updateAction -> !(updateAction instanceof SetLastName))
                    .collect(Collectors.toList());
                        
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
                    .of(CTP_CLIENT)
                    .beforeUpdateCallback(beforeUpdateCallback)
                    .build();
````

##### beforeCreateCallback
During the sync process, if a customer draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * customer draft that should be created

 Please refer to the [example in the product sync document](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/usage/PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### batchSize
A number that could be used to set the batch size with which customers are fetched and processed,
as customers are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding customers
from the target project on the commercetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for customer sync.

````java                         
final CustomerSyncOptions customerSyncOptions = 
         CustomerSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for customer sync.

````java
final CustomerSyncOptions customerSyncOptions = 
         CustomerSyncOptionsBuilder.of(sphereClient).cacheSize(5000).build();
````

### Running the sync
When all prerequisites are fulfilled, follow those steps to run the sync:

````java
// instantiating a customer sync
final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

// execute the sync on your list of customers
CompletionStage<CustomerSyncStatistics> syncStatisticsStage = customerSync.sync(customerDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `CustomerSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed customers, and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CustomerSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 100 customers were processed in total (11 created, 87 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.

#### More examples of how to use the sync

 [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/customers/CustomerSyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions
A utility method provided by the library to compare a `Customer` to a new `CustomerDraft`. The results are collected in a list of customer update actions.
```java
List<UpdateAction<Customer>> updateActions = CustomerSyncUtils.buildActions(customer, customerDraft, customerSyncOptions);
```

### Build particular update action(s)
The library provides utility methods to compare specific fields of a `Customer` and a new `CustomerDraft`, and builds the update action(s) as a result.
One example is the `buildChangeEmailUpdateAction` which compare email addresses:
````java
Optional<UpdateAction<Customer>> updateAction = CustomerUpdateActionUtils.buildChangeEmailAction(oldCustomer, customerDraft);
````

More examples for particular update actions can be found in the test scenarios for [CustomerUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/customers/utils/CustomerUpdateActionUtilsTest.java)
and [AddressUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/customers/utils/AddressUpdateActionUtilsTest.java).

## Caveats
The library does not support the synchronization of the `password` field of existing customers.
For customers that do not exist in the project, a password will be created with the given customer draft’s password.
