package com.commercetools.sync.sdk2.commons.utils;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.graph_ql.GraphQLResponseBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nonnull;

public class TestUtils {
  public static <T> T readObjectFromResource(final String resourcePath, final Class<T> objectType) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    return fromInputStream(resourceAsStream, objectType);
  }

  @Nonnull
  public static ApiHttpResponse<GraphQLResponse> mockGraphQLResponse(
      final String jsonResponseString) throws JsonProcessingException {
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final Map responseMap = objectMapper.readValue(jsonResponseString, Map.class);
    final ApiHttpResponse<GraphQLResponse> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody())
        .thenReturn(GraphQLResponseBuilder.of().data(responseMap).build());
    return apiHttpResponse;
  }

  @Nonnull
  public static ApiHttpResponse<JsonNode> mockJsonNodeResponse(final String jsonResponseString)
      throws JsonProcessingException {
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final JsonNode jsonNode = objectMapper.readValue(jsonResponseString, JsonNode.class);
    final ApiHttpResponse<JsonNode> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(jsonNode);
    return apiHttpResponse;
  }
}
