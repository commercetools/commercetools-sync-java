package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BadGatewayException;
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

import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_TIMEOUT;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_TIMEOUT_TIME_UNIT;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.INITIAL_RETRY_DELAY;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.MAX_PARALLEL_REQUESTS;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.MAX_RETRIES;
import static io.sphere.sdk.client.TestDoubleSphereClientFactory.createHttpTestDouble;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RetryableSphereClientWithExponentialBackoffTest {

    @Test
    void of_WithClientConfigAndDefaults_ReturnsSphereClient() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = RetryableSphereClientWithExponentialBackoff.of(clientConfig).build();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void of_WithClientConfigAndOtherConfigValues_ReturnsSphereClient() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = RetryableSphereClientWithExponentialBackoff
                .of(clientConfig)
                .withTimeout(DEFAULT_TIMEOUT)
                .withInitialDelay(INITIAL_RETRY_DELAY)
                .withTimeUnit(DEFAULT_TIMEOUT_TIME_UNIT)
                .withMaxRetries(MAX_RETRIES)
                .withMaxParallelRequests(MAX_PARALLEL_REQUESTS)
                .build();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void of_withRetryDecorator_ShouldRetryWhen502HttpResponse() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502)));

        final long maxRetryAttempt = 1L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
    void of_withDefaultRetryDecorator_ShouldRetryWhen502HttpResponse() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
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
                retryableSphereClientWithExponentialBackoff.calculateDurationWithExponentialRandomBackoff(
                        retryContext.getAttempt(), INITIAL_RETRY_DELAY, DEFAULT_TIMEOUT);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            //fails within : 2100 millisecond -> 1200 millisecond max delay + 900 millisecond (3*300) thread sleep
            .failsWithin(2100, TimeUnit.MILLISECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class)
            .withMessageContaining("502");

        // first request + retries
        verify(mockSphereUnderlyingClient, times(3)).execute(customerUpdateCommand);
    }

    @Test
    void of_withRetryDecorator_ShouldRetryWhen503HttpResponse() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.SERVICE_UNAVAILABLE_503)));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
    void of_withRetryDecorator_ShouldRetryWhen504HttpResponse() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.GATEWAY_TIMEOUT_504)));

        final long maxRetryAttempt = 3L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
            .failsWithin(4, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(GatewayTimeoutException.class)
            .withMessageContaining("504");

        verify(mockSphereUnderlyingClient, times(4)).execute(customerUpdateCommand);
    }

    @Test
    void of_withRetryDecorator_ShouldNotRetryWhen400HttpResponse() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_REQUEST_400,
                "{\"statusCode\":\"400\"}")));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig);
        long maxDelay = 0;
        for (long failedRetryAttempt = 1; failedRetryAttempt <= 10; failedRetryAttempt++) {

            maxDelay += INITIAL_RETRY_DELAY * ((long) Math.pow(2, failedRetryAttempt - 1)) * 2;

            /* One example of wait times of retries:
            Retry 1: 318 millisecond
            Retry 2: 740 millisecond
            Retry 3: 1284 millisecond
            Retry 4: 2002 millisecond
            Retry 5: 6054 millisecond
            Retry 6: 8690 millisecond
            Retry 7: 15567 millisecond
            Retry 8: 30000 millisecond
            Retry 9: 30000 millisecond
            Retry 10: 30000 millisecond
            */
            final Duration duration =
                    retryableSphereClientWithExponentialBackoff.calculateDurationWithExponentialRandomBackoff(
                            failedRetryAttempt, INITIAL_RETRY_DELAY, DEFAULT_TIMEOUT);

            assertThat(duration.toMillis())
                .isLessThanOrEqualTo(maxDelay)
                .isLessThanOrEqualTo(DEFAULT_TIMEOUT);
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