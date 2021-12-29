package com.commercetools.sync.sdk2.commons.utils.retry;

import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.sync.sdk2.commons.utils.ClientConfig;
import io.sphere.sdk.models.Base;
import io.vrap.rmf.base.client.VrapHttpClient;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * To create a Sphere Client with retry logic which computes a exponential backoff time delay in
 * milliseconds. And handle all the configurations for the creation of client.
 */
public final class RetryableProjectApiClientBuilder extends Base {
  protected static final long DEFAULT_MAX_DELAY = 60000;
  protected static final long DEFAULT_INITIAL_RETRY_DELAY = 200;
  protected static final int DEFAULT_MAX_RETRY_ATTEMPT = 5;
  protected static final int DEFAULT_MAX_PARALLEL_REQUESTS = 20;
  private static final List<Integer> DEFAULT_STATUS_CODES_TO_RETRY =
      Arrays.asList(500, 502, 503, 504);

  private final ClientConfig clientConfig;
  private final VrapHttpClient httpClient;
  private long maxDelay;
  private long initialRetryDelay;
  private int maxRetryAttempt;
  private int maxParallelRequests;
  private List<Integer> statusCodesToRetry;

  private RetryableProjectApiClientBuilder(
      @Nonnull final ClientConfig clientConfig, @Nonnull final VrapHttpClient httpClient) {

    this.clientConfig = clientConfig;
    this.httpClient = httpClient;
    this.maxDelay = DEFAULT_MAX_DELAY;
    this.initialRetryDelay = DEFAULT_INITIAL_RETRY_DELAY;
    this.maxRetryAttempt = DEFAULT_MAX_RETRY_ATTEMPT;
    this.maxParallelRequests = DEFAULT_MAX_PARALLEL_REQUESTS;
    this.statusCodesToRetry = DEFAULT_STATUS_CODES_TO_RETRY;
  }

  /**
   * Creates a new instance of {@link RetryableProjectApiClientBuilder} given a {@link ClientConfig}
   * responsible for creation of a API Client.
   *
   * @param clientConfig the client configuration for the client.
   * @param httpClient client to execute requests
   * @return the instantiated {@link RetryableProjectApiClientBuilder}.
   */
  public static RetryableProjectApiClientBuilder of(
      @Nonnull final ClientConfig clientConfig, @Nonnull final VrapHttpClient httpClient) {

    return new RetryableProjectApiClientBuilder(clientConfig, httpClient);
  }

  /**
   * Sets the maxDelay value value in milliseconds.
   *
   * @param maxDelay - build with maxDelay value.
   * @return {@link RetryableProjectApiClientBuilder} with given maxDelay value.
   */
  public RetryableProjectApiClientBuilder withMaxDelay(final long maxDelay) {
    this.maxDelay = maxDelay;
    return this;
  }

  /**
   * Sets the initialDelay value in milliseconds.
   *
   * @param initialDelay - build with initialDelay value. If initialDelay is equal or greater than
   *     maxDelay then, a {@link IllegalArgumentException} will be thrown.
   * @return {@link RetryableProjectApiClientBuilder} with given initialDelay value.
   */
  public RetryableProjectApiClientBuilder withInitialDelay(final long initialDelay) {
    if (initialDelay < maxDelay) {
      this.initialRetryDelay = initialDelay;
    } else {
      throw new IllegalArgumentException(
          format("InitialDelay %s is less than MaxDelay %s.", initialDelay, maxDelay));
    }
    return this;
  }

  /**
   * Sets the Max Retry value, It should be greater than 1 for the Retry attempt.
   *
   * @param maxRetryAttempt - build with maxRetries value. If maxRetryAttempt is less than 1 then, a
   *     {@link IllegalArgumentException} will be thrown.
   * @return {@link RetryableProjectApiClientBuilder} with given maxRetries value.
   */
  public RetryableProjectApiClientBuilder withMaxRetryAttempt(final int maxRetryAttempt) {
    if (maxRetryAttempt > 0) {
      this.maxRetryAttempt = maxRetryAttempt;
    } else {
      throw new IllegalArgumentException(
          format("MaxRetryAttempt %s cannot be less than 1.", maxRetryAttempt));
    }
    return this;
  }

  /**
   * Sets the Max Parallel Requests value, It should be always positive number.
   *
   * @param maxParallelRequests - build with maxParallelRequests value. If maxParallelRequests is
   *     less than 1 then, a {@link IllegalArgumentException} will be thrown.
   * @return {@link RetryableProjectApiClientBuilder} with given maxParallelRequests value.
   */
  public RetryableProjectApiClientBuilder withMaxParallelRequests(final int maxParallelRequests) {
    if (maxParallelRequests > 0) {
      this.maxParallelRequests = maxParallelRequests;
    } else {
      throw new IllegalArgumentException(
          format("MaxParallelRequests %s cannot be less than 0", maxParallelRequests));
    }
    return this;
  }

  /**
   * Sets the Retry Error Status Codes.
   *
   * @param statusCodesToRetry - build with retryErrorStatusCodes.
   * @return {@link RetryableProjectApiClientBuilder} with given retryErrorStatusCodes.
   */
  public RetryableProjectApiClientBuilder withStatusCodesToRetry(
      final List<Integer> statusCodesToRetry) {
    this.statusCodesToRetry = statusCodesToRetry;
    return this;
  }

  /**
   * creates a Retry ProjectApiRoot client using the class configuration values.
   *
   * @return the instantiated {@link ProjectApiRoot}
   */
  public ProjectApiRoot build() {
    return ApiRootBuilder.of(httpClient)
        .defaultClient(
            ClientCredentials.of()
                .withClientId(clientConfig.getClientId())
                .withClientSecret(clientConfig.getClientSecret())
                .build(),
            clientConfig.getAuthUrl(),
            clientConfig.getApiUrl())
        .withRetryMiddleware(maxRetryAttempt, statusCodesToRetry)
        .build(clientConfig.getProjectKey());
  }
}
