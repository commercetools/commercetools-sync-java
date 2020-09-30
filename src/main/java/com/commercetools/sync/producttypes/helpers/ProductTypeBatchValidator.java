package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_REFERENCE;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ProductTypeBatchValidator
    extends BaseBatchValidator<ProductTypeDraft, ProductTypeSyncOptions, ProductTypeSyncStatistics> {

    static final String PRODUCT_TYPE_DRAFT_KEY_NOT_SET = "ProductTypeDraft with name: %s doesn't have a key. "
        + "Please make sure all productType drafts have keys.";
    static final String PRODUCT_TYPE_DRAFT_IS_NULL = "ProductTypeDraft is null.";
    static final String PRODUCT_TYPE_HAS_INVALID_REFERENCES = "ProductTypeDraft with key: '%s' has invalid productType "
        + "references on the following AttributeDefinitionDrafts: %s";

    public ProductTypeBatchValidator(@Nonnull final ProductTypeSyncOptions syncOptions,
                                     @Nonnull final ProductTypeSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link ProductTypeDraft}&gt; of drafts this method attempts to validate
     * drafts and collect referenced keys from the draft
     * and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link ProductTypeDraft}&gt;
     * ,{@link Set}&lt;{@link String}&gt;&gt;
     * which contains the {@link Set} of valid drafts and referenced product type keys.
     *
     * <p>A valid productType draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has no invalid productType reference on an attributeDefinitionDraft
     * with either a NestedType or SetType AttributeType.
     * A valid reference is simply one which has its id field's value not blank (null/empty)</li>
     * </ol>
     *
     * @param productTypeDrafts the product type drafts to validate and collect referenced product type keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ProductTypeDraft}&gt;,
     *      {@link Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and
     *      referenced product type keys.
     */
    @Override
    public ImmutablePair<Set<ProductTypeDraft>, Set<String>> validateAndCollectReferencedKeys(
        @Nonnull final List<ProductTypeDraft> productTypeDrafts) {

        final Set<String> productTypeKeys = new HashSet<>();

        final Set<ProductTypeDraft> validDrafts = productTypeDrafts
            .stream()
            .filter(productTypeDraft -> isValidProductTypeDraft(productTypeDraft, productTypeKeys))
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, productTypeKeys);
    }

    private boolean isValidProductTypeDraft(
        @Nullable final ProductTypeDraft productTypeDraft,
        @Nonnull final Set<String> productTypeKeys) {

        if (productTypeDraft == null) {
            handleError(PRODUCT_TYPE_DRAFT_IS_NULL);
        } else if (isBlank(productTypeDraft.getKey())) {
            handleError(format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraft.getName()));
        } else {
            try {
                final Set<String> referencedProductTypeKeys = getReferencedProductTypeKeys(productTypeDraft);
                productTypeKeys.addAll(referencedProductTypeKeys);
                return true;
            } catch (SyncException syncException) {
                handleError(syncException);
            }
        }

        return false;
    }

    @Nonnull
    private static Set<String> getReferencedProductTypeKeys(@Nonnull final ProductTypeDraft productTypeDraft)
        throws SyncException {

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productTypeDraft.getAttributes();
        if (attributeDefinitionDrafts == null || attributeDefinitionDrafts.isEmpty()) {
            return emptySet();
        }

        final Set<String> referencedProductTypeKeys = new HashSet<>();
        final List<String> invalidAttributeDefinitionNames = new ArrayList<>();

        for (AttributeDefinitionDraft attributeDefinitionDraft : attributeDefinitionDrafts) {
            if (attributeDefinitionDraft != null) {
                final AttributeType attributeType = attributeDefinitionDraft.getAttributeType();
                try {
                    getProductTypeKey(attributeType).ifPresent(referencedProductTypeKeys::add);
                } catch (InvalidReferenceException invalidReferenceException) {
                    invalidAttributeDefinitionNames.add(attributeDefinitionDraft.getName());
                }
            }
        }

        if (!invalidAttributeDefinitionNames.isEmpty()) {
            final String errorMessage = format(PRODUCT_TYPE_HAS_INVALID_REFERENCES, productTypeDraft.getKey(),
                invalidAttributeDefinitionNames);
            throw new SyncException(errorMessage,
                new InvalidReferenceException(BLANK_ID_VALUE_ON_REFERENCE));
        }

        return referencedProductTypeKeys;
    }

    /**
     * This method is meant be only used internally by the library.
     * @param attributeType the attributeType to attempt to fetch the product type key of, if it contains a
     *                      nestedType reference.
     * @return an optional containing the productType key or empty if it does not contain a productType reference.
     * @throws InvalidReferenceException thrown if the productType key in the nested reference is invalid.
     */
    @Nonnull
    public static Optional<String> getProductTypeKey(@Nonnull final AttributeType attributeType)
        throws InvalidReferenceException {

        if (attributeType instanceof NestedAttributeType) {
            final NestedAttributeType nestedElementType = (NestedAttributeType) attributeType;
            return Optional.of(getProductTypeKey(nestedElementType));
        } else if (attributeType instanceof SetAttributeType) {
            final SetAttributeType setAttributeType = (SetAttributeType) attributeType;
            return getProductTypeKey(setAttributeType.getElementType());
        }
        return Optional.empty();
    }

    @Nonnull
    private static String getProductTypeKey(@Nonnull final NestedAttributeType nestedAttributeType)
        throws InvalidReferenceException {

        final String key = nestedAttributeType.getTypeReference().getId();
        if (isBlank(key)) {
            throw new InvalidReferenceException(BLANK_ID_VALUE_ON_REFERENCE);
        }
        return key;
    }
}
