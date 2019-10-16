# ⚡ Quick Start

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [1. Installation](#1-installation)
- [2. Setup Syncing Options](#2-setup-syncing-options)
- [3. Start Syncing](#3-start-syncing)
- [4. And you're done ✨](#4-and-youre-done-)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### 1. Installation
- Make sure you have `JDK 8` installed.
- Add the following dependencies in your application:
- For Maven users: 
````xml
<!-- Add commercetools-jvm-sdk dependencies. -->
<dependency>
  <groupId>com.commercetools.sdk.jvm.core</groupId>
  <artifactId>commercetools-models</artifactId>
  <version>1.46.0</version>
</dependency>
<dependency>
  <groupId>com.commercetools.sdk.jvm.core</groupId>
  <artifactId>commercetools-java-client</artifactId>
  <version>1.46.0</version>
</dependency>
<dependency>
  <groupId>com.commercetools.sdk.jvm.core</groupId>
  <artifactId>commercetools-convenience</artifactId>
  <version>1.46.0</version>
</dependency>

<!-- Add commercetools-sync-java dependency. -->
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>1.6.0</version>
</dependency>
````
- For Gradle users:
````groovy
// Add commercetools-jvm-sdk dependencies.
implementation 'com.commercetools.sdk.jvm.core:commercetools-models:1.37.0'
implementation 'com.commercetools.sdk.jvm.core:commercetools-java-client:1.37.0'
implementation 'com.commercetools.sdk.jvm.core:commercetools-convenience:1.37.0'

// Add commercetools-sync-java dependency.
implementation 'com.commercetools:commercetools-sync-java:1.6.0'
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
 // Transform your product feed batch into a list of ProductDrafts using your preferred way.
 final List<ProductDraft> productDraftsBatch = ...
 
 final ProductSync productSync = new ProductSync(productSyncOptions);
 
 // execute the sync on your list of products
 final CompletionStage<ProductSyncStatistics> syncStatisticsStage = productSync.sync(productDraftsBatch);
 ````
### 4. And you're done ✨
 ````java
 final ProductSyncStatistics stats = syncStatisticsStage.toCompletebleFuture()
                                                        .join();
 stats.getReportMessage(); 
 /*"Summary: 2000 product(s) were processed in total (1000 created, 995 updated, 5 failed to sync and 0 
 product(s) with missing reference(s))."*/
 ````


#### More Details
*[Product Sync](PRODUCT_SYNC.md), [ProductType Sync](PRODUCT_TYPE_SYNC.md), 
[Category Sync](CATEGORY_SYNC.md), [Inventory Sync](INVENTORY_SYNC.md), 
[Type Sync](TYPE_SYNC.md), [CartDiscount Sync](CART_DISCOUNT_SYNC.md)*
