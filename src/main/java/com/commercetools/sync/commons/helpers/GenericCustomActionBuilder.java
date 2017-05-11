package com.commercetools.sync.commons.helpers;


import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * A Generic Custom update action builder that creates update actions that are of the same type as the Generic type T
 * provided by the subclass of this abstract class. For example, if the subclass has T as {@link io.sphere.sdk.categories.Category}
 * then all the methods would build custom update actions of the type {@link io.sphere.sdk.categories.Category}
 *
 * @param <T> the type of the resource to create update actions for.
 */
public abstract class GenericCustomActionBuilder<T extends Custom & Resource<T>> {
    /**
     * Creates a CTP "setCustomType" update action on the given resource {@link T} that removes the custom type set on
     * the given resource {@link T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @return a setCustomType update action that removes the custom type from the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildRemoveCustomTypeAction();

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@link T} (which currently could either
     * be a {@link Category} or a {@link Channel}).
     *
     * @param customTypeKey       the key of the new custom type.
     * @param customFieldsJsonMap the custom fields map of JSON values.
     * @return a setCustomType update action of the type of the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildSetCustomTypeAction(@Nullable final String customTypeKey,
                                                                       @Nullable final Map<String, JsonNode> customFieldsJsonMap);

    /**
     * Creates a CTP "setCustomField" update action on the given resource {@link T} that updates a custom field with
     * {@code customFieldName} and a {@code customFieldValue} on the given
     * resource {@link T} (which currently could either be a {@link Category} or a {@link Channel}).
     *
     * @param customFieldName  the name of the custom field to update.
     * @param customFieldValue the new JSON value of the custom field.
     * @return a setCustomField update action on the provided field name, with the provided value
     * on the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildSetCustomFieldAction(@Nullable final String customFieldName,
                                                                        @Nullable final JsonNode customFieldValue);
}
