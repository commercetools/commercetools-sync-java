![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync java
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Javadoc](https://javadoc.io/badge2/com.commercetools/commercetools-sync-java/javadoc.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/10.0.1/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)


> Note: The current version of this library uses [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2). This doc already contains updated information. Please migrate to this version, using our [Migration Guide](/docs/MIGRATION_GUIDE.md). The support for JVM SDK v1 has been discontinued with [commercetools-sync-java v9.2.3](https://github.com/commercetools/commercetools-sync-java/tree/9.2.3)
> If migration isn't an option for you, you can still use deprecated versions of this library available at [Maven central](https://central.sonatype.com/artifact/com.commercetools/commercetools-sync-java/9.2.3).

More at https://commercetools.github.io/commercetools-sync-java

Java library which allows to import/synchronise (import changes) the data from any arbitrary source to commercetools project.

Supported resources: [Categories](/docs/usage/CATEGORY_SYNC.md), [Products](/docs/usage/PRODUCT_SYNC.md), [InventoryEntries](/docs/usage/INVENTORY_SYNC.md), [ProductTypes](/docs/usage/PRODUCT_TYPE_SYNC.md), [Types](/docs/usage/TYPE_SYNC.md), [CartDiscounts](/docs/usage/CART_DISCOUNT_SYNC.md), [States](/docs/usage/STATE_SYNC.md), [TaxCategories](/docs/usage/TAX_CATEGORY_SYNC.md), [CustomObjects](/docs/usage/CUSTOM_OBJECT_SYNC.md), [Customers](/docs/usage/CUSTOMER_SYNC.md), [ShoppingLists](/docs/usage/SHOPPING_LIST_SYNC.md)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Usage](#usage)
  - [Quick Start](/docs/usage/QUICK_START.md)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
    - [Maven](#maven)
    - [Gradle](#gradle)
    - [SBT](#sbt)
    - [Ivy](#ivy)
- [Release Notes](/docs/RELEASE_NOTES.md)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->
## Usage

Create your own event or cronjob based application and use the library to transform any external data (JSON, CSV, XML, REST API, DB, ...) into [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2) resource draft objects (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-sdk-java-v2/blob/main/commercetools/commercetools-sdk-java-api/src/main/java-generated/com/commercetools/api/models/category/CategoryDraft.java)) and import those into the commercetools project.

Notes:

- It is often more efficient if you can setup your external data source to provide you only the changes (deltas) instead of the full data set on every import iteration.
- There is dockerized ready-to-use CLI application [commercetools-project-sync](https://github.com/commercetools/commercetools-project-sync) which based on this library can synchronize entire data catalogue between the 2 commercetools projects.
- During a synchronisation, resources are either created or updated, but **not** deleted.

⚡ See the [Quick Start Guide](/docs/usage/QUICK_START.md) for more information on building a product importer!

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/3469524/126317637-a946a81c-2948-4751-86bb-02bcecfeca95.png)

### Prerequisites
 
 - Library requires the min JDK version `>= 11`.
   > The library tested with each major JDK version (i.e: 11, 12, 13...) as well as some specific updates of LTS versions (i.e: 11.0.3 and above).
 - A target commercetools project for syncing your source data to.

### Installation

There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:

#### Maven 

````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>10.0.1</version>
</dependency>
````

#### Gradle

````groovy
implementation 'com.commercetools:commercetools-sync-java:10.0.1'
````

#### SBT 

````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "10.0.1"
````

#### Ivy 

````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="10.0.1"/>
````

**Note**: To avoid `commercetools JVM SDK` libraries version mismatch between projects.
 It is better not to add `commercetools JVM SDK` dependencies explicitly into your project and use them from `commercetools-Sync-Java` dependencies instead. 
 Please remove them if you have already added the below dependencies in your project.

For Gradle users, remove: 
```groovy
implementation "com.commercetools.sdk:commercetools-http-client:${version}"
implementation "com.commercetools.sdk:commercetools-sdk-java-api:${version}"
```

For Maven users, remove:

````xml
<dependency>
  <groupId>com.commercetools.sdk</groupId>
  <artifactId>commercetools-http-client</artifactId>
  <version>version</version>
</dependency>
<dependency>
  <groupId>com.commercetools.sdk</groupId>
  <artifactId>commercetools-sdk-java-v2</artifactId>
  <version>version</version>
</dependency>
````

If you want to use a different `commercetools JVM SDK` version than the version used in this project. 
, below you will find examples on how to exclude `commercetools JVM SDK` from commercetools-sync-java library. Beware that library might not work with the older `commercetools JVM SDK` versions.

For Gradle:
````groovy
implementation('com.commercetools:commercetools-sync-java') {
    exclude group: 'com.commercetools.sdk', module: 'commercetools-http-client'
    exclude group: 'com.commercetools.sdk', module: 'commercetools-sdk-java-api'
}
````

For Maven:
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>version</version>
  <exclusions>
    <exclusion>
        <groupId>com.commercetools.sdk</groupId>
        <artifactId>commercetools-http-client</artifactId>
    </exclusion>
    <exclusion>
        <groupId>com.commercetools.sdk</groupId>
        <artifactId>commercetools-sdk-java-v2</artifactId>
    </exclusion>
  </exclusions>
</dependency>
````
