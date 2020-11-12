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

    private SphereClientConfigOptions(SphereClientConfig clientConfig, long timeout, long initialDelay,
                                      TimeUnit timeUnit, int maxRetries, int maxParallelRequests) {
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

        public Builder(SphereClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            this.timeout = DEFAULT_TIMEOUT;
            this.timeUnit = DEFAULT_TIMEOUT_TIME_UNIT;
            this.initialDelay = INITIAL_RETRY_DELAY;
            this.maxRetries = MAX_RETRIES;
            this.maxParallelRequests = MAX_PARALLEL_REQUESTS;
        }

        public Builder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withInitialDelay(long initialDelay) {
            if(initialDelay < timeout) {
                this.initialDelay = initialDelay;
            }
            return this;
        }

        public Builder withTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder withMaxRetries(int maxRetries) {
            if(maxRetries > 1) {
              this.maxRetries = maxRetries;
            }
            return this;
        }

        public Builder withMaxParallelRequests(int maxParallelRequests) {
            this.maxParallelRequests = maxParallelRequests;
            return this;
        }

        public SphereClientConfigOptions build() {
            return new SphereClientConfigOptions(this.clientConfig, this.timeout, this.initialDelay,
                    this.timeUnit, this.maxRetries, this.maxParallelRequests);
        }
    }
}

