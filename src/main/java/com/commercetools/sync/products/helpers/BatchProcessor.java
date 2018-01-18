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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.isProductReference;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class BatchProcessor {
    private static final String PRODUCT_DRAFT_KEY_NOT_SET = "ProductDraft with name: %s doesn't have a key. "
        + "Please make sure all product drafts have keys.";
    private static final String PRODUCT_DRAFT_IS_NULL = "ProductDraft is null.";
    private static final String PRODUCT_VARIANT_DRAFT_IS_NULL = "ProductVariantDraft at position [%s] of ProductDraft "
        + "with key %s is null.";
    private static final String PRODUCT_VARIANT_DRAFT_SKU_NOT_SET = "ProductVariantDraft at position [%s] of "
        + "ProductDraft with key %s has no SKU set. Please make sure all variants have SKUs.";
    private static final String PRODUCT_VARIANT_DRAFT_KEY_NOT_SET = "ProductVariantDraft at position [%s] of "
        + "ProductDraft with key %s has no key set. Please make sure all variants have keys.";

    private final List<ProductDraft> productDrafts;
    private final ProductSync productSync;
    private final Set<ProductDraft> validDrafts = ConcurrentHashMap.newKeySet();
    private final Set<String> keysToCache = ConcurrentHashMap.newKeySet();

    public BatchProcessor(@Nonnull final List<ProductDraft> productDrafts, @Nonnull final ProductSync productSync) {
        this.productDrafts = productDrafts;
        this.productSync = productSync;
    }

    /**
     * This method validates the batch of drafts, and only for valid drafts it TODO: NEED TO FIX DOCU.
     *
     * <p>A valid product draft is one which satisfies the following conditions:
     * <ol>
     *     <li>It has a key which is not blank (null/empty)</li>
     *     <li>It has all variants and master variant valid</li>
     *     <li>A variant is valid if it satisfies the following conditions:
     *          <ol>
     *              <li>It has a key which is not blank (null/empty)</li>
     *              <li>It has a SKU which is not blank (null/empty)</li>
     *          </ol>
     *     </li>
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
     * Note: Null attributes are skipped since they are validated at a later stage in
     *   ({@link com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils})
     * @param variantDraft the variant draft to get the referenced product keys for.
     * @return the set of referenced product keys.
     */
    private Set<String> getReferencedProductKeys(@Nonnull final ProductVariantDraft variantDraft) {
        final List<AttributeDraft> attributeDrafts = variantDraft.getAttributes();
        return attributeDrafts != null ? attributeDrafts.stream().filter(Objects::nonNull)
                                                        .map(BatchProcessor::getReferencedProductKeys)
                                                        .flatMap(Collection::stream)
                                                        .collect(Collectors.toSet()) : emptySet();
    }

    /**
     * Get a set of referenced product keys given an attribute draft.
     * @param attributeDraft the attribute to get the referenced keys from.
     * @return set of referenced product keys given an attribute draft.
     */
    @Nonnull
    public static Set<String> getReferencedProductKeys(@Nonnull final AttributeDraft attributeDraft) {
        final JsonNode attributeDraftValue = attributeDraft.getValue();
        if (attributeDraftValue == null) {
            return emptySet();
        }
        if (attributeDraftValue.isArray()) {
            return getReferencedProductKeysFromSet(attributeDraft);
        } else {
            if (isProductReference(attributeDraftValue)) {
                final String productKeyFromReference = getProductKeyFromReference(attributeDraftValue);
                return productKeyFromReference != null ? singleton(productKeyFromReference) : emptySet();
            }
            return emptySet();
        }
    }

    @Nonnull
    private static Set<String> getReferencedProductKeysFromSet(
        @Nonnull final AttributeDraft attributeDraft) {
        final JsonNode attributeDraftValue = attributeDraft.getValue();
        final Spliterator<JsonNode> attributeReferencesIterator = attributeDraftValue.spliterator();

        return StreamSupport.stream(attributeReferencesIterator, false)
                            .filter(Objects::nonNull)
                            .filter(reference -> !reference.isNull())
                            .map(BatchProcessor::getProductKeyFromReference)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
    }

    @Nullable
    private static String getProductKeyFromReference(@Nonnull final JsonNode referenceValue) {
        return referenceValue.get(REFERENCE_ID_FIELD).asText();
    }

    private List<String> getProductDraftErrorsAndAcceptConsumer(@Nonnull final ProductDraft productDraft,
                                                                @Nonnull final Consumer<ProductVariantDraft>
                                                                    variantConsumer) {
        final List<String> errorMessages = new ArrayList<>(
            getVariantDraftErrorsAndAcceptConsumer(productDraft.getMasterVariant(), "masterVariant",
                requireNonNull(productDraft.getKey()), variantConsumer));

        for (int i = 0; i < productDraft.getVariants().size(); i++) {
            errorMessages.addAll(getVariantDraftErrors(allVariants.get(i), i, requireNonNull(productDraft.getKey())));
        }

        // Only if there is no errors on all variants, then accept consumer.
        if (errorMessages.isEmpty()) {
            allVariants.forEach(variantConsumer);
        }

        return errorMessages;
    }

    private List<String> getVariantDraftErrorsAndAcceptConsumer(@Nullable final ProductVariantDraft productVariantDraft,
                                                                @Nonnull final String productDraftKey,
                                              final int variantPosition,
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
