package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.AddFieldDefinition;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionOrder;
import io.sphere.sdk.types.commands.updateactions.RemoveFieldDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.types.utils.FieldDefinitionUpdateActionUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class TypeUpdateFieldDefinitionActionUtils {

    /**
     * Compares a list of {@link FieldDefinition}s with a list of {@link FieldDefinition}s.
     * The method serves as a generic implementation for field definitions syncing and building the required
     * update actions (AddFieldDefinition, RemoveFieldDefinition, ChangeFieldDefinitionOrder)
     * and 1-1 update actions on field definitions (e.g. changeFieldDefinitionLabel, etc..) for the required
     * resource.
     *
     * <p>If the list of new {@link FieldDefinition}s is {@code null}, then remove actions are built for
     * every existing field definition in the {@code oldFieldDefinitions} list.
     *
     * @param oldFieldDefinitions       the old list of field definitions.
     * @param newFieldDefinitions the new list of field definitions.
     * @return a list of field definitions update actions if the list of field definitions is not identical.
     *         Otherwise, if the field definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are field definitions drafts with duplicate names.
     */
    @Nonnull
    public static List<UpdateAction<Type>> buildFieldDefinitionsUpdateActions(
        @Nonnull final List<FieldDefinition> oldFieldDefinitions,
        @Nullable final List<FieldDefinition> newFieldDefinitions)
        throws BuildUpdateActionException {

        if (newFieldDefinitions != null) {
            return buildUpdateActions(
                oldFieldDefinitions,
                newFieldDefinitions
            );
        } else {
            return oldFieldDefinitions
                .stream()
                .map(FieldDefinition::getName)
                .map(RemoveFieldDefinition::of)
                .collect(Collectors.toList());
        }
    }

    /**
     * Compares a list of {@link FieldDefinition}s with a list of {@link FieldDefinition}s.
     * The method serves as an implementation for field definitions syncing and building the required
     * update actions (AddFieldDefinition, RemoveFieldDefinition, ChangeFieldDefinitionOrder)
     * and 1-1 update actions on field definitions (e.g. changeFieldDefinitionLabel, etc..) for the required
     * resource.
     *
     * @param oldFieldDefinitions       the old list of field definitions.
     * @param newFieldDefinitions the new list of field definitions drafts.
     * @return a list of field definitions update actions if the list of field definitions is not identical.
     *         Otherwise, if the field definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are field definitions drafts with duplicate names.
     */
    @Nonnull
    private static List<UpdateAction<Type>> buildUpdateActions(
        @Nonnull final List<FieldDefinition> oldFieldDefinitions,
        @Nonnull final List<FieldDefinition> newFieldDefinitions)
        throws BuildUpdateActionException {

        try {

            final List<UpdateAction<Type>> updateActions =
                buildRemoveFieldDefinitionOrFieldDefinitionUpdateActions(
                        oldFieldDefinitions,
                        newFieldDefinitions
                );

            updateActions.addAll(
                buildAddFieldDefinitionUpdateActions(
                    oldFieldDefinitions,
                    newFieldDefinitions
                )
            );

            buildChangeFieldDefinitionOrderUpdateAction(
                oldFieldDefinitions,
                newFieldDefinitions
            )
                .ifPresent(updateActions::add);

            return updateActions;

        } catch (final DuplicateNameException | DuplicateKeyException exception) {
            throw new BuildUpdateActionException(exception);
        }
    }

    /**
     * Checks if there are any field definitions which are not existing in the
     * {@code newFieldDefinitions}. If there are, then "remove" field definition update actions are
     * built.
     * Otherwise, if the field definition still exists in the new field definition, then compare the field definition
     * fields (label, etc..), and add the computed actions to the list of update actions.
     *
     * <p>Note: If the field type field changes, the old field definition is removed and the new field
     *     definition is added with the new field type.
     *
     * @param oldFieldDefinitions       the list of old {@link FieldDefinition}s.
     * @param newFieldDefinitions       the list of new {@link FieldDefinition}s.
     * @return a list of field definition update actions if there are field that are not existing
     *         in the new draft. If the field definition still exists in the new draft, then compare the field
     *         definition fields (name, label, etc..), and add the computed actions to the list of update actions.
     *         Otherwise, if the field definitions are identical, an empty optional is returned.
     */
    @Nonnull
    private static List<UpdateAction<Type>> buildRemoveFieldDefinitionOrFieldDefinitionUpdateActions(
            @Nonnull final List<FieldDefinition> oldFieldDefinitions,
            @Nonnull final List<FieldDefinition> newFieldDefinitions) {

        final Map<String, FieldDefinition> newFieldDefinitionsNameMap =
                newFieldDefinitions
                        .stream().collect(
                        toMap(FieldDefinition::getName, fieldDefinition -> fieldDefinition,
                                (fieldDefinitionA, fieldDefinitionB) -> {
                                    throw new DuplicateNameException(format("Field definitions have duplicated names. "
                                                    + "Duplicated field definition name: '%s'. "
                                                    + "Field definitions names are expected "
                                                    + "to be unique inside their type.",
                                            fieldDefinitionA.getName()));
                                }
                        ));

        return oldFieldDefinitions
                .stream()
                .map(oldFieldDefinition -> {
                    final String oldFieldDefinitionName = oldFieldDefinition.getName();
                    final FieldDefinition matchingNewFieldDefinition =
                            newFieldDefinitionsNameMap.get(oldFieldDefinitionName);

                    if (matchingNewFieldDefinition == null) {
                        return singletonList(RemoveFieldDefinition.of(oldFieldDefinitionName));
                    } else {
                        if (matchingNewFieldDefinition.getType() != null) {
                            // field type is required so if null we let commercetools to throw exception
                            if (haveSameFieldType(oldFieldDefinition, matchingNewFieldDefinition)) {
                                return buildActions(oldFieldDefinition, matchingNewFieldDefinition);
                            } else {
                                return Arrays.asList(
                                        RemoveFieldDefinition.of(oldFieldDefinitionName),
                                        AddFieldDefinition.of(matchingNewFieldDefinition)
                                );
                            }
                        } else {
                            return new ArrayList<UpdateAction<Type>>();
                        }
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Compares the field types of the {@code fieldDefinitionA} and the {@code fieldDefinitionB} and
     * returns true if both field definitions have the same field type, false otherwise.
     *
     * @param fieldDefinitionA the first field to compare.
     * @param fieldDefinitionB the second field definition to compare.
     * @return true if both field definitions have the same field type, false otherwise.
     */
    private static boolean haveSameFieldType(
        @Nonnull final FieldDefinition fieldDefinitionA,
        @Nonnull final FieldDefinition fieldDefinitionB) {

        return fieldDefinitionA.getType().getClass() == fieldDefinitionB.getType().getClass();
    }

    /**
     * Compares the order of a list of old {@link FieldDefinition}s and a list of new
     * {@link FieldDefinition}s.
     *  If there is a change in order, then a change field definition order
     * (with the new order) is built. If there are no changes in order an empty optional is returned.
     *
     * @param oldFieldDefinitions the list of old {@link FieldDefinition}s
     * @param newFieldDefinitions the list of new {@link FieldDefinition}s
     * @return a list of field definition update actions if the the order of field definitions is not
     *         identical. Otherwise, if the field definitions order is identical, an empty optional is returned.
     */
    @Nonnull
    private static Optional<UpdateAction<Type>> buildChangeFieldDefinitionOrderUpdateAction(
        @Nonnull final List<FieldDefinition> oldFieldDefinitions,
        @Nonnull final List<FieldDefinition> newFieldDefinitions) {

        final List<String> newNames = newFieldDefinitions
            .stream()
            .map(FieldDefinition::getName)
            .collect(toList());

        final List<String> existingNames = oldFieldDefinitions
            .stream()
            .map(FieldDefinition::getName)
            .filter(newNames::contains)
            .collect(toList());

        final List<String> notExistingNames = newNames
            .stream()
            .filter(newName -> !existingNames.contains(newName))
            .collect(toList());

        final List<String> newFieldDefinitionsOrderNames = newFieldDefinitions
            .stream()
            .map(FieldDefinition::getName)
            .collect(toList());

        final List<String> allNames = Stream.concat(existingNames.stream(), notExistingNames.stream())
                                            .collect(toList());

        return buildUpdateAction(
            allNames,
            newNames,
            () -> ChangeFieldDefinitionOrder.of(newFieldDefinitionsOrderNames)
        );
    }

    /**
     * Checks if there are any new field definition drafts which are not existing in the
     * {@code oldFieldDefinitionNameMap}. If there are, then "add" field definition update actions are built.
     * Otherwise, if there are no new field definitions, then an empty list is returned.
     *
     * @param oldFieldDefinitions the list of old {@link FieldDefinition}s
     * @param newFieldDefinitions the list of new {@link FieldDefinition}s
     *
     * @return a list of field definition update actions if there are new field definition that should be added.
     *         Otherwise, if the field definitions are identical, an empty optional is returned.
     */
    @Nonnull
    private static List<UpdateAction<Type>> buildAddFieldDefinitionUpdateActions(
        @Nonnull final List<FieldDefinition> oldFieldDefinitions,
        @Nonnull final List<FieldDefinition> newFieldDefinitions) {

        final Map<String, FieldDefinition> oldFieldDefinitionsNameMap =
                oldFieldDefinitions
                        .stream()
                        .collect(toMap(FieldDefinition::getName, fieldDefinition -> fieldDefinition));

        return newFieldDefinitions
            .stream()
            .filter(fieldDefinition -> !oldFieldDefinitionsNameMap.containsKey(fieldDefinition.getName()))
            .map(fieldDefinition -> FieldDefinition.of(fieldDefinition.getType(),
                    fieldDefinition.getName(),
                    fieldDefinition.getLabel(),
                    fieldDefinition.isRequired(),
                    fieldDefinition.getInputHint()))
            .map(AddFieldDefinition::of)
            .collect(Collectors.toList());
    }

    private TypeUpdateFieldDefinitionActionUtils() {
    }
}
