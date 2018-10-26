package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static java.util.Optional.ofNullable;

public final class AttributeDefinitionValidationUtils {

    public static boolean areValid(@Nonnull final AttributeDefinition oldAttributeDefinition,
                                   @Nonnull final AttributeDefinitionDraft newAttributeDefinitionDraft) {
        return isValid(oldAttributeDefinition) && isValid(newAttributeDefinitionDraft)
            && oldAttributeDefinition.getName().equals(newAttributeDefinitionDraft.getName());
    }

    public static boolean isValid(@Nonnull final AttributeDefinition attributeDefinition) {
        return StringUtils.isNotBlank(attributeDefinition.getName())
            && ofNullable(attributeDefinition.getAttributeType()).isPresent();
    }

    public static boolean isValid(@Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {
        return StringUtils.isNotBlank(attributeDefinitionDraft.getName())
            && ofNullable(attributeDefinitionDraft.getAttributeType()).isPresent();
    }

    private AttributeDefinitionValidationUtils() {
    }
}