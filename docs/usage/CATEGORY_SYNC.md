# Category Sync

Module used for importing/syncing Categories into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [Category](https://docs.commercetools.com/http-api-projects-categories.html#category) 
against a [CategoryDraft](https://docs.commercetools.com/http-api-projects-categories.html#categorydraft).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
  - [Sync list of category drafts](#sync-list-of-category-drafts)
    - [Prerequisites](#prerequisites)
    - [About SyncOptions](#about-syncoptions)
    - [Running the sync](#running-the-sync)
  - [Build all update actions](#build-all-update-actions)
  - [Build particular update action(s)](#build-particular-update-actions)
- [Caveats](#caveats)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage

### Sync list of category drafts

<!-- TODO - GITHUB ISSUE#138: Split into explanation of how to "sync from project to project" vs "import from feed"-->

#### Prerequisites

1. The sync expects a list of `CategoryDraft`s that have their `key` fields set to be matched with
categories in the target CTP project. Also, the categories in the target project are expected to have the `key` fields set,
otherwise they won't be matched.

2. Every category may have a reference to a `parent category` and a reference to the `Type` of its custom fields. 
These references are matched by their `key`s. Therefore, in order for the sync to resolve the 
actual ids of the references, their `key`s has to be supplied.
 
   - When syncing from a source commercetools project, you can use [`mapToCategoryDrafts`](https://commercetools.github.io/commercetools-sync-java/v/2.2.1/com/commercetools/sync/categories/utils/CategoryReferenceResolutionUtils.html#mapToCategoryDrafts-java.util.List-)
     method that maps from a `Category` to `CategoryDraft` in order to make them ready for reference resolution by the sync:
     ````java     
     final List<CategoryDraft> categoryDrafts = CategoryReferenceResolutionUtils.mapToCategoryDrafts(categories);
     ````  

3. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

4. After the `sphereClient` is set up, a `CategorySyncOptions` should be built as follows: 
````java
// instantiating a CategorySyncOptions
final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(sphereClient).build();
````

#### About SyncOptions
`SyncOptions` is an object which provides a place for users to add certain configurations to customize the sync process.
Available configurations:

##### 1. `errorCallback`
A callback that is called whenever an error event occurs during the sync process. Each resource executes its own 
error-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the error-event:

* sync exception
* category draft from the source
* category of the target project (only provided if an existing category could be found)
* the update-actions, which failed (only provided if an existing category could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CategorySync.class);
 final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
         .of(sphereClient)
         .errorCallback((syncException, draft, category, updateActions) -> 
            logger.error(new SyncException("My customized message"), syncException)).build();
````
    
##### 2. `warningCallback`
A callback that is called whenever a warning event occurs during the sync process. Each resource executes its own 
warning-callback. When sync process of particular resource runs successfully, it is not triggered. It contains the 
following context about the warning message:

* sync exception
* category draft from the source 
* category of the target project (only provided if an existing category could be found)

##### Example 
````java
 final Logger logger = LoggerFactory.getLogger(CategorySync.class);
 final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
         .of(sphereClient)
         .warningCallback((syncException, draft, category, updateActions) -> 
            logger.warn(new SyncException("My customized message"), syncException)).build();
````

##### 3. `beforeUpdateCallback`
During the sync process if a target category and a category draft are matched, this callback can be used to 
intercept the **_update_** request just before it is sent to commercetools platform. This allows the user to modify 
update actions array with custom actions or discard unwanted actions. The callback provides the following information :
 
 * category draft from the source
 * category from the target project
 * update actions that were calculated after comparing both

##### Example
````java
final TriFunction<
        List<UpdateAction<Category>>, CategoryDraft, Category, List<UpdateAction<Category>>> 
            beforeUpdateCategoryCallback =
            (updateActions, newCategoryDraft, oldCategory) ->  updateActions.stream()
                    .filter(updateAction -> !(updateAction instanceof RemoveAsset))
                    .collect(Collectors.toList());
                        
final CategorySyncOptions categorySyncOptions = 
        CategorySyncOptionsBuilder.of(sphereClient).beforeUpdateCallback(beforeUpdateCategoryCallback).build();
````

##### 4. `beforeCreateCallback`
During the sync process if a category draft should be created, this callback can be used to intercept 
the **_create_** request just before it is sent to commercetools platform.  It contains following information : 

 * category draft that should be created
 
Please refer to [example in product sync document](PRODUCT_SYNC.md#example-set-publish-stage-if-category-references-of-given-product-draft-exists).

##### 5. `batchSize`
A number that could be used to set the batch size with which categories are fetched and processed,
as categories are obtained from the target project on commercetools platform in batches for better performance. The 
algorithm accumulates up to `batchSize` resources from the input list, then fetches the corresponding categories
from the target project on commecetools platform in a single request. Playing with this option can slightly improve or 
reduce processing speed. If it is not set, the default batch size is 50 for category sync.
##### Example
````java                         
final CategorySyncOptions categorySyncOptions = 
         CategorySyncOptionsBuilder.of(sphereClient).batchSize(30).build();
````

#### Running the sync
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

1. [Sync from another CTP project as a source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/categories/CategorySyncIT.java).
2. [Sync from an external source](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/CategorySyncIT.java).

*Make sure to read the [Important Usage Tips](IMPORTANT_USAGE_TIPS.md) for optimal performance.*

### Build all update actions

A utility method provided by the library to compare a Category with a new CategoryDraft and results in a list of category update actions. 
```java
List<UpdateAction<Category>> updateActions = CategorySyncUtils.buildActions(category, categoryDraft, categorySyncOptions);
```

Examples of its usage can be found in the tests 
[here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/test/java/com/commercetools/sync/categories/utils/CategorySyncUtilsTest.java).


### Build particular update action(s)

Utility methods provided by the library to compare the specific fields of a Category and a new CategoryDraft, and in turn, build
 the update action. One example is the `buildChangeNameUpdateAction` which compares names:
````java
Optional<UpdateAction<Category>> updateAction = buildChangeNameUpdateAction(oldCategory, categoryDraft);
````
More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/externalsource/categories/updateactionutils).


## Caveats   
1. The library will sync all field types of custom fields, except `ReferenceType`. [#87](https://github.com/commercetools/commercetools-sync-java/issues/87). 
