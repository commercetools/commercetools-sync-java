package com.commercetools.sync.commons.asserts.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.types.customupdateactions.SetCustomFieldBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbstractSetCustomFieldAssert<
        T extends AbstractSetCustomFieldAssert<T, S>, S extends SetCustomFieldBase>
    extends AbstractUpdateActionAssert<T, S> {

  AbstractSetCustomFieldAssert(@Nullable final S actual, @Nonnull final Class<T> selfType) {
    super(actual, selfType);
  }

  /**
   * Verifies that the actual {@link SetCustomFieldBase} value has identical fields as the ones
   * supplied.
   *
   * @param actionName the update action name.
   * @param customFieldName the custom field name to update.
   * @param customFieldValue the new custom field name to update.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual value is {@code null}.
   * @throws AssertionError if the actual fields do not match the supplied values.
   */
  public AbstractSetCustomFieldAssert hasValues(
      @Nonnull final String actionName,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue) {

    super.hasValues(actionName);
    assertThat(actual.getName()).isEqualTo(customFieldName);
    assertThat(actual.getValue()).isEqualTo(customFieldValue);
    return myself;
  }
}
