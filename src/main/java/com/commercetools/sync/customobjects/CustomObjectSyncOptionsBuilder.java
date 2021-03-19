package com.commercetools.sync.customobjects;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import javax.annotation.Nonnull;

public final class CustomObjectSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        CustomObjectSyncOptionsBuilder,
        CustomObjectSyncOptions,
        CustomObject<JsonNode>,
        CustomObjectDraft<JsonNode>,
        CustomObject<JsonNode>> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private CustomObjectSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link CustomObjectSyncOptionsBuilder} given a {@link SphereClient}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link CustomObjectSyncOptionsBuilder}
   */
  public static CustomObjectSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
    return new CustomObjectSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link CustomObjectSyncOptions} enriched with all attributes provided
   * to {@code this} builder.
   *
   * @return new instance of {@link CustomObjectSyncOptions}
   */
  @Override
  public CustomObjectSyncOptions build() {
    return new CustomObjectSyncOptions(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }

  /**
   * Returns an instance of this class to be used in the superclass's generic methods. Please see
   * the JavaDoc in the overridden method for further details.
   *
   * @return an instance of this class.
   */
  @Override
  protected CustomObjectSyncOptionsBuilder getThis() {
    return this;
  }
}
