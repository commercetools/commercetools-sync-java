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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.calculateDurationWithExponentialRandomBackoff;
import static com.commercetools.sync.commons.utils.RetryableSphereClientWithExponentialBackoff.decorateSphereClient;
import static com.commercetools.sync.commons.utils.SphereClientConfigOptions.DEFAULT_TIMEOUT;
import static com.commercetools.sync.commons.utils.SphereClientConfigOptions.INITIAL_RETRY_DELAY;
import static com.commercetools.sync.commons.utils.SphereClientConfigOptions.MAX_PARALLEL_REQUESTS;
import static com.commercetools.sync.commons.utils.SphereClientConfigOptions.MAX_RETRIES;
import static io.sphere.sdk.client.TestDoubleSphereClientFactory.createHttpTestDouble;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RetryableSphereClientWithExponentialBackoffTest {
    private static final long TIMEOUT = 20000;
    private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;

    @Test
    void createClient_WithConfig_ReturnsSphereClient() {
        final SphereClientConfig clientConfig =
                SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient = RetryableSphereClientWithExponentialBackoff.of(clientConfig);

        assertThat(sphereClient instanceof SphereClient).isTrue();

        assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
    }

    @Test
    void createClient_WithConfig_ReturnsBlockingSphereClient() {
        final SphereClientConfig clientConfig =
            SphereClientConfig.of("project-key", "client-id", "client-secret");
        final SphereClient sphereClient =
                RetryableSphereClientWithExponentialBackoff.of(clientConfig, TIMEOUT, TIMEOUT_TIME_UNIT);

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
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
    void createClient_Simulate_Stuck_Sdk() {
        final Function<RetryContext, Duration> durationFunction = retryContext -> {
            throw new IllegalArgumentException();
        };
        final CompletionStage<Duration> durationCompletableFuture = CompletableFuture.supplyAsync(
            () -> durationFunction.apply(null));
        final Function<RetryContext, CompletionStage<Duration>> f = retryContext -> durationCompletableFuture;
        final Executor executor = Executors.newSingleThreadExecutor();
        final CompletableFuture<Duration> result = new CompletableFuture<>();
        try {
            final CompletionStage<Duration> initialCompletionStage = f.apply(null);
            initialCompletionStage.whenCompleteAsync((res, firstError) -> {
                final boolean isErrorCase = firstError != null;
                if (isErrorCase) {
                    // Error Case Exception should be thrown
                } else {
                    result.complete(res);
                }
            }, executor);
        } catch (final Throwable exception) { //necessary if f.apply() throws directly an exception
            result.completeExceptionally(exception);
        }
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
            calculateDurationWithExponentialRandomBackoff(retryContext.getAttempt(),
                    INITIAL_RETRY_DELAY, DEFAULT_TIMEOUT);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
    void createClient_withRetryDecorator_ShouldRetryWhen503HttpResponse() {
        final SphereClient mockSphereUnderlyingClient =
            spy(createHttpTestDouble(intent -> HttpResponse.of(HttpStatusCode.SERVICE_UNAVAILABLE_503)));

        final long maxRetryAttempt = 2L;
        final Function<RetryContext, Duration> durationFunction = retryContext -> Duration.ofSeconds(2);

        final SphereClient decoratedSphereClient =
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
            decorateSphereClient(mockSphereUnderlyingClient, maxRetryAttempt, durationFunction, MAX_PARALLEL_REQUESTS);

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
            final Duration duration = calculateDurationWithExponentialRandomBackoff(failedRetryAttempt,
                    INITIAL_RETRY_DELAY, DEFAULT_TIMEOUT);

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
