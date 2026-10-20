package com.commercetools.sync.commons;

import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

public class ExceptionUtils {

  public static ConcurrentModificationException createConcurrentModificationException(
      final String message) {
    final String json = getErrorResponseJsonString(409);

    return new ConcurrentModificationException(
        409,
        message,
        null,
        message,
        new ApiHttpResponse<>(409, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  public static BadGatewayException createBadGatewayException() {
    final String json = getErrorResponseJsonString(500);
    return new BadGatewayException(
        500, "", null, "", new ApiHttpResponse<>(500, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  public static BadRequestException createBadRequestException() {
    final String json = getErrorResponseJsonString(400);
    return new BadRequestException(
        400, "", null, "", new ApiHttpResponse<>(400, null, json.getBytes(StandardCharsets.UTF_8)));
  }

  public static String getErrorResponseJsonString(Integer errorCode) {
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(errorCode)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json;
    try {
      json = ow.writeValueAsString(errorResponse);
    } catch (JacksonException e) {
      // ignore the error
      json = null;
    }
    return json;
  }
}
