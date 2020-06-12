#!/bin/bash

echo "TRAVIS_PULL_REQUEST $TRAVIS_PULL_REQUEST"
echo "TRAVIS_TAG $TRAVIS_TAG"

export TAG=`if [ "$TRAVIS_PULL_REQUEST" = "false" -a -n "$TRAVIS_TAG" ] ; then echo "$TRAVIS_TAG" ; fi`

if [ "$TAG" ]; then
  echo "Build is tagged. Uploading artifact $TAG to Bintray."
  ./gradlew --info -Dbuild.version="$TAG" benchmarkCommit || exit 1
  ./gradlew --info -Dbuild.version="$TAG" mkdocsPublish || exit 1
  ./gradlew --info -Dbuild.version="$TAG" bintrayUpload
  if [[ $? != 0 ]]; then
    printf "\nWARNING: Github pages $TAG is published to github, but bintray upload failed.\n\n"
    exit 1
  fi
else
  echo "This build doesn't publish the library since it is not tagged."
fi
