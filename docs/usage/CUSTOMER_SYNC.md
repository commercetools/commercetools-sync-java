# Customer Sync

Module used for importing/syncing Customers into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Customer](https://docs.commercetools.com/api/projects/customers#customer) 
against a [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of customer drafts](#sync-list-of-customer-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of customer drafts

#### Prerequisites
1. Create a `sphereClient`:
Use the [ClientConfigurationUtils](https://github.com/commercetools/commercetools-sync-java/blob/2.3.0/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) which apply the best practices for `SphereClient` creation.
If you have custom requirements for the sphere client creation, have a look into the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md).
  
2. The sync expects a list of `CustomerDraft`s that have their `key` fields set to be matched with customers in the 
target CTP project. The customers in the target project need to have the `key` fields set, otherwise they won't be 
matched.

3. To sync customer address data, every customer [Address](https://docs.commercetools.com/api/types#address) needs a 
unique key to match the existing `Address` with the new Address. 

4. Every customer may have a reference to their [CustomerGroup](https://docs.commercetools.com/api/projects/customerGroups#customergroup) 
and/or the [Type](https://docs.commercetools.com/api/projects/customers#set-custom-type) of their custom fields. 
The `CustomerGroup` and `Type` references should be expanded with a key.
Any reference that is not expanded will have its id in place and not replaced by the key will be considered as existing 
resources on the target commercetools project and the library will issue an update/create an API request without reference
resolution.

     - When syncing from a source commercetools project, you can use [`mapToCustomerDrafts`](https://commercetools.github.io/commercetools-sync-java/v/2.3.0/com/commercetools/sync/customers/utils/CustomerReferenceResolutionUtils.html#mapToCustomerDrafts-java.util.List-)
    method that maps from a `Customer` to `CustomerDraft` to make them ready for reference resolution by the sync:

    ````java
    final List<CustomerDraft> customerDrafts = CustomerReferenceResolutionUtils.mapToCustomertDrafts(customerDrafts);
    ````

5. After the `sphereClient` is set up, a `CustomerSyncOptions` should be built as follows:
````java
// instantiating a CustomerSyncOptions
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* customer draft from the source
* customer of the target project (only provided if an existing customer could be found)
* the update-actions, which failed (only provided if an existing customer could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CustomerSync.class);
 final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, customer, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* customer draft from the source 
* customer of the target project (only provided if an existing cart discount could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CustomerSync.class);
 final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, customer, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target customer and a customer draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * customer draft from the source
 * customer from the target project
 * update actions that were calculated after comparing both

##### Example
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

##### 4. `beforeCreateCallback`
During the sync process if a cart discount draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * customer draft that should be created
 ##### Example
 Please refer to the [example in the product sync document](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/usage/PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### 5. `batchSize`
A number that could be used to set the batch size with which customers are fetched and processed,
as customers are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding customers
from the target project on commercetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for customer sync.
##### Example
````java                         
final CustomerSyncOptions customerSyncOptions = 
         CustomerSyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

#### Running the sync
When all prerequisites are fulfilled, follow those steps to run the sync:

````java
// instantiating a cart discount sync
final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

// execute the sync on your list of customers
CompletionStage<CustomerSyncStatistics> syncStatisticsStage = customerSync.sync(customerDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `CustomerSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed cart discounts, and the processing time of the last sync batch in different time units and in a
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
