# Customer Sync

Module used for importing/syncing Customers into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Customer](https://docs.commercetools.com/api/projects/customers#customer) 
against a [CustomerDraft](https://docs.commercetools.com/api/projects/customers#customerdraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
- [Usage](#usage)
  - [Sync list of cart discount drafts](#sync-list-of-customer-drafts)
    - [Prerequisites](#prerequisites)
    - [Running the sync](#running-the-sync)
    - [More examples of how to use the sync](#more-examples-of-how-to-use-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of customer drafts

#### Prerequisites
1. The sync expects a list of `CustomerDraft`s that have their `key` fields set to be matched with customers in the 
target CTP project. The customers in the target project need to have the `key` fields set, otherwise they won't be 
matched.

2. To sync customer address data, every customer [Address](https://docs.commercetools.com/api/types#address) needs a 
unique key to match the existing `Address` with the new Address. 

3. Every customer may have a reference to their [CustomerGroup](https://docs.commercetools.com/api/projects/customerGroups#customergroup) 
and/or the [Type](https://docs.commercetools.com/api/projects/customers#set-custom-type) of their custom fields. 
The `CustomerGroup` and `Type` references should be expanded with a key.
Any reference that is not expanded will have its id in place and not replaced by the key will be considered as existing 
resources on the target commercetools project and the library will issue an update/create an API request without reference
resolution.

 - When syncing from a source commercetools project, you can use [`mapToCustomerDrafts`](TODO: insert link once existing)
    method that maps from a `Customer` to `CustomerDraft` to make them ready for reference resolution by the sync:

    ````java
    final List<CustomerDraft> customerDrafts = CustomerReferenceResolutionUtils.mapToCustomertDrafts(customerDrafts);
    ````

4. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

5. After the `sphereClient` is set up, a `CustomerSyncOptions` should be built as follows:
````java
// instantiating a CustomerSyncOptions
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).

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

More examples of those utils for different cart discounts can be found [here](TODO: add link).


##Caveats
Customer passwords are not being synced.

