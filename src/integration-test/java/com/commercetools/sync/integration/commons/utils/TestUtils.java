package com.commercetools.sync.integration.commons.utils;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;

import java.io.InputStream;

public class TestUtils {
  public static <T> T readObjectFromResource(final String resourcePath, final Class<T> objectType) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    return fromInputStream(resourceAsStream, objectType);
  }
}
