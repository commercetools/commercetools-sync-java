package com.commercetools.sync.commons.helpers;


import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * A Generic Custom update action builder that creates update actions that are of the same type as the Generic type T
 * provided by the subclass of this abstract class. For example, if the subclass has T as
 * {@link io.sphere.sdk.categories.Category} then all the methods would build custom update actions
 * of the type {@link io.sphere.sdk.categories.Category}
 *
 * @param <T> the type of the resource to create update actions for.
 */
public abstract class GenericCustomActionBuilder<T extends Resource<T>> {
    /**
     * Creates a CTP "setCustomType" update action on the given resource {@code T} that removes the custom type set on
     * the given resource {@code T}. If the resource that has the custom fields is a secondary resource (e.g. Price or
     * asset) and not a primary resource (e.g Category, Product, Channel, etc..), the {@code variantId} and the
     * {@code objectId} will be used to identify the resource.
     *
     * @param variantId an optional field which could be used to identify the variant that holds the a resource
     *                  (e.g. asset) which has the custom fields.
     * @param objectId  an optional field which could be used to identify the id of the resource
     *                  (e.g. asset, price, etc..) which has the custom fields.
     * @return a setCustomType update action that removes the custom type from the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildRemoveCustomTypeAction(@Nullable final Integer variantId,
                                                                @Nullable final String objectId);

    /**
     * Creates a CTP "setCustomType" update action on the given resource {@code T}. If the resource that has the custom
     * fields is a secondary resource (e.g. Price or asset) and not a primary resource (e.g Category, Product, Channel,
     * etc..), the {@code variantId} and the {@code objectId} will be used to identify the resource.
     *
     * @param variantId           an optional field which could be used to identify the variant that holds the a
     *                            resource (e.g. asset) which has the custom fields.
     * @param objectId            an optional field which could be used to identify the id of the resource
     *                            (e.g. asset, price, etc..) which has the custom fields.
     * @param customTypeId        the id of the new custom type.
     * @param customFieldsJsonMap the custom fields map of JSON values.
     * @return a setCustomType update action of the type of the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildSetCustomTypeAction(@Nullable final Integer variantId,
                                                             @Nullable final String objectId,
                                                             @Nullable final String customTypeId,
                                                             @Nullable final Map<String, JsonNode> customFieldsJsonMap);

    /**
     * Creates a CTP "setCustomField" update action on the given resource {@code T} that updates a custom field with
     * {@code customFieldName} and a {@code customFieldValue} on the given resource {@code T}. If the resource that has
     * the custom fields is a secondary resource (e.g. Price or asset) and not a primary resource (e.g Category,
     * Product, Channel, etc..), the {@code variantId} and the {@code objectId} will be used to identify the resource.
     *
     * @param variantId        an optional field which could be used to identify the variant that holds the a resource
     *                         (e.g. asset) which has the custom fields.
     * @param objectId         an optional field which could be used to identify the id of the resource
     *                         (e.g. asset, price, etc..) which has the custom fields.
     * @param customFieldName  the name of the custom field to update.
     * @param customFieldValue the new JSON value of the custom field.
     * @return a setCustomField update action on the provided field name, with the provided value
     *         on the resource it's requested on.
     */
    @Nonnull
    public abstract UpdateAction<T> buildSetCustomFieldAction(@Nullable final Integer variantId,
                                                              @Nullable final String objectId,
                                                              @Nullable final String customFieldName,
                                                              @Nullable final JsonNode customFieldValue);
}
