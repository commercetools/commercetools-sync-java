package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.CustomerGroupDraftBuilder;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.customergroups.commands.CustomerGroupDeleteCommand;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import javax.annotation.Nonnull;

public final class CustomerGroupITUtils {

  /**
   * Deletes all CustomerGroups from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the CustomerGroups from.
   */
  public static void deleteCustomerGroups(@Nonnull final SphereClient ctpClient) {
    queryAndExecute(ctpClient, CustomerGroupQuery.of(), CustomerGroupDeleteCommand::of);
  }

  /**
   * Creates a {@link CustomerGroup} in the CTP project defined by the {@code ctpClient} in a
   * blocking fashion.
   *
   * @param ctpClient defines the CTP project to create the CustomerGroup in.
   * @param name the name of the CustomerGroup to create.
   * @param key the key of the CustomerGroup to create.
   * @return the created CustomerGroup.
   */
  public static CustomerGroup createCustomerGroup(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final String name,
      @Nonnull final String key) {
    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of(name).key(key).build();

    return executeBlocking(ctpClient.execute(CustomerGroupCreateCommand.of(customerGroupDraft)));
  }

  private CustomerGroupITUtils() {}
}
