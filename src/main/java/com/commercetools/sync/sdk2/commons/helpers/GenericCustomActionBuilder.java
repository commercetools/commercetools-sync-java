package com.commercetools.sync.sdk2.commons.helpers;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.customer.Customer;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A generic custom update action builder that creates update actions for example for the type
 * {@link Customer}
 *
 * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
 *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
 */
public interface GenericCustomActionBuilder<
    ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>> {
  /**
   * Creates a CTP "setCustomType" update action on the given resource {@code T} that removes the
   * custom type set on the given resource {@code T}. If the resource that has the custom fields is
   * a secondary resource (e.g. price or asset) and not a primary resource (e.g Category, Product,
   * Channel, etc..), the {@code variantId} and the {@code objectId} will be used to identify this
   * secondary resource within its container.
   *
   * @param variantId an optional field which could be used to identify the variant that holds the a
   *     resource (e.g. asset) which has the custom fields.
   * @param objectId an optional field which could be used to identify the id of the resource (e.g.
   *     asset, price, etc..) which has the custom fields.
   * @return a setCustomType update action that removes the custom type from the resource it's
   *     requested on.
   */
  @Nonnull
  ResourceUpdateActionT buildRemoveCustomTypeAction(
      @Nullable Long variantId, @Nullable String objectId);

  /**
   * Creates a CTP "setCustomType" update action on the given resource {@code T}. If the resource
   * that has the custom fields is a secondary resource (e.g. Price or asset) and not a primary
   * resource (e.g Category, Product, Channel, etc..), the {@code variantId} and the {@code
   * objectId} will be used to identify this secondary resource within its container.
   *
   * @param variantId an optional field which could be used to identify the variant that holds the a
   *     resource (e.g. asset) which has the custom fields.
   * @param objectId an optional field which could be used to identify the id of the resource (e.g.
   *     asset, price, etc..) which has the custom fields.
   * @param customTypeId the id of the new custom type.
   * @param customFieldsJsonMap the custom fields map of JSON values.
   * @return a setCustomType update action of the type of the resource it's requested on.
   */
  @Nonnull
  ResourceUpdateActionT buildSetCustomTypeAction(
      @Nullable Long variantId,
      @Nullable String objectId,
      @Nonnull String customTypeId,
      @Nullable Map<String, Object> customFieldsJsonMap);

  /**
   * Creates a CTP "setCustomField" update action on the given resource {@code T} that updates a
   * custom field with {@code customFieldName} and a {@code customFieldValue} on the given resource
   * {@code T}. If the resource that has the custom fields is a secondary resource (e.g. Price or
   * asset) and not a primary resource (e.g Category, Product, Channel, etc..), the {@code
   * variantId} and the {@code objectId} will be used to identify this secondary resource within its
   * container.
   *
   * @param variantId an optional field which could be used to identify the variant that holds the a
   *     resource (e.g. asset) which has the custom fields.
   * @param objectId an optional field which could be used to identify the id of the resource (e.g.
   *     asset, price, etc..) which has the custom fields.
   * @param customFieldName the name of the custom field to update.
   * @param customFieldValue the new JSON value of the custom field.
   * @return a setCustomField update action on the provided field name, with the provided value on
   *     the resource it's requested on.
   */
  @Nonnull
  ResourceUpdateActionT buildSetCustomFieldAction(
      @Nullable Long variantId,
      @Nullable String objectId,
      @Nullable String customFieldName,
      @Nullable Object customFieldValue);
}
