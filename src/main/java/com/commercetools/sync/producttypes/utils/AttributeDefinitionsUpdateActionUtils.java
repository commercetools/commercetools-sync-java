package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.producttypes.helpers.AttributeDefinitionCustomBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionHelper.ensureAttributeDefinitionDraftIsValid;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionHelper.ensureAttributeDefinitionIsValid;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildActions;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class AttributeDefinitionsUpdateActionUtils {

    /**
     * Compares a list of {@link AttributeDefinition}s with a list of {@link AttributeDefinitionDraft}s to
     * returns a {@link List} of {@link UpdateAction}&lt;{@link ProductType}&gt;.If both lists have identical
     * AttributeDefinitions, then no update actions are needed and hence an empty {@link List} is returned.
     *
     * <p>If the list of new {@link AttributeDefinitionDraft}s is {@code null}, then remove actions are built for
     * every existing attribute definition in the {@code oldAttributeDefinitions} list.
     *
     * @param oldAttributeDefinitions       the old list of attribute definitions.
     * @param newAttributeDefinitionsDrafts the new list of attribute definitions drafts.
     * @return a list of attribute definitions update actions if the list of attribute definitions are not identical.
     *         Otherwise, if the attribute definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are attribute definitions drafts with duplicate names or
     *         there are attribute definitions with the null attribute type.
     */
    @Nonnull
    public static List<UpdateAction<ProductType>> buildAttributeDefinitionsUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nullable final List<AttributeDefinitionDraft> newAttributeDefinitionsDrafts)
        throws BuildUpdateActionException {

        if (newAttributeDefinitionsDrafts != null) {
            return buildUpdateActions(
                oldAttributeDefinitions,
                newAttributeDefinitionsDrafts
            );
        } else {
            return oldAttributeDefinitions
                .stream()
                .map(AttributeDefinition::getName)
                .map(RemoveAttributeDefinition::of)
                .collect(Collectors.toList());
        }
    }

    /**
     * Compares a list of {@link AttributeDefinition}s with a list of {@link AttributeDefinitionDraft}s.
     * The method serves as an implementation for attribute definitions syncing. The method takes in functions
     * for building the required update actions (AddAttribute, RemoveAttribute, ChangeAttributeOrder and 1-1
     * update actions on attribute definitions (e.g. changeAttributeName, changeAttributeLabel, etc..) for the required
     * resource.
     *
     * @param oldAttributeDefinitions       the old list of attribute definitions.
     * @param newAttributeDefinitionsDrafts the new list of attribute definitions drafts.
     * @return a list of attribute definitions update actions if the list of attribute definitions is not identical.
     *         Otherwise, if the attribute definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are attribute definitions drafts with duplicate names or
     *         there are attribute definitions with the null attribute type.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionsDrafts)
        throws BuildUpdateActionException {

        try {
            final List<UpdateAction<ProductType>> updateActions =
                buildRemoveAttributeDefinitionOrAttributeDefinitionUpdateActions(
                    oldAttributeDefinitions,
                    newAttributeDefinitionsDrafts
                );

            updateActions.addAll(
                buildAddAttributeDefinitionUpdateActions(
                    oldAttributeDefinitions,
                    newAttributeDefinitionsDrafts
                )
            );

            buildChangeAttributeDefinitionOrderUpdateAction(
                oldAttributeDefinitions,
                newAttributeDefinitionsDrafts
            )
                .ifPresent(updateActions::add);

            return updateActions;

        } catch (final DuplicateNameException | DuplicateKeyException exception) {
            throw new BuildUpdateActionException(exception);
        }
    }

    /**
     * Checks if there are any attribute definitions which are not existing in the
     * {@code newAttributeDefinitionsDrafts}. If there are, then "remove" attribute definition update actions are
     * built.
     * Otherwise, if the attribute definition still exists in the new draft, then compare the attribute definition
     * fields (name, label, etc..), and add the computed actions to the list of update actions.
     *
     * <p>Note: If the attribute type field changes, the old attribute definition is removed and the new attribute
     *     definition is added with the new attribute type.
     *
     * @param oldAttributeDefinitions             the list of old {@link AttributeDefinition}s.
     * @param newAttributeDefinitionsDrafts       the list of new {@link AttributeDefinitionDraft}s.
     * @return a list of attribute definition update actions if there are attribute definitions that are not existing
     *         in the new draft. If the attribute definition still exists in the new draft, then compare the attribute
     *         definition fields (name, label, etc..), and add the computed actions to the list of update actions.
     *         Otherwise, if the attribute definitions are identical, an empty optional is returned.
     * @throws BuildUpdateActionException in case there are attribute definitions drafts with duplicate names or
     *         there are attribute definitions with the null attribute type.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildRemoveAttributeDefinitionOrAttributeDefinitionUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionsDrafts) throws BuildUpdateActionException {

        final Map<String, AttributeDefinitionDraft> newAttributesDefinitionsDraftsNameMap =
            newAttributeDefinitionsDrafts
                .stream().collect(
                toMap(AttributeDefinitionDraft::getName, attributeDefinitionDraft -> attributeDefinitionDraft,
                    (attributeDefinitionDraftA, attributeDefinitionDraftB) -> {
                        throw new DuplicateNameException(format("Attribute definitions drafts have duplicated names. "
                                + "Duplicated attribute definition name: '%s'. "
                                + "Attribute definitions names are expected to be unique inside their product type.",
                            attributeDefinitionDraftA.getName()));
                    }
                ));

        final List<UpdateAction<ProductType>> updateActions = new ArrayList<>();

        for (AttributeDefinition oldAttributeDefinition : oldAttributeDefinitions) {

            ensureAttributeDefinitionIsValid(oldAttributeDefinition);

            final String oldAttributeDefinitionName = oldAttributeDefinition.getName();
            final AttributeDefinitionDraft matchingNewAttributeDefinitionDraft =
                    newAttributesDefinitionsDraftsNameMap.get(oldAttributeDefinitionName);

            if (matchingNewAttributeDefinitionDraft != null) {

                ensureAttributeDefinitionDraftIsValid(matchingNewAttributeDefinitionDraft);

                if (haveSameAttributeType(oldAttributeDefinition.getAttributeType(),
                        matchingNewAttributeDefinitionDraft.getAttributeType())) {
                    updateActions.addAll(buildActions(oldAttributeDefinition, matchingNewAttributeDefinitionDraft));
                } else {
                    // since there is no way to change an attribute type on CTP,
                    // we remove the attribute definition and add a new one with a new attribute type
                    updateActions.add(RemoveAttributeDefinition.of(oldAttributeDefinitionName));
                    updateActions.add(AddAttributeDefinition.of(matchingNewAttributeDefinitionDraft));
                }
            } else {
                updateActions.add(RemoveAttributeDefinition.of(oldAttributeDefinitionName));
            }
        }

        return updateActions;
    }

    /**
     * Compares the attribute types of the {@code attributeDefinitionA} and the {@code attributeDefinitionB} and
     * returns true if both attribute definitions have the same attribute type, false otherwise.
     *
     * @param attributeTypeA the first attribute type to compare.
     * @param attributeTypeB the second attribute type to compare.
     * @return true if both attribute definitions have the same attribute type, false otherwise.
     */
    private static boolean haveSameAttributeType(
        @Nonnull final AttributeType attributeTypeA,
        @Nonnull final AttributeType attributeTypeB) {

        return attributeTypeA.getClass() == attributeTypeB.getClass();
    }

    /**
     * Compares the order of a list of old {@link AttributeDefinition}s and a list of new
     * {@link AttributeDefinitionDraft}s. If there is a change in order, then a change attribute definition order
     * (with the new order) is built. If there are no changes in order an empty optional is returned.
     *
     * @param oldAttributeDefinitions      the list of old {@link AttributeDefinition}s
     * @param newAttributeDefinitionDrafts the list of new {@link AttributeDefinitionDraft}s
     * @return a list of attribute definition update actions if the order of attribute definitions is not
     *         identical. Otherwise, if the attribute definitions order is identical, an empty optional is returned.
     */
    @Nonnull
    private static Optional<UpdateAction<ProductType>> buildChangeAttributeDefinitionOrderUpdateAction(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionDrafts) {


        final List<String> newNames = newAttributeDefinitionDrafts
            .stream()
            .map(AttributeDefinitionDraft::getName)
            .collect(toList());

        final List<String> existingNames = oldAttributeDefinitions
            .stream()
            .map(AttributeDefinition::getName)
            .filter(newNames::contains)
            .collect(toList());

        final List<String> notExistingNames = newNames
            .stream()
            .filter(newName -> !existingNames.contains(newName))
            .collect(toList());

        final List<AttributeDefinition> newAttributeDefinitionsOrder = newAttributeDefinitionDrafts
            .stream()
            .map(AttributeDefinitionCustomBuilder::of)
            .collect(toList());

        final List<String> allNames = Stream.concat(existingNames.stream(), notExistingNames.stream())
                                            .collect(toList());

        return buildUpdateAction(
            allNames,
            newNames,
            () -> ChangeAttributeOrder.of(newAttributeDefinitionsOrder)
        );


    }

    /**
     * Checks if there are any new attribute definition drafts which are not existing in the
     * {@code oldAttributeDefinitionNameMap}. If there are, then "add" attribute definition update actions are built.
     * Otherwise, if there are no new attribute definitions, then an empty list
     * is returned.
     *
     * @param oldAttributeDefinitions      the list of old {@link AttributeDefinition}s.
     * @param newAttributeDefinitionDrafts the list of new {@link AttributeDefinitionDraft}s.
     *
     * @return a list of attribute definition update actions if there are new attribute definition that should be added.
     *         Otherwise, if the attribute definitions are identical, an empty optional is returned.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildAddAttributeDefinitionUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionDrafts) {

        final Map<String, AttributeDefinition> oldAttributeDefinitionNameMap =
                oldAttributeDefinitions
                        .stream()
                        .collect(toMap(AttributeDefinition::getName, attributeDefinition -> attributeDefinition));

        return newAttributeDefinitionDrafts
            .stream()
            .filter(attributeDefinitionDraft -> !oldAttributeDefinitionNameMap
                .containsKey(attributeDefinitionDraft.getName())
            )
            .map(AddAttributeDefinition::of)
            .collect(Collectors.toList());
    }

    private AttributeDefinitionsUpdateActionUtils() {
    }
}
