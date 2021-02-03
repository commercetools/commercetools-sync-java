# Contributing to commercetools-sync-java

These are the contribution guidelines for the commercetools-sync-java.

Thanks for taking the time to contribute :+1::tada: All contributions are welcome! 
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Contribution process for all committers](#contribution-process-for-all-committers)
  - [Typos](#typos)
  - [Features or Bug Fixes](#features-or-bug-fixes)
- [Development](#development)
  - [Build](#build)
      - [Run unit tests](#run-unit-tests)
      - [Package JARs](#package-jars)
      - [Package JARs and run tests](#package-jars-and-run-tests)
      - [Full build with tests, but without install to maven local repo (Recommended)](#full-build-with-tests-but-without-install-to-maven-local-repo-recommended)
      - [Install to local maven repo](#install-to-local-maven-repo)
      - [Publish JavaDoc](#publish-javadoc)
      - [Build and publish to Bintray](#build-and-publish-to-bintray)
  - [Integration Tests](#integration-tests)
    - [Running](#running)
- [Using the google java style and code formatter](#using-the-google-java-style-and-code-formatter)
  - [IntelliJ, Android Studio, and other JetBrains IDEs](#intellij-android-studio-and-other-jetbrains-ides)
  - [Eclipse](#eclipse)
  - [Spotless commands](#spotless-commands)
      - [Run Spotless Style Check](#run-spotless-style-check)
      - [Fix Spotless style violations](#fix-spotless-style-violations)
  - [Ignoring mass reformatting commits with git blame](#ignoring-mass-reformatting-commits-with-git-blame)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Contribution process for all committers

### Typos

If you have push access to the repository you can fix them directly otherwise just make a pull request.

### Features or Bug Fixes

- Every PR should address an issue on the repository. If the issue doesn't exist, please create it first and link PR with the issue. 
- After your PR is approved by all reviewers and the build is green:
    - Use `Squash and merge` option on a pull request on GitHub, with that the pull request's commits should be squashed into a single commit. 
        > Instead of seeing all of a contributor's individual commit messages, the commits should be combined into one commit message with a clear commit description. 
    - Delete the branch when the PR is closed.
    - Close the issue **only** if the change was released.
    
## Development
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

##### Publish JavaDoc
````bash
./gradlew clean javadoc gitPublishPush -Dbuild.version={version}
````

##### Build and publish to Bintray
````bash
./gradlew clean build bintrayUpload -Dbuild.version={version} 
````

For more detailed information on the build and the release process, see [Build and Release](BUILD.md) documentation.

### Integration Tests

1. The integration tests of the library require to have two CTP projects (a source project and a target project) where the 
data will be tested to be synced from the source to the target project. 

2. Running the tests does the following:
    - Clean all the data on both projects.
    - Create test data in either/both projects depending on the test.
    - Execute the tests.
    - Clean all the data in both projects, leaving them empty.

#### Running

To run the integration tests, CTP credentials are required. The credential can be obtained once you create a CTP project.
For details, please refer to the following link:
https://docs.commercetools.com/merchant-center/projects.html#creating-a-project 

  1. Use credentials Java properties file `/src/integration-test/resources/it.properties`:
    
  ```properties
  source.projectKey=aaaaa
  source.clientId=bbbbbbb
  source.clientSecret=ccc
    
  target.projectKey=ddddd
  target.clientId=eeeeeee
  target.clientSecret=fff
  ```
    
  Use [`it.properties.skeleton`](/src/integration-test/resources/it.properties.skeleton) 
  as a template to setup the credentials.
  
  **Note**: the `it.properties` file must be ignored by VCS. 
   
   2. Set the following environment variables:
  ```bash
  export SOURCE_PROJECT_KEY = xxxxxxxxxxxxx
  export SOURCE_CLIENT_ID = xxxxxxxxxxxxxxx
  export SOURCE_CLIENT_SECRET = xxxxxxxxxxx
  export TARGET_PROJECT_KEY = xxxxxxxxxxxxx
  export TARGET_CLIENT_ID = xxxxxxxxxxxxxxx
  export TARGET_CLIENT_SECRET = xxxxxxxxxxx
  ```

  **Note**: `it.properties` file has precedence over environment variables. If the file exists - 
  the environment variables are ignored. If the existing `it.properties` file is empty or one of the properties 
  is missing - exception will be thrown on the execution of the tests

If one of the two options above is set - run the integration tests:
```bash
./gradlew integrationTest
```

## Using the google java style and code formatter
 
We are using `google-java-format` to format Java source code to comply with [Google Java Style](https://google.github.io/styleguide/javaguide.html).

### IntelliJ, Android Studio, and other JetBrains IDEs

A [google-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/8527) is available from the plugin repository. To install it, go to your IDE's settings and select the `Plugins` category. Click the `Marketplace` tab, search for the `google-java-format` plugin, and click the `Install` button.

The plugin will be disabled by default. To enable it in the current project, go to `File→Settings...→google-java-format Settings` (or `IntelliJIDEA→Preferences...→Other Settings→google-java-format Settings` on macOS) and check the `Enable google-java-format` checkbox. (A notification will be presented when you first open a project offering to do this for you.)

To enable it by default in new projects, use `File→Other Settings→Default Settings...`.
When enabled, it will replace the normal `Reformat Code` action, which can be triggered by the `Code` menu or with the Ctrl-Alt (by default) keyboard shortcut.

### Eclipse

[google-java-format Eclipse plugin](https://github.com/google/google-java-format/releases/download/google-java-format-1.6/google-java-format-eclipse-plugin_1.6.0.jar) can be downloaded from the releases page. Drop it into the Eclipse[drop-ins folder](http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fp2_dropins_format.html)to activate the plugin.

The plugin adds a `google-java-format` formatter implementation that can be configured in `Window > Preferences > Java > Code Style > Formatter > Formatter Implementation`.

### Spotless commands
##### Run Spotless Style Check
````bash
./gradlew spotlessCheck
````

##### Fix Spotless style violations
````bash
./gradlew spotlessApply
````

### Ignoring mass reformatting commits with git blame

To exclude the formatting commits git blame supports writing the commit hashes into a file and then referencing the file with `--ignore-revs-file`.                     
To be able to archive that `git blame ./file.java --ignore-revs-file .git-blame-ignore-revs` command to ignore this revision to find a better git history.      
                         
Also `git config blame.ignoreRevsFile .git-blame-ignore-revs` could be configured to ignore this revision always.
                                                          
> We create `.git-blame-ignore-revs` that could be found in the repository.   
