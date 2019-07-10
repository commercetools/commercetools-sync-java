package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.exceptions.InvalidProductTypeDraftException;
import com.commercetools.sync.commons.exceptions.InvalidReferenceException;
import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class BatchProcessor {
    static final String PRODUCT_TYPE_DRAFT_KEY_NOT_SET = "ProductTypeDraft with name: %s doesn't have a key. "
        + "Please make sure all productType drafts have keys.";
    static final String PRODUCT_TYPE_DRAFT_IS_NULL = "ProductTypeDraft is null.";
    static final String PRODUCT_TYPE_HAS_INVALID_REFERENCES = "ProductTypeDraft with key: '%s' has invalid productType "
        + "references on the following AttributeDefinitionDrafts: %s";

    private final List<ProductTypeDraft> productTypeDrafts;
    private final ProductTypeSync productTypeSync;
    private final Set<ProductTypeDraft> validDrafts = new HashSet<>();
    private final Set<String> keysToCache = new HashSet<>();

    public BatchProcessor(@Nonnull final List<ProductTypeDraft> productTypeDrafts,
                          @Nonnull final ProductTypeSync productTypeSync) {
        this.productTypeDrafts = productTypeDrafts;
        this.productTypeSync = productTypeSync;
    }

    /**
     * This method validates the batch of drafts, and only for valid drafts it adds the valid draft
     * to a {@code validDrafts} set, and adds the keys of those drafts and their referenced productTypes to a
     * {@code keysToCache} set.
     *
     *
     * <p>A valid productType draft is one which satisfies the following conditions:
     * <ol>
     * <li>Is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has no invalid productType reference on an attributeDefinitionDraft
     * with either a NestedType or SetType AttributeType.
     * A valid reference is simply one which has its id field's value not blank (null/empty)</li>
     * </ol>
     */
    public void validateBatch() {
        for (ProductTypeDraft productTypeDraft : productTypeDrafts) {
            if (productTypeDraft != null) {
                final String productTypeDraftKey = productTypeDraft.getKey();
                if (isNotBlank(productTypeDraftKey)) {
                    try {
                        final Set<String> referencedProductTypeKeys = getReferencedProductTypeKeys(productTypeDraft);
                        keysToCache.addAll(referencedProductTypeKeys);
                        keysToCache.add(productTypeDraftKey);
                        validDrafts.add(productTypeDraft);
                    } catch (InvalidProductTypeDraftException invalidProductTypeDraftException) {
                        handleError(invalidProductTypeDraftException);
                    }
                } else {
                    final String errorMessage = format(PRODUCT_TYPE_DRAFT_KEY_NOT_SET, productTypeDraft.getName());
                    handleError(new InvalidProductTypeDraftException(errorMessage));
                }
            } else {
                handleError(new InvalidProductTypeDraftException(PRODUCT_TYPE_DRAFT_IS_NULL));
            }
        }
    }

    @Nonnull
    private static Set<String> getReferencedProductTypeKeys(@Nonnull final ProductTypeDraft productTypeDraft)
        throws InvalidProductTypeDraftException {

        final List<AttributeDefinitionDraft> attributeDefinitionDrafts = productTypeDraft.getAttributes();
        if (attributeDefinitionDrafts == null) {
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
            throw new InvalidProductTypeDraftException(errorMessage,
                new InvalidReferenceException(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        }

        return referencedProductTypeKeys;
    }

    @Nonnull
    private static Optional<String> getProductTypeKey(@Nonnull final AttributeType attributeType)
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
            throw new InvalidReferenceException(BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER);
        }
        return key;
    }

    private void handleError(@Nonnull final Throwable throwable) {
        productTypeSync.getSyncOptions().applyErrorCallback(throwable.getMessage(), throwable);
        productTypeSync.getStatistics().incrementFailed();
    }

    public Set<ProductTypeDraft> getValidDrafts() {
        return validDrafts;
    }

    public Set<String> getKeysToCache() {
        return keysToCache;
    }
}
