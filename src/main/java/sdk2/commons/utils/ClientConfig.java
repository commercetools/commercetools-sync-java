package sdk2.commons.utils;

import com.commercetools.api.defaultconfig.ServiceRegion;
import io.sphere.sdk.client.CorrelationIdGenerator;
import io.sphere.sdk.client.SphereClientConfigBuilder;
import io.sphere.sdk.models.Base;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The full api and auth configuration for a commercetools client.
 *
 * @see SphereClientConfigBuilder
 *
 */
public final class ClientConfig extends Base {

    public static final String API_URL = ServiceRegion.GCP_EUROPE_WEST1.getApiUrl();
    public static final String AUTH_URL = ServiceRegion.GCP_EUROPE_WEST1.getOAuthTokenUrl();

    private final String projectKey;
    private final String clientId;
    private final String clientSecret;
    private final String authUrl;
    private final String apiUrl;
    private final CorrelationIdGenerator correlationIdGenerator;

    ClientConfig(final String projectKey, final String clientId, final String clientSecret, final String authUrl, final String apiUrl, final CorrelationIdGenerator correlationIdGenerator) {
        this.apiUrl = requireNonBlank(apiUrl, "apiUrl");
        this.projectKey = requireNonBlank(projectKey, "projectKey");
        this.clientId = requireNonBlank(clientId, "clientId");
        this.clientSecret = requireNonBlank(clientSecret, "clientSecret");
        this.authUrl = requireNonBlank(authUrl, "authUrl");
        this.correlationIdGenerator = correlationIdGenerator;
    }

    public static ClientConfig of(final String projectKey, final String clientId, final String clientSecret) {
        return of(projectKey, clientId, clientSecret, AUTH_URL, API_URL);
    }

    public static ClientConfig of(final String projectKey, final String clientId, final String clientSecret, final String authUrl, final String apiUrl) {
        return new ClientConfig(projectKey, clientId, clientSecret, authUrl, apiUrl, CorrelationIdGenerator.of(projectKey));
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public CorrelationIdGenerator getCorrelationIdGenerator() {
        return correlationIdGenerator;
    }

    public static String requireNonBlank(final String parameter, final String parameterName) {
        if (isBlank(parameter)) {
            throw new IllegalArgumentException(format("%s is null or empty.", parameterName));
        }
        return parameter;
    }
}
