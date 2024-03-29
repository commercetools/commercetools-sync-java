package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.CustomValueConverter.convertCustomValueObjDataToJsonNode;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.common.BaseResource;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.commercetools.sync.commons.models.Custom;
import com.commercetools.sync.commons.models.CustomDraft;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomUpdateActionUtils {
  private static final String CUSTOM_TYPE_IDS_NOT_SET =
      "Custom type ids are not set for both the old and new %s.";
  private static final String CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED =
      "Failed to build custom fields update " + "actions on the %s with id '%s'. Reason: %s";
  private static final String CUSTOM_TYPE_ID_IS_BLANK =
      "New resource's custom type id is blank (empty/null).";

  /**
   * This method is a syntactic sugar for the method {@link
   * #buildCustomUpdateActions(com.commercetools.sync.commons.models.Custom,
   * com.commercetools.sync.commons.models.CustomDraft,
   * com.commercetools.sync.commons.helpers.GenericCustomActionBuilder, Long, Function, Function,
   * Function, com.commercetools.sync.commons.BaseSyncOptions)} but this one is only for primary
   * resources (i.e resources which have their own endpoints for example channels, categories,
   * inventory entries. For more details of the inner logic and different scenarios, check the
   * Javadoc of the other method.
   *
   * @param <CustomT> the type of the old {@link com.commercetools.sync.commons.models.Custom} which
   *     has the custom fields.
   * @param <CustomDraftT> the type of the new resource {@link
   *     com.commercetools.sync.commons.models.CustomDraft}.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param oldResource the resource which should be updated.
   * @param newResource the resource draft where we get the new custom fields.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param syncOptions responsible for supplying the sync options to the sync utility method.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   * @see #buildCustomUpdateActions(com.commercetools.sync.commons.models.Custom,
   *     com.commercetools.sync.commons.models.CustomDraft,
   *     com.commercetools.sync.commons.helpers.GenericCustomActionBuilder, Long, Function,
   *     Function, Function, com.commercetools.sync.commons.BaseSyncOptions) )
   */
  @Nonnull
  public static <
          CustomT extends Custom,
          CustomDraftT extends CustomDraft,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildPrimaryResourceCustomUpdateActions(
          @Nonnull final CustomT oldResource,
          @Nonnull final CustomDraftT newResource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nonnull final BaseSyncOptions syncOptions) {
    return buildCustomUpdateActions(
        oldResource,
        newResource,
        customActionBuilder,
        null,
        Custom::getId,
        Custom::getTypeId,
        resource -> null, // No update ID needed for primary resources.
        syncOptions);
  }

  /**
   * Compares the {@link CustomFields} of an old resource {@code T} (for example {@link Category},
   * {@link Product}, etc..), to the {@link CustomFieldsDraft}, of a new resource draft {@code S}
   * (for example {@link CategoryDraft}, {@link ProductVariantDraft}, etc..), and returns a {@link
   * List}&lt;{@link ResourceUpdateAction}&gt; as a result. If no update action is needed, for
   * example in the case where both the {@link CustomFields} and the {@link CustomFieldsDraft} are
   * null, an empty {@link List}&lt;{@link ResourceUpdateAction}&gt; is returned. A {@link
   * BaseSyncOptions} instance is injected into the method which is responsible for supplying the
   * sync options to the sync utility method. For example, custom error callbacks for errors. The
   * {@link com.commercetools.sync.services.TypeService} is injected also for fetching the key of
   * the old resource type from it's cache (see {@link
   * CustomUpdateActionUtils#buildNonNullCustomFieldsUpdateActions(CustomFields, CustomFieldsDraft,
   * Custom, GenericCustomActionBuilder, Long, Function, Function, Function, BaseSyncOptions)}).
   *
   * <p>An update action will be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>If the new resources's custom type is set, but old resources's custom type is not. A
   *       "setCustomType" update actions is added, which sets the custom type (and all it's fields
   *       to the old resource).
   *   <li>If the new resource's custom type is not set, but the old resource's custom type is set.
   *       A "setCustomType" update action is added, which removes the type set on the old resource.
   *   <li>If both the resources custom types are the same and the custom fields are both set. The
   *       custom field values of both resources are then calculated. (see {@link
   *       CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom,
   *       GenericCustomActionBuilder, Long, Function)} )})
   *   <li>If the keys of both custom types are different, then a "setCustomType" update action is
   *       added, where the old resource's custom type is set to be as the new one's.
   *   <li>If both resources custom type keys are identical but the custom fields of the new
   *       resource's custom type is not set.
   * </ol>
   *
   * <p>An update action will <b>not</b> be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>If both the resources' custom types are not set.
   *   <li>If both the resources' custom type keys are not set.
   *   <li>Custom fields are both empty.
   *   <li>Custom field JSON values have different ordering.
   *   <li>Custom field values are identical.
   * </ol>
   *
   * @param <ResourceDraftT> the type of the new {@link BaseResource} which is the super {@link
   *     BaseResource} of the Resource to update.
   * @param <CustomT> the type of the old {@link Custom} which has the custom fields.
   * @param <CustomDraftT> the type of the new resource {@link CustomDraft}.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param newMainResourceDraft the main resource of the resource draft where we get the new custom
   *     fields.
   * @param oldResource the resource which should be updated.
   * @param newResourceDraft the resource draft where we get the new custom fields.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param resourceIdGetter a function used to get the id of the resource being updated.
   * @param resourceTypeIdGetter a function used to get the Type id of the resource being updated.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static <
          ResourceDraftT,
          CustomT extends Custom,
          CustomDraftT extends CustomDraft,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildCustomUpdateActions(
          @Nullable final ResourceDraftT newMainResourceDraft,
          @Nonnull final CustomT oldResource,
          @Nonnull final CustomDraftT newResourceDraft,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> resourceIdGetter,
          @Nonnull final Function<CustomT, String> resourceTypeIdGetter,
          @Nonnull final Function<CustomT, String> updateIdGetter,
          @Nonnull final BaseSyncOptions syncOptions) {

    final CustomFields oldResourceCustomFields = oldResource.getCustom();
    final CustomFieldsDraft newResourceCustomFields = newResourceDraft.getCustom();
    if (oldResourceCustomFields != null && newResourceCustomFields != null) {
      try {
        return buildNonNullCustomFieldsUpdateActions(
            oldResourceCustomFields,
            newResourceCustomFields,
            oldResource,
            customActionBuilder,
            variantId,
            resourceIdGetter,
            resourceTypeIdGetter,
            updateIdGetter,
            syncOptions);
      } catch (BuildUpdateActionException exception) {
        final String errorMessage =
            format(
                CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED,
                resourceTypeIdGetter.apply(oldResource),
                resourceIdGetter.apply(oldResource),
                exception.getMessage());
        syncOptions.applyErrorCallback(
            new SyncException(errorMessage, exception),
            oldResource,
            newMainResourceDraft != null ? newMainResourceDraft : newResourceDraft,
            null);
      }
    } else {
      if (oldResourceCustomFields == null) {
        if (newResourceCustomFields != null) {
          // New resource's custom fields are set, but old resources's custom fields are not set. So
          // we
          // should set the custom type and fields of the new resource to the old one.
          final String newCustomFieldsTypeId = newResourceCustomFields.getType().getId();
          if (isBlank(newCustomFieldsTypeId)) {
            final String errorMessage =
                format(
                    CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED,
                    resourceTypeIdGetter.apply(oldResource),
                    resourceIdGetter.apply(oldResource),
                    CUSTOM_TYPE_ID_IS_BLANK);
            syncOptions.applyErrorCallback(
                new SyncException(errorMessage, null),
                oldResource,
                newMainResourceDraft != null ? newMainResourceDraft : newResourceDraft,
                null);
          } else {
            final Map<String, Object> newCustomFieldsJsonMap =
                newResourceCustomFields.getFields().values();
            final Optional<ResourceUpdateActionT> updateAction =
                GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                    newCustomFieldsTypeId,
                    newCustomFieldsJsonMap,
                    oldResource,
                    customActionBuilder,
                    variantId,
                    resourceIdGetter,
                    resourceTypeIdGetter,
                    updateIdGetter,
                    syncOptions);
            return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
          }
        }
      } else {
        // New resource's custom fields are not set, but old resource's custom fields are set. So we
        // should remove the custom type from the old resource.

        return singletonList(
            customActionBuilder.buildRemoveCustomTypeAction(
                variantId, updateIdGetter.apply(oldResource)));
      }
    }
    return Collections.emptyList();
  }

  @Nonnull
  private static <
          CustomT extends Custom,
          CustomDraftT extends CustomDraft,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildCustomUpdateActions(
          @Nonnull final CustomT oldResource,
          @Nonnull final CustomDraftT newResourceDraft,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> resourceIdGetter,
          @Nonnull final Function<CustomT, String> resourceTypeIdGetter,
          @Nonnull final Function<CustomT, String> updateIdGetter,
          @Nonnull final BaseSyncOptions syncOptions) {
    return buildCustomUpdateActions(
        null,
        oldResource,
        newResourceDraft,
        customActionBuilder,
        variantId,
        resourceIdGetter,
        resourceTypeIdGetter,
        updateIdGetter, // No update ID needed for primary resources.
        syncOptions);
  }

  /**
   * Compares a non-null {@link CustomFields} to a non-null {@link CustomFieldsDraft} and returns a
   * {@link List}&lt;{@link ResourceUpdateAction}&gt; as a result. The keys are used to compare the
   * custom types. The key of the old resource custom type is fetched from the caching mechanism of
   * the {@link com.commercetools.sync.services.TypeService} instance supplied as a param to the
   * method. The key of the new resource custom type is expected to be set on the type. If no update
   * action is needed an empty {@link List}&lt;{@link ResourceUpdateAction}&gt; is returned.
   *
   * <p>An update action will be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>If both the resources custom type keys are the same and the custom fields are both set.
   *       The custom field values of both resources are then calculated. (see {@link
   *       CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom,
   *       GenericCustomActionBuilder, Long, Function)})
   *   <li>If the keys of both custom types are different, then a "setCustomType" update action is
   *       added, where the old resource's custom type is set to be as the new one's.
   *   <li>If both resources custom type keys are identical but the custom fields of the new
   *       resource's custom type is not set.
   * </ol>
   *
   * <p>An update action will <bold>not</bold> be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>If both the resources' custom type keys are not set.
   * </ol>
   *
   * @param <CustomT> the type of the old {@link Custom} which has the custom fields.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param oldCustomFields the old resource's custom fields.
   * @param newCustomFields the new resource draft's custom fields.
   * @param resource the resource that the custom fields are on. It is used to identify the type of
   *     the resource, to call the corresponding update actions.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param resourceIdGetter a function used to get the id of the resource being updated.
   * @param resourceTypeIdGetter a function used to get the Type id of the resource being updated.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @param syncOptions responsible for supplying the sync options to the sync utility method.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull
  static <
          CustomT extends Custom,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildNonNullCustomFieldsUpdateActions(
          @Nonnull final CustomFields oldCustomFields,
          @Nonnull final CustomFieldsDraft newCustomFields,
          @Nonnull final CustomT resource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> resourceIdGetter,
          @Nonnull final Function<CustomT, String> resourceTypeIdGetter,
          @Nonnull final Function<CustomT, String> updateIdGetter,
          @Nonnull final BaseSyncOptions syncOptions)
          throws BuildUpdateActionException {

    final String oldCustomTypeId = oldCustomFields.getType().getId();

    final Map<String, Object> oldCustomFieldsJsonMap = oldCustomFields.getFields().values();
    final String newCustomTypeId = newCustomFields.getType().getId();
    final FieldContainer fields = newCustomFields.getFields();
    Map<String, Object> newCustomFieldsJsonMap = Collections.emptyMap();
    if (fields != null) {
      newCustomFieldsJsonMap = fields.values();
    }

    if (Objects.equals(oldCustomTypeId, newCustomTypeId)) {
      if (isBlank(oldCustomTypeId)) {
        throw new BuildUpdateActionException(
            format(CUSTOM_TYPE_IDS_NOT_SET, resourceTypeIdGetter.apply(resource)));
      }
      if (newCustomFieldsJsonMap == null) {
        // New resource's custom fields are null/not set. So we should unset old custom fields.
        final Optional<ResourceUpdateActionT> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                null,
                resource,
                customActionBuilder,
                variantId,
                resourceIdGetter,
                resourceTypeIdGetter,
                updateIdGetter,
                syncOptions);

        return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
      }
      // old and new resource's custom fields are set. So we should calculate update actions for the
      // the fields of both.
      return buildSetCustomFieldsUpdateActions(
          oldCustomFieldsJsonMap,
          newCustomFieldsJsonMap,
          resource,
          customActionBuilder,
          variantId,
          updateIdGetter);
    } else {
      final Optional<ResourceUpdateActionT> updateAction =
          GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
              newCustomTypeId,
              newCustomFieldsJsonMap,
              resource,
              customActionBuilder,
              variantId,
              resourceIdGetter,
              resourceTypeIdGetter,
              updateIdGetter,
              syncOptions);
      return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }
  }

  /**
   * Compares two {@link Map} objects representing a map of the custom field name to the JSON
   * representation of the value of the corresponding custom field. It returns a {@link
   * List}&lt;{@link ResourceUpdateAction}&gt; as a result. If no update action is needed an empty
   * {@link List}&lt;{@link ResourceUpdateAction}&gt; is returned.
   *
   * <p>An update action will be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>A custom field value is changed.
   *   <li>A custom field value is removed.
   *   <li>A new custom field value is added.
   * </ol>
   *
   * <p>An update action will <bold>not</bold> be added to the result list in the following cases:-
   *
   * <ol>
   *   <li>Custom fields are both empty.
   *   <li>Custom field JSON values have different ordering.
   *   <li>Custom field values are identical.
   * </ol>
   *
   * @param <CustomT> the type of the old {@link Custom} which has the custom fields.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param oldCustomFields the old resource's custom fields map of JSON values.
   * @param newCustomFields the new resource's custom fields map of JSON values.
   * @param resource the resource that the custom fields are on. It is used to identify the type of
   *     the resource, to call the corresponding update actions.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull
  static <
          CustomT extends Custom,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildSetCustomFieldsUpdateActions(
          @Nonnull final Map<String, Object> oldCustomFields,
          @Nonnull final Map<String, Object> newCustomFields,
          @Nonnull final CustomT resource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> updateIdGetter) {

    final List<ResourceUpdateActionT> customFieldsUpdateActions =
        buildNewOrModifiedCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            resource,
            customActionBuilder,
            variantId,
            updateIdGetter);

    final List<ResourceUpdateActionT> removedCustomFieldsActions =
        buildRemovedCustomFieldsUpdateActions(
            oldCustomFields,
            newCustomFields,
            resource,
            customActionBuilder,
            variantId,
            updateIdGetter);

    customFieldsUpdateActions.addAll(removedCustomFieldsActions);
    return customFieldsUpdateActions;
  }

  /**
   * Traverses the new resource's custom fields map of JSON values {@link Map} to create
   * "setCustomField" update actions that represent either the modification of an existent custom
   * field's value or the addition of a new one. It returns a {@link List}&lt;{@link
   * ResourceUpdateAction}&gt; as a result. If no update action is needed an empty {@link
   * List}&lt;{@link ResourceUpdateAction}&gt; is returned.
   *
   * <p>Note: Null value custom fields are filtered out. In other words, no update actions would be
   * built for fields with null values.
   *
   * @param <CustomT> the type of the old {@link Custom} which has the custom fields.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param oldCustomFields the old resource's custom fields map of JSON values.
   * @param newCustomFields the new resource's custom fields map of JSON values.
   * @param resource the resource that the custom fields are on. It is used to identify the type of
   *     the resource, to call the corresponding update actions.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull
  private static <
          CustomT extends Custom,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildNewOrModifiedCustomFieldsUpdateActions(
          @Nonnull final Map<String, Object> oldCustomFields,
          @Nonnull final Map<String, Object> newCustomFields,
          @Nonnull final CustomT resource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> updateIdGetter) {

    return newCustomFields.keySet().stream()
        .filter(
            newCustomFieldName -> {
              // Convert Object to JSONNode
              final JsonNode newCustomFieldValue =
                  convertCustomValueObjDataToJsonNode(newCustomFields.get(newCustomFieldName));
              final JsonNode oldCustomFieldValue =
                  convertCustomValueObjDataToJsonNode(oldCustomFields.get(newCustomFieldName));
              return !isNullJsonValue(newCustomFieldValue)
                  && !Objects.equals(newCustomFieldValue, oldCustomFieldValue);
            })
        .map(
            newCustomFieldName ->
                customActionBuilder.buildSetCustomFieldAction(
                    variantId,
                    updateIdGetter.apply(resource),
                    newCustomFieldName,
                    newCustomFields.get(newCustomFieldName)))
        .collect(Collectors.toList());
  }

  private static boolean isNullJsonValue(@Nullable final JsonNode value) {
    return !Objects.nonNull(value) || value.isNull();
  }

  /**
   * Traverses the old resource's custom fields map of JSON values {@link Map} to create update
   * "setCustomField" update actions that represent removal of an existent custom field from the new
   * resource's custom fields. It returns a {@link List}&lt;{@link ResourceUpdateAction}&gt; as a
   * result. If no update action is needed an empty {@link List}&lt;{@link ResourceUpdateAction}&gt;
   * is returned.
   *
   * @param <CustomT> the type of the old {@link Custom} which has the custom fields.
   * @param <ResourceUpdateActionT> extends ResourceUpdateAction (e.g {@link
   *     com.commercetools.api.models.customer.CustomerChangeEmailAction}
   * @param oldCustomFields the old resource's custom fields map of JSON values.
   * @param newCustomFields the new resources's custom fields map of JSON values.
   * @param resource the resource that the custom fields are on. It is used to identify the type of
   *     the resource, to call the corresponding update actions.
   * @param customActionBuilder the builder instance responsible for building the custom update
   *     actions.
   * @param variantId optional field representing the variant id in case the oldResource is an
   *     asset.
   * @param updateIdGetter a function used to get the id/key needed for updating the resource that
   *     has the custom fields.
   * @return a list that contains all the update actions needed, otherwise an empty list if no
   *     update actions are needed.
   */
  @Nonnull
  private static <
          CustomT extends Custom,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildRemovedCustomFieldsUpdateActions(
          @Nonnull final Map<String, Object> oldCustomFields,
          @Nonnull final Map<String, Object> newCustomFields,
          @Nonnull final CustomT resource,
          @Nonnull final GenericCustomActionBuilder<ResourceUpdateActionT> customActionBuilder,
          @Nullable final Long variantId,
          @Nonnull final Function<CustomT, String> updateIdGetter) {

    return oldCustomFields.keySet().stream()
        .filter(
            oldCustomFieldsName -> {
              // Convert Object to JSONNode
              final JsonNode newCustomFieldValue =
                  convertCustomValueObjDataToJsonNode(newCustomFields.get(oldCustomFieldsName));
              final JsonNode oldCustomFieldValue =
                  convertCustomValueObjDataToJsonNode(oldCustomFields.get(oldCustomFieldsName));
              return isNullJsonValue(newCustomFieldValue)
                  && oldCustomFieldValue != newCustomFieldValue;
            })
        .map(
            oldCustomFieldsName ->
                customActionBuilder.buildSetCustomFieldAction(
                    variantId, updateIdGetter.apply(resource), oldCustomFieldsName, null))
        .collect(Collectors.toList());
  }

  private CustomUpdateActionUtils() {}
}
