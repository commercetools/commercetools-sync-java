package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.sdk2.types.utils.FieldDefinitionUpdateActionUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldType;
import com.commercetools.api.models.type.TypeAddFieldDefinitionActionBuilder;
import com.commercetools.api.models.type.TypeChangeFieldDefinitionOrderActionBuilder;
import com.commercetools.api.models.type.TypeRemoveFieldDefinitionActionBuilder;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This class is only meant for the internal use of the commercetools-sync-java library. */
final class FieldDefinitionsUpdateActionUtils {

  /**
   * This method compares two lists of FieldDefinitions and performs actions for syncing and
   * updating the field definitions. It handles adding, removing, changing order, changing labels
   * etc. on the field definitions.
   *
   * <p>If the list of new {@link FieldDefinition}s is {@code null}, then remove actions are built
   * for every existing field definition in the {@code oldFieldDefinitions} list.
   *
   * <p>Note: Null field definitions are ignored and filtered out from {@code newFieldDefinitions}.
   *
   * @param oldFieldDefinitions the old list of field definitions.
   * @param newFieldDefinitions the new list of field definitions.
   * @return a list of field definitions update actions if the list of field definitions is not
   *     identical. Otherwise, if the field definitions are identical, an empty list is returned.
   * @throws BuildUpdateActionException in case there are field definitions with duplicate names or
   *     enums duplicate keys.
   */
  @Nonnull
  static List<TypeUpdateAction> buildFieldDefinitionsUpdateActions(
      @Nonnull final List<FieldDefinition> oldFieldDefinitions,
      @Nullable final List<FieldDefinition> newFieldDefinitions)
      throws BuildUpdateActionException {

    if (newFieldDefinitions != null) {
      return buildUpdateActions(
          oldFieldDefinitions,
          newFieldDefinitions.stream().filter(Objects::nonNull).collect(toList()));
    } else {
      return oldFieldDefinitions.stream()
          .map(FieldDefinition::getName)
          .map(name -> TypeRemoveFieldDefinitionActionBuilder.of().fieldName(name).build())
          .collect(Collectors.toList());
    }
  }

  /**
   * Compares a list of {@link FieldDefinition}s with a list of {@link FieldDefinition}s. The method
   * serves as an implementation for field definitions syncing and building the required update
   * actions (AddFieldDefinition, RemoveFieldDefinition, ChangeFieldDefinitionOrder) and 1-1 update
   * actions on field definitions (e.g. changeFieldDefinitionLabel, etc..) for the required
   * resource.
   *
   * @param oldFieldDefinitions the old list of field definitions.
   * @param newFieldDefinitions the new list of field definitions drafts.
   * @return a list of field definitions update actions if the list of field definitions is not
   *     identical. Otherwise, if the field definitions are identical, an empty list is returned.
   * @throws BuildUpdateActionException in case there are field definitions with duplicate names or
   *     enums duplicate keys.
   */
  @Nonnull
  private static List<TypeUpdateAction> buildUpdateActions(
      @Nonnull final List<FieldDefinition> oldFieldDefinitions,
      @Nonnull final List<FieldDefinition> newFieldDefinitions)
      throws BuildUpdateActionException {

    try {

      final List<TypeUpdateAction> updateActions =
          buildRemoveFieldDefinitionOrFieldDefinitionUpdateActions(
              oldFieldDefinitions, newFieldDefinitions);

      updateActions.addAll(
          buildAddFieldDefinitionUpdateActions(oldFieldDefinitions, newFieldDefinitions));

      buildChangeFieldDefinitionOrderUpdateAction(oldFieldDefinitions, newFieldDefinitions)
          .ifPresent(updateActions::add);

      return updateActions;

    } catch (final DuplicateNameException | DuplicateKeyException exception) {
      throw new BuildUpdateActionException(exception);
    }
  }

