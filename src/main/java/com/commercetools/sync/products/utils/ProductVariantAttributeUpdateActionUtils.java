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
import java.util.Map;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.lang.String.format;

// TODO: Add tests.
public final class ProductVariantAttributeUpdateActionUtils {
    public static final String ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA = "Cannot find the attribute with the name '%s'"
        + " in the supplied attribute metadata.";

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
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name and whether it has the constraint "SameForAll" or not.
     * @return A filled optional with the update action or an empty optional if the attributes are identical.
     * @throws BuildUpdateActionException thrown if attribute as not found in the {@code attributeMetaData} or
     *          if the attribute is required and the new value is null.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildProductVariantAttributeUpdateAction(
        final int variantId,
        @Nullable final Attribute oldProductVariantAttribute,
        @Nonnull final AttributeDraft newProductVariantAttribute,
        @Nonnull final Map<String, AttributeMetaData> attributesMetaData) throws BuildUpdateActionException {

        final String newProductVariantAttributeName = newProductVariantAttribute.getName();
        final JsonNode newProductVariantAttributeValue = newProductVariantAttribute.getValue();
        final JsonNode oldProductVariantAttributeValue = oldProductVariantAttribute != null
            ? oldProductVariantAttribute.getValueAsJsonNode() : null;

        final AttributeMetaData attributeMetaData = attributesMetaData.get(newProductVariantAttributeName);

        if (attributeMetaData == null) {
            final String errorMessage = format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, newProductVariantAttributeName);
            throw new BuildUpdateActionException(errorMessage);
        }

    static UpdateAction<Product> buildUnSetAttribute(@Nonnull final Integer variantId,
                                                     @Nonnull final String attributeName,
                                                     @Nonnull final Map<String, AttributeMetaData> attributesMetaData)
            throws BuildUpdateActionException {

        final AttributeMetaData attributeMetaData = attributesMetaData.get(attributeName);

        if (attributeMetaData == null) {
            final String errorMessage = format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, attributeName);
            throw new BuildUpdateActionException(errorMessage);
        }

        return buildUnSetAttribute(variantId, attributeName, attributeMetaData);
    }

    private static UpdateAction<Product> buildUnSetAttribute(@Nonnull final Integer variantId,
                                                             @Nonnull final String attributeName,
                                                             @Nonnull final AttributeMetaData attributeMetaData) {

        return attributeMetaData.isSameForAll()
                ? SetAttributeInAllVariants.ofUnsetAttribute(attributeName, true) :
                SetAttribute.ofUnsetAttribute(variantId, attributeName, true);
    }

    private ProductVariantAttributeUpdateActionUtils() {
    }
}
