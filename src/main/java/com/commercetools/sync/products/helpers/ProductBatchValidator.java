package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.commons.utils.SyncUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.products.ProductSyncOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.isReferenceOfType;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ProductBatchValidator
    extends BaseBatchValidator<ProductDraft, ProductSyncOptions, ProductSyncStatistics> {

    static final String PRODUCT_DRAFT_KEY_NOT_SET = "ProductDraft with name: %s doesn't have a key. "
        + "Please make sure all product drafts have keys.";
    static final String PRODUCT_DRAFT_IS_NULL = "ProductDraft is null.";
    static final String PRODUCT_VARIANT_DRAFT_IS_NULL = "ProductVariantDraft at position '%d' of ProductDraft "
        + "with key '%s' is null.";
    static final String PRODUCT_VARIANT_DRAFT_SKU_NOT_SET = "ProductVariantDraft at position '%d' of "
        + "ProductDraft with key '%s' has no SKU set. Please make sure all variants have SKUs.";
    static final String PRODUCT_VARIANT_DRAFT_KEY_NOT_SET = "ProductVariantDraft at position '%d' of "
        + "ProductDraft with key '%s' has no key set. Please make sure all variants have keys.";

    public ProductBatchValidator(@Nonnull final ProductSyncOptions syncOptions,
                                 @Nonnull final ProductSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link ProductDraft}&gt; of drafts this method attempts to validate
     * drafts and collect referenced keys from the draft and return an {@link ImmutablePair}&lt;{@link Set}&lt;
     * {@link ProductDraft}&gt;,{@link ProductBatchValidator.ReferencedKeys}&gt;
     * which contains the {@link Set} of valid drafts and referenced keys within a wrapper.
     *
     * <p>A valid product draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has all variants AND master variant valid</li>
     * <li>A variant is valid if it satisfies the following conditions:
     * <ol>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has a SKU which is not blank (null/empty)</li>
     * </ol>
     * </li>
     * </ol>
     *
     * @param productDrafts the product drafts to validate and collect referenced keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ProductDraft}&gt;,
     *      {@link ProductBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts
     *      and referenced keys within a wrapper.
     */
    @Override
    public ImmutablePair<Set<ProductDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<ProductDraft> productDrafts) {

        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<ProductDraft> validDrafts =  productDrafts
            .stream()
            .filter(this::isValidProductDraft)
            .peek(productDraft -> collectReferencedKeys(referencedKeys, productDraft))
            .collect(Collectors.toSet());
        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidProductDraft(@Nullable final ProductDraft productDraft) {
        if (productDraft == null) {
            handleError(PRODUCT_DRAFT_IS_NULL);
        } else if (isBlank(productDraft.getKey())) {
            handleError(format(PRODUCT_DRAFT_KEY_NOT_SET, productDraft.getName()));
        } else {
            final List<String> draftErrors = getVariantDraftErrorsInAllVariants(productDraft);
            if (!draftErrors.isEmpty()) {
                draftErrors.forEach(this::handleError);
            } else {
                return true;
            }
        }
        return false;
    }

    private void collectReferencedKeys(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ProductDraft productDraft) {

        collectReferencedKeyFromResourceIdentifier(productDraft.getProductType(), referencedKeys.productTypeKeys::add);
        collectReferencedKeysInCategories(referencedKeys, productDraft);
        collectReferencedKeysInVariants(referencedKeys, productDraft);
        collectReferencedKeyFromResourceIdentifier(productDraft.getTaxCategory(),
            referencedKeys.taxCategoryKeys::add);
        collectReferencedKeyFromReference(productDraft.getState(), referencedKeys.stateKeys::add);
    }

    private void collectReferencedKeysInCategories(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ProductDraft productDraft)  {

        productDraft
            .getCategories()
            .stream()
            .filter(Objects::nonNull)
            .forEach(resourceIdentifier ->
                collectReferencedKeyFromResourceIdentifier(resourceIdentifier, referencedKeys.categoryKeys::add));
    }

    private void collectReferencedKeysInVariants(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ProductDraft productDraft) {

        getAllVariants(productDraft).forEach(variantDraft -> {
            collectReferencedKeysInPrices(referencedKeys, variantDraft);
            collectReferencedKeysInAttributes(referencedKeys, variantDraft);
            collectReferencedKeysFromAssetDrafts(variantDraft.getAssets(), referencedKeys.typeKeys::add);
        });
    }

    @Nonnull
    private List<ProductVariantDraft> getAllVariants(@Nonnull final ProductDraft productDraft) {
        final List<ProductVariantDraft> allVariants = new ArrayList<>();
        allVariants.add(productDraft.getMasterVariant());
        if (productDraft.getVariants() != null) {
            allVariants.addAll(
                productDraft.getVariants()
                            .stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
        }
        return allVariants;
    }

    private void collectReferencedKeysInPrices(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ProductVariantDraft variantDraft) {

        if (variantDraft.getPrices() == null) {
            return;
        }

        variantDraft
            .getPrices()
            .stream()
            .filter(Objects::nonNull)
            .forEach(priceDraft -> {
                collectReferencedKeyFromReference(priceDraft.getCustomerGroup(),
                    referencedKeys.customerGroupKeys::add);
                collectReferencedKeyFromResourceIdentifier(priceDraft.getChannel(),
                    referencedKeys.channelKeys::add);
                collectReferencedKeyFromCustomFieldsDraft(priceDraft.getCustom(),
                    referencedKeys.typeKeys::add);
            });
    }

    private void collectReferencedKeysInAttributes(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final ProductVariantDraft variantDraft) {
        referencedKeys.productKeys.addAll(getReferencedProductKeys(variantDraft));

        referencedKeys.categoryKeys.addAll(
            getReferencedKeysWithReferenceTypeId(variantDraft, Category.referenceTypeId()));

        referencedKeys.productTypeKeys.addAll(
            getReferencedKeysWithReferenceTypeId(variantDraft, ProductType.referenceTypeId()));

        referencedKeys.customObjectCompositeIdentifiers.addAll(
            getReferencedKeysWithReferenceTypeId(variantDraft, CustomObject.referenceTypeId()));
    }

    @Nonnull
    private List<String> getVariantDraftErrorsInAllVariants(@Nonnull final ProductDraft productDraft) {
        final List<String> errorMessages = new ArrayList<>();

        // don't filter the nulls
        final List<ProductVariantDraft> allVariants = new ArrayList<>();
        allVariants.add(productDraft.getMasterVariant());
        allVariants.addAll(productDraft.getVariants());

        for (int i = 0; i < allVariants.size(); i++) {
            errorMessages.addAll(getVariantDraftErrorsInAllVariants(allVariants.get(i),
                i, requireNonNull(productDraft.getKey())));
        }

        return errorMessages;
    }

    @Nonnull
    private List<String> getVariantDraftErrorsInAllVariants(@Nullable final ProductVariantDraft productVariantDraft,
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
    public static Set<String> getReferencedProductKeys(@Nonnull final ProductVariantDraft variantDraft) {
        return getReferencedKeysWithReferenceTypeId(variantDraft, Product.referenceTypeId());
    }

    private static Set<String> getReferencedKeysWithReferenceTypeId(
        @Nonnull final ProductVariantDraft variantDraft,
        @Nonnull final String referenceTypeId) {

        final List<AttributeDraft> attributeDrafts = variantDraft.getAttributes();
        if (attributeDrafts == null) {
            return emptySet();
        }
        return attributeDrafts
            .stream()
            .filter(Objects::nonNull)
            .map(attributeDraft ->
                getReferencedKeysWithReferenceTypeId(attributeDraft, referenceTypeId))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    @Nonnull
    private static Set<String> getReferencedKeysWithReferenceTypeId(
        @Nonnull final AttributeDraft attributeDraft,
        @Nonnull final String referenceTypeId) {

        final JsonNode attributeDraftValue = attributeDraft.getValue();
        if (attributeDraftValue == null) {
            return emptySet();
        }

        final List<JsonNode> allAttributeReferences = attributeDraftValue.findParents(REFERENCE_TYPE_ID_FIELD);

        return allAttributeReferences
            .stream()
            .filter(reference -> isReferenceOfType(reference, referenceTypeId))
            .map(reference -> reference.get(REFERENCE_ID_FIELD).asText())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public static class ReferencedKeys {
        private final Set<String> productKeys = new HashSet<>();
        private final Set<String> productTypeKeys = new HashSet<>();
        private final Set<String> categoryKeys = new HashSet<>();
        private final Set<String> taxCategoryKeys = new HashSet<>();
        private final Set<String> stateKeys = new HashSet<>();
        private final Set<String> typeKeys = new HashSet<>();
        private final Set<String> channelKeys = new HashSet<>();
        private final Set<String> customerGroupKeys = new HashSet<>();
        private final Set<String> customObjectCompositeIdentifiers = new HashSet<>();

        public Set<String> getProductKeys() {
            return productKeys;
        }

        public Set<String> getProductTypeKeys() {
            return productTypeKeys;
        }

        public Set<String> getCategoryKeys() {
            return categoryKeys;
        }

        public Set<String> getTaxCategoryKeys() {
            return taxCategoryKeys;
        }

        public Set<String> getStateKeys() {
            return stateKeys;
        }

        public Set<String> getTypeKeys() {
            return typeKeys;
        }

        public Set<String> getChannelKeys() {
            return channelKeys;
        }

        public Set<String> getCustomerGroupKeys() {
            return customerGroupKeys;
        }

        /**
         * Applies mapping of {@link Set}&lt;{@link String}&gt; identifiers
         * (collected from reference id fields of product `key-value-document` references) to {@link Set}&lt;
         * {@link CustomObjectCompositeIdentifier}&gt; to be used for caching purposes.
         *
         * <p>Note: Invalid identifiers and uuid formatted identifiers will be filtered out.
         * Validation handling will be part of the {@link VariantReferenceResolver}.
         *
         * @return a result set with valid identifiers mapped to {@link CustomObjectCompositeIdentifier}.
         */
        public Set<CustomObjectCompositeIdentifier> getCustomObjectCompositeIdentifiers() {
            if (!customObjectCompositeIdentifiers.isEmpty()) {
                return customObjectCompositeIdentifiers
                    .stream()
                    .map((identifierAsString) -> {
                        if (SyncUtils.isUuid(identifierAsString)) {
                            return null;
                        }
                        try {
                            return CustomObjectCompositeIdentifier.of(identifierAsString);
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(toSet());
            }

            return Collections.emptySet();
        }
    }
}
