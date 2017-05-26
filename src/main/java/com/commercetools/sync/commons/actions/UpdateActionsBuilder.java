package com.commercetools.sync.commons.actions;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;

import java.util.List;

public interface UpdateActionsBuilder<T extends Resource<T>, S> {
    List<UpdateAction<T>> buildActions(T oldResource, S newResourceDraft);
}
