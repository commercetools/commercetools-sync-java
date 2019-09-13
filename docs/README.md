![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-sync-java.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sync-java)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks 1.5.0](https://img.shields.io/badge/Benchmarks-1.5.0-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download](https://api.bintray.com/packages/commercetools/maven/commercetools-sync-java/images/download.svg) ](https://bintray.com/commercetools/maven/commercetools-sync-java/_latestVersion)
[![Javadoc](http://javadoc-badge.appspot.com/com.commercetools/commercetools-sync-java.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/1.5.0/)
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

The library supports synchronising the following entities in commercetools
    
 - [Categories](usage/CATEGORY_SYNC.md)
 - [Products](usage/PRODUCT_SYNC.md)
 - [InventoryEntries](usage/INVENTORY_SYNC.md)
 - [ProductTypes](usage/PRODUCT_TYPE_SYNC.md)
 - [Types](usage/TYPE_SYNC.md)
 - [CartDiscounts](usage/CART_DISCOUNT_SYNC.md)

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/9512131/31230702-0f2255a6-a9e5-11e7-9412-04ed52641dde.png)


### Prerequisites
 
 - Make sure you have `JDK 8` installed.
 - [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a dependency in your JVM-based 
  application. (Make sure to use a version `>= 1.46.0`).
 - a target commercetools project for syncing your source data to.


### Installation
There are multiple ways to add the commercetools sync dependency to your project, based on your dependency manager. 
Here are the most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>1.5.0</version>
</dependency>
````
#### Gradle
````groovy
implementation 'com.commercetools:commercetools-sync-java:1.5.0'
````
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "1.5.0"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="1.5.0"/>
````
