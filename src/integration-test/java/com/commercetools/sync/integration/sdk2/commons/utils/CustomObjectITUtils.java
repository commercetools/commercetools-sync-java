package com.commercetools.sync.integration.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
public final class CustomObjectITUtils {

  /**
   * Deletes customObjects from CTP project which have key/container used in integration test,
   * represented by provided {@code ctpClient}.
   *
   * @param ctpClient represents the CTP project the custom objects will be deleted from.
   */
  public static void deleteCustomObject(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String key,
      @Nonnull final String container) {
    try {
      ctpClient.customObjects().withContainerAndKey(container, key).delete().execute().join();
    } catch (Exception e) {
      if (!(e.getCause() instanceof NotFoundException)) {
        throw e;
      }
    }
  }

  /**
   * Create a custom object in target CTP project, represented by provided {@code ctpClient}.
   *
   * @param ctpClient represents the CTP project the custom object will be created.
   * @param key represents the key field required in custom object.
   * @param container represents the container field required in custom object.
   * @param value represents the value field required in custom object.
   */
  public static CustomObject createCustomObject(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String key,
      @Nonnull final String container,
      @Nonnull final JsonNode value) {

    CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of().container(container).key(key).value(value).build();
    return ctpClient
        .customObjects()
        .post(customObjectDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture()
        .join();
  }

  /**
   * This method is expected to be used only by tests, it only works on projects with less than or
   * equal to 20 custom objects. Otherwise, it won't delete all the custom objects in the project of
   * the client.
   *
   * @param ctpClient the client to delete the custom objects from.
   */
  public static void deleteWaitingToBeResolvedCustomObjects(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String container) {
    ctpClient
        .customObjects()
        .withContainer(container)
        .get()
        .execute()
        .toCompletableFuture()
        .join()
        .getBody()
        .getResults()
        .forEach(
            customObject ->
                ctpClient
                    .customObjects()
                    .withContainerAndKey(customObject.getContainer(), customObject.getKey())
                    .delete()
                    .execute()
                    .toCompletableFuture()
                    .join());
  }

  private CustomObjectITUtils() {}
}
