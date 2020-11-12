package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SphereClientConfig;

import java.util.concurrent.TimeUnit;

public class SphereClientConfigOptions {
    protected static final long DEFAULT_TIMEOUT = 30000;
    protected static final long INITIAL_RETRY_DELAY = 200;
    protected static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    protected static final int MAX_RETRIES = 5;
    protected static final int MAX_PARALLEL_REQUESTS = 20;

    private SphereClientConfig clientConfig;
    protected long timeout;
    protected long initialDelay;
    protected TimeUnit timeUnit;
    protected int maxRetries;
    protected int maxParallelRequests;

    private SphereClientConfigOptions(final SphereClientConfig clientConfig, final long timeout,
                                      final long initialDelay, final TimeUnit timeUnit,
                                      final int maxRetries, final int maxParallelRequests) {
        this.clientConfig = clientConfig;
        this.timeout = timeout;
        this.initialDelay = initialDelay;
        this.timeUnit = timeUnit;
        this.maxRetries = maxRetries;
        this.maxParallelRequests = maxParallelRequests;
    }

    public SphereClientConfig getClientConfig() {
        return clientConfig;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMaxParallelRequests() {
        return maxParallelRequests;
    }

    public static class Builder {
        private SphereClientConfig clientConfig;
        protected long timeout;
        protected long initialDelay;
        protected TimeUnit timeUnit;
        protected int maxRetries;
        protected int maxParallelRequests;

        /**
         * Creates a SphereClientConfig Builder with Default values
         * @param clientConfig the client configuration for the client.
         */
        public Builder(final SphereClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            this.timeout = DEFAULT_TIMEOUT;
            this.timeUnit = DEFAULT_TIMEOUT_TIME_UNIT;
            this.initialDelay = INITIAL_RETRY_DELAY;
            this.maxRetries = MAX_RETRIES;
            this.maxParallelRequests = MAX_PARALLEL_REQUESTS;
        }

        /**
         * Sets the timeout
         * @param timeout - build with timeout value.
         */
        public Builder withTimeout(final long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the timeout
         * @param initialDelay - build with initialDelay value.
         */
        public Builder withInitialDelay(final long initialDelay) {
            if (initialDelay < timeout) {
                this.initialDelay = initialDelay;
            }
            return this;
        }

        /**
         * Sets the timeUnit
         * @param timeUnit - build with timeUnit value.
         */
        public Builder withTimeUnit(final TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Sets the Max Retry
         * @param maxRetries - build with maxRetries value.
         */
        public Builder withMaxRetries(final int maxRetries) {
            if (maxRetries > 1) {
                this.maxRetries = maxRetries;
            }
            return this;
        }

        /**
         * Sets the Max Parallel Requests
         * @param maxParallelRequests - build with maxParallelRequests value.
         */
        public Builder withMaxParallelRequests(final int maxParallelRequests) {
            this.maxParallelRequests = maxParallelRequests;
            return this;
        }

        public SphereClientConfigOptions build() {
            return new SphereClientConfigOptions(this.clientConfig, this.timeout, this.initialDelay,
                    this.timeUnit, this.maxRetries, this.maxParallelRequests);
        }
    }
}

