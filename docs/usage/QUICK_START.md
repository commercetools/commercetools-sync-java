# Quick Start ⚡

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [1. Installation](#1-installation)
  - [Maven](#maven)
  - [Gradle](#gradle)
- [2. Setup Syncing Options](#2-setup-syncing-options)
- [3. Start Syncing](#3-start-syncing)
- [4. And you're done ✨](#4-and-youre-done-)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### 1. Installation
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

### 2. Setup Syncing Options

 ```java
 final Logger logger = LoggerFactory.getLogger(MySync.class);
 final ProductSyncOptions productsyncOptions = ProductSyncOptionsBuilder
                                 .of(sphereClient)
                                 .errorCallBack(logger::error)
                                 .warningCallBack(logger::warn)
                                 .build();
 ```
 
### 3. Start Syncing
 ````java
 // Transform your product feed batch into a list of ProductDrafts 
 // using your preferred way.
 final List<ProductDraft> productDraftsBatch = ...
 
 final ProductSync productSync = new ProductSync(productSyncOptions);
 
 // execute the sync on your list of products
 final CompletionStage<ProductSyncStatistics> syncStatisticsStage = 
                        productSync.sync(productDraftsBatch);
 ````
### 4. And you're done ✨
 ````java
 final ProductSyncStatistics stats = syncStatisticsStage.toCompletebleFuture()
                                                        .join();
 stats.getReportMessage(); 
 /*"Summary: 2000 products were processed in total 
            (1000 created, 995 updated and 5 failed to sync)."*/
 ````