# CartDiscount Sync

The module used for importing/syncing CartDiscounts into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [CartDiscount](https://docs.commercetools.com/api/projects/cartDiscounts#cartdiscount) 
against a [CartDiscountDraft](https://docs.commercetools.com/api/projects/cartDiscounts#cartdiscountdraft).

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
- [Migration Guide](#migration-guide)
  - [Client configuration and creation](#client-configuration-and-creation)
  - [Signature of CartDiscountSyncOptions](#signature-of-cartdiscountsyncoptions)
  - [Build CartDiscountDraft (syncing from external project)](#build-cartdiscountdraft-syncing-from-external-project)
  - [Query for Cart Discounts (syncing from CTP project)](#query-for-cart-discounts-syncing-from-ctp-project)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
        
### Prerequisites

#### ProjectApiRoot

Use the [ClientConfigurationUtils](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
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
| [CartDiscountDraft](https://docs.commercetools.com/api/projects/cartDiscounts#cartdiscountdraft) | `key` |  Also, the cart discounts in the target project are expected to have the `key` fields set. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in the sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field|Type|
|:---|:---|
| `custom.type` | ResourceIdentifier to a Type |  

> Note that a reference without the key field will be considered as an existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toCartDiscountDrafts`](/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountTransformUtils.java)
method that transforms(resolves by querying and caching key-id pairs) and maps from a `CartDiscount` to `CartDiscountDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build a ByProjectKeyCartDiscountsGet for fetching cart discounts from a source CTP project without any references expanded for the sync:
final ByProjectKeyCartDiscountsGet byProjectKeyCartDiscountsGet = client.cartDiscounts().get();

// Query all cart discounts (NOTE this is just for example, please adjust your logic)
final List<CartDiscount> cartDiscounts = QueryUtils.queryAll(byProjectKeyCartDiscountsGet,
        (cartDiscounts) -> cartDiscounts)
        .thenApply(lists -> lists.stream().flatMap(List::stream).collect(Collectors.toList()))
        .toCompletableFuture()
        .join();
````

In order to transform and map the `CartDiscount` to `CartDiscountDraft`, 
Utils method `toCartDiscountDrafts` requires `projectApiRoot`, implementation of [`ReferenceIdToKeyCache`](/src/main/java/com/commercetools/sync/commons/utils/ReferenceIdToKeyCache.java) and `cartDiscounts` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](/src/main/java/com/commercetools/sync/commons/utils/CaffeineReferenceIdToKeyCacheImpl.java).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from CartDiscount to CartDiscountDraft. With help of the cache same reference keys can be reused.
CompletableFuture<List<CartDiscountDraft>> cartDiscountDrafts = CartDiscountTransformUtils.toCartDiscountDrafts(client, referenceIdToKeyCache, cartDiscounts);
````

##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

```` java
final CartDiscountDraft cartDiscountDraft = CartDiscountDraftBuilder.of()
            .cartPredicate("cartPredicate")
            .name(LocalizedString.ofEnglish("foo"))
            .requiresDiscountCode(true)
            .sortOrder("0.1")
            .target(CartDiscountTargetBuilder::shippingBuilder)
            .value(CartDiscountValueAbsoluteDraftBuilder.of()
                    .money(
                            MoneyBuilder.of()
                                    .centAmount(10L)
                                    .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                                    .build())
                    .build())
            .key("cart-discount-key")
            .custom(CustomFieldsDraftBuilder.of().type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("type-key")).fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap())).build()) // note that custom type provided with key
            .build();
````

#### SyncOptions

After the `ProjectApiRoot` is set up, a `CartDiscountSyncOptions` should be built as follows:

````java
// instantiating a CartDiscountSyncOptions
final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder.of(projectApiRoot).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* cart discount draft from the source
* cart discount of the target project (only provided if an existing cart discount could be found)
* the update-actions, which failed (only provided if an existing cart discount could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CartDiscountSync.class);
 final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, cartDiscount, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When the sync process of a particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* cart discount draft from the source 
* cart discount of the target project (only provided if an existing cart discount could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CartDiscountSync.class);
 final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
         .of(projectApiRoot)
         .warningCallback((syncException, draft, cartDiscount, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process, if a target cart discount and a cart discount draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to the commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * cart discount draft from the source
 * cart discount from the target project
 * update actions that were calculated after comparing both

````java
final TriFunction<
        List<CartDiscountUpdateAction>, CartDiscountDraft, CartDiscount, List<CartDiscountUpdateAction>> 
            beforeUpdateCartDiscountCallback =
            (updateActions, newCartDiscountDraft, oldCartDiscount) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof CartDiscountChangeCartPredicateAction))
                    .collect(Collectors.toList());
                        
final CartDiscountSyncOptions cartDiscountSyncOptions = 
        CartDiscountSyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateCartDiscountCallback).build();
````

##### beforeCreateCallback
During the sync process, if a cart discount draft should be created, this callback can be used to intercept the **_create_** request just before it is sent to the commercetools platform.  It contains the following information : 

 * cart discount draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#beforeCreateCallback).

##### batchSize
A number that could be used to set the batch size with which cart discounts are fetched and processed,
as cart discounts are obtained from the target project on the commercetools platform in batches for better performance. The algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding cart discounts from the target project on the commecetools platform in a single request. Playing with this option can slightly improve or reduce processing speed. If it is not set, the default batch size is 50 for cart discount sync.

````java                         
final CartDiscountSyncOptions cartDiscountSyncOptions = 
         CartDiscountSyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. These cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve the overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for cart discount sync.
````java
final CartDiscountSyncOptions cartDiscountSyncOptions = 
         CartDiscountSyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build(); 
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a cart discount sync
final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

// execute the sync on your list of cart discounts
CompletionStage<CartDiscountSyncStatistics> syncStatisticsStage = cartDiscountSync.sync(cartDiscountDrafts);
````
The result of completing the `syncStatisticsStage` in the previous code snippet contains a `CartDiscountSyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created,
failed, processed cart discounts and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CartDiscountSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage();
/*"Summary: 100 cart discounts were processed in total (11 created, 87 updated, 2 failed to sync)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync.
 2. It is not known by the sync which batch is going to be the last one supplied.

 
#### More examples of how to use the sync
 
 1. [Sync from another CTP project as a source](/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/cartdiscounts).
 2. [Sync from an external source](/src/integration-test/java/com/commercetools/sync/integration/externalsource/cartdiscounts).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a `CartDiscount` with a new `CartDiscountDraft` and results in a list of cart discount update actions.
```java
List<CartDiscountUpdateAction> updateActions = CartDiscountSyncUtils.buildActions(cartDiscount, cartDiscountDraft, cartDiscountSyncOptions);
```

### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a `CartDiscount` and a new `CartDiscountDraft`, and in turn builds
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<CartDiscountUpdateAction> updateAction = CartDiscountUpdateActionUtils.buildChangeNameAction(oldCartDiscount, cartDiscountDraft);
````
More examples of those utils for different cart discounts can be found [here](/src/test/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtilsTest.java).

## Migration Guide

The cart-discount-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
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

For client creation use [ClientConfigurationUtils](/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java) which apply the best practices for `ProjectApiRoot` creation.
If you have custom requirements for the client creation make sure to replace `SphereClientFactory` with `ApiRootBuilder` as described in this [Migration Document](https://docs.commercetools.com/sdk/java-sdk-migrate#client-configuration-and-creation).

### Signature of CartDiscountSyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `CartDiscount`, `CartDiscountDraft` and `CartDiscountUpdateAction` from `package com.commercetools.api.models.cart_discount.*`

> Note: Type `UpdateAction<CartDiscount>` has changed to `CartDiscountUpdateAction`. Make sure you create and supply a specific CartDiscountUpdateAction in `beforeUpdateCallback`. For that you can use the [library-utilities](/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountSyncUtils.java) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a cart discount update action to change name taking the 'newName' of the cartDiscountDraft
    final Function<LocalizedString, CartDiscountUpdateAction> createBeforeUpdateAction =
        (newName) -> CartDiscountChangeNameAction.builder().name(newName).build();

// Add the change name action to the list of update actions before update is executed
    final TriFunction<
            List<CartDiscountUpdateAction>, CartDiscountDraft, CartDiscount, List<CartDiscountUpdateAction>>
        beforeUpdateCartDiscountCallback =
            (updateActions, newCartDiscountDraft, oldCartDiscount) -> {
              final CartDiscountUpdateAction beforeUpdateAction =
                  createBeforeUpdateAction.apply(newCartDiscountDraft.getName());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build CartDiscountDraft (syncing from external project)

The cartDiscount-sync expects a list of `CartDiscountDraft`s to process. If you use java-sync-library to sync your cart discounts from any external system into a commercetools platform project you have to convert your data into CTP compatible `CartDiscountDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// CartDiscountDraftBuilder in v1 takes parameters 'name', 'cartPredicate', 'value', 'target', 'sortOrder', 'requiresDiscountCode' 
final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of(
              ofEnglish("name"),
              "1 = 1",
              CartDiscountValue.ofAbsolute(MoneyImpl.of(20, EUR)),
              "sku = \"0123456789\" or sku = \"0246891213\"",
              "0.2",
              false)
          .key("key")
          .active(false)
          .build()

// CartDiscountDraftBuilder in v2
final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(ofEnglish("name"))
            .cartPredicate("1 = 1")
            .value(
              CartDiscountValueAbsoluteDraftBuilder.of()
                .money(
                    CentPrecisionMoneyBuilder.of()
                      .centAmount(20L)
                      .fractionDigits(0)
                      .currencyCode(DefaultCurrencyUnits.EUR.getCurrencyCode())
                      .build()
                )
                .build()
            )
            .target("sku = \"0123456789\" or sku = \"0246891213\"")
            .sortOrder("0.2")
            .requiresDiscountCode(false)
            .key("key")
            .isActive(false)
            .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for Cart Discounts (syncing from CTP project)

If you sync cart discounts between different commercetools projects you probably use [CartDiscountTransformUtils#toCartDiscountDrafts](/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountTransformUtils.java) to transform `CartDiscount` into `CartDiscountDraft` which can be used by the cartDiscount-sync.
However, if you need to query `Cart Discounts` from a commercetools project instead of passing `CartDiscountQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: CartDiscountQuery to fetch all cart discounts
final CartDiscountQuery query = CartDiscountQuery.of();

final PagedQueryResult<CartDiscount> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all cart discounts in one line
final CartDiscountPagedQueryResponse result = apiRoot.cart discounts().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

## Caveats   
1. Syncing cart discounts with a `CartDiscountValue` of type `giftLineItem` is not supported yet. [#411](https://github.com/commercetools/commercetools-sync-java/issues/411).
