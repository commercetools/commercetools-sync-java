# Quick Start

### Installation
There are multiple ways to download the commercetools sync dependency, based on your dependency manager. Here are the 
most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>v1.0.0-M14</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:v1.0.0-M14'
````


### Import Products

#### Setup Syncing Options

 ```java
 final Logger logger = LoggerFactory.getLogger(MySync.class);
 final ProductSyncOptions productsyncOptions = ProductSyncOptionsBuilder.of(sphereClient)
                                                                        .errorCallBack(logger::error)
                                                                        .warningCallBack(logger::warn)
                                                                        .build();
 ```
 
#### Start Syncing
 ````java
 
 // Transform your product feed batch into a list of ProductDrafts using your prefered way.
 final List<ProductDraft> productDraftsBatch = ...
 
 final ProductSync productSync = new ProductSync(productSyncOptions);
 
 // execute the sync on your list of products
 CompletionStage<ProductSyncStatistics> syncStatisticsStage = productSync.sync(productDraftsBatch);
 ````
#### And you're Done!
 ````java
 final ProductSyncStatistics stats = syncStatisticsStage.toCompletebleFuture().join();
 stats.getReportMessage(); 
 /*"Summary: 2000 products were processed in total (1000 created, 995 updated and 5 failed to sync)."*/
 ````