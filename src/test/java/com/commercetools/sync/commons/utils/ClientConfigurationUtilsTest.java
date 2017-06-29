package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.retry.RetryContext;
import io.sphere.sdk.zones.Zone;
import io.sphere.sdk.zones.queries.ZoneByIdGet;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.getRetryOn5xxClient;
import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.retryOn5xxDurationCalculator;
import static io.sphere.sdk.client.TestDoubleSphereClientFactory.createHttpTestDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientConfigurationUtilsTest {
    private static final SphereRequest<Zone> MOCK_SPHERE_REQUEST = ZoneByIdGet.of("123");
    private static final String MOCK_RESPONSE_BODY = "{\"id\":\"123\","
        + "\"version\":1,"
        + "\"createdAt\":\"2001-09-11T14:00:00.000Z\","
        + "\"lastModifiedAt\":\"2001-09-11T14:00:00.000Z\","
        + "\"name\":\"test\","
        + "\"locations\":[{\"country\":\"DE\"}]}";

    @Test
    public void getRetryOn5xxClient_ShouldReturnNewSphereClientInstance() {
        final SphereClient delegate = mock(SphereClient.class);
        final SphereClient retryOn5xxClient = getRetryOn5xxClient(delegate);

        assertThat(retryOn5xxClient).isNotNull();
        assertThat(retryOn5xxClient).isInstanceOf(SphereClient.class);
        assertThat(retryOn5xxClient).isNotEqualTo(delegate);
    }

    @Test
    public void getRetryOn5xxClient_ShouldReturnClientWhichRetryOn500() {
        getRetryOn5xxClient_ShouldReturnClientWhichRetryOnStatus(500);
    }

    @Test
    public void getRetryOn5xxClient_ShouldReturnClientWhichRetryOn502() {
        getRetryOn5xxClient_ShouldReturnClientWhichRetryOnStatus(502);
    }

    @Test
    public void getRetryOn5xxClient_ShouldReturnClientWhichRetryOn503() {
        getRetryOn5xxClient_ShouldReturnClientWhichRetryOnStatus(503);
    }

    @Test
    public void getRetryOn5xxClient_ShouldReturnClientWhichRetryOn504() {
        getRetryOn5xxClient_ShouldReturnClientWhichRetryOnStatus(504);
    }

    @Test
    public void retryOn5xxDurationCalculator_ShouldReturnProperValues() {
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(1L))).isEqualTo(Duration.ofSeconds(2));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(2L))).isEqualTo(Duration.ofSeconds(2));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(3L))).isEqualTo(Duration.ofSeconds(2));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(4L))).isEqualTo(Duration.ofSeconds(4));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(5L))).isEqualTo(Duration.ofSeconds(4));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(6L))).isEqualTo(Duration.ofSeconds(4));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(7L))).isEqualTo(Duration.ofSeconds(8));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(8L))).isEqualTo(Duration.ofSeconds(8));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(9L))).isEqualTo(Duration.ofSeconds(8));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(25L))).isEqualTo(Duration.ofSeconds(512));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(26L))).isEqualTo(Duration.ofSeconds(512));
        assertThat(retryOn5xxDurationCalculator(getMockRetryContext(27L))).isEqualTo(Duration.ofSeconds(512));
    }

    private void getRetryOn5xxClient_ShouldReturnClientWhichRetryOnStatus(final int status) {
        final AtomicInteger callsPerformed = new AtomicInteger(0);
        final SphereClient sphereClient = createHttpTestDouble(
            getFunctionFailingOnFirstRequest(status, callsPerformed));
        SphereClient retrySphereClient = getRetryOn5xxClient(sphereClient);

        final Zone zone = retrySphereClient.execute(MOCK_SPHERE_REQUEST).toCompletableFuture().join();
        assertThat(zone.getId()).isEqualTo("123");
        assertThat(callsPerformed.get()).isEqualTo(2);
    }

    private RetryContext getMockRetryContext(final Long attempt) {
        final RetryContext retryContext = mock(RetryContext.class);
        when(retryContext.getAttempt()).thenReturn(attempt);
        return retryContext;
    }

    /**
     * Return function that:
     * <ul>
     *     <li>increments {@code callsPerformed} counter</li>
     *     <li>returns response of a status {@code failStatus} if the {@code callsPerformed} counter equals 1</li>
     *     <li>returns response of a status 200 and a response body
     *     {@link ClientConfigurationUtilsTest#MOCK_RESPONSE_BODY} otherwise</li>
     * </ul>
     *
     * @param failStatus a status code that would be returned inside {@link HttpResponse} after the first invocation
     * @param callsPerformed a counter of a method's invocations. Should be initialized with 0
     * @return function that simulates {@link HttpResponse} for any {@link HttpRequestIntent}
     */
    private Function<HttpRequestIntent, HttpResponse> getFunctionFailingOnFirstRequest(final Integer failStatus,
                                                                                       final AtomicInteger
                                                                                           callsPerformed) {
        return (intent) -> {
            if (callsPerformed.incrementAndGet() == 1) {
                return HttpResponse.of(failStatus);
            } else {
                return HttpResponse.of(200, MOCK_RESPONSE_BODY);
            }
        };
    }
}
