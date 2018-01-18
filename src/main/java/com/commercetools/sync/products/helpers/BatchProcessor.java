package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSync;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.AttributeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.isProductReference;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class BatchProcessor {
    static final String PRODUCT_DRAFT_KEY_NOT_SET = "ProductDraft with name: %s doesn't have a key. "
        + "Please make sure all product drafts have keys.";
    static final String PRODUCT_DRAFT_IS_NULL = "ProductDraft is null.";
    static final String PRODUCT_VARIANT_DRAFT_IS_NULL = "ProductVariantDraft at position '%d' of ProductDraft "
        + "with key '%s' is null.";
    static final String PRODUCT_VARIANT_DRAFT_SKU_NOT_SET = "ProductVariantDraft at position '%d' of "
        + "ProductDraft with key '%s' has no SKU set. Please make sure all variants have SKUs.";
    static final String PRODUCT_VARIANT_DRAFT_KEY_NOT_SET = "ProductVariantDraft at position '%d' of "
        + "ProductDraft with key '%s' has no key set. Please make sure all variants have keys.";

    private final List<ProductDraft> productDrafts;
    private final ProductSync productSync;
    private final Set<ProductDraft> validDrafts = new HashSet<>();
    private final Set<String> keysToCache = new HashSet<>();

    public BatchProcessor(@Nonnull final List<ProductDraft> productDrafts, @Nonnull final ProductSync productSync) {
        this.productDrafts = productDrafts;
        this.productSync = productSync;
    }

    /**
     * This method validates the batch of drafts, and only for valid drafts it adds the valid draft
     * to {@code validDrafts} set, and adds the keys of all referenced products to
     * {@code keysToCache}.
     *
     *
     * <p>A valid product draft is one which satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has all variants AND master variant valid</li>
     * <li>A variant is valid if it satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has a SKU which is not blank (null/empty)</li>
     * </ol>
     * </li>
     * </ol>
     */
    public void validateBatch() {
        for (ProductDraft productDraft : productDrafts) {
            if (productDraft != null) {
                final String productKey = productDraft.getKey();
                if (isNotBlank(productKey)) {
                    final Consumer<ProductVariantDraft> productVariantDraftConsumer =
                        productVariantDraft -> keysToCache.addAll(getReferencedProductKeys(productVariantDraft));
                    // Validate draft and add referenced keys
                    final List<String> draftErrors = getProductDraftErrorsAndAcceptConsumer(productDraft,
                        productVariantDraftConsumer);

                    if (!draftErrors.isEmpty()) {
                        handleError(draftErrors);
                    } else {
                        // Add current draft key
                        keysToCache.add(productKey);
                        validDrafts.add(productDraft);
                    }
                } else {
                    final String errorMessage = format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName());
                    handleError(errorMessage);
                }
            } else {
                handleError(PRODUCT_DRAFT_IS_NULL);
            }
        }
    }

    /**
     * Get a set of referenced product keys on all attribute drafts on the supplied Product
     * Variant Draft.
     *
     * <p>Note: Null attributes are skipped since they are validated at a later stage in
     * ({@link com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils})
     *
     * @param variantDraft the variant draft to get the referenced product keys from.
     * @return the set of referenced product keys.
     */
    @Nonnull
    static Set<String> getReferencedProductKeys(@Nonnull final ProductVariantDraft variantDraft) {
        final List<AttributeDraft> attributeDrafts = variantDraft.getAttributes();
        if (attributeDrafts == null) {
            return emptySet();
        }
        return attributeDrafts.stream()
                              .filter(Objects::nonNull)
                              .map(BatchProcessor::getReferencedProductKeys)
                              .flatMap(Collection::stream)
                              .collect(Collectors.toSet());
    }

    /**
     * Get a set of referenced product keys given an attribute draft.
     *
     * @param attributeDraft the attribute to get the referenced keys from.
     * @return set of referenced product keys given an attribute draft.
     */
    @Nonnull
    static Set<String> getReferencedProductKeys(@Nonnull final AttributeDraft attributeDraft) {
        final JsonNode attributeDraftValue = attributeDraft.getValue();
        if (attributeDraftValue == null) {
            return emptySet();
        }
        return attributeDraftValue.isArray()
            ? getReferencedProductKeysFromSet(attributeDraftValue) :
            getProductKeyFromReference(attributeDraftValue).map(Collections::singleton)
                                                           .orElse(emptySet());
    }

    /**
     * Gets a set of referenced product keys (if any) given a JsonNode representin a
     * reference set.
     *
     * @param referenceSet the product reference set JsonNode.
     * @return set of referenced product keys given an attribute draft.
     */
    @Nonnull
    static Set<String> getReferencedProductKeysFromSet(@Nonnull final JsonNode referenceSet) {
        return StreamSupport.stream(referenceSet.spliterator(), false)
                            .filter(Objects::nonNull)
                            .map(BatchProcessor::getProductKeyFromReference)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet());
    }

    /**
     * Gets a referenced product key (if any) given a JsonNode representing a reference.
     *
     * @param referenceValue the product reference JsonNode.
     * @return referenced product key given a JsonNode.
     */
    @Nonnull
    static Optional<String> getProductKeyFromReference(@Nonnull final JsonNode referenceValue) {
        return isProductReference(referenceValue)
            ? ofNullable(referenceValue.get(REFERENCE_ID_FIELD)).map(JsonNode::asText) : empty();
    }

    /**
     * Gets a list of error messages that exist on a product draft. Only if there are no errors
     * on the product draft then it accepts the supplied {@code variantConsumer} on all the variants
     * of the supplied product draft.
     *
     * <p>The errors that could exist on a product draft could be any of the following:
     * <ul>
     * <li>Null variant</li>
     * <li>Variant key not set</li>
     * <li>Variant sku not set</li>
     * </ul>
     *
     * @param productDraft    the product draft to validate.
     * @param variantConsumer the consumer to accept on all variants of product draft, if valid.
     * @return a list of error messages resulting from the product draft validation.
     */
    @Nonnull
    static List<String> getProductDraftErrorsAndAcceptConsumer(@Nonnull final ProductDraft productDraft,
                                                               @Nonnull final Consumer<ProductVariantDraft>
                                                                   variantConsumer) {
        final List<String> errorMessages = new ArrayList<>();
        final List<ProductVariantDraft> allVariants = new ArrayList<>();
        allVariants.add(productDraft.getMasterVariant());
        if (productDraft.getVariants() != null) {
            allVariants.addAll(productDraft.getVariants());
        }
        for (int i = 0; i < allVariants.size(); i++) {
            errorMessages.addAll(getVariantDraftErrors(allVariants.get(i), i, requireNonNull(productDraft.getKey())));
        }

        // Only if there is no errors on all variants, then accept consumer.
        if (errorMessages.isEmpty()) {
            allVariants.forEach(variantConsumer);
        }

        return errorMessages;
    }

    /**
     * Gets a list of error messages that exist on a product variant draft.
     *
     * <p>The errors that could exist on a product variant draft could be any of the following:
     * <ul>
     * <li>Null variant</li>
     * <li>Variant key not set</li>
     * <li>Variant sku not set</li>
     * </ul>
     *
     * @param productVariantDraft the variant draft to validate.
     * @param variantPosition     the position of the variant on the product draft (master variant is at position 0).
     * @param productDraftKey     the key of the product draft.
     * @return a list of error messages resulting from the variant validation.
     */
    @Nonnull
    static List<String> getVariantDraftErrors(@Nullable final ProductVariantDraft productVariantDraft,
                                              final int variantPosition,
                                              @Nonnull final String productDraftKey) {
        final List<String> errorMessages = new ArrayList<>();
        if (productVariantDraft != null) {
            if (isBlank(productVariantDraft.getKey())) {
                errorMessages.add(format(PRODUCT_VARIANT_DRAFT_KEY_NOT_SET, variantPosition, productDraftKey));
            }
            if (isBlank(productVariantDraft.getSku())) {
                errorMessages.add(format(PRODUCT_VARIANT_DRAFT_SKU_NOT_SET, variantPosition, productDraftKey));
            }
        } else {
            errorMessages.add(format(PRODUCT_VARIANT_DRAFT_IS_NULL, variantPosition, productDraftKey));
        }
        return errorMessages;
    }


    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed products to sync.
     *
     * @param errorMessages The error messages describing the reasons of failure.
     */
    private void handleError(@Nonnull final List<String> errorMessages) {
        errorMessages.forEach(productSync.getSyncOptions()::applyErrorCallback);
        productSync.getStatistics().incrementFailed();
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed products to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     */
    private void handleError(@Nonnull final String errorMessage) {
        productSync.getSyncOptions().applyErrorCallback(errorMessage);
        productSync.getStatistics().incrementFailed();
    }

    public Set<ProductDraft> getValidDrafts() {
        return validDrafts;
    }

    public Set<String> getKeysToCache() {
        return keysToCache;
    }
}
