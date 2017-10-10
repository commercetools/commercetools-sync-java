# commercetools sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-sync-java.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sync-java)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)

Java API which exposes utilities for building update actions and automatic syncing of CTP data from external sources 
 such as CSV, XML, JSON, etc.. or an already existing CTP project into a target project.


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
- [Usage](#usage)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
    - [Maven](#maven)
    - [Gradle](#gradle)
- [Roadmap](#roadmap)
- [Javadocs](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M1/)
<!-- END doctoc generated TOC please keep comment here to allow auto update -->
## Usage

commercetools sync is a Java library that could be used to synchronise CTP data in any of the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data coming from an already-existing commercetools project in the form of 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).


Currently this library supports synchronising
 - [Categories](docs/usage/CATEGORY_SYNC.md)
 - [Products](docs/usage/PRODUCT_SYNC.md) (_Beta_: Not recommended for production use yet.)
 - [InventoryEntries](docs/usage/INVENTORY_SYNC.md) (_Beta_: Not recommended for production use yet.)

### Prerequisites
 
 - install Java 8
 - a target CTP project to which your source of data would be synced to.


### Installation
There are multiple ways to download the commercetools sync dependency, based on your dependency manager. Here are the 
most popular ones:
#### Maven 
````xml
<dependency>
  <groupId>com.commercetools</groupId>
  <artifactId>commercetools-sync-java</artifactId>
  <version>v1.0.0-M2-beta</version>
</dependency>
````
#### Gradle
````groovy
compile 'com.commercetools:commercetools-sync-java:v1.0.0-M2-beta'
````
<!-- TODO #### SBT 
````java
libraryDependencies ++= Seq(
    "com.commercetools" % "commercetools-sync-java" % "v1.0.0-M2-beta",
 )
````-->

## Roadmap
https://github.com/commercetools/commercetools-sync-java/milestones