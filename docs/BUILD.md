# Build and release workflow

The main goal of the build process is to publish the artifacts to public repositories, 
like [JCenter](https://jcenter.bintray.com/) and [Maven Central](https://search.maven.org/).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Release a new version](#release-a-new-version)
- [Publish workflow](#publish-workflow)
  - [Full build with tests, documentation publishing and Bintray upload](#full-build-with-tests-documentation-publishing-and-bintray-upload)
  - [Publish to local maven repo](#publish-to-local-maven-repo)
  - [Publish snapshots to oss.sonatype.org](#publish-snapshots-to-osssonatypeorg)
  - [Publish to Bintray](#publish-to-bintray)
  - [Publish to Maven](#publish-to-maven)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Release a new version

### Before Release

Before making a new release make sure to:
 - Create a new PR to prepare for the new release. In this PR, you should make sure all
 the dependencies are up-to-date and make sure the new release's version is updated 
 everywhere in the docs and the project files where needed.
 - Make sure the squashed commit message when merging this PR to master contains the words 
 `"Prepare for release "`. For example, before releasing version `1.10.0`, this commit message should be
  `"Prepare for release 1.10.0"`. This is important to trigger and publish new benchmarks
 for the library.
    
### Release
    
To release the library, you need to create a new git commit tag.

This will trigger a new Travis build. The tag can be created via the command line

```bash
git tag -a {version} -m "Minor text adjustments."
git push --tags
```

or github UI "Draft new Release":
https://github.com/commercetools/commercetools-sync-java/releases

Having a tag in the commit will cause Travis to call the `bintrayUpload` task specified in the Gradle build scripts, which uploads
the artifacts to Bintray. The artifacts have to be then promoted/published from Bintray to _JCenter_ and/or 
_Maven Central_. See [Publish workflow](#publish-workflow) below for more details.

Please make sure to add a section for the release in the [release notes](/docs/RELEASE_NOTES.md). 

# Publish workflow

## Full build with tests, documentation publishing and Bintray upload

```
./gradlew clean build gitPublishPush bintrayUpload -Dbuild.version={version}
```

## Publish to local maven repo
 
This step may be used for local test versions:
```
./gradlew clean install -Dbuild.version={version}
```

If you want to review full generated `pom.xml` (with license, scm, developers) like it will be published, then use:
```
./gradlew clean publishToMavenLocal -Dbuild.version={version}
```

where `publishToMavenLocal` is a task from 
[`maven-publish`](https://docs.gradle.org/3.3/userguide/publishing_maven.html#publishing_maven:install)
plugin.

## Publish snapshots to oss.sonatype.org

To publish to [OSS Sonatype snapshots](https://oss.sonatype.org/content/repositories/snapshots/com/commercetools/)
repo the following command is used:

```bash
./gradlew clean build uploadArchives -Dbuild.version={version}-SNAPSHOT
```

The `-SNAPSHOT` suffix is mandatory. 

**Note**: for publishing to OSS Sonatype you need to specify **API** User/Key (not login credentials) for  
`OSS_USER`/`OSS_KEY` environment variables or `ossUser`/`ossKey` Gradle build properties 
(the properties have precedence over environment variables). 

See more configuration details of the oss uploading in [oss-publish.gradle](/gradle-scripts/oss-publish.gradle) file.


## Publish to Bintray

[Bintray documentation about the publishing process](https://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/)

Bintray publish is performed by [`gradle-bintray-plugin`](https://github.com/bintray/gradle-bintray-plugin). 
The artifacts are published to [bintray commercetools maven repo](https://bintray.com/commercetools/maven/commercetools-sync-java).

If you are a new developer in the project - update contributors list in 
[`maven-publish.gradle`](/gradle-scripts/maven-publish.gradle)`-> pomConfig -> developers`.

To initiate publish call:
```
./gradlew clean build bintrayUpload -Dbuild.version={version}
```

**NOTE**: Bintray does not allow to publish snapshots thus `{version}` should not contain _SNAPSHOT_.
If you wish to use snapshots, https://oss.jfrog.com account should be configured.
See https://blog.bintray.com/2014/02/11/bintray-as-pain-free-gateway-to-maven-central/ for more info.

To publish the artifacts to Bintray:
1. Go to https://bintray.com/commercetools/maven/commercetools-sync-java
2. Make sure you are logged in with the commercetools account.
3. You will see a notice _Notice: You have 24 unpublished item(s) for this package (expiring in 6 days and 22 hours)_
4. Click _Publish_

After publishing to Bintray artifacts are available in [Bintray Download](http://dl.bintray.com/commercetools/maven/com/commercetools/commercetools-sync-java/)
but still not available in [JCenter](https://jcenter.bintray.com/com/commercetools/commercetools-sync-java/). 

To publish the artifacts to JCenter do the next:
  1. On the version page go to the Maven Central tab.
  2. Enter the commercetools Sonatype Maven Central API Key and password.
  3. Click _Sync_ and you’re done! 
  
Your package should now be available in [JCenter commercetools-sync-java](https://jcenter.bintray.com/com/commercetools/commercetools-sync-java/) 
and will be synced to Maven Central (and they usually take their time). In case of a sync problem, Bintray will automatically take care of any needed cleanup. 

## Publish to Maven

Publishing to Maven Central requires the following steps:

 1. Build the app and upload to Bintray (see the steps above for integration tests)
 1. [Signing up the app with PGP key](https://blog.bintray.com/2013/08/06/fight-crime-with-gpg/): for now we use Bintray's 
    "a stock built-in key-pair so that it can auto-sign every file you upload"
 1. [Manually release from Bintray web page to Maven Central](https://blog.bintray.com/2015/09/17/publishing-your-maven-project-to-bintray/)
 
**Note**: Maven Central has much stricter requirements to published artifacts, e.g. it should have mandatory POM fields 
(like developers list, SCM references; this is configured in the [build script](/maven-publish.gradle)) 
and mandatory signed by GPG key (could be performed by Bintray settings). For more info about Maven Central 
requirements see [Requirements](http://central.sonatype.org/pages/requirements.html) page.

As soon as artifacts are synced you will be able to find them in the Maven Central repo and mirrors:

https://repo1.maven.org/maven2/com/commercetools/commercetools-sync-java/
http://repo2.maven.org/maven2/com/commercetools/commercetools-sync-java/
