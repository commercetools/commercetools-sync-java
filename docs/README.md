![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![CI](https://github.com/commercetools/commercetools-sync-java/workflows/CI/badge.svg)](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACI)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 5.1.2](https://img.shields.io/badge/Benchmarks-5.1.2-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download from JCenter](https://img.shields.io/badge/Bintray_JCenter-5.1.2-green.svg) ](https://bintray.com/commercetools/maven/commercetools-sync-java/_latestVersion)
[![Download from Maven Central](https://img.shields.io/badge/Maven_Central-5.1.2-blue.svg)](https://search.maven.org/artifact/com.commercetools/commercetools-sync-java/5.1.2/jar) 
[![Javadoc](http://javadoc-badge.appspot.com/com.commercetools/commercetools-sync-java.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/5.1.2/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)

 
Java library that imports commercetools platform data in the following ways:
             
 1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
 [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
 (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).
 
 2. Synchronise data from another commercetools project as 
 [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
 (e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).
 
 
 > Synchronise: Resources will either be created or updated. But they will **not** be deleted.
 
 ⚡ See the [Quick Start Guide](https://commercetools.github.io/commercetools-sync-java/doc/usage/QUICK_START/) for more information on building a product importer!
 
 🔛 Check out the [commercetools-project-sync](https://github.com/commercetools/commercetools-project-sync) for a ready-to-use CLI application that syncs your entire data catalogue between 2 commercetools projects! 

The library supports synchronising the following entities in commercetools
    
 - [Categories](usage/CATEGORY_SYNC.md)
 - [Products](usage/PRODUCT_SYNC.md)
 - [InventoryEntries](usage/INVENTORY_SYNC.md)
 - [ProductTypes](usage/PRODUCT_TYPE_SYNC.md)
 - [Types](usage/TYPE_SYNC.md)
 - [CartDiscounts](usage/CART_DISCOUNT_SYNC.md)
 - [States](usage/STATE_SYNC.md)
 - [TaxCategories](/docs/usage/TAX_CATEGORY_SYNC.md)
 - [CustomObjects](/docs/usage/CUSTOM_OBJECT_SYNC.md)
 - [Customers](/docs/usage/CUSTOMER_SYNC.md)
 - [ShoppingLists](/docs/usage/SHOPPING_LIST_SYNC.md)

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/9512131/31230702-0f2255a6-a9e5-11e7-9412-04ed52641dde.png)


### Prerequisites
 
 - Make sure you have `JDK 8` installed.
 - [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a dependency in your JVM-based 
  application. (Make sure to use a version `>=` [1.60.0](https://search.maven.org/artifact/com.commercetools.sdk.jvm.core/commercetools-jvm-sdk/1.60.0/pom)).
 - a target commercetools project for syncing your source data to.


### Installation
There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>5.1.2</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:5.1.2'
````
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "5.1.2"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="5.1.2"/>
````
