#!/bin/bash
set -e

export SOLUTION_INFO_PATH="src/main/java/com/commercetools/sync/commons/utils/SyncSolutionInfo.java"
export VERSION_PLACEHOLDER="#{LIB_VERSION}"
export LIB_VERSION="dev-version"

export TAG=`if [ "$TRAVIS_PULL_REQUEST" = "false" -a -n "$TRAVIS_TAG" ] ; then echo "$TRAVIS_TAG" ; fi`

if [ "$TAG" ]; then
  set LIB_VERSION=${TAG}
  fi

sed -i "" "s/$VERSION_PLACEHOLDER/$LIB_VERSION/g" "$SOLUTION_INFO_PATH"
printf "\n\nInjecting library version '$LIB_VERSION' into $SOLUTION_INFO_PATH\n\n"



