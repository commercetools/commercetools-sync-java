package com.commercetools.sync.integration.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraft;
import com.commercetools.api.models.customer_group.CustomerGroupDraftBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class CustomerGroupITUtils {

  /**
   * Deletes all CustomerGroups from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the CustomerGroups from.
   */
  public static void deleteCustomerGroups(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
            ctpClient.customerGroups().get(),
            customerGroups -> {
              CompletableFuture.allOf(
                      customerGroups.stream()
                          .map(customerGroup -> deleteCustomerGroup(ctpClient, customerGroup))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<CustomerGroup> deleteCustomerGroup(
      ProjectApiRoot ctpClient, CustomerGroup customerGroup) {
    return ctpClient
        .customerGroups()
        .delete(customerGroup)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
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
  public static CustomerGroup ensureCustomerGroup(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String name,
      @Nonnull final String key) {
    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of().groupName(name).key(key).build();

    return customerGroupExists(key, ctpClient)
        .thenCompose(
            customerGroup ->
                customerGroup
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () ->
                            ctpClient
                                .customerGroups()
                                .create(customerGroupDraft)
                                .execute()
                                .thenApply(ApiHttpResponse::getBody)))
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<Optional<CustomerGroup>> customerGroupExists(
      @Nonnull final String customerGroupKey, @Nonnull final ProjectApiRoot ctpClient) {

    return ctpClient
        .customerGroups()
        .withKey(customerGroupKey)
        .get()
        .execute()
        .handle(
            (customerGroupApiHttpResponse, throwable) -> {
              if (throwable != null) {
                return Optional.empty();
              }
              return Optional.of(customerGroupApiHttpResponse.getBody().get());
            });
  }

  private CustomerGroupITUtils() {}
}
