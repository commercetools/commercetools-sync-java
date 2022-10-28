package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.createClient;
import static java.lang.String.format;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;

public class SphereClientUtils {

  public static final String IT_PROPERTIES = "it.properties";

  public static final SphereClientConfig CTP_SOURCE_CLIENT_CONFIG = getCtpSourceClientConfig();
  public static final SphereClientConfig CTP_TARGET_CLIENT_CONFIG = getCtpTargetClientConfig();

  public static final SphereClient CTP_SOURCE_CLIENT = createClient(CTP_SOURCE_CLIENT_CONFIG);
  public static final SphereClient CTP_TARGET_CLIENT = createClient(CTP_TARGET_CLIENT_CONFIG);

  private static SphereClientConfig getCtpSourceClientConfig() {
    return getCtpClientConfig("source.", "SOURCE");
  }

  private static SphereClientConfig getCtpTargetClientConfig() {
    return getCtpClientConfig("target.", "TARGET");
  }

  private static SphereClientConfig getCtpClientConfig(
      @Nonnull final String propertiesPrefix, @Nonnull final String envVarPrefix) {
    try {
      InputStream propStream =
          SphereClientUtils.class.getClassLoader().getResourceAsStream(IT_PROPERTIES);
      if (propStream != null) {
        Properties itProperties = new Properties();
        itProperties.load(propStream);
        return SphereClientConfig.ofProperties(itProperties, propertiesPrefix);
      }
    } catch (Exception exception) {
      throw new IllegalStateException(
          format(
              "IT properties file \"%s\" found, but CTP properties"
                  + " for prefix \"%s\" can't be read",
              IT_PROPERTIES, propertiesPrefix),
          exception);
    }

    return SphereClientConfig.ofEnvironmentVariables(envVarPrefix);
  }
}
