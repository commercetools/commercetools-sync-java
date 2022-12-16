package com.commercetools.sync.sdk2.commons.models;

import com.commercetools.api.models.type.CustomFields;
import javax.annotation.Nullable;

public interface Custom {

  String getId();

  String getTypeId();

  @Nullable
  CustomFields getCustom();
}
