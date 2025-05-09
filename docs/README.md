![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 10.0.7](https://img.shields.io/badge/Benchmarks-10.0.7-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download from Maven Central](https://img.shields.io/badge/Maven_Central-10.0.7-blue.svg)](https://search.maven.org/artifact/com.commercetools/commercetools-sync-java/10.0.7/jar) 
[![Javadoc](https://javadoc.io/badge2/com.commercetools/commercetools-sync-java/javadoc.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/10.0.7/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)


Java library which allows to import/synchronise (import changes) the data from any arbitrary source to commercetools project.

Supported resources: [Categories](./usage/CATEGORY_SYNC.md), [Products](./usage/PRODUCT_SYNC.md), [InventoryEntries](./usage/INVENTORY_SYNC.md), [ProductTypes](./usage/PRODUCT_TYPE_SYNC.md), [Types](./usage/TYPE_SYNC.md), [CartDiscounts](./usage/CART_DISCOUNT_SYNC.md), [States](./usage/STATE_SYNC.md), [TaxCategories](./usage/TAX_CATEGORY_SYNC.md), [CustomObjects](./usage/CUSTOM_OBJECT_SYNC.md), [Customers](./usage/CUSTOMER_SYNC.md), [ShoppingLists](./usage/SHOPPING_LIST_SYNC.md)

## Usage

Create you own event or cronjob based application and use the library to transform any external data (JSON, CSV, XML, REST API, DB, ...) into [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)) and import those into the commercetools project.

Notes:

- It is often more efficient if you can setup your external data source to provide you only the changes (deltas) instead of the full data set on every import iteration.
- There is dockerized ready-to-use CLI application [commercetools-project-sync](https://github.com/commercetools/commercetools-project-sync) which based on this library can synchronize entire data catalogue between the 2 commercetools projects.
- During a synchronisation, resources are either created or updated, but **not** deleted.

⚡ See the [Quick Start Guide](./usage/QUICK_START.md) for more information on building a product importer!

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/3469524/126317637-a946a81c-2948-4751-86bb-02bcecfeca95.png)

### Prerequisites

- Library requires the min JDK version `>= 8`.
  > The library tested with each major JDK version (i.e: 8, 9, 10, 11, 12, 13...) as well as some specific updates of LTS versions (i.e: 8.0.192, 11.0.3).
- A target commercetools project for syncing your source data to.

### Installation
There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>10.0.7</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:10.0.7'
````
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "10.0.7"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="10.0.7"/>
````
