![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 5.1.3](https://img.shields.io/badge/Benchmarks-5.1.3-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download from Maven Central](https://img.shields.io/badge/Maven_Central-5.1.3-blue.svg)](https://search.maven.org/artifact/com.commercetools/commercetools-sync-java/5.1.3/jar) 
[![Javadoc](http://javadoc-badge.appspot.com/com.commercetools/commercetools-sync-java.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/5.1.3/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)

More at https://commercetools.github.io/commercetools-sync-java
 
Java library for importing and syncing (taking care of changes) data into one or more commercetools projects from external data files or from another commercetools project.

The library supports synchronising the following entities in commercetools
    
 - [Categories](/docs/usage/CATEGORY_SYNC.md)
 - [Products](/docs/usage/PRODUCT_SYNC.md)
 - [InventoryEntries](/docs/usage/INVENTORY_SYNC.md)
 - [ProductTypes](/docs/usage/PRODUCT_TYPE_SYNC.md)
 - [Types](/docs/usage/TYPE_SYNC.md)
 - [CartDiscounts](/docs/usage/CART_DISCOUNT_SYNC.md)
 - [States](/docs/usage/STATE_SYNC.md)
 - [TaxCategories](/docs/usage/TAX_CATEGORY_SYNC.md)
 - [CustomObjects](/docs/usage/CUSTOM_OBJECT_SYNC.md)
 - [Customers](/docs/usage/CUSTOMER_SYNC.md)
 - [ShoppingLists](/docs/usage/SHOPPING_LIST_SYNC.md)


![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/9512131/31230702-0f2255a6-a9e5-11e7-9412-04ed52641dde.png)
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
- [Roadmap](#roadmap)
- [Release Notes](/docs/RELEASE_NOTES.md)
- [Javadoc](https://commercetools.github.io/commercetools-sync-java/v/5.1.3/)
- [Benchmarks](https://commercetools.github.io/commercetools-sync-java/benchmarks/)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->
## Usage

commercetools sync is a Java library that imports commercetools platform data in the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data from another commercetools project as 
[commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).


> **Note**: During a synchronisation, resources are either created or updated, but **not** deleted.

⚡ See the [Quick Start Guide](/docs/usage/QUICK_START.md) for more information on building a product importer!

🔛 Check out the [commercetools-project-sync](https://github.com/commercetools/commercetools-project-sync) for a ready-to-use CLI application that syncs your entire data catalogue between 2 commercetools projects!

### Prerequisites
 
 - Library is requires the min JDK version `>= 8`.
   > The library tested with each major JDK version (i.e: 8, 9, 10, 11, 12, 13...) as well as some specific updates of LTS versions (i.e: 8.0.192, 11.0.3).        
 - [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a dependency in your JVM-based 
  application. (Make sure to use a version `>= 1.60.0`).
 - a target commercetools project for syncing your source data to.


### Installation

There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:

#### Maven 

````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>5.1.3</version>
</dependency>
````

#### Gradle

````groovy
implementation 'com.commercetools:commercetools-sync-java:5.1.3'
````

#### SBT 

````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "5.1.3"
````

#### Ivy 

````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="5.1.3"/>
````
