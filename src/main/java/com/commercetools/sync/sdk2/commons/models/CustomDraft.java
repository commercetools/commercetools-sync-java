package com.commercetools.sync.sdk2.commons.models;

import com.commercetools.api.models.type.CustomFieldsDraft;
import javax.annotation.Nullable;

/** Interface for draft objects which include custom fields. */
public interface CustomDraft {
  @Nullable
  CustomFieldsDraft getCustom();
}
