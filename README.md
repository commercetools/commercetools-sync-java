# commercetools sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-sync-java.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sync-java)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)

Java API which exposes utilities for building update actions and automatic syncing of CTP data from external sources 
 such as CSV, XML, JSON, etc.. or an already existing CTP project into a target project.


- [Short-term roadmap](#short-term-roadmap)
- [Usage](#usage)
- [Contributing](#contributing)

## Usage

commercetools sync is a Java library that could be used to synchronise in any of the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data coming from an already-existing commercetools project in the form of 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource objects 
(e.g. [Category](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/Category.java)).


Currently this library supports synchronising the following commercetools resources:-
- [Category](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/categories#commercetools-category-sync)
- [InventoryEntry](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/inventories#commercetools-inventory-sync)


### Prerequisites
 
 install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

<!--- TODO 
### Installation

#### Maven 

#### SBT 

#### Gradle -->

## Short-term roadmap
https://github.com/commercetools/commercetools-sync-java/milestones

## Contributing

### Development
##### Run unit tests
````bash
./gradlew test
````

##### Compile project, run unit tests and assemble main classes into a jar archive
````bash
./gradlew clean build
````

<!--- TODO ### Executing integration tests -->
