package com.commercetools.sync.sdk2.commons.utils;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
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

  public static <T> T readObjectFromResource(
      final String jsonResourcePath, TypeReference<T> resourceClass) {
    final String jsonString = stringFromResource(jsonResourcePath);
    final T resource = fromJsonString(jsonString, resourceClass);

    return resource;
  }
}
