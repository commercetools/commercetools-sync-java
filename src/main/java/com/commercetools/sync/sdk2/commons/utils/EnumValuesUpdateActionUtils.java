package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toMap;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.WithKey;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The utils in this class are only meant for the internal use of the commercetools-sync-java
 * library.
 */
public final class EnumValuesUpdateActionUtils {

  /**
   * Compares a list of old {@code oldEnumValues} with a list of new {@code newEnumValues} for a
   * given definition. The method serves as an implementation for enum values syncing. The method
   * takes in functions for building the required update actions (AddEnumValue, ChangeEnumValueOrder
   * and 1-1 update actions on enum values (e.g. changeLabel) for the required resource.
   *
   * @param definitionName the definition name whose enum values are going to be synced.
   * @param oldEnumValues the old list of plain enum values.
   * @param newEnumValues the new list of plain enum values.
   * @param removeEnumCallback the function that is called in order to remove the new enum instance.
   * @param matchingEnumCallback the function that is called to get the update action resulting from
   *     comparing the enum value fields one by one.
   * @param addEnumCallback the function that is called in order to add the new enum instance.
   * @param changeOrderEnumCallback the function that is called to apply the change in the order.
   * @param changeOrderWithKeysEnumCallback the function that is called to apply the change in the
   *     order with keys.
   * @param <WithKeyT> the enum type of the elements to change the order for.
   * @param <ResourceUpdateActionT> the type of the resource in which the update actions will be
   *     applied on.
   * @return a list of enum values update actions if the list of plain enum values is not identical.
   *     Otherwise, if the plain enum values are identical, an empty list is returned.
   */
  @Nonnull
  public static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nullable final List<WithKeyT> newEnumValues,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT> removeEnumCallback,
          @Nullable
              final TriFunction<String, WithKeyT, WithKeyT, List<ResourceUpdateActionT>>
                  matchingEnumCallback,
          @Nullable final BiFunction<String, WithKeyT, ResourceUpdateActionT> addEnumCallback,
          @Nullable
              final BiFunction<String, List<WithKeyT>, ResourceUpdateActionT>
                  changeOrderEnumCallback,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  changeOrderWithKeysEnumCallback) {

    if (newEnumValues != null) {
      return buildUpdateActions(
          definitionName,
          oldEnumValues,
          newEnumValues,
          removeEnumCallback,
          matchingEnumCallback,
          addEnumCallback,
          changeOrderEnumCallback,
          changeOrderWithKeysEnumCallback);
    } else if (removeEnumCallback != null) {
      return buildRemoveEnumValuesUpdateAction(
              definitionName, oldEnumValues, null, removeEnumCallback)
          .map(Collections::singletonList)
          .orElse(emptyList());
    }

    return emptyList();
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildUpdateActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  removeEnumValuesUpdateActionCallback,
          @Nullable
              final TriFunction<String, WithKeyT, WithKeyT, List<ResourceUpdateActionT>>
                  matchingEnumCallback,
          @Nullable final BiFunction<String, WithKeyT, ResourceUpdateActionT> addEnumCallback,
          @Nullable
              final BiFunction<String, List<WithKeyT>, ResourceUpdateActionT>
                  changeOrderEnumCallback,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  changeOrderWithKeysEnumCallback) {

    final Optional<ResourceUpdateActionT> removeEnumValuesUpdateAction =
        getRemoveEnumValuesUpdateAction(
            definitionName, oldEnumValues, newEnumValues, removeEnumValuesUpdateActionCallback);

    final List<ResourceUpdateActionT> matchingEnumValuesUpdateActions =
        getMatchingEnumValuesUpdateActions(
            definitionName, oldEnumValues, newEnumValues, matchingEnumCallback);

    final List<ResourceUpdateActionT> addEnumValuesUpdateActions =
        getAddEnumValuesUpdateActions(
            definitionName, oldEnumValues, newEnumValues, addEnumCallback);

    final Optional<ResourceUpdateActionT> changeEnumValuesOrderUpdateAction =
        getChangeEnumValuesOrderUpdateAction(
            definitionName, oldEnumValues, newEnumValues, changeOrderEnumCallback);

    final Optional<ResourceUpdateActionT> changeEnumValuesWithKeysOrderUpdateActions =
        getChangeEnumValuesWithKeysOrderUpdateAction(
            definitionName, oldEnumValues, newEnumValues, changeOrderWithKeysEnumCallback);

    return Stream.of(
            removeEnumValuesUpdateAction.map(Collections::singletonList).orElse(emptyList()),
            matchingEnumValuesUpdateActions,
            addEnumValuesUpdateActions,
            changeEnumValuesOrderUpdateAction.map(Collections::singletonList).orElse(emptyList()),
            changeEnumValuesWithKeysOrderUpdateActions
                .map(Collections::singletonList)
                .orElse(emptyList()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> getChangeEnumValuesWithKeysOrderUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  changeOrderWithKeysEnumCallback) {

    return changeOrderWithKeysEnumCallback == null
        ? Optional.empty()
        : buildChangeEnumValuesWithKeysOrderUpdateAction(
            definitionName, oldEnumValues, newEnumValues, changeOrderWithKeysEnumCallback);
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> getChangeEnumValuesOrderUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable
              final BiFunction<String, List<WithKeyT>, ResourceUpdateActionT>
                  changeOrderEnumCallback) {

    return changeOrderEnumCallback == null
        ? Optional.empty()
        : buildChangeEnumValuesOrderUpdateAction(
            definitionName, oldEnumValues, newEnumValues, changeOrderEnumCallback);
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> getAddEnumValuesUpdateActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable final BiFunction<String, WithKeyT, ResourceUpdateActionT> addEnumCallback) {

    return addEnumCallback == null
        ? emptyList()
        : buildAddEnumValuesUpdateActions(
            definitionName, oldEnumValues, newEnumValues, addEnumCallback);
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> getMatchingEnumValuesUpdateActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable
              final TriFunction<String, WithKeyT, WithKeyT, List<ResourceUpdateActionT>>
                  matchingEnumCallback) {

    return matchingEnumCallback == null
        ? emptyList()
        : buildMatchingEnumValuesUpdateActions(
            definitionName, oldEnumValues, newEnumValues, matchingEnumCallback);
  }

  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> getRemoveEnumValuesUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nullable
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  removeEnumValuesUpdateActionCallback) {

    return removeEnumValuesUpdateActionCallback == null
        ? Optional.empty()
        : buildRemoveEnumValuesUpdateAction(
            definitionName, oldEnumValues, newEnumValues, removeEnumValuesUpdateActionCallback);
  }

  /**
   * Compares the order of a list of old enum values and a list of new enum values. If there is a
   * change in order, then a change of enum values order (with the new order) is built. If there are
   * no changes in order an empty optional is returned.
   *
   * @param definitionName the definition name whose enum values belong to.
   * @param oldEnumValues the list of old enum values.
   * @param newEnumValues the list of new enum values.
   * @param changeOrderEnumCallback the function that is called to apply the change in the order.
   * @param <WithKeyT> the enum type of the elements to change the order for.
   * @param <ResourceUpdateActionT> the type of the resource in which the update actions will be
   *     applied on.
   * @return an optional update action if the the order of the enum values is not identical.
   *     Otherwise, if the enum values order is identical, an empty optional is returned.
   */
  @Nonnull
  static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> buildChangeEnumValuesOrderUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nonnull
              final BiFunction<String, List<WithKeyT>, ResourceUpdateActionT>
                  changeOrderEnumCallback) {

    final Pair<List<String>, List<String>> keysPair =
        getAllKeysAndNewKeysPair(oldEnumValues, newEnumValues);
    return buildUpdateAction(
        keysPair.getLeft(), // all keys
        keysPair.getRight(), // new keys
        () -> changeOrderEnumCallback.apply(definitionName, newEnumValues));
  }

  /**
   * Compares the order of a list of old enum values and a list of new enum values. If there is a
   * change in order, then a change of enum values order (with the new order) is built. If there are
   * no changes in order an empty optional is returned.
   *
   * @param definitionName the definition name whose enum values belong to.
   * @param oldEnumValues the list of old enum values.
   * @param newEnumValues the list of new enum values.
   * @param changeOrderEnumCallback the function that is called to apply the change in the order.
   * @param <WithKeyT> the enum type of the elements to change the order for.
   * @return an optional update action if the the order of the enum values is not identical.
   *     Otherwise, if the enum values order is identical, an empty optional is returned.
   */
  @Nonnull
  private static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> buildChangeEnumValuesWithKeysOrderUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nonnull
              final BiFunction<String, List<String>, ResourceUpdateActionT>
                  changeOrderEnumCallback) {

    final Pair<List<String>, List<String>> keysPair =
        getAllKeysAndNewKeysPair(oldEnumValues, newEnumValues);
    return buildUpdateAction(
        keysPair.getLeft(), // all keys
        keysPair.getRight(), // new keys
        () -> changeOrderEnumCallback.apply(definitionName, keysPair.getRight()));
  }

  @Nonnull
  private static <WithKeyT extends WithKey>
      Pair<List<String>, List<String>> getAllKeysAndNewKeysPair(
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues) {

    final List<String> newKeys =
        newEnumValues.stream().map(WithKey::getKey).collect(Collectors.toList());

    final List<String> existingKeys =
        oldEnumValues.stream()
            .map(WithKey::getKey)
            .filter(newKeys::contains)
            .collect(Collectors.toList());

    final List<String> notExistingKeys =
        newKeys.stream()
            .filter(newKey -> !existingKeys.contains(newKey))
            .collect(Collectors.toList());

    final List<String> allKeys =
        Stream.concat(existingKeys.stream(), notExistingKeys.stream()).collect(Collectors.toList());

    return ImmutablePair.of(allKeys, newKeys);
  }

  /**
   * Checks if there are any new enum values which are not existing in the {@code oldEnumValues}. If
   * there are, then "add" enum values update actions are built. Otherwise, if there are no new enum
   * values, then an empty list is returned.
   *
   * @param definitionName the definition name whose enum values belong to.
   * @param oldEnumValues the list of olf enum values.
   * @param newEnumValues the list of new enum values.
   * @param addEnumCallback the function that is called in order to add the new enum instance.
   * @param <WithKeyT> the enum type of the element to add.
   * @param <ResourceUpdateActionT> the type of the resource in which the update actions will be
   *     applied on.
   * @return a list of enum values update actions if there are new enum value that should be added.
   *     Otherwise, if the enum values are identical, an empty optional is returned.
   */
  @Nonnull
  static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildAddEnumValuesUpdateActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nonnull final BiFunction<String, WithKeyT, ResourceUpdateActionT> addEnumCallback) {

    final Map<String, WithKeyT> oldEnumValuesKeyMap =
        getEnumValuesKeyMapWithKeyValidation(definitionName, oldEnumValues);

    final Map<String, WithKeyT> newEnumValuesKeyMap =
        getEnumValuesKeyMapWithKeyValidation(definitionName, newEnumValues);

    return newEnumValuesKeyMap.values().stream()
        .filter(newEnumValue -> !oldEnumValuesKeyMap.containsKey(newEnumValue.getKey()))
        .map(newEnumValue -> addEnumCallback.apply(definitionName, newEnumValue))
        .collect(Collectors.toList());
  }

  /**
   * Checks if there are any old enum values which are not existing in the {@code newEnumValues}. If
   * there are, then "remove" enum values update actions are built. Otherwise, if there are no old
   * enum values, then an empty list is returned.
   *
   * @param definitionName the definition name whose enum values belong to.
   * @param oldEnumValues the list of old enum values.
   * @param newEnumValues the list of new enum values.
   * @param removeEnumCallback the function that is called in order to remove the new enum instance.
   * @param <WithKeyT> the enum type of the elements of the list.
   * @param <ResourceUpdateActionT> the type of the resource in which the update actions will be
   *     applied on.
   * @return a list of enum values update actions if there are any old enum value that should be
   *     removed. Otherwise, if the enum values are identical, an empty optional is returned.
   */
  @Nonnull
  static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      Optional<ResourceUpdateActionT> buildRemoveEnumValuesUpdateAction(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nullable final List<WithKeyT> newEnumValues,
          @Nonnull
              final BiFunction<String, List<String>, ResourceUpdateActionT> removeEnumCallback) {

    final Map<String, WithKeyT> newEnumValuesKeyMap =
        getEnumValuesKeyMapWithKeyValidation(
            definitionName, ofNullable(newEnumValues).orElse(emptyList()));

    final List<String> keysToRemove =
        oldEnumValues.stream()
            .map(WithKey::getKey)
            .filter(oldEnumValueKey -> !newEnumValuesKeyMap.containsKey(oldEnumValueKey))
            .collect(Collectors.toList());

    return keysToRemove.isEmpty()
        ? empty()
        : of(removeEnumCallback.apply(definitionName, keysToRemove));
  }

  /**
   * Checks if there are any enum values which are existing in the {@code newEnumValues}. If there
   * are, then compare the enum value fields, and add the computed actions to the list of update
   * actions.
   *
   * @param definitionName the definition name whose enum values belong to.
   * @param oldEnumValues the list of old enum values.
   * @param newEnumValues the list of new enum values.
   * @param matchingEnumCallback the function that is called to get the update action resulting from
   *     comparing the enum value fields one by one.
   * @param <WithKeyT> the enum type of the elements of the list.
   * @param <ResourceUpdateActionT> the type of the resource in which the update actions will be
   *     applied on.
   * @return a list of enum update actions if there are enum values that are existing in the map of
   *     new enum values. If the enum value still exists, then compare the enum value fields
   *     (label), and add the computed actions to the list of update actions. Otherwise, if the enum
   *     values are identical, an empty optional is returned.
   */
  @Nonnull
  static <
          WithKeyT extends WithKey,
          ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>>
      List<ResourceUpdateActionT> buildMatchingEnumValuesUpdateActions(
          @Nonnull final String definitionName,
          @Nonnull final List<WithKeyT> oldEnumValues,
          @Nonnull final List<WithKeyT> newEnumValues,
          @Nonnull
              final TriFunction<String, WithKeyT, WithKeyT, List<ResourceUpdateActionT>>
                  matchingEnumCallback) {

    final Map<String, WithKeyT> newEnumValuesKeyMap =
        getEnumValuesKeyMapWithKeyValidation(definitionName, newEnumValues);

    return oldEnumValues.stream()
        .filter(oldEnumValue -> newEnumValuesKeyMap.containsKey(oldEnumValue.getKey()))
        .map(
            oldEnumValue ->
                matchingEnumCallback.apply(
                    definitionName, oldEnumValue, newEnumValuesKeyMap.get(oldEnumValue.getKey())))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /**
   * Given a list of new enum values, gets a map where the keys are the enum value key, and the
   * values are the enum instances.
   *
   * @param definitionName the definition name whose the enum values belong to.
   * @param enumValues the list of enum values.
   * @param <WithKeyT> the enum type of the elements of the list.
   * @return a map with the enum value key as a key of the map, and the enum value as a value of the
   *     map.
   */
  @Nonnull
  private static <WithKeyT extends WithKey>
      Map<String, WithKeyT> getEnumValuesKeyMapWithKeyValidation(
          @Nonnull final String definitionName, @Nonnull final List<WithKeyT> enumValues) {

    return enumValues.stream()
        .collect(
            toMap(
                WithKey::getKey,
                enumValue -> enumValue,
                (enumValueA, enumValueB) -> {
                  throw new DuplicateKeyException(
                      format(
                          "Enum Values have duplicated keys. "
                              + "Definition name: '%s', Duplicated enum value: '%s'. "
                              + "Enum Values are expected to be unique inside their definition.",
                          definitionName, enumValueA.getKey()));
                }));
  }

  private EnumValuesUpdateActionUtils() {}
}
