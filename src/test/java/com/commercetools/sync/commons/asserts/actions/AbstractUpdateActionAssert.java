package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.commands.UpdateAction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;

public class AbstractUpdateActionAssert<
        S extends AbstractUpdateActionAssert<S, A>, A extends UpdateAction>
    extends AbstractAssert<S, A> {

  AbstractUpdateActionAssert(@Nullable final A actual, @Nonnull final Class<S> selfType) {
    super(actual, selfType);
  }

  /**
   * Verifies that the actual {@link UpdateAction} value has identical fields as the ones supplied.
   *
   * @param actionName the update action name.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public S hasValues(final String actionName) {
    assertThat(actual).isNotNull();
    assertThat(actual.getAction()).isEqualTo(actionName);
    return myself;
  }
}
