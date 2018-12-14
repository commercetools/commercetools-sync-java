package com.commercetools.sync.producttypes.helpers;

import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;

import javax.annotation.Nonnull;

import static java.util.Optional.ofNullable;


public final class AttributeDefinitionCustomBuilder {
    /**
     * Given an {@link AttributeDefinitionDraft} returns a {@link AttributeDefinition} as a result. If the
     * 'attributeConstraint' field is 'null', its default value will be 'NONE'. If the 'inputHint' field is
     * 'null', its default value will be 'SINGLE_LINE'. If the 'isRequired' field is null, its default value will be
     * 'false'. If the 'isSearchable' field is null, its default value will be 'false'.
     *
     * @param attributeDefinitionDraft  the attribute definition draft which should be transformed.
     * @return The attribute definition with the same fields as the attribute definition draft.
     */
    public static AttributeDefinition of(@Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {
        return AttributeDefinitionBuilder
            .of(
                attributeDefinitionDraft.getName(),
                attributeDefinitionDraft.getLabel(),
                attributeDefinitionDraft.getAttributeType())
            .isRequired(
                ofNullable(attributeDefinitionDraft.isRequired())
                .orElse(false))
            .attributeConstraint(
                ofNullable(attributeDefinitionDraft.getAttributeConstraint())
                .orElse(AttributeConstraint.NONE)) // Default value is NONE according to commercetools API
            .inputTip(attributeDefinitionDraft.getInputTip())
            .inputHint(
                ofNullable(attributeDefinitionDraft.getInputHint())
                .orElse(TextInputHint.SINGLE_LINE)) // Default value is SINGLE_LINE according to commercetools API
            .isSearchable(
                ofNullable(attributeDefinitionDraft.isSearchable())
                .orElse(true)) // Default value is true according to commercetools API
            .build();
    }

    private AttributeDefinitionCustomBuilder() { }
}
