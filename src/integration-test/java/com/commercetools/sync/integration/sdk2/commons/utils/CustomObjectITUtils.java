package com.commercetools.sync.integration.sdk2.commons.utils;

import com.commercetools.api.client.ProjectApiRoot;
import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
public final class CustomObjectITUtils {
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
