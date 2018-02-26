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
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;

// TODO: Add tests.
public final class ProductVariantAttributeUpdateActionUtils {
    private static final String ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA = "Cannot find the attribute with the name '%s'"
        + " in the supplied attribute metadata.";
    private static final String REQUIRED_ATTRIBUTE_VALUE_IS_NULL = "The attribute with the name '%s' is null but it is"
        + " required to have a value according to the supplied attribute metadata.";

    /**
     * Compares the attributes of a {@link AttributeDraft} and a {@link Attribute} to build either a
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} or a
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants}.
     *
     * <p>If the attribute is sameForAll a
     * {@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants} is built. Otherwise,
     * a {@link io.sphere.sdk.products.commands.updateactions.SetAttribute} is built.
     *
     * <p>If both the {@link AttributeDraft} and the {@link Attribute} have identical values, then
     * no update action is needed and hence an empty {@link List} is returned.
     *
     * @param variantId the id of the variant of that the attribute belong to. It is used only in the error
     *                           messages if any.
     * @param oldProductVariantAttribute the {@link Attribute} which should be updated.
     * @param newProductVariantAttribute the {@link AttributeDraft} where we get the new value.
     * @param attributeMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name, whether a value is required or not and whether it has the
     *                           constraint "SameForAll" or not.
     * @return A filled optional with the update action or an empty optional if the attributes are identical.
     * @throws BuildUpdateActionException thrown if attribute as not found in the {@code attributeMetaData} or
     *          if the attribute is required and the new value is null.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildProductVariantAttributeUpdateAction(
        final int variantId,
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
                        () -> SetAttribute.of(variantId, newProductVariantAttribute, true));
            } else {
                return attributeMetaData.isSameForAll()
                    ? Optional.of(SetAttributeInAllVariants.ofUnsetAttribute(newProductVariantAttributeName, true)) :
                    Optional.of(SetAttribute.ofUnsetAttribute(variantId, newProductVariantAttributeName, true));
            }
        }

    }

    private ProductVariantAttributeUpdateActionUtils() {
    }
}
