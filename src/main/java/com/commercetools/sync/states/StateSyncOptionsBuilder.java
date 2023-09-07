package com.commercetools.sync.states;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateUpdateAction;
import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

public final class StateSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        StateSyncOptionsBuilder, StateSyncOptions, State, StateDraft, StateUpdateAction> {

  public static final int BATCH_SIZE_DEFAULT = 50;

  private StateSyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link StateSyncOptionsBuilder} given a {@link
   * com.commercetools.api.client.ProjectApiRoot} responsible for interaction with the target CTP
   * project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 500).
   *
   * @param ctpClient instance of the {@link com.commercetools.api.client.ProjectApiRoot}
   *     responsible for interaction with the target CTP project.
   * @return new instance of {@link StateSyncOptionsBuilder}
   */
  public static StateSyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {

    return new StateSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Creates new instance of {@link StateSyncOptions} enriched with all attributes provided to
   * {@code this} builder.
   *
   * @return new instance of {@link StateSyncOptions}
   */
  @Override
  public StateSyncOptions build() {
    return new StateSyncOptions(
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
  protected StateSyncOptionsBuilder getThis() {
    return this;
  }
}
