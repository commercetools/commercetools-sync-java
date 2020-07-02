<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Release workflow](#release-workflow)
  - [Step 1: Release](#step-1-release)
  - [Step 2: Publish](#step-2-publish)
  - [Step 3: Sync](#step-3-sync)
  - [Final Step](#final-step)
    - [Checklist](#checklist)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Release workflow

The main goal of the build process is to publish the artifacts to public repositories [JCenter](https://bintray.com/commercetools/maven/commercetools-sync-java) and [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java).
     
## Step 1: Release

Create a new PR for the new release: 
- Increment the release version to the new library version, please follow the [semantic versioning](https://semver.org/) for finding the new version.
- Make sure all the dependencies are up-to-date everywhere in the documentation files and the project files where needed.
- Make sure to add a section for the release in the [release notes](/docs/RELEASE_NOTES.md). 
- Ask for review for this PR and then "squash and merge" to master.

For example PR, see: https://github.com/commercetools/commercetools-sync-java/pull/412

------
    
To release the library, you need to ["create a new release"](https://github.com/commercetools/commercetools-sync-java/releases/new) with Github, 
describe the new release as below and publish it. 

For example, define the link to the release notes pointing to a released version:
```markdown
#### 1.8.2
- [Release notes](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/RELEASE_NOTES.md#182----april-30-2020)
- [Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.8.2/)
```

> Additionally define important changes, breaking changes or important new features into the description.

After the release build status is **success** follow the next steps below. Check [here](https://travis-ci.org/commercetools/commercetools-sync-java) for Travis build status.

## Step 2: Publish

- Go to [https://bintray.com/commercetools/maven/commercetools-sync-java](https://bintray.com/commercetools/maven/commercetools-sync-java)
- You will see a notice _Notice: You have 24 unpublished item(s) for this package (expiring in 6 days and 22 hours)_
- Click the _Publish_ button.

## Step 3: Sync

- Click to the Maven Central tab.
- Enter the commercetools Sonatype Maven Central API Key and password (into User token key and User token password fields)
- Click the _Sync_ button and check the `Sync Status`. 

<img width="600" alt="Screenshot 2020-07-02 at 09 48 57" src="https://user-images.githubusercontent.com/3469524/86331559-906e3e80-bc49-11ea-9390-e813bc12c163.png">

## Final Step

Ensure the new version is publicly available at [JCenter](https://bintray.com/commercetools/maven/commercetools-sync-java) and [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java). 

### Checklist 

- [Publish](#step-2-publish) and [Sync](#step-3-sync) completed without an issue.
- The new version is available in the bintray: [https://dl.bintray.com/commercetools/maven/com/commercetools/commercetools-sync-java/](https://dl.bintray.com/commercetools/maven/com/commercetools/commercetools-sync-java/)
- The new version is available in the maven: [https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/](https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/)
