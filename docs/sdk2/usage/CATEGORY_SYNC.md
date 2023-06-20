# Category Sync

Module used for importing/syncing Categories into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Category](https://docs.commercetools.com/api/projects/categories#category) 
against a [CategoryDraft](https://docs.commercetools.com/api/projects/categories#categorydraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Prerequisites](#prerequisites)
    - [ProjectApiRoot](#projectapiroot)
    - [Required Fields](#required-fields)
    - [Reference Resolution](#reference-resolution)
      - [Persistence of Category Drafts with irresolvable parent](#persistence-of-category-drafts-with-irresolvable-parent)
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
  - [Signature of CategorySyncOptions](#signature-of-categorysyncoptions)
  - [Build CategoryDraft (syncing from external project)](#build-categorydraft-syncing-from-external-project)
  - [Query for Categories (syncing from CTP project)](#query-for-categories-syncing-from-ctp-project)
  - [JVM-SDK-V2 migration guide](#jvm-sdk-v2-migration-guide)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Prerequisites

#### ProjectApiRoot

Use the [ClientConfigurationUtils](#todo) which apply the best practices for `ProjectApiRoot` creation.
To create `ClientCredentials` which are required for creating a client please use the `ClientCredentialsBuilder` provided in java-sdk-v2 [Client OAUTH2 package](#todo)
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

The following fields are **required** to be set in, otherwise they won't be matched by sync:

|Draft|Required Fields|Note|
|---|---|---|
| [CategoryDraft](https://docs.commercetools.com/api/projects/categories#categorydraft) | `key` |  Also, the categories in the target project are expected to have the `key` fields set. | 

#### Reference Resolution 

In commercetools, a reference can be created by providing the key instead of the ID with the type [ResourceIdentifier](https://docs.commercetools.com/api/types#resourceidentifier).
When the reference key is provided with a `ResourceIdentifier`, the sync will resolve the resource with the given key and use the ID of the found resource to create or update a reference.
Therefore, in order to resolve the actual ids of those references in sync process, `ResourceIdentifier`s with their `key`s have to be supplied. 

|Reference Field| Type                             |
|:---|:---------------------------------|
| `parent` | CategoryResourceIdentifier       |  
| `custom.type` | TypeResourceIdentifier           |  
| `assets.custom.type` | TypeResourceIdentifier | 

> Note that a reference without the key field will be considered as existing resource on the target commercetools project and the library will issue an update/create an API request without reference resolution.

##### Persistence of Category Drafts with irresolvable parent

A CategoryDraft X could have a parent Category Y. But It could be that the parent Category Y is not supplied before X, 
which means the sync could fail to create/updating X. It could also be that Y is not supplied at all in this batch but at a later batch.
 
The library keeps track of such "referencing" Category Drafts like X and persists them in storage 
(**Commercetools platform `customObjects` in the target project** , in this case) 
to keep them and create/update them accordingly whenever the referenced drafts exist in the target project.

The `customObject` will have a `container:` **`"commercetools-sync-java.UnresolvedReferencesService.categoryDrafts"`**
and a `key` representing the key of the  category Drafts that is waiting to be created/updated.

Here is an example of a `CustomObject` in the target project that represents a category Draft with the key `categoryKey1`.  
Being persisted as `CustomObject` means that the referenced parent Category with the key `nonExistingParent` does not exist yet.

```json
{
      "id": "81dcb42f-1959-4412-a4bb-8ad420d0d11f",
      "version": 1,
      "createdAt": "2021-01-13T12:19:45.937Z",
      "lastModifiedAt": "2021-01-13T12:19:45.937Z",
      "lastModifiedBy": {
        "clientId": "7OSAGVPscneW_KS4nqskFkrd",
        "isPlatformClient": false
      },
      "createdBy": {
        "clientId": "7OSAGVPscneW_KS4nqskFkrd",
        "isPlatformClient": false
      },
      "container": "commercetools-sync-java.UnresolvedReferencesService.categoryDrafts",
      "key": "8732a63fa8ca457e86f4075340d65154e7e2476a",
      "value": {
        "missingReferencedKeys": [
          "nonExistingParent"
        ],
        "waitingDraft": {
          "custom": {
            "type": {
              "key": "oldCategoryCustomTypeKey"
            },
            "fields": {
              "backgroundColor": {
                "de": "rot",
                "en": "red"
              },
              "invisibleInShop": false
            }
          },
          "key": "categoryKey1",
          "name": {
            "en": "furniture"
          },
          "parent": {
            "key": "nonExistingParent"
          },
          "slug": {
            "en": "new-furniture1"
          }
        }
      }
    }
 
```
As soon, as the referenced parent Category Draft is supplied to the sync, the Category will be created/updated and the 
`CustomObject` will be removed from the target project.

##### Syncing from a commercetools project

When syncing from a source commercetools project, you can use [`toCategoryDrafts`](#todo)
method that transforms(resolves by querying and caching key-id pairs) and maps from a `Category` to `CategoryDraft` using cache in order to make them ready for reference resolution by the sync, for example: 

````java
// Build ByProjectKeyCategoriesGet for fetching categories from a source CTP project without any references expanded for the sync:
final ByProjectKeyCategoriesGet byProjectKeyCategoriesGet = client.categories().get();

// Query all categories (NOTE this is just for example, please adjust your logic)
final List<Categories> categories = QueryUtils.queryAll(byProjectKeyCategoriesGet,
            (categories) -> categories)
            .thenApply(lists -> lists.stream().flatMap(List::stream).collect(Collectors.toList()))
            .toCompletableFuture()
            .join();
````

In order to transform and map the `Category` to `CategoryDraft`, 
Utils method `toCategoryDrafts` requires `projectApiRoot`, implementation of [`ReferenceIdToKeyCache`](#todo) and `categories` as parameters.
For cache implementation, You can use your own cache implementation or use the class in the library - which implements the cache using caffeine library with an LRU (Least Recently Used) based cache eviction strategy[`CaffeineReferenceIdToKeyCacheImpl`](#todo).
Example as shown below:

````java
//Implement the cache using library class.
final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

//For every reference fetch its key using id, cache it and map from Category to CategoryDraft. With help of the cache same reference keys can be reused.
final CompletableFuture<List<CategoryDraft>> categoryDrafts = CategoryTransformUtils.toCategoryDrafts(client, referenceIdToKeyCache, categories);
````

##### Syncing from an external resource

- When syncing from an external resource, `ResourceIdentifier`s with their `key`s have to be supplied as following example:

````java
final CategoryDraft categoryDraft = 
    CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug"))
                        .parent(ResourceIdentifier.ofKey("parent-category-key")) // note that parent provided with key
                        .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
                        .assets(singletonList(
                            AssetDraftBuilder.of(emptyList(), LocalizedString.ofEnglish("asset-name"))
                                             .custom(CustomFieldsDraft.ofTypeKeyAndJson("type-key", emptyMap())) // note that custom type provided with key
                                             .key("asset-key")
                                             .build()
                        ))
                        .build();
````


````java
final CustomFieldsDraft customFields = CustomFieldsDraftBuilder.of()
                                       .type(TypeResourceIdentifierBuilder.of().key("type-key").build()) // note that custom type provided with key
                                       .fields(FieldContainerBuilder.of().values(Collections.emptyMap()).build())
                                       .build();

final CategoryResourceIdentifier categoryResourceIdentifier = CategoryResourceIdentifierBuilder.of()
        .key("category-key")
        .build();

final CategoryDraft categoryDraft = CategoryDraftBuilder.of()
        .key("category-key")
        .slug(LocalizedString.of(Locale.ENGLISH, "category-slug"))
        .name(LocalizedString.of(Locale.ENGLISH, "category-name"))
        .description(LocalizedString.of(Locale.ENGLISH, "category-description"))
        .externalId("external-id")
        .metaDescription(LocalizedString.of(Locale.ENGLISH, "meta-description"))
        .metaKeywords(LocalizedString.of(Locale.ENGLISH, "meta-keywords"))
        .metaTitle(LocalizedString.of(Locale.ENGLISH, "meta-title"))
        .orderHint("order-hint")
        .custom(customFields)
        .parent(categoryResourceIdentifier)
        .build()
````

#### SyncOptions

After the `ProjectApiRoot` is set up, a `CategorySyncOptions` should be built as follows:
````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(projectApiRoot).build();
````

`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### errorCallback
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* category draft from the source
* category of the target project (only provided if an existing category could be found)
* the update-actions, which failed (only provided if an existing category could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CategorySync.class);
 final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
         .of(projectApiRoot)
         .errorCallback((syncException, draft, category, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### warningCallback
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* category draft from the source 
* category of the target project (only provided if an existing category could be found)

````java
 final Logger logger = LoggerFactory.getLogger(CategorySync.class);
 final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
         .of(projectApiRoot)
         .warningCallback((syncException, draft, category, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### beforeUpdateCallback
During the sync process if a target category and a category draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * category draft from the source
 * category from the target project
 * update actions that were calculated after comparing both

````java
// Example: Ignore update actions that change category name
final TriFunction<
        List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>> 
            beforeUpdateCategoryCallback =
            (updateActions, newCategoryDraft, oldCategory) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof CategoryChangeNameAction))
                    .collect(Collectors.toList());
                        
final CategorySyncOptions categorySyncOptions = 
        CategorySyncOptionsBuilder.of(projectApiRoot).beforeUpdateCallback(beforeUpdateCategoryCallback).build();
````

##### beforeCreateCallback
During the sync process if a category draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * category draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#beforeCreateCallback).

##### batchSize
A number that could be used to set the batch size with which categories are fetched and processed,
as categories are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding categories
from the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is **50** for category sync.

````java                         
final CategorySyncOptions categorySyncOptions = 
         CategorySyncOptionsBuilder.of(projectApiRoot).batchSize(30).build();
````

##### cacheSize
In the service classes of the commercetools-sync-java library, we have implemented an in-memory [LRU cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)) to store a map used for the reference resolution of the library.
The cache reduces the reference resolution based calls to the commercetools API as the required fields of a resource will be fetched only one time. This cached fields then might be used by another resource referencing the already resolved resource instead of fetching from commercetools API. It turns out, having the in-memory LRU cache will improve overall performance of the sync library and commercetools API.
which will improve the overall performance of the sync and commercetools API.

Playing with this option can change the memory usage of the library. If it is not set, the default cache size is `10.000` for category sync.
````java
final CategorySyncOptions categorySyncOptions = 
         CategorySyncOptionsBuilder.of(projectApiRoot).cacheSize(5000).build(); 
````

### Running the sync
After all the aforementioned points in the previous section have been fulfilled, to run the sync:
````java
// instantiating a category sync
final CategorySync categorySync = new CategorySync(categorySyncOptions);

// execute the sync on your list of categories
CompletionStage<CategorySyncStatistics> syncStatisticsStage = categorySync.sync(categoryDrafts);
````
The result of the completing the `syncStatisticsStage` in the previous code snippet contains a `CategorySyncStatistics`
which contains all the stats of the sync process; which includes a report message, the total number of updated, created, 
failed, processed categories and the processing time of the last sync batch in different time units and in a
human-readable format.

````java
final CategorySyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
stats.getReportMessage(); 
/*"Summary: 2000 categories were processed in total (1000 created, 995 updated, 5 failed to sync and 0 categories with a missing parent)."*/
````

__Note__ The statistics object contains the processing time of the last batch only. This is due to two reasons:

 1. The sync processing time should not take into account the time between supplying batches to the sync. 
 2. It is not known by the sync which batch is going to be the last one supplied.

##### More examples of how to use the sync

1. [Sync from another CTP project as a source](#todo).
2. [Sync from an external source](#todo).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a Category with a new CategoryDraft and results in a list of category update actions. 
```java
final List<CategoryUpdateAction> updateActions = CategorySyncUtils.buildActions(category, categoryDraft, categorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](#todo).


### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a Category and a new CategoryDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
final Optional<CategoryUpdateAction> updateAction = buildChangeNameUpdateAction(oldCategory, categoryDraft);
````
More examples of those utils for different fields can be found [here](#todo).

## Migration Guide

The category-sync uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
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

For client creation use [ClientConfigurationUtils](#todo) which apply the best practices for `ProjectApiRoot` creation.
If you have custom requirements for the client creation make sure to replace `SphereClientFactory` with `ApiRootBuilder` as described in this [Migration Document](https://docs.commercetools.com/sdk/java-sdk-migrate#client-configuration-and-creation).

### Signature of CategorySyncOptions

As models and update actions have changed in the JVM-SDK-V2 the signature of SyncOptions is different. It's constructor now takes a `ProjectApiRoot` as first argument. The callback functions are signed with `CategoryDraft`, `CategoryProjection` and `CategoryUpdateAction` from `package com.commercetools.api.models.category.*`

> Note: Type `UpdateAction<Category>` has changed to `CategoryUpdateAction`. Make sure you create and supply a specific CategoryUpdateAction in `beforeUpdateCallback`. For that you can use the [library-utilities](#todo) or use a JVM-SDK builder ([see also](https://docs.commercetools.com/sdk/java-sdk-migrate#update-resources)):

```java
// Example: Create a category update action to change name taking the 'newName' of the categoryDraft
    final Function<LocalizedString, CategoryUpdateAction> createBeforeUpdateAction =
        (newName) -> CategoryChangeNameAction.builder().name(newName).build();

// Add the change name action to the list of update actions before update is executed
    final TriFunction<
            List<CategoryUpdateAction>, CategoryDraft, CategoryProjection, List<CategoryUpdateAction>>
        beforeUpdateCategoryCallback =
            (updateActions, newCategoryDraft, oldCategory) -> {
              final CategoryUpdateAction beforeUpdateAction =
                  createBeforeUpdateAction.apply(newCategoryDraft.getName());
              updateActions.add(beforeUpdateAction);
              return updateActions;
            };
```

### Build CategoryDraft (syncing from external project)

The category-sync expects a list of `CategoryDraft`s to process. If you use java-sync-library to sync your categories from any external system into a commercetools platform project you have to convert your data into CTP compatible `CategoryDraft` type. This was done in previous version using `DraftBuilder`s.
The V2 SDK do not have inheritance for `DraftBuilder` classes but the differences are minor and you can replace it easily. Here's an example:

```java
// CategoryDraftBuilder in v1 takes parameters 'name' and 'slug'
final CategoryDraft categoryDraft =
              CategoryDraftBuilder
                      .of(ofEnglish("name"), ofEnglish("slug"))
                      .key("category-key")
                      .build();

// CategoryDraftBuilder in v2
final CategoryDraft categoryDraft =
              CategoryDraftBuilder
                      .of()
                      .name(LocalizedString.ofEnglish("name"))
                      .slug(LocalizedString.ofEnglish("slug"))
                      .key("category-key")
                      .build();
```
For more information, see the [Guide to replace DraftBuilders](https://docs.commercetools.com/sdk/java-sdk-migrate#using-draftbuilders).

### Query for Categories (syncing from CTP project)

If you sync categories between different commercetools projects you probably use [CategoryTransformUtils#toCategoryDrafts](#todo) to transform `Category` into `CategoryDraft` which can be used by the category-sync.
However, if you need to query `Categories` from a commercetools project instead of passing `CategoryQuery`s to a `sphereClient`, create (and execute) requests directly from the `apiRoot`.
Here's an example:

```java
// SDK v1: CategoryQuery to fetch all categories
final CategoryQuery query = CategoryQuery.of();

final PagedQueryResult<Category> pagedQueryResult = sphereClient.executeBlocking(query);

// SDK v2: Create and execute query to fetch all categories in one line
final CategoryPagedQueryResponse result = apiRoot.categories().get().executeBlocking().getBody();
```
[Read more](https://docs.commercetools.com/sdk/java-sdk-migrate#query-resources) about querying resources.

### JVM-SDK-V2 migration guide

On any other needs to migrate your project using jvm-sdk-v2 please refer to it's [Migration Guide](https://docs.commercetools.com/sdk/java-sdk-migrate). 