  /**
   * Checks if there are any field definitions which are not existing in the {@code
   * newFieldDefinitions}. If there are, then "remove" field definition update actions are built.
   * Otherwise, if the field definition still exists in the new field definition, then compare the
   * field definition fields (label, etc..), and add the computed actions to the list of update
   * actions.
   *
   * <p>Note: If the field type is different, the old field definition is removed and the new field
   * definition is added with the new field type.
   *
   * @param oldFieldDefinitions the list of old {@link FieldDefinition}s.
   * @param newFieldDefinitions the list of new {@link FieldDefinition}s.
   * @return a list of field definition update actions if there are field that are not existing in
   *     the new draft. If the field definition still exists in the new draft, then compare the
   *     field definition fields (name, label, etc..), and add the computed actions to the list of
   *     update actions. Otherwise, if the field definitions are identical, an empty optional is
   *     returned.
   * @throws DuplicateNameException in case there are field definitions drafts with duplicate names.
   * @throws DuplicateKeyException in case there are enum values with duplicate keys.
   */
  @Nonnull
  private static List<TypeUpdateAction> buildRemoveFieldDefinitionOrFieldDefinitionUpdateActions(
      @Nonnull final List<FieldDefinition> oldFieldDefinitions,
      @Nonnull final List<FieldDefinition> newFieldDefinitions) {

    final Map<String, FieldDefinition> newFieldDefinitionsNameMap =
        newFieldDefinitions.stream()
            .collect(
                toMap(
                    FieldDefinition::getName,
                    fieldDefinition -> fieldDefinition,
                    (fieldDefinitionA, fieldDefinitionB) -> {
                      throw new DuplicateNameException(
                          format(
                              "Field definitions have duplicated names. "
                                  + "Duplicated field definition name: '%s'. Field definitions names are "
                                  + "expected to be unique inside their type.",
                              fieldDefinitionA.getName()));
                    }));

    return oldFieldDefinitions.stream()
        .map(
            oldFieldDefinition -> {
              final String oldFieldDefinitionName = oldFieldDefinition.getName();
              final FieldDefinition matchingNewFieldDefinition =
                  newFieldDefinitionsNameMap.get(oldFieldDefinitionName);

              return ofNullable(matchingNewFieldDefinition)
                  .map(
                      newFieldDefinition -> {
                        if (newFieldDefinition.getType() != null) {
                          // field type is required so if null we let CTP to throw exception
                          if (haveSameFieldType(
                              oldFieldDefinition.getType(), newFieldDefinition.getType())) {
                            return buildActions(oldFieldDefinition, newFieldDefinition);
                          } else {
                            // since there is no way to change a field type on CTP,
                            // we remove the field definition and add a new one with a new field
                            // type
                            return Arrays.asList(
                                TypeRemoveFieldDefinitionActionBuilder.of()
                                    .fieldName(oldFieldDefinitionName)
                                    .build(),
                                TypeAddFieldDefinitionActionBuilder.of()
                                    .fieldDefinition(newFieldDefinition)
                                    .build());
                          }
                        } else {
                          return new ArrayList<TypeUpdateAction>();
                        }
                      })
                  .orElseGet(
                      () ->
                          singletonList(
                              TypeRemoveFieldDefinitionActionBuilder.of()
                                  .fieldName(oldFieldDefinitionName)
                                  .build()));
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /**
   * Compares the field types of the {@code fieldTypeA} and the {@code fieldTypeB}.
   *
   * @param fieldTypeA the first type to compare.
   * @param fieldTypeB the second field type to compare.
   * @return true if both field types equal, false otherwise.
   */
  private static boolean haveSameFieldType(
      @Nonnull final FieldType fieldTypeA, @Nonnull final FieldType fieldTypeB) {
    return fieldTypeA.getClass() == fieldTypeB.getClass();
  }

  /**
   * Compares the order of a list of old {@link FieldDefinition}s and a list of new {@link
   * FieldDefinition}s. If there is a change in order, then a change field definition order (with
   * the new order) is built. If there are no changes in order an empty optional is returned.
   *
   * @param oldFieldDefinitions the list of old {@link FieldDefinition}s
   * @param newFieldDefinitions the list of new {@link FieldDefinition}s
   * @return a list of field definition update actions if the order of field definitions is not
   *     identical. Otherwise, if the field definitions order is identical, an empty optional is
   *     returned.
   */
  @Nonnull
  private static Optional<TypeUpdateAction> buildChangeFieldDefinitionOrderUpdateAction(
      @Nonnull final List<FieldDefinition> oldFieldDefinitions,
      @Nonnull final List<FieldDefinition> newFieldDefinitions) {

    final List<String> newNames =
        newFieldDefinitions.stream().map(FieldDefinition::getName).collect(toList());

    final List<String> existingNames =
        oldFieldDefinitions.stream()
            .map(FieldDefinition::getName)
            .filter(newNames::contains)
            .collect(toList());

    final List<String> notExistingNames =
        newNames.stream().filter(newName -> !existingNames.contains(newName)).collect(toList());

    final List<String> allNames =
        Stream.concat(existingNames.stream(), notExistingNames.stream()).collect(toList());

    return buildUpdateAction(
        allNames,
        newNames,
        () -> TypeChangeFieldDefinitionOrderActionBuilder.of().fieldNames(newNames).build());
  }

  /**
   * Checks if there are any new field definition drafts which are not existing in the {@code
   * oldFieldDefinitions}. If there are, then "add" field definition update actions are built.
   * Otherwise, if there are no new field definitions, then an empty list is returned.
   *
   * @param oldFieldDefinitions the list of old {@link FieldDefinition}s.
   * @param newFieldDefinitions the list of new {@link FieldDefinition}s.
   * @return a list of field definition update actions if there are new field definition that should
   *     be added. Otherwise, if the field definitions are identical, an empty optional is returned.
   */
  @Nonnull
  private static List<TypeUpdateAction> buildAddFieldDefinitionUpdateActions(
      @Nonnull final List<FieldDefinition> oldFieldDefinitions,
      @Nonnull final List<FieldDefinition> newFieldDefinitions) {

    final Map<String, FieldDefinition> oldFieldDefinitionsNameMap =
        oldFieldDefinitions.stream()
            .collect(toMap(FieldDefinition::getName, fieldDefinition -> fieldDefinition));

    return newFieldDefinitions.stream()
        .filter(
            fieldDefinition -> !oldFieldDefinitionsNameMap.containsKey(fieldDefinition.getName()))
        .map(
            fieldDefinition ->
                FieldDefinitionBuilder.of()
                    .type(fieldDefinition.getType())
                    .name(fieldDefinition.getName())
                    .label(fieldDefinition.getLabel())
                    .required(fieldDefinition.getRequired())
                    .inputHint(fieldDefinition.getInputHint())
                    .build())
        .map(
            fieldDefinition ->
                TypeAddFieldDefinitionActionBuilder.of().fieldDefinition(fieldDefinition).build())
        .collect(Collectors.toList());
  }

  private FieldDefinitionsUpdateActionUtils() {}
}
