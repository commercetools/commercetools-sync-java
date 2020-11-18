package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.GatewayTimeoutException;
import io.sphere.sdk.client.InternalServerErrorException;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_INITIAL_RETRY_DELAY;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_MAX_DELAY;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_MAX_PARALLEL_REQUESTS;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.DEFAULT_MAX_RETRY_ATTEMPT;
import static io.sphere.sdk.client.TestDoubleSphereClientFactory.createHttpTestDouble;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .withMaxDelay(DEFAULT_MAX_DELAY)
                .withInitialDelay(DEFAULT_INITIAL_RETRY_DELAY)
                .withMaxRetryAttempt(DEFAULT_MAX_RETRY_ATTEMPT)
                .withMaxParallelRequests(DEFAULT_MAX_PARALLEL_REQUESTS)
                .withStatusCodesToRetry(Arrays.asList(500, 502, 503, 504))
                .build();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void of_WithInitialDelayGreaterThanMaxDelay_ThrowsIllegalArgumentException() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");

        assertThatThrownBy(() -> RetryableSphereClientWithExponentialBackoff
            .of(clientConfig)
            .withMaxDelay(1)
            .withInitialDelay(2).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_WithMaxRetryAttemptLessThanZero_ThrowsIllegalArgumentException() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");

        assertThatThrownBy(() -> RetryableSphereClientWithExponentialBackoff
            .of(clientConfig)
            .withMaxRetryAttempt(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_WithMaxParallelRequestsLessThanOne_ThrowsIllegalArgumentException() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");

        assertThatThrownBy(() -> RetryableSphereClientWithExponentialBackoff
            .of(clientConfig)
            .withMaxParallelRequests(0).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withRetryDecorator_ShouldRetryWhen500HttpResponse() {
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        final SphereClient mockSphereUnderlyingClient =
                spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.INTERNAL_SERVER_ERROR_500)));

        final long maxRetryAttempt = 1L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

        final CustomerUpdateCommand customerUpdateCommand = getCustomerUpdateCommand();

        assertThat(decoratedSphereClient.execute(customerUpdateCommand))
                .failsWithin(2, TimeUnit.SECONDS) // first retry will be in 1 second, see: durationFunction.
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(InternalServerErrorException.class)
                .withMessageContaining("500");

        // first request + retry.
        verify(mockSphereUnderlyingClient, times(2)).execute(customerUpdateCommand);
    }

    @Test
    void of_withRetryDecorator_ShouldRetryWhen502HttpResponse() {
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_GATEWAY_502)));

        final long maxRetryAttempt = 1L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

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
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
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
                        retryContext.getAttempt(), DEFAULT_INITIAL_RETRY_DELAY, DEFAULT_MAX_DELAY);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

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
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.SERVICE_UNAVAILABLE_503)));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

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
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.GATEWAY_TIMEOUT_504)));

        final long maxRetryAttempt = 3L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(1);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

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
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.BAD_REQUEST_400,
                "{\"statusCode\":\"400\"}")));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient = retryableSphereClientWithExponentialBackoff.decorateSphereClient(
                mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, DEFAULT_MAX_PARALLEL_REQUESTS);

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
        final RetryableSphereClientWithExponentialBackoff retryableSphereClientWithExponentialBackoff =
            of_RetryableSphereClientWithExponentialBackoff();
        long maxDelay = 0;
        for (long failedRetryAttempt = 1; failedRetryAttempt <= 10; failedRetryAttempt++) {

            maxDelay += DEFAULT_INITIAL_RETRY_DELAY * ((long) Math.pow(2, failedRetryAttempt - 1)) * 2;

            /* One example of wait times of retries:
            Retry 1: 226 millisecond
            Retry 2: 788 millisecond
            Retry 3: 1214 millisecond
            Retry 4: 2135 millisecond
            Retry 5: 3332 millisecond
            Retry 6: 8662 millisecond
            Retry 7: 24898 millisecond
            Retry 8: 28659 millisecond
            Retry 9: 60000 millisecond
            Retry 10: 60000 millisecond
            */
            final Duration duration =
                    retryableSphereClientWithExponentialBackoff.calculateDurationWithExponentialRandomBackoff(
                            failedRetryAttempt, DEFAULT_INITIAL_RETRY_DELAY, DEFAULT_MAX_DELAY);

            assertThat(duration.toMillis())
                .isLessThanOrEqualTo(maxDelay)
                .isLessThanOrEqualTo(DEFAULT_MAX_DELAY);
        }
    }

    private RetryableSphereClientWithExponentialBackoff of_RetryableSphereClientWithExponentialBackoff() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");
        return RetryableSphereClientWithExponentialBackoff.of(clientConfig);
    }

    private CustomerUpdateCommand getCustomerUpdateCommand() {
        final List<UpdateAction<Customer>> updateActions = singletonList(ChangeEmail.of(""));
        return CustomerUpdateCommand.of(
            Versioned.of(UUID.randomUUID().toString(), 1L),
            updateActions
        );
    }
}
