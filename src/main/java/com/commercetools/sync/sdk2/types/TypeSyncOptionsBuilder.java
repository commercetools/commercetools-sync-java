package com.commercetools.sync.sdk2.types;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class TypeSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        TypeSyncOptionsBuilder, TypeSyncOptions, Type, TypeDraft, TypeUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private TypeSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link TypeSyncOptionsBuilder} given a {@link ProjectApiRoot}
   * responsible for interaction with the target CTP project, with the default batch size ({@code
   * BATCH_SIZE_DEFAULT} = 50).
   *
   * @param ctpClient instance of the {@link ProjectApiRoot} responsible for interaction with the
   *     target CTP project.
   * @return new instance of {@link TypeSyncOptionsBuilder}
   */
  public static TypeSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new TypeSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link TypeSyncOptions} enriched with all fields provided to {@code
   * this} builder.
   *
   * @return new instance of {@link TypeSyncOptions}
   */
  @Override
  public TypeSyncOptions build() {
    return new TypeSyncOptions(
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
  protected TypeSyncOptionsBuilder getThis() {
    return this;
  }
}
