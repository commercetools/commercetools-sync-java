# Customer Sync

The module used for importing/syncing Customers into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Customer](https://docs.commercetools.com/api/projects/customers#customer) 
against a [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft).

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
  - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites
#### ProjectApiRoot

Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
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
| [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft) | `key` |  Also, the customers in the target project are expected to have the `key` fields set. | 
| [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft) | `address.key` |  Every customer [BaseAddress](https://docs.commercetools.com/api/types#ctp:api:type:BaseAddress) needs a unique key to match the existing `Address` with the new `AddressDraft`. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `customerGroup` | CustomerGroupResourceIdentifier | 
| `stores` | List of StoreResourceIdentifier | 
| `custom.type` | TypeResourceIdentifier |  

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toCustomerDrafts`](https://github.com/commercetools/commercetools-sync-java/blob/java-sdk-v2-product-sync-migration/src/main/java/com/commercetools/sync/customers/utils/CustomerTransformUtils.java#L42)
 method that transforms(resolves by querying and caching key-id pairs) and maps from a `Customer` to `CustomerDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build ByProjectKeyCustomersGet for fetching customers from a source CTP project without any references expanded for the sync:
final ByProjectKeyCustomersGet byProjectKeyCustomersGet = client.customers().get();

// Query all customers (NOTE this is just for example, please adjust your logic)
final List<Customer> customers = QueryUtils.queryAll(byProjectKeyCustomersGet,
            (customers) -> customers)
            .thenApply(lists -> lists.stream().flatMap(List::stream).collect(Collectors.toList()))
            .toCompletableFuture()
            .join();
````

In order to transform and map the `Customer` to `CustomerDraft`, 
Utils method `toCustomerDrafts` requires `projectApiRoot`, implementation of [`ReferenceIdToKeyCache`](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `customers` as parameters.
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
final CustomFieldsDraft customFields = CustomFieldsDraftBuilder.of()
                                       .type(TypeResourceIdentifierBuilder.of().key("type-key").build()) // note that custom type provided with key
                                       .fields(FieldContainerBuilder.of().values(Collections.emptyMap()).build())
                                       .build();
final AddressDraft address = AddressDraftBuilder.of()
                                                .key("address-key-1") // note that addresses has to be provided with their keys
                                                .country("DE")
                                                .build();
final CustomerDraft customerDraft = CustomerDraftBuilder.of()
        .email("email@example.com")
        .password("password")
        .key("customer-key")
        .customerGroup(CustomerGroupRescourceIdentifierBuilder.of().key("customer-group-key").build()) // note that customergroup reference provided with key
        .addresses(address)
        .custom(customFields)
        .stores(StoresResourceIdentifierBuilder.of().key("store-key1").build(), StoresResourceIdentifierBuilder.of().key("store-key2").build()) // note that store reference provided with key
        .build();
````

#### SyncOptions

After the `ProjectApiRoot` is set up, a `CustomerSyncOptions` should be built as follows:
````java
// instantiating a CustomerSyncOptions
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(projectApiRoot).build();
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
         .of(projectApiRoot)
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
         .of(projectApiRoot)
         .warningCallback((syncException, draft, customer) -> 
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
// Example: Ignore update actions which contain setting of lastName action
final TriFunction<List<CustomerUpdateAction>, CustomerDraft, Customer,
                  List<CustomerUpdateAction>> beforeUpdateCallback, =
            (updateActions, newCustomerDraft, oldCustomer) ->  updateActions
                    .stream()
                    .filter(updateAction -> !(updateAction instanceof CustomerSetLastNameActionImpl))
                    .collect(Collectors.toList());
                        
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
                    .of(projectApiRoot)
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
         CustomerSyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for customer sync.

````java
final CustomerSyncOptions customerSyncOptions = 
         CustomerSyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build();
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
List<CustomerUpdateAction> updateActions = CustomerSyncUtils.buildActions(customer, customerDraft, customerSyncOptions);
```

### Build particular update action(s)
The library provides utility methods to compare specific fields of a `Customer` and a new `CustomerDraft`, and builds the update action(s) as a result.
One example is the `buildChangeEmailUpdateAction` which compare email addresses:
````java
Optional<CustomerUpdateAction> updateAction = CustomerUpdateActionUtils.buildChangeEmailUpdateAction(oldCustomer, customerDraft);
````

More examples for particular update actions can be found in the test scenarios for [CustomerUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/customers/utils/CustomerUpdateActionUtilsTest.java)
and [AddressUpdateActionUtils](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/customers/utils/AddressUpdateActionUtilsTest.java).

## Caveats
The library does not support the synchronization of the `password` field of existing customers.
For customers that do not exist in the project, a password will be created with the given customer draft’s password.
