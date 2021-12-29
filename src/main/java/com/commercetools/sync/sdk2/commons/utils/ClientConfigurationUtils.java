package com.commercetools.sync.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerChangeEmailAction;
import com.commercetools.api.models.customer.CustomerUpdate;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.http.okhttp4.CtOkHttp4Client;
import com.commercetools.sync.sdk2.commons.utils.retry.RetryableProjectApiClientBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public final class ClientConfigurationUtils {

  /**
   * Creates a RetryableProjectApiClient {@link ProjectApiRoot}.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link ProjectApiRoot}.
   */
  public static ProjectApiRoot createClient(@Nonnull final ClientConfig clientConfig) {
    return RetryableProjectApiClientBuilder.of(clientConfig, new CtOkHttp4Client()).build();
  }

  public static void main(String[] args) {
    final CustomerUpdate cu = CustomerUpdate.of();
    cu.setVersion(1L);

    List<CustomerUpdateAction> updateActions = new ArrayList<>();

    final CustomerChangeEmailAction customerChangeEmailAction = CustomerChangeEmailAction.of();
    customerChangeEmailAction.setEmail("email");
    updateActions.add(customerChangeEmailAction);

    cu.setActions(updateActions);

    ClientConfig clientConfig = ClientConfig.of("", "", "");
    final ProjectApiRoot client = createClient(clientConfig);
    final Customer updatedCust =
        client
            .customers()
            .withId("id")
            .post(cu)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    System.out.println(updatedCust);
  }
}
