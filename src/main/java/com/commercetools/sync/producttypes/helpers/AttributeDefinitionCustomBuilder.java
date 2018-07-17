package com.commercetools.sync.producttypes.helpers;

import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;

import javax.annotation.Nonnull;
import java.util.Optional;


public final class AttributeDefinitionCustomBuilder {
    /**
     * Given an {@link AttributeDefinitionDraft} returns a {@link AttributeDefinition} as a result. If the
     * 'attributeConstraint' field is 'null', its default value will be 'NONE'. If the 'inputHint' field is
     * 'null, its default value will be 'SINGLE_LINE'.
     *
     * @param attributeDefinitionDraft  the attribute definition draft which should be transformed.
     * @return The attribute definition with the same fields as the  attribute definition draft.
     */
    public static AttributeDefinition of(@Nonnull final AttributeDefinitionDraft attributeDefinitionDraft) {
        return AttributeDefinitionBuilder.of(
            attributeDefinitionDraft.getName(),
            attributeDefinitionDraft.getLabel(),
            attributeDefinitionDraft.getAttributeType()
        )
        .isRequired(attributeDefinitionDraft.isRequired())
        .attributeConstraint(Optional.ofNullable(
            attributeDefinitionDraft.getAttributeConstraint())
            .orElse(AttributeConstraint.NONE) // Default value is NONE according to commercetools API
        )
        .inputTip(attributeDefinitionDraft.getInputTip())
        .inputHint(Optional.ofNullable(
            attributeDefinitionDraft.getInputHint())
            .orElse(TextInputHint.SINGLE_LINE) // Default value is SINGLE_LINE according to commercetools API
        )
        .isSearchable(attributeDefinitionDraft.isSearchable())
        .build();

    }

    private AttributeDefinitionCustomBuilder() { }
}
