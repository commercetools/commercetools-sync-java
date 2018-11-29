![commercetools-java-sync-logos 002](https://user-images.githubusercontent.com/9512131/31182587-90d47f0a-a924-11e7-9716-66e6bec7f79b.png)
# commercetools sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-sync-java.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sync-java)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)
[![Benchmarks M14](https://img.shields.io/badge/Benchmarks-M14-orange.svg)](https://commercetools.github.io/commercetools-sync-java/benchmarks/)
[![Download](https://api.bintray.com/packages/commercetools/maven/commercetools-sync-java/images/download.svg) ](https://bintray.com/commercetools/maven/commercetools-sync-java/_latestVersion)
[![Javadoc](http://javadoc-badge.appspot.com/com.commercetools/commercetools-sync-java.svg?label=Javadoc)](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/)
[![Known Vulnerabilities](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646/badge.svg)](https://snyk.io/test/github/commercetools/commercetools-sync-java/4b2e26113d591bda158217c5dc1cf80a88665646)

 
Java Library used to import and/or sync (taking care of changes) data into one or more commercetools projects from external sources such 
as CSV, XML, JSON, etc.. or even from an already existing commercetools project.

Currently this library supports synchronising the following entities in commercetools
    
 - [Categories](/docs/usage/CATEGORY_SYNC.md)
 - [Products](/docs/usage/PRODUCT_SYNC.md)
 - [InventoryEntries](/docs/usage/INVENTORY_SYNC.md)
 - [ProductTypes](/docs/usage/PRODUCT_TYPE_SYNC.md)

![commercetools-java-sync-final 001](https://user-images.githubusercontent.com/9512131/31230702-0f2255a6-a9e5-11e7-9412-04ed52641dde.png)

## Usage

The library could be used to synchronise CTP data in any of the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data coming from an already-existing commercetools project in the form of 
[commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).


> Synchronise: Resources will either be created or updated. But they will **not** be deleted.

⚡ Take a look at the [Quick Start Guide](/docs/usage/QUICK_START.md) to find out how to build a product importer in a glance!

### Prerequisites
 
 - Make sure you have `JDK 8` installed.
 - a target CTP project to which your source of data would be synced to.
 - [commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a dependency in your JVM-based 
  application. (Make sure to use a version `>= 1.35.0`).


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
#### SBT 
````
libraryDependencies += "com.commercetools" % "commercetools-sync-java" % "v1.0.0-M14"
````
#### Ivy 
````xml
<dependency org="com.commercetools" name="commercetools-sync-java" rev="v1.0.0-M14"/>
````


## Roadmap
https://github.com/commercetools/commercetools-sync-java/milestones
