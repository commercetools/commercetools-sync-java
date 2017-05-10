# commercetools category sync

Java module that can be used to synchronise your new commercetools categories to your existing 
commercetools project.

- [What it offers?](#what-it-offers)
- [How to use it?](#how-to-use-it)
- [How does it work?](#how-does-it-work)


## What it offers?

1. Synchronise categories coming from an external system, in any form (CSV, XML, etc..), that has been already mapped to 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
[CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java) 
objects to the desired commercetools project.
2. Synchronise categories coming from an already-existing commercetools project in the form of 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
[Category](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/Category.java)
objects to another commercetools project.

3. Build any of the following commercetools [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) update action
objects given an old category, represented by a [Category](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/Category.java),
and a new category, represented by a [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java):
    - ChangeName
    - ChangeSlug
    - SetDescription
    - ChangeParent
    - ChangeOrderHint
    - SetMetaTitle
    - SetMetaKeywords
    - SetMetaDescription
    - category custom fields update actions:
        - SetCustomType
        - SetCustomField

## How to use it?
In order to use the category sync an instance of
 [CategorySyncOptions](https://github.com/commercetools/commercetools-sync-java/blob/develop/src/main/java/com/commercetools/sync/categories/CategorySyncOptions.java) have to be injected.
 
 In order to instantiate a `CategorySyncOptions`, a `ctpClient` is required:
  #### `ctpClient` [Required]
  Defines the configuration of the commercetools project that categories are going to be synced to. 
  ````java
  // instantiating a ctpClient
  final SphereClientConfig clientConfig = SphereClientConfig.of("project-key", "client-id", "client-secret");
  final CtpClient ctpClient = new CtpClient(clientConfig);
  
  // instantiating a CategorySyncOptions
  final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient).build();
  ````
  
  The category sync can then do any of the following:
  ##### sync
  Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
  [Category](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/Category.java)
  objects.
  ````java
  // instantiating a category sync
  final CategorySync categorySync = new CategorySync(categorySyncOptions);
  
  // execute the sync on your list of categories
  categorySync.sync(categories);
  ````
  
  ##### syncDrafts
  Used to sync a list of [JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) 
  [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java) 
  objects.
  ````java
  // instantiating a category sync
  final CategorySync categorySync = new CategorySync(categorySyncOptions);
  
  // execute the sync on your list of categories
  categorySync.syncDrafts(categoryDrafts);
  ````
  ##### getStatistics
  Used to get an object  containing all the stats of the sync process; which includes a report message, the total number
  of updated, created, failed, processed categories and the processing time of the sync in different time units and in a
  human readable format.
  ````java
  categorySync.syncDrafts(categoryDrafts);
  categorySync.getStatistics().getCreated(); // 1000
  categorySync.getStatistics().getFailed(); // 5
  categorySync.getStatistics().getUpdated(); // 995
  categorySync.getStatistics().getProcessed(); // 2000
  categorySync.getStatistics().getReportMessage(); 
  /*"Summary: 2000 categories were processed in total (1000 created, 995 updated and 5 categories failed to sync).");*/
   ````
    
  <!--- TODO Also add code snippets for building update actions utils! -->
  
  Additional configuration for the sync can be configured on the `CategorySyncOptions` instance, according to the need 
  of the user of the sync:
  #### `errorCallBack` [Optional]
  Defines an optional field which represents a callback function which will be called whenever an event occurs
  that leads to an error alert from the sync process. The function takes two parameters: a `String` error message and a 
  `Throwable` exception. 
  
  An example of setting a callback would be for example logging the error as follows:
  ````java
  // instantiate a logger
  final Logger LOGGER = LoggerFactory.getLogger(myclass.class);

  // instantiating a CategorySyncOptions
  final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                    .of(ctpClient)
                                                    .setErrorCallBack(LOGGER::error)
                                                    .build();
  ````
  
  #### `warningCallBack` [Optional]
  Defines an optional field which represents a callback function which will be called whenever an event occurs
  that leads to an warning alert from the sync process. The function takes one parameter: a `String` error message.
  An example of setting a callback would be for example logging the error as follows:
      
  ````java
   // instantiate a logger
   final Logger LOGGER = LoggerFactory.getLogger(myclass.class);
    
   // instantiating a CategorySyncOptions
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .setWarningCallBack(LOGGER::warn)
                                                        .build();
   ````
  
  #### `removeOtherLocales` [Optional]
  Defines an optional field which represents a boolean flag which adds additional localizations without deleting
  existing ones. If set to true, which is the default value of the option, it deletes the existing localizations. 
  If set to false, it doesn't delete the existing ones.
  ````java
  // instantating options which deletes the existing localizations
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .setRemoveOtherLocales(true)
                                                        .build();
  ````
  #### `removeOtherSetEntries` [Optional]
  Defines an optional field which represents a boolean flag which adds additional Set entries without deleting
  existing ones. If set to true, which is the default value of the option, it deletes the existing Set entries. 
  If set to false, it doesn't delete the existing ones.
  ````java
  // instantating options which deletes the existing Set entries
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .removeOtherSetEntries(true)
                                                        .build();
  ````
  #### `removeOtherCollectionEntries` [Optional]
  Defines an optional field which represents a boolean flag which adds collection (e.g. Assets, Images etc.) entries 
  without deleting existing ones. If set to true, which is the default value of the option, it deletes the existing 
  collection entries. If set to false, it doesn't delete the existing ones.
  ````java
  // instantating options which deletes the existing collection entries
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .removeOtherCollectionEntries(true)
                                                        .build();
  ````
  #### `removeOtherProperties` [Optional]
  Defines an optional field which represents a boolean flag which adds object properties (e.g. custom fields, product attributes, etc..) 
  without deleting existing ones. If set to true, which is the default value of the option, it deletes the existing 
  object properties. If set to false, it doesn't delete the existing ones.
  ````java
  // instantating options which deletes the existing object properties
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .removeOtherProperties(true)
                                                        .build();
  ````
  #### `updateActionsFilter` [Optional]
  Defines an optional field which represents updateActions filter function which can be applied on generated list of 
  update actions to produce a resultant list after the filter function has been applied.
  ````java
  
  // instanting a reverse filter function
  final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> reverseOrderFilter = (unfilteredList) -> {
              Collections.reverse(unfilteredList);
              return unfilteredList;
          };
  // instantating options which reverses an update actions list order
   final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
                                                        .of(ctpClient)
                                                        .setUpdateActionsFilter(reverseOrderFilter)
                                                        .build();
  ````
  
  
  

## How does it work?

The category sync uses the `externalId` to match new categories to existing ones. If a category with the same 
`externalId` is found we will call it an update as the tool will then update the existing category properties - like name
 etc. - to those values defined in the category. If no matching category is found the tool will create a new one. The 
 sync, however, will never delete a category.
 
 When two categories are matched, the sync will compute a list of update actions that are needed for existing category to
 be exactly the same as the new one. This list of update actions will then be issued to the commercetools platform, eventually
 updating the category.