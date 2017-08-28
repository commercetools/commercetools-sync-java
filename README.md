# commercetools sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-sync-java.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sync-java)
[![codecov](https://codecov.io/gh/commercetools/commercetools-sync-java/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-sync-java)

Java API which exposes utilities for building update actions and automatic syncing of CTP data from external sources 
 such as CSV, XML, JSON, etc.. or an already existing CTP project into a target project.


- [Usage](#usage)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

## Usage

commercetools sync is a Java library that could be used to synchronise CTP data in any of the following ways:

1. Synchronise data coming from an external system in any form (CSV, XML, etc..) that has been already mapped to 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).

2. Synchronise data coming from an already-existing commercetools project in the form of 
[JVM-SDK](https://github.com/commercetools/commercetools-jvm-sdk) resource draft objects 
(e.g. [CategoryDraft](https://github.com/commercetools/commercetools-jvm-sdk/blob/master/commercetools-models/src/main/java/io/sphere/sdk/categories/CategoryDraft.java)).


Currently this library supports synchronising [Categories](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/categories#commercetools-category-sync)
and [InventoryEntries](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/inventories#commercetools-inventory-sync).



### Prerequisites
 
 - install Java 8
 - a target CTP project to which your source of data would be synced to.

<!--- TODO 
### Installation

#### Maven 

#### SBT 

#### Gradle -->

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
./gradlew clean jar`
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

##### Publish to Bintray
````bash
./gradlew clean -Dbuild.version={version} bintrayUpload
````

For more detailed information on build and release process, see [Build and Release](BUILD.md) documentation.

<!--- TODO ### Executing integration tests only-->
