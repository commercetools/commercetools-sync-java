<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Release workflow](#release-workflow)
  - [Step 1: Pull request](#step-1-pull-request)
  - [Step 2: Release library in Github](#step-2-release-library-in-github)
  - [Step 3: Publish to Maven central](#step-3-publish-to-maven-central)
    - [Login into nexus repository manager](#login-into-nexus-repository-manager)
    - [Locate and examine your staging repository](#locate-and-examine-your-staging-repository)
    - [Close and drop or release your staging repository](#close-and-drop-or-release-your-staging-repository)
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

The creation of a github release triggers a [github action](https://github.com/commercetools/commercetools-sync-java/actions?query=workflow%3ACD),
which will deploy the library to the the staging repository of the[nexus repository manager](https://oss.sonatype.org/).

## Step 3: Publish to Maven central

### Login into nexus repository manager
You need to login to [nexus repository manager](https://oss.sonatype.org/) in order to access and work with your staging repositories. 

### Locate and examine your staging repository
Once you are logged in you will be able to access the build promotion menu in the left hand navigation and select the
staging repository. The staging repository you created during the deployment will have a name starting with the
groupId for your projects with the dots removed appended with a dash and a 4 digit number. E.g. if your project groupId
is com.example.applications, your staging profile name would start with comexampleapplications. The sequential numbers 
start at 1000 and are incremented per deployment so you could e.g. have a staging repository name of comexampleapplication-1010.

Select the staging repository and the panel below the list will display further details about the repository.

### Close and drop or release your staging repository
After your deployment the repository will be in an open status. You can evaluate the deployed components in the
repository using the Contents tab. If you believe everything is correct, you can press the close button above the list.
This will trigger the evaluations of the components against the requirements.Once you have successfully closed the staging repository, you can release it by pressing the Release button. 
This will move the components into the release repository of [nexus repository manager](https://oss.sonatype.org/)  where it will be synced to the Central Repository.

## Final Step
After the component is moved to the release repository ensure that the new version is synced to [Maven Central](https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java). 

### Checklist 

- Publish completed without an issue.
- The new version is available in the (https://mvnrepository.com/artifact/com.commercetools/commercetools-sync-java)