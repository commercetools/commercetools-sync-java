package com.commercetools.sync.commons.utils;

import io.vrap.rmf.base.client.SolutionInfo;

public final class SyncSolutionInfo extends SolutionInfo {
  private static final String LIB_NAME = "commercetools-sync-java";
  /** This value is injected by the script at gradle-scripts/set-library-version.gradle. */
  public static final String LIB_VERSION = "#{LIB_VERSION}";

  private static final String LIB_WEBSITE =
      "https://github.com/commercetools/commercetools-sync-java";

  private static final String LIB_HELP =
      "https://commercetools.atlassian.net/servicedesk/customer/portal/22";

  /**
   * Extends {@link SolutionInfo} class of the JVM SDK to append to the User-Agent header with
   * information of the commercetools-sync-java library
   *
   * <p>A User-Agent header with a solution information looks like this: * {@code
   * commercetools-sdk-java-v2/1.4.1 Java/1.8.0_92-b14 (Mac OS X; x86_64)} {@value LIB_NAME}/{@value
   * LIB_VERSION}
   */
  public SyncSolutionInfo() {
    setName(LIB_NAME);
    setVersion(LIB_VERSION);
    setWebsite(LIB_WEBSITE);
    setEmergencyContact(LIB_HELP);
  }
}
