package com.commercetools.sync.commons.asserts.actions;

import io.sphere.sdk.types.customupdateactions.SetCustomFieldBase;
import javax.annotation.Nullable;

public final class SetCustomFieldAssert
    extends AbstractSetCustomFieldAssert<SetCustomFieldAssert, SetCustomFieldBase> {

  SetCustomFieldAssert(@Nullable final SetCustomFieldBase actual) {
    super(actual, SetCustomFieldAssert.class);
  }
}
