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
- [Contributing](#contributing)
  - [Build](#build)
      - [Run unit tests](#run-unit-tests)
      - [Package JARs](#package-jars)
      - [Package JARs and run tests](#package-jars-and-run-tests)
      - [Full build with tests, but without install to maven local repo (Recommended)](#full-build-with-tests-but-without-install-to-maven-local-repo-recommended)
      - [Install to local maven repo](#install-to-local-maven-repo)
      - [Build and publish JavaDoc](#build-and-publish-javadoc)
      - [Publish to Bintray](#publish-to-bintray)
  - [Integration Tests](#integration-tests)
    - [Running](#running)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

- [Javadocs](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M1/)
## Usage

commercetools sync is a Java library that could be used to synchronise CTP data in any of the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data coming from an already-existing commercetools project in the form of 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).


Currently this library supports synchronising
 - [Categories](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/categories#commercetools-category-sync)
 - [InventoryEntries](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/inventories#commercetools-inventory-sync). (_Beta_: Not recommended for production use yet.)


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
  <version>v1.0.0-M1</version>
  <type>pom</type>
</dependency>
````
#### Gradle
````groovy
compile 'com.commercetools:commercetools-sync-java:v1.0.0-M1'
````
<!-- TODO #### SBT 
````java
libraryDependencies ++= Seq(
    "com.commercetools" % "commercetools-sync-java" % "v1.0.0-M1",
 )
````-->

## Roadmap
https://github.com/commercetools/commercetools-sync-java/milestones

## Contributing

- Every PR should address an issue on the repository. If the issue doesn't exist, please create it first.
- Pull requests should always follow the following naming convention: 
`[issue-number]-[pr-name]`. For example,
to address issue #65 which refers to a style bug, the PR addressing it should have a name that looks something like
 `65-fix-style-bug`.
- Commit messages should always be prefixed with the number of the issue that they address. 
For example, `#65: Remove redundant space.`
- After your PR is merged to master:
    - Delete the branch.
    - Mark the issue it addresses with the `merged-to-master` label.
    - Close the issue **only** if the change was released.

### Build
##### Run unit tests
````bash
./gradlew test
````

##### Package JARs
````bash
./gradlew clean jar
````

##### Package JARs and run tests
````bash
./gradlew clean check
````

##### Full build with tests, but without install to maven local repo (Recommended)
````bash
./gradlew clean build
````

##### Install to local maven repo
````bash
./gradlew clean install
````

##### Build and publish JavaDoc
````bash
./gradlew clean -Dbuild.version={version} gitPublishPush
````

##### Publish to Bintray
````bash
./gradlew clean -Dbuild.version={version} bintrayUpload
````

For more detailed information on build and release process, see [Build and Release](BUILD.md) documentation.

### Integration Tests

1. The integration tests of the library require to have two CTP projects (a source project and a target project) were the 
data will be tested to be synced on from the source to the target project. 
2. Running the tests does the following:
    - Clean all the data in both projects.
    - Create test data in either/both projects depending on the test.
    - Execute the tests.
    - Clean all the data in both projects, leaving them empty.

#### Running 
Before running the integration tests make sure the following environment variables are set:
````bash
export SOURCE_PROJECT_KEY = xxxxxxxxxxxxx
export SOURCE_CLIENT_ID = xxxxxxxxxxxxxxx
export SOURCE_CLIENT_SECRET = xxxxxxxxxxx
export TARGET_PROJECT_KEY = xxxxxxxxxxxxx
export TARGET_CLIENT_ID = xxxxxxxxxxxxxxx
export TARGET_CLIENT_SECRET = xxxxxxxxxxx
````

then run the integration tests:
````bash
./gradlew integrationTest
````