![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 6.0.0](https://img.shields.io/badge/Benchmarks-6.0.0-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download from Maven Central](https://img.shields.io/badge/Maven_Central-6.0.0-blue.svg)](https://search.maven.org/artifact/com.commercetools/commercetools-sync-java/6.0.0/jar) 
[![Javadoc](http://javadoc-badge.appspot.com/com.commercetools/commercetools-sync-java.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/6.0.0/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)


Java library which allows to import/synchronise (import changes) the data from any arbitrary source to commercetools project.

Supported resources: [Categories](/docs/usage/CATEGORY_SYNC.md), [Products](/docs/usage/PRODUCT_SYNC.md), [InventoryEntries](/docs/usage/INVENTORY_SYNC.md), [ProductTypes](/docs/usage/PRODUCT_TYPE_SYNC.md), [Types](/docs/usage/TYPE_SYNC.md), [CartDiscounts](/docs/usage/CART_DISCOUNT_SYNC.md), [States](/docs/usage/STATE_SYNC.md), [TaxCategories](/docs/usage/TAX_CATEGORY_SYNC.md), [CustomObjects](/docs/usage/CUSTOM_OBJECT_SYNC.md), [Customers](/docs/usage/CUSTOMER_SYNC.md), [ShoppingLists](/docs/usage/SHOPPING_LIST_SYNC.md)
             
 1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
 [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
 (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).
 
 2. Synchronise data from another commercetools project as 
 [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
 (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).
 
 
 > Synchronise: Resources will either be created or updated. But they will **not** be deleted.
 
 ⚡ See the [Quick Start Guide](https://commercetools.github.io/commercetools-sync-java/doc/usage/QUICK_START/) for more information on building a product importer!
 
 🔛 Check out the [commercetools-project-sync](https://github.com/commercetools/commercetools-project-sync) for a ready-to-use CLI application that syncs your entire data catalogue between 2 commercetools projects! 

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/3469524/126317637-a946a81c-2948-4751-86bb-02bcecfeca95.png)

### Prerequisites

- Library requires the min JDK version `>= 8`.
  > The library tested with each major JDK version (i.e: 8, 9, 10, 11, 12, 13...) as well as some specific updates of LTS versions (i.e: 8.0.192, 11.0.3).
- [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a dependency in your JVM-based
  application. (Make sure to use a version `>= 1.64.0`).
- A target commercetools project for syncing your source data to.

### Installation
There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>6.0.0</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:6.0.0'
````
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "6.0.0"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="6.0.0"/>
````
