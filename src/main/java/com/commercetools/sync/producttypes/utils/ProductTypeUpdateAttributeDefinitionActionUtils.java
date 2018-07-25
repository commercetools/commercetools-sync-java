package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateNameException;
import com.commercetools.sync.producttypes.helpers.AttributeDefinitionCustomBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeAttributeOrder;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.producttypes.utils.AttributeDefinitionUpdateActionUtils.buildActions;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class ProductTypeUpdateAttributeDefinitionActionUtils {
    /**
     * Compares a list of {@link AttributeDefinition}s with a list of {@link AttributeDefinitionDraft}s.
     * The method serves as a generic implementation for attribute definitions syncing. The method takes in functions
     * for building the required update actions (AddAttribute, RemoveAttribute, ChangeAttributeOrder and 1-1
     * update actions on attribute definitions (e.g. changeAttributeName, changeAttributeLabel, etc..) for the required
     * resource.
     *
     * <p>If the list of new {@link AttributeDefinitionDraft}s is {@code null}, then remove actions are built for
     * every existing attribute definition in the {@code oldAttributeDefinitions} list.
     *
     * @param oldAttributeDefinitions                       the old list of attribute definitions.
     * @param newAttributeDefinitionsDrafts                 the new list of attribute definitions drafts.
     *
     * @return a list of attribute definitions update actions if the list of attribute definitions is not identical.
     *         Otherwise, if the attribute definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are attribute definitions drafts with duplicate names.
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
     * @param oldAttributeDefinitions                       the old list of attribute definitions.
     * @param newAttributeDefinitionsDrafts                 the new list of attribute definitions drafts.
     *
     * @return a list of attribute definitions update actions if the list of attribute definitions is not identical.
     *         Otherwise, if the attribute definitions are identical, an empty list is returned.
     * @throws BuildUpdateActionException in case there are attribute definitions drafts with duplicate names.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionsDrafts)
        throws BuildUpdateActionException {

        final HashSet<String> removedAttributesDefinitionsNames = new HashSet<>();

        final Map<String, AttributeDefinition> oldAttributesDefinitionsNameMap =
            oldAttributeDefinitions
                .stream()
                .collect(toMap(AttributeDefinition::getName, attributeDefinition -> attributeDefinition));

        final Map<String, AttributeDefinitionDraft> newAttributesDefinitionsDraftsNameMap;
        try {
            newAttributesDefinitionsDraftsNameMap = newAttributeDefinitionsDrafts.stream().collect(
                toMap(AttributeDefinitionDraft::getName, attributeDefinitionDraft -> attributeDefinitionDraft,
                    (attributeDefinitionDraftA, attributeDefinitionDraftB) -> {
                        throw new DuplicateNameException(format("Attribute definitions drafts have duplicated names. "
                            + "Duplicated attribute definition name: '%s'. "
                            + "Attribute definitions names are expected to be unique inside their product type.",
                            attributeDefinitionDraftA.getName()));
                    }
                ));
        } catch (final DuplicateNameException exception) {
            throw new BuildUpdateActionException(exception);
        }

        // Remove or compare if matching
        final List<UpdateAction<ProductType>> updateActions =
            buildRemoveAttributeDefinitionOrAttributeDefinitionUpdateActions(
                oldAttributeDefinitions,
                removedAttributesDefinitionsNames,
                newAttributesDefinitionsDraftsNameMap
            );

        // Add before changing the order because commercetools API doesn't allow to add an attribute definition
        // in a particular position. They are added at the end of the attribute definition list.
        updateActions.addAll(
            buildAddAttributeDefinitionUpdateActions(
                newAttributeDefinitionsDrafts,
                oldAttributesDefinitionsNameMap
            )
        );

        // Change the order of attribute definition list
        buildChangeAttributeDefinitionOrderUpdateAction(
            oldAttributeDefinitions,
            newAttributeDefinitionsDrafts
        )
                .ifPresent(updateActions::add);

        return updateActions;
    }

    /**
     * Checks if there are any attribute definitions which are not existing in the
     * {@code newAttributeDefinitionDraftsNameMap}. If there are, then "remove" attribute definition update actions are
     * built.
     * Otherwise, if the attribute definition still exists in the new draft, then compare the attribute definition
     * fields (name, label, etc..), and add the computed actions to the list of update actions.
     *
     * @param oldAttributeDefinitions                       the list of old {@link AttributeDefinition}s.
     * @param removedAttributeDefinitionNames               a set containing names of removed attribute definitions.
     * @param newAttributeDefinitionDraftsNameMap           a map of names to attribute definition drafts of the new
     *                                                      list of attribute definition drafts.
     *
     * @return a list of attribute definition update actions if there are attribute definitions that are not existing
     *      in the new draft. If the attribute definition still exists in the new draft, then compare the attribute
     *      definition fields (name, label, etc..), and add the computed actions to the list of update actions.
     *      Otherwise, if the attribute definitions are identical, an empty optional is returned.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildRemoveAttributeDefinitionOrAttributeDefinitionUpdateActions(
        @Nonnull final List<AttributeDefinition> oldAttributeDefinitions,
        @Nonnull final Set<String> removedAttributeDefinitionNames,
        @Nonnull final Map<String, AttributeDefinitionDraft> newAttributeDefinitionDraftsNameMap) {

        return oldAttributeDefinitions
            .stream()
            .map(oldAttributeDefinition -> {
                final String oldAttributeDefinitionName = oldAttributeDefinition.getName();
                final AttributeDefinitionDraft matchingNewAttributeDefinitionDraft =
                    newAttributeDefinitionDraftsNameMap.get(oldAttributeDefinitionName);
                return ofNullable(matchingNewAttributeDefinitionDraft)
                    .map(attributeDefinitionDraft ->
                        buildActions(
                            oldAttributeDefinition,
                            attributeDefinitionDraft
                        )
                    )
                    .orElseGet(() -> {
                        removedAttributeDefinitionNames.add(oldAttributeDefinitionName);
                        return singletonList(RemoveAttributeDefinition.of(oldAttributeDefinitionName));
                    });
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * Compares the order of a list of old {@link AttributeDefinition}s and a list of new
     * {@link AttributeDefinitionDraft}s. If there is a change in order, then a change attribute definition order
     * (with the new order) is built. If there are no changes in order an empty optional is returned.
     *
     * @param oldAttributeDefinitions                       the list of old {@link AttributeDefinition}s
     * @param newAttributeDefinitionDrafts                  the list of new {@link AttributeDefinitionDraft}s

     * @return a list of attribute definition update actions if the the order of attribute definitions is not
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
     * @param newAttributeDefinitionDrafts                  the list of new {@link AttributeDefinitionDraft}s.
     * @param oldAttributeDefinitionNameMap                 a map of names to AttributeDefinition of the old list
     *                                                      of attribute definition.
     * @return a list of attribute definition update actions if there are new attribute definition that should be added.
     *         Otherwise, if the attribute definitions are identical, an empty optional is returned.
     */
    @Nonnull
    private static List<UpdateAction<ProductType>> buildAddAttributeDefinitionUpdateActions(
        @Nonnull final List<AttributeDefinitionDraft> newAttributeDefinitionDrafts,
        @Nonnull final Map<String, AttributeDefinition> oldAttributeDefinitionNameMap) {

        // Bug in the commercetools JVM SDK. AddAttributeDefinition should expect an AttributeDefinitionDraft rather
        // than AttributeDefinition. That's why we use AttributeDefinitionCustomBuilder
        // TODO It will be fixed in https://github.com/commercetools/commercetools-jvm-sdk/issues/1786
        return newAttributeDefinitionDrafts
            .stream()
            .filter(attributeDefinitionDraft ->
                !oldAttributeDefinitionNameMap
                .containsKey(attributeDefinitionDraft.getName())
            )
            .map(AttributeDefinitionCustomBuilder::of)
            .map(AddAttributeDefinition::of)
            .collect(Collectors.toList());
    }

    private ProductTypeUpdateAttributeDefinitionActionUtils() { }
}
