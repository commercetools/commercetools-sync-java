<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Release workflow](#release-workflow)
  - [Step 1: Pull request](#step-1-pull-request)
  - [Step 2: Release library in Github](#step-2-release-library-in-github)
  - [Final Step](#final-step)
    - [Checklist](#checklist)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Release workflow

The main goal of the build process is to publish the artifacts to public repository [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java).
     
## Step 1: Pull request

Create a new PR for the new release: 
- Increment the release version to the new library version, please follow the [semantic versioning](https://semver.org/) for finding the new version.
- Make sure all the dependencies are up-to-date everywhere in the documentation files and the project files where needed.
- Make sure to add a section for the release in the [release notes](/docs/RELEASE_NOTES.md). 
- Ask for review for this PR and then "squash and merge" to master.

For example PR, see: https://github.com/commercetools/commercetools-sync-java/pull/412

------
## Step 2: Release library in Github   
To release the library, you need to ["create a new release"](https://github.com/commercetools/commercetools-sync-java/releases/new) with Github, 
describe the new release as below and publish it. 

For example, define the link to the release notes pointing to a released version:
```markdown
#### 1.8.2
- [Release notes](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/RELEASE_NOTES.md#182----april-30-2020)
- [Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.8.2/)
```

> Additionally define important changes, breaking changes or important new features into the description.

The creation of a github release triggers a [github action](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACD), which will deploy the library to [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java).

## Final Step
After the release build status is **success** ensure that the new version is publicly available at [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java). 

### Checklist 

- [Publish] completed without an issue.
- The new version is available in the maven: [https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/](https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/)
- The new version is available in the maven: [https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/](https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/)
