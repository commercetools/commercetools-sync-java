package com.commercetools.sync.commons.asserts.actions;

import io.sphere.sdk.types.customupdateactions.SetCustomTypeBase;

import javax.annotation.Nullable;

public final class SetCustomTypeAssert
    extends AbstractSetCustomTypeAssert<SetCustomTypeAssert, SetCustomTypeBase> {

    SetCustomTypeAssert(@Nullable final SetCustomTypeBase actual) {
        super(actual, SetCustomTypeAssert.class);
    }
}
