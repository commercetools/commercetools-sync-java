package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SolutionInfo;

import javax.annotation.Nullable;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SyncSolutionInfo extends SolutionInfo {
    static final String UNSPECIFIED = "unspecified";

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
        final String implementationTitle = getClass().getPackage().getImplementationTitle();
        final String implementationVersion = getClass().getPackage().getImplementationVersion();
        final String solutionName = isAttributeUnspecified(implementationTitle)
            ? "commercetools-sync-java" : implementationTitle;
        final String solutionVersion = isAttributeUnspecified(implementationVersion)
            ? "DEBUG-VERSION" : implementationVersion;
        setName(solutionName);
        setVersion(solutionVersion);
    }

    static boolean isAttributeUnspecified(@Nullable final String attributeValue) {
        return isBlank(attributeValue) || UNSPECIFIED.equals(attributeValue);
    }
}