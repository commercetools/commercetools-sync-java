package com.commercetools.sync.commons.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.commercetools.sync.commons.models.Custom;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class GenericUpdateActionUtils {

  /**
   * Creates a CTP "setCustomType" update action on the given resource {@code T} according to the
   * type of the {@link com.commercetools.sync.commons.helpers.GenericCustomActionBuilder} passed.
   * If the {@code customTypeId} passed is blank (null/empty), the error callback is triggered with
   * an error message that the setCustomType update action cannot be built with a blank id, and an
   * empty optional is returned.
   *
   * @param <CustomT> the type of the resource which has the custom fields.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param customTypeId the id of the new custom type.
   * @param customFieldsJsonMap the custom fields map of JSON values.
   * @param resource the resource which has the custom fields.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param resourceIdGetter a function used to get the id of the resource being updated.
   * @param resourceTypeIdGetter a function used to get the type id of the resource being updated.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method.
   * @return a setCustomType update action of the type of the resource it's requested on, or an
   *     empty optional if the {@code customTypeId} is blank.
   */
  @Nonnull
  static <
          CustomT extends Custom,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> buildTypedSetCustomTypeUpdateAction(
          @Nullable final String customTypeId,
          @Nullable final Map<String, Object> customFieldsJsonMap,
          @Nonnull final CustomT resource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> resourceIdGetter,
          @Nonnull final Function<CustomT, String> resourceTypeIdGetter,
          @Nonnull final Function<CustomT, String> updateIdGetter,
          @Nonnull final BaseSyncOptions syncOptions) {

    if (!isBlank(customTypeId)) {
      return Optional.of(
          customActionBuilder.buildSetCustomTypeAction(
              variantId, updateIdGetter.apply(resource), customTypeId, customFieldsJsonMap));
    } else {
      final String errorMessage =
          format(
              "Failed to build 'setCustomType' update action on the "
                  + "%s with id '%s'. Reason: New Custom Type id is blank (null/empty).",
              resourceTypeIdGetter.apply(resource), resourceIdGetter.apply(resource));
      syncOptions.applyErrorCallback(errorMessage);
      return Optional.empty();
    }
  }

  private GenericUpdateActionUtils() {}
}
