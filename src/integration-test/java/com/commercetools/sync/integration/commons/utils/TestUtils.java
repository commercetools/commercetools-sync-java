package com.commercetools.sync.integration.commons.utils;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

public class TestUtils {
  public static String stringFromResource(final String resourcePath) {
    try {
      return IOUtils.toString(
          Objects.requireNonNull(
              Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)),
          StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T readObjectFromResource(final String resourcePath, final Class<T> objectType) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    return fromInputStream(resourceAsStream, objectType);
  }
}
