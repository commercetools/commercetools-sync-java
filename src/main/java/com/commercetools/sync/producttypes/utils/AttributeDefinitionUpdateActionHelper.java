package com.commercetools.sync.producttypes.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

final class AttributeDefinitionUpdateActionHelper {

    static void ensureAttributeDefinitionsAreValid(
        @Nonnull final AttributeDefinition attributeDefinition,
        @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) throws BuildUpdateActionException {

        if (AttributeDefinitionValidationUtils.areValid(attributeDefinition, attributeDefinitionDraft)) {
            return;
        }

        final List<String> errors = new ArrayList<>();
        boolean areNamesValid = true;

        if (StringUtils.isBlank(attributeDefinition.getName())
            && StringUtils.isBlank(attributeDefinitionDraft.getName())) {
            errors.add("Names are not set for both the old and new/draft attribute definitions.");
        } else if (StringUtils.isBlank(attributeDefinition.getName())) {
            errors.add("Name is not set for the old attribute definition.");
        } else if (StringUtils.isBlank(attributeDefinitionDraft.getName())) {
            errors.add("Name is not set for the new/draft attribute definition.");
        } else if (!attributeDefinition.getName().equals(attributeDefinitionDraft.getName())) {
            errors.add("Names are not equal for the attribute definitions.");
        }

        if (!errors.isEmpty()) {
            areNamesValid = false;
        }

        if (attributeDefinition.getAttributeType() == null
            && attributeDefinitionDraft.getAttributeType() == null) {
            errors.add("Attribute types are not set for both the old and new/draft attribute definitions.");
        } else if (attributeDefinition.getAttributeType() == null) {
            errors.add("Attribute type is not set for the old attribute definition.");
        } else if (attributeDefinitionDraft.getAttributeType() == null) {
            errors.add("Attribute type is not set for the new/draft attribute definition.");
        }

        if (areNamesValid) {
            errors.add(format("Attribute definitions name: '%s'.", attributeDefinition.getName()));
        }

        errors.add("Attribute definitions are expected to be valid.");
        throw new BuildUpdateActionException(String.join(" ", errors));

    }

    static void ensureAttributeDefinitionIsValid(
        @Nonnull final AttributeDefinition attributeDefinition) throws BuildUpdateActionException {

        if (AttributeDefinitionValidationUtils.isValid(attributeDefinition)) {
            return;
        }

        final List<String> errors = new ArrayList<>();
        boolean isNameValid = true;

        if (StringUtils.isBlank(attributeDefinition.getName())) {
            isNameValid = false;
            errors.add("Name is not set for the old attribute definition.");
        }

        if (attributeDefinition.getAttributeType() == null) {
            errors.add("Attribute type is not set for the old attribute definition.");
        }

        if (isNameValid) {
            errors.add(format("Attribute definition name: '%s'.", attributeDefinition.getName()));
        }

        errors.add("Attribute definition is expected to be valid.");
        throw new BuildUpdateActionException(String.join(" ", errors));

    }

    static void ensureAttributeDefinitionTypeIsValid(
        @Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) throws BuildUpdateActionException {

        if (AttributeDefinitionValidationUtils.isValid(attributeDefinitionDraft)) {
            return;
        }

        final List<String> errors = new ArrayList<>();
        errors.add("Attribute type is not set for the new/draft attribute definition.");
        errors.add(format("Attribute definition name: '%s'.", attributeDefinitionDraft.getName()));
        errors.add("Attribute definition draft is expected to be valid.");
        throw new BuildUpdateActionException(String.join(" ", errors));

    }

    private AttributeDefinitionUpdateActionHelper() {
    }
}
