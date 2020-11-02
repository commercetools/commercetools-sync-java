package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.GatewayTimeoutException;
import io.sphere.sdk.client.ServiceUnavailableException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.retry.RetryContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.DEFAULT_WAIT_BASE_MILLISECONDS;
import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.MAX_RETRIES;
import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.calculateDurationWithExponentialRandomBackoff;
import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.decorateSphereClient;
import static io.sphere.sdk.client.TestDoubleSphereClientFactory.createHttpTestDouble;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ClientConfigurationUtilsTest {

    @Test
    void createClient_WithConfig_ReturnsBlockingSphereClient() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);

        assertThat(sphereClient instanceof BlockingSphereClient).isTrue();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void createClient_withRetryDecorator_ShouldRetryWhen502HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502)));

        final long maxRetryAttempt = 1L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            .failsWithin(2, TimeUnit.SECONDS) // first retry will be in 1 second, see: durationFunction.
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class)
            .withMessageContaining("502");

        // first request + retry.
        verify(mockSphereUnderlyingClient, times(2)).execute(customerUpdateCommand);
    }

    @Test
    void createClient_withDefaultRetryDecorator_ShouldRetryWhen502HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException expected) {
                }

                return HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502);
            }));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext ->
            calculateDurationWithExponentialRandomBackoff(retryContext.getAttempt());

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            //fails within : 1500 millisecond -> 600 millisecond max sleep + 900 millisecond (3*300) thread sleep
            .failsWithin(1500, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class)
            .withMessageContaining("502");

        // first request + retries
        verify(mockSphereUnderlyingClient, times(3)).execute(customerUpdateCommand);
    }

    @Test
    void createClient_withRetryDecorator_ShouldRetryWhen503HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.SERVICE_UNAVAILABLE_503)));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            .failsWithin(5, TimeUnit.SECONDS) // first retry will be in 2 second, see: durationFunction.
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(ServiceUnavailableException.class)
            .withMessageContaining("503");

        // first request + retries
        verify(mockSphereUnderlyingClient, times(3)).execute(customerUpdateCommand);
    }

    @Test
    void createClient_withRetryDecorator_ShouldRetryWhen504HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.GATEWAY_TIMEOUT_504)));

        final long maxRetryAttempt = 3L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            .failsWithin(4, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(GatewayTimeoutException.class)
            .withMessageContaining("504");

        verify(mockSphereUnderlyingClient, times(4)).execute(customerUpdateCommand);
    }

    @Test
    void createClient_withRetryDecorator_ShouldNotRetryWhen400HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_REQUEST_400,
                "{\"statusCode\":\"400\"}")));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            .failsWithin(1, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(ErrorResponseException.class)
            .withMessageContaining("400");

        // No retry, only first request.
        verify(mockSphereUnderlyingClient, times(1)).execute(customerUpdateCommand);
    }

    @Test
    void calculateExponentialRandomBackoff_withRetries_ShouldReturnRandomisedDurations() {
        assertThat(MAX_RETRIES).isGreaterThan(1);

        long maxSleep = 0;
        for (long failedRetryAttempt = 1; failedRetryAttempt <= MAX_RETRIES; failedRetryAttempt++) {
            maxSleep += DEFAULT_WAIT_BASE_MILLISECONDS * ((long) Math.pow(2, failedRetryAttempt - 1));

            /* One example of wait times of retries:
            Retry 1: 61 millisecond
            Retry 2: 274 millisecond
            Retry 3: 552 millisecond
            Retry 4: 472 millisecond
            Retry 5: 1533 millisecond */
            final Duration duration = calculateDurationWithExponentialRandomBackoff(failedRetryAttempt);
            assertThat(duration.toMillis()).isLessThanOrEqualTo(maxSleep);
        }
    }

    private CustomerUpdateCommand getCustomerUpdateCommand() {
        final List<UpdateAction<Customer>> updateActions = singletonList(ChangeEmail.of(""));
        return CustomerUpdateCommand.of(
            Versioned.of(UUID.randomUUID().toString(), 1L),
            updateActions
        );
    }
}
