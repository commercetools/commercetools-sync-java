![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 11.0.0](https://img.shields.io/badge/Benchmarks-11.0.0-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download from Maven Central](https://img.shields.io/badge/Maven_Central-11.0.0-blue.svg)](https://search.maven.org/artifact/com.commercetools/commercetools-sync-java/11.0.0/jar) 
[![Javadoc](https://javadoc.io/badge2/com.commercetools/commercetools-sync-java/javadoc.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/11.0.0/)
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

- Library requires the min JDK version `>= 17`.
  > Version 11.x of the library uses [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2) 20.x, which is based on Jackson 3. If your application uses JVM SDK 19.x or earlier (based on Jackson 2), or runs on JDK 11 to 16, use version 10.x of this library.
- A target commercetools project for syncing your source data to.

### Installation
There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>11.0.0</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:11.0.0'
````
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "11.0.0"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="11.0.0"/>
````
