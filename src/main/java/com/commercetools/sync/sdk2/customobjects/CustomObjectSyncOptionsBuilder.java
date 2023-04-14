package com.commercetools.sync.sdk2.customobjects;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customobjects.models.NoopResourceUpdateAction;
import javax.annotation.Nonnull;

public final class CustomObjectSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        CustomObjectSyncOptionsBuilder,
        CustomObjectSyncOptions,
        CustomObject,
        CustomObjectDraft,
        NoopResourceUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private CustomObjectSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link
   * com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder} given a {@link
   * com.commercetools.api.client.ProjectApiRoot} responsible for interaction with the target CTP
   * project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link com.commercetools.api.client.ProjectApiRoot}
   *     responsible for interaction with the target CTP project.
   * @return new instance of {@link
   *     com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder}
   */
  public static CustomObjectSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new CustomObjectSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link com.commercetools.sync.customobjects.CustomObjectSyncOptions}
   * enriched with all attributes provided to {@code this} builder.
   *
   * @return new instance of {@link com.commercetools.sync.customobjects.CustomObjectSyncOptions}
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
