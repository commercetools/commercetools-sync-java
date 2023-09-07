package com.commercetools.sync.customobjects.models;

import com.commercetools.api.models.ResourceUpdateAction;

/**
 * This class is a fake class that represents a CTP resource with no update actions. Such resource
 * is currently custom objects. Since they're exception, I created this class so that we can still
 * use a common BaseSyncOptions for all resources and do not have to break the whole generics just
 * because of the custom object inconsistency.
 */
public class NoopResourceUpdateAction implements ResourceUpdateAction<NoopResourceUpdateAction> {
  @Override
  public String getAction() {
    return null;
  }

  @Override
  public NoopResourceUpdateAction get() {
    return this;
  }
}
