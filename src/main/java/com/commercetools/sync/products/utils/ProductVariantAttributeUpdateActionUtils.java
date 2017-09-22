package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.AttributeMetaData;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;

// TODO: Add JAVADOC AND TESTS
public final class ProductVariantAttributeUpdateActionUtils {
    private static final String ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA = "Cannot find the attribute with the name '%s'"
        + " in the supplied attribute metadata.";
    private static final String REQUIRED_ATTRIBUTE_VALUE_IS_NULL = "The attribute with the name '%s' is null but it is"
        + " required to have a value according to the supplied attribute metadata.";

    /**
     ** Compares the attributes of an {@link Attribute} and an {@link AttributeDraft} to build either
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} or
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants} update actions.
     * TODO: Add JavaDoc
     *
     * @param variantSku TODO: Add JavaDoc
     * @param oldProductVariantAttribute TODO: Add JavaDoc
     * @param newProductVariantAttribute TODO: Add JavaDoc
     * @param attributeMetaData TODO: Add JavaDoc
     * @return TODO: Add JavaDoc
     * @throws BuildUpdateActionException TODO: Add JavaDoc
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildProductVariantAttributeUpdateAction(
        @Nonnull final String variantSku,
        @Nullable final Attribute oldProductVariantAttribute,
        @Nonnull final AttributeDraft newProductVariantAttribute,
        @Nullable final AttributeMetaData attributeMetaData)
        throws BuildUpdateActionException {
        final String newProductVariantAttributeName = newProductVariantAttribute.getName();
        final JsonNode newProductVariantAttributeValue = newProductVariantAttribute.getValue();
        final JsonNode oldProductVariantAttributeValue = oldProductVariantAttribute != null
            ? oldProductVariantAttribute.getValueAsJsonNode() : null;

        if (attributeMetaData == null) {
            final String errorMessage = format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newProductVariantAttributeName);
            throw new BuildUpdateActionException(errorMessage);
        }

        if (attributeMetaData.isRequired() && newProductVariantAttributeValue == null) {
            final String errorMessage = format(REQUIRED_ATTRIBUTE_VALUE_IS_NULL, newProductVariantAttributeName);
            throw new BuildUpdateActionException(errorMessage);
        } else {
            if (newProductVariantAttributeValue != null) {
                return attributeMetaData.isSameForAll()
                    ? buildUpdateAction(oldProductVariantAttributeValue, newProductVariantAttributeValue,
                        () -> SetAttributeInAllVariants.of(newProductVariantAttribute, true)) :
                    buildUpdateAction(oldProductVariantAttributeValue, newProductVariantAttributeValue,
                        () -> SetAttribute.ofSku(variantSku, newProductVariantAttribute, true));
            } else {
                return attributeMetaData.isSameForAll()
                    ? Optional.of(SetAttributeInAllVariants.ofUnsetAttribute(newProductVariantAttributeName, true)) :
                    Optional.of(SetAttribute.ofUnsetAttributeForSku(variantSku, newProductVariantAttributeName, true));
            }
        }

    }
}
