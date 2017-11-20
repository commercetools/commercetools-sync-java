package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SolutionInfo;

import static java.util.Optional.ofNullable;

public class SyncSolutionInfo extends SolutionInfo {
    /**
     * Extends {@link SolutionInfo} class of the JVM SDK to append to the User-Agent header with information of the
     * commercetools-sync-java library
     *
     * <p>A User-Agent header with a solution information looks like this:
     * {@code commercetools-jvm-sdk/1.4.1 (AHC/2.0) Java/1.8.0_92-b14 (Mac OS X; x86_64)
     * {implementationTitle}/{implementationVersion}}</p>
     *
     */
    public SyncSolutionInfo() {
        final String implementationTitle = ofNullable(getClass().getPackage().getImplementationTitle())
            .orElse("commercetools-sync-java");
        final String implementationVersion = ofNullable(getClass().getPackage().getImplementationVersion())
            .orElse("DEBUG-VERSION");
        setName(implementationTitle);
        setVersion(implementationVersion);
    }
}