package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomDraft;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class CustomUpdateActionUtils {
    private static final String CUSTOM_TYPE_IDS_NOT_SET = "Custom type ids are not set for both the old and new %s.";
    private static final String CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED = "Failed to build custom fields update "
        + "actions on the %s with id '%s'. Reason: %s";
    private static final String CUSTOM_TYPE_ID_IS_BLANK = "New resource's custom type id is blank (empty/null).";

    /**
     * This method is a syntactic sugar for the method {@link #buildCustomUpdateActions(Custom, CustomDraft,
     * GenericCustomActionBuilder, Integer, Function, Function, Function, BaseSyncOptions)}, but this one is only for
     * primary resources (i.e resources which have their own endpoints for example channels, categories, inventory
     * entries. For more details of the inner logic and different scenarios, check the Javadoc of the other method.
     *
     * @param <T>                 the type of the old {@link Resource} which has the custom fields.
     * @param <S>                 the type of the new resource {@link CustomDraft}.
     * @param oldResource         the resource which should be updated.
     * @param newResource         the resource draft where we get the new custom fields.
     * @param customActionBuilder the builder instance responsible for building the custom update actions.
     * @param syncOptions         responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     * @see #buildCustomUpdateActions(Custom, CustomDraft, GenericCustomActionBuilder, Integer, Function, Function,
     *      Function, BaseSyncOptions)  )
     */
    @Nonnull
    public static <T extends Custom & Resource<T>, S extends CustomDraft> List<UpdateAction<T>>
        buildPrimaryResourceCustomUpdateActions(
        @Nonnull final T oldResource,
        @Nonnull final S newResource,
        @Nonnull final GenericCustomActionBuilder<T> customActionBuilder,
        @Nonnull final BaseSyncOptions syncOptions) {

        return buildCustomUpdateActions(oldResource, newResource, customActionBuilder, null,
            resource -> resource.getId(),
            resource -> resource.toReference().getTypeId(),
            resource -> null, // No update ID needed for primary resources.
            syncOptions);
    }


    /**
     * Compares the {@link CustomFields} of an old resource {@code T} (for example {@link Category},
     * {@link io.sphere.sdk.products.Product}, etc..), to the {@link CustomFieldsDraft}, of a new
     * resource draft {@code S} (for example {@link CategoryDraft}, {@link io.sphere.sdk.products.ProductVariantDraft},
     * etc..), and returns a {@link List}&lt;{@link UpdateAction}&gt; as a result. If no update action is needed,
     * for example in the case where both the {@link CustomFields} and the {@link CustomFieldsDraft} are null, an empty
     * {@link List}&lt;{@link UpdateAction}&gt; is returned. A {@link BaseSyncOptions} instance is injected into the
     * method which is responsible for supplying the sync options to the sync utility method. For example, custom error
     * callbacks for errors. The {@link TypeService} is injected also for fetching the key of the old resource type
     * from it's cache (see {@link CustomUpdateActionUtils#buildNonNullCustomFieldsUpdateActions(CustomFields,
     * CustomFieldsDraft, Custom, GenericCustomActionBuilder, Integer, Function, Function, Function, BaseSyncOptions)}).
     *
     * <p>An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>If the new resources's custom type is set, but old resources's custom type is not. A "setCustomType" update
     * actions is added, which sets the custom type (and all it's fields to the old resource).</li>
     * <li>If the new resource's custom type is not set, but the old resource's custom type is set. A
     * "setCustomType" update action is added, which removes the type set on the old resource.</li>
     * <li>If both the resources custom types are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see
     * {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom, GenericCustomActionBuilder,
     * Integer, Function)} )})</li>
     * <li>If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.</li>
     * <li>If both resources custom type keys are identical but the custom fields of the new resource's custom type is
     * not set.</li>
     * </ol>
     *
     * <p>An update action will <b>not</b> be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources' custom types are not set.</li>
     * <li>If both the resources' custom type keys are not set.</li>
     * <li>Custom fields are both empty.</li>
     * <li>Custom field JSON values have different ordering.</li>
     * <li>Custom field values are identical.</li>
     * </ol>
     *
     * @param <T> the type of the old {@link Resource} which has the custom fields.
     * @param <S> the type of the new resource {@link CustomDraft}.
     * @param <U> the type of the resource in which the update actions will be applied on.
     *
     * @param oldResource the resource which should be updated.
     * @param newResource the resource draft where we get the new custom fields.
     * @param customActionBuilder the builder instance responsible for building the custom update actions.
     * @param variantId optional field representing the variant id in case the oldResource is an asset.
     * @param resourceIdGetter a function used to get the id of the resource being updated.
     * @param resourceTypeIdGetter a function used to get the Type id of the resource being updated.
     * @param updateIdGetter a function used to get the id/key needed for updating the resource that has the custom
     *                       fields.
     * @param syncOptions responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an
     *      empty list if no update actions are needed.
     */
    @Nonnull
    public static <T extends Custom, S extends CustomDraft, U extends Resource<U>> List<UpdateAction<U>>
        buildCustomUpdateActions(
        @Nonnull final T oldResource,
        @Nonnull final S newResource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> resourceIdGetter,
        @Nonnull final Function<T, String> resourceTypeIdGetter,
        @Nonnull final Function<T, String> updateIdGetter,
        @Nonnull final BaseSyncOptions syncOptions) {

        final CustomFields oldResourceCustomFields = oldResource.getCustom();
        final CustomFieldsDraft newResourceCustomFields = newResource.getCustom();
        if (oldResourceCustomFields != null && newResourceCustomFields != null) {
            try {
                return buildNonNullCustomFieldsUpdateActions(oldResourceCustomFields, newResourceCustomFields,
                    oldResource, customActionBuilder, variantId, resourceIdGetter, resourceTypeIdGetter,
                    updateIdGetter, syncOptions);
            } catch (BuildUpdateActionException exception) {
                final String errorMessage = format(CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED,
                    resourceTypeIdGetter.apply(oldResource),
                    resourceIdGetter.apply(oldResource), exception.getMessage());
                syncOptions.applyErrorCallback(errorMessage, exception);
            }
        } else {
            if (oldResourceCustomFields == null) {
                if (newResourceCustomFields != null) {
                    // New resource's custom fields are set, but old resources's custom fields are not set. So we
                    // should set the custom type and fields of the new resource to the old one.
                    final String newCustomFieldsTypeId = newResourceCustomFields.getType().getId();
                    if (isBlank(newCustomFieldsTypeId)) {
                        final String errorMessage = format(CUSTOM_FIELDS_UPDATE_ACTIONS_BUILD_FAILED,
                            resourceTypeIdGetter.apply(oldResource), resourceIdGetter.apply(oldResource),
                            CUSTOM_TYPE_ID_IS_BLANK);
                        syncOptions.applyErrorCallback(errorMessage);
                    } else {
                        final Map<String, JsonNode> newCustomFieldsJsonMap = newResourceCustomFields.getFields();
                        final Optional<UpdateAction<U>> updateAction = buildTypedSetCustomTypeUpdateAction(
                            newCustomFieldsTypeId, newCustomFieldsJsonMap, oldResource, customActionBuilder,
                            variantId, resourceIdGetter, resourceTypeIdGetter, updateIdGetter, syncOptions);
                        return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
                    }
                }
            } else {
                // New resource's custom fields are not set, but old resource's custom fields are set. So we
                // should remove the custom type from the old resource.

                return singletonList(
                    customActionBuilder.buildRemoveCustomTypeAction(variantId, updateIdGetter.apply(oldResource)));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Compares a non null {@link CustomFields} to a non null {@link CustomFieldsDraft} and returns a
     * {@link List&lt;UpdateAction&gt;} as a result. The keys are used to compare the custom types. The key of the old
     * resource custom type is fetched from the caching mechanism of the {@link TypeService} instance supplied as
     * a param to the method. The key of the new resource custom type is expected to be set on the type.
     * If no update action is needed an empty {@link List&lt;UpdateAction&gt;} is returned.
     *
     * <p>An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources custom type keys are the same and the custom fields are both set. The custom
     * field values of both resources are then calculated. (see
     * {@link CustomUpdateActionUtils#buildSetCustomFieldsUpdateActions(Map, Map, Custom, GenericCustomActionBuilder,
     * Integer, Function)})
     * </li>
     * <li>If the keys of both custom types are different, then a "setCustomType" update action is added, where the
     * old resource's custom type is set to be as the new one's.</li>
     * <li>If both resources custom type keys are identical but the custom fields
     * of the new resource's custom type is not set.</li>
     * </ol>
     *
     * <p>An update action will <bold>not</bold> be added to the result list in the following cases:-
     * <ol>
     * <li>If both the resources' custom type keys are not set.</li>
     * </ol>
     *
     * @param <T>                    the type of the old {@link Resource} which has the custom fields.
     * @param <U>                    the type of the resource in which the update actions will be applied on.
     * @param oldCustomFields        the old resource's custom fields.
     * @param newCustomFields        the new resource draft's custom fields.
     * @param resource               the resource that the custom fields are on. It is used to identify the type of the
     *                               resource, to call the corresponding update actions.
     * @param customActionBuilder    the builder instance responsible for building the custom update actions.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param resourceIdGetter       a function used to get the id of the resource being updated.
     * @param resourceTypeIdGetter   a function used to get the Type id of the resource being updated.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @param syncOptions            responsible for supplying the sync options to the sync utility method.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update
     *         actions are needed.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> List<UpdateAction<U>> buildNonNullCustomFieldsUpdateActions(
        @Nonnull final CustomFields oldCustomFields,
        @Nonnull final CustomFieldsDraft newCustomFields,
        @Nonnull final T resource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> resourceIdGetter,
        @Nonnull final Function<T, String> resourceTypeIdGetter,
        @Nonnull final Function<T, String> updateIdGetter,
        @Nonnull final BaseSyncOptions syncOptions) throws BuildUpdateActionException {

        final String oldCustomTypeId = oldCustomFields.getType().getId();
        final Map<String, JsonNode> oldCustomFieldsJsonMap = oldCustomFields.getFieldsJsonMap();
        final String newCustomTypeId = newCustomFields.getType().getId();
        final Map<String, JsonNode> newCustomFieldsJsonMap = newCustomFields.getFields();

        if (Objects.equals(oldCustomTypeId, newCustomTypeId)) {
            if (isBlank(oldCustomTypeId)) {
                throw new BuildUpdateActionException(
                    format(CUSTOM_TYPE_IDS_NOT_SET, resourceTypeIdGetter.apply(resource)));
            }
            if (newCustomFieldsJsonMap == null) {
                // New resource's custom fields are null/not set. So we should unset old custom fields.
                final Optional<UpdateAction<U>> updateAction = buildTypedSetCustomTypeUpdateAction(newCustomTypeId,
                    null, resource, customActionBuilder, variantId, resourceIdGetter,
                    resourceTypeIdGetter, updateIdGetter, syncOptions);

                return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
            }
            // old and new resource's custom fields are set. So we should calculate update actions for the
            // the fields of both.
            return buildSetCustomFieldsUpdateActions(oldCustomFieldsJsonMap, newCustomFieldsJsonMap, resource,
                customActionBuilder, variantId, updateIdGetter);
        } else {
            final Optional<UpdateAction<U>> updateAction = buildTypedSetCustomTypeUpdateAction(newCustomTypeId,
                newCustomFieldsJsonMap, resource, customActionBuilder, variantId, resourceIdGetter,
                resourceTypeIdGetter, updateIdGetter, syncOptions);
            return updateAction.map(Collections::singletonList).orElseGet(Collections::emptyList);
        }
    }

    /**
     * Compares two {@link Map} objects representing a map of the custom field name to the JSON representation
     * of the value of the corresponding custom field. It returns a {@link List&lt;UpdateAction&gt;} as a result.
     * If no update action is needed an empty {@link List&lt;UpdateAction&gt;} is returned.
     *
     * <p>An update action will be added to the result list in the following cases:-
     * <ol>
     * <li>A custom field value is changed.</li>
     * <li>A custom field value is removed.</li>
     * <li>A new custom field value is added.</li>
     * </ol>
     *
     * <p>An update action will <bold>not</bold> be added to the result list in the following cases:-
     * <ol>
     * <li>Custom fields are both empty.</li>
     * <li>Custom field JSON values have different ordering.</li>
     * <li>Custom field values are identical.</li>
     * </ol>
     *
     * @param <T>                    the type of the old {@link Resource} which has the custom fields.
     * @param <U>                    the type of the resource in which the update actions will be applied on.
     * @param oldCustomFields        the old resource's custom fields map of JSON values.
     * @param newCustomFields        the new resource's custom fields map of JSON values.
     * @param resource               the resource that the custom fields are on. It is used to identify the type of
     *                               the resource, to call the corresponding update actions.
     * @param customActionBuilder    the builder instance responsible for building the custom update actions.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @return a list that contains all the update actions needed, otherwise an empty list if no
     *         update actions are needed.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> List<UpdateAction<U>> buildSetCustomFieldsUpdateActions(
        @Nonnull final Map<String, JsonNode> oldCustomFields,
        @Nonnull final Map<String, JsonNode> newCustomFields,
        @Nonnull final T resource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> updateIdGetter) {

        final List<UpdateAction<U>> customFieldsUpdateActions =
            buildNewOrModifiedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource,
                customActionBuilder, variantId, updateIdGetter);

        final List<UpdateAction<U>> removedCustomFieldsActions =
            buildRemovedCustomFieldsUpdateActions(oldCustomFields, newCustomFields, resource,
                customActionBuilder, variantId, updateIdGetter);

        customFieldsUpdateActions.addAll(removedCustomFieldsActions);
        return customFieldsUpdateActions;
    }

    /**
     * Traverses the new resource's custom fields map of JSON values {@link Map} to create
     * "setCustomField" update actions that represent either the modification of an existent custom field's
     * value or the addition of a new one. It returns a {@link List&lt;UpdateAction&gt;} as a result.
     * If no update action is needed an empty {@link List&lt;UpdateAction&gt;} is returned.
     *
     * @param <T>                    the type of the old {@link Resource} which has the custom fields.
     * @param <U>                    the type of the resource in which the update actions will be applied on.
     * @param oldCustomFields        the old resource's custom fields map of JSON values.
     * @param newCustomFields        the new resource's custom fields map of JSON values.
     * @param resource               the resource that the custom fields are on. It is used to identify the
     *                               type of the resource, to call the corresponding update actions.
     * @param customActionBuilder    the builder instance responsible for building the custom update actions.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> List<UpdateAction<U>> buildNewOrModifiedCustomFieldsUpdateActions(
        @Nonnull final Map<String, JsonNode> oldCustomFields,
        @Nonnull final Map<String, JsonNode> newCustomFields,
        @Nonnull final T resource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> updateIdGetter) {

        return newCustomFields.keySet().stream()
                              .filter(newCustomFieldName -> !Objects.equals(newCustomFields.get(newCustomFieldName),
                                  oldCustomFields.get(newCustomFieldName)))
                              .map(newCustomFieldName -> customActionBuilder.buildSetCustomFieldAction(variantId,
                                  updateIdGetter.apply(resource), newCustomFieldName,
                                  newCustomFields.get(newCustomFieldName)))
                              .collect(Collectors.toList());
    }

    /**
     * Traverses the old resource's custom fields map of JSON values {@link Map} to create update
     * "setCustomField" update actions that represent removal of an existent custom field from the new resource's custom
     * fields. It returns a {@link List&lt;UpdateAction&gt;} as a result.
     * If no update action is needed an empty {@link List&lt;UpdateAction&gt;} is returned.
     *
     * @param <T>                    the type of the old {@link Resource} which has the custom fields.
     * @param <U>                    the type of the resource in which the update actions will be applied on.
     * @param oldCustomFields        the old resource's custom fields map of JSON values.
     * @param newCustomFields        the new resources's custom fields map of JSON values.
     * @param resource               the resource that the custom fields are on. It is used to identify the type of
     *                               the resource, to call the corresponding update actions.
     * @param customActionBuilder    the builder instance responsible for building the custom update actions.
     * @param variantId              optional field representing the variant id in case the oldResource is an asset.
     * @param updateIdGetter         a function used to get the id/key needed for updating the resource that has the
     *                               custom fields.
     * @return a list that contains all the update actions needed, otherwise an empty list if no update actions are
     *         needed.
     */
    @Nonnull
    static <T extends Custom, U extends Resource<U>> List<UpdateAction<U>> buildRemovedCustomFieldsUpdateActions(
        @Nonnull final Map<String, JsonNode> oldCustomFields,
        @Nonnull final Map<String, JsonNode> newCustomFields,
        @Nonnull final T resource,
        @Nonnull final GenericCustomActionBuilder<U> customActionBuilder,
        @Nullable final Integer variantId,
        @Nonnull final Function<T, String> updateIdGetter) {

        return oldCustomFields.keySet().stream()
                              .filter(oldCustomFieldsName -> Objects.isNull(newCustomFields.get(oldCustomFieldsName)))
                              .map(oldCustomFieldsName -> customActionBuilder.buildSetCustomFieldAction(variantId,
                                  updateIdGetter.apply(resource), oldCustomFieldsName, null))
                              .collect(Collectors.toList());
    }

    private CustomUpdateActionUtils() {
    }
}

