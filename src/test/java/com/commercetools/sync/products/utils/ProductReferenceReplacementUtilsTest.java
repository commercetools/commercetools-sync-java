package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.commercetools.sync.commons.MockUtils.getAssetMockWithCustomFields;
import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.getPriceMockWithReferences;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductVariantMock;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductReferenceReplacementUtilsTest {

    @Test
    void replaceProductsReferenceIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceReferencesWhereExpanded() {
        final String resourceKey = "key";
        final ProductType productType = getProductTypeMock(resourceKey);
        final Reference<ProductType> productTypeReference =
            Reference.ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productType.getId(), productType);
        final Reference<ProductType> nonExpandedProductTypeReference = ProductType.referenceOfId(productType.getId());

        final TaxCategory taxCategory = getTaxCategoryMock(resourceKey);
        final Reference<TaxCategory> taxCategoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategory.getId(), taxCategory);
        final Reference<TaxCategory> nonExpandedTaxCategoryReference = TaxCategory.referenceOfId(taxCategory.getId());

        final State state = getStateMock(resourceKey);
        final Reference<State> stateReference =
            Reference.ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);
        final Reference<State> nonExpandedStateReference = State.referenceOfId(state.getId());

        final Channel channel = getChannelMock(resourceKey);

        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);
        final Price price = getPriceMockWithReferences(channelReference, null);

        final Type customType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final Asset asset1 =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), customType));
        final Asset asset2 =
            getAssetMockWithCustomFields(Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
                UUID.randomUUID().toString()));

        final ProductVariant productVariant = getProductVariantMock(singletonList(price), asList(asset1, asset2));

        final Product productWithNonExpandedProductType = getProductMock(singletonList(productVariant));

        when(productWithNonExpandedProductType.getProductType())
            .thenReturn(nonExpandedProductTypeReference);
        when(productWithNonExpandedProductType.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedProductType.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedTaxCategory = getProductMock(singletonList(productVariant));

        when(productWithNonExpandedTaxCategory.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedTaxCategory.getTaxCategory())
            .thenReturn(nonExpandedTaxCategoryReference);
        when(productWithNonExpandedTaxCategory.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedSate = getProductMock(singletonList(productVariant));

        when(productWithNonExpandedSate.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedSate.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedSate.getState()).thenReturn(nonExpandedStateReference);


        final List<Product> products =
            asList(productWithNonExpandedProductType, productWithNonExpandedTaxCategory, productWithNonExpandedSate);


        final List<ProductDraft> productDraftsWithKeysOnReferences = ProductReferenceReplacementUtils
            .replaceProductsReferenceIdsWithKeys(products);

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getProductType)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(productType.getId(), productType.getKey(),
                                                         productType.getKey());

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getTaxCategory)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(taxCategory.getKey(), taxCategory.getId(),
                                                         taxCategory.getKey());

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getState)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(state.getKey(), state.getKey(), state.getId());

        final String asset2CustomTypeId = asset2.getCustom().getType().getId();
        final String assetCustomTypeKey = customType.getKey();

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getMasterVariant)
                                                     .flatExtracting(ProductVariantDraft::getAssets)
                                                     .extracting(AssetDraft::getCustom)
                                                     .extracting(CustomFieldsDraft::getType)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(assetCustomTypeKey, asset2CustomTypeId,
                                                         assetCustomTypeKey, asset2CustomTypeId, assetCustomTypeKey,
                                                         asset2CustomTypeId);
    }

    @Test
    void replaceProductsReferenceIdsWithKeys_WithNullProducts_ShouldSkipNullProducts() {
        final String resourceKey = "key";
        final ProductType productType = getProductTypeMock(resourceKey);
        final Reference<ProductType> productTypeReference =
            Reference.ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productType.getId(), productType);
        final Reference<ProductType> nonExpandedProductTypeReference = ProductType.referenceOfId(productType.getId());

        final TaxCategory taxCategory = getTaxCategoryMock(resourceKey);
        final Reference<TaxCategory> taxCategoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategory.getId(), taxCategory);
        final Reference<TaxCategory> nonExpandedTaxCategoryReference = TaxCategory.referenceOfId(taxCategory.getId());

        final State state = getStateMock(resourceKey);
        final Reference<State> stateReference =
            Reference.ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);

        final Channel channel = getChannelMock(resourceKey);

        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);
        final Price price = getPriceMockWithReferences(channelReference, null);
        final ProductVariant productVariant = getProductVariantMock(singletonList(price));

        final Category category = getCategoryMock(resourceKey);
        final Reference<Category> categoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(Category.referenceTypeId(), category.getId(), category);

        final Product productWithNonExpandedProductType =
            getProductMock(singleton(categoryReference), null, singletonList(productVariant));

        when(productWithNonExpandedProductType.getProductType())
            .thenReturn(nonExpandedProductTypeReference);
        when(productWithNonExpandedProductType.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedProductType.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedTaxCategory = getProductMock(singletonList(productVariant));

        when(productWithNonExpandedTaxCategory.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedTaxCategory.getTaxCategory())
            .thenReturn(nonExpandedTaxCategoryReference);
        when(productWithNonExpandedTaxCategory.getState()).thenReturn(stateReference);

        final List<Product> products =
            asList(productWithNonExpandedProductType, productWithNonExpandedTaxCategory, null);


        final List<ProductDraft> productDraftsWithKeysOnReferences = ProductReferenceReplacementUtils
            .replaceProductsReferenceIdsWithKeys(products);

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getProductType)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(productType.getId(), productType.getKey());

        assertThat(productDraftsWithKeysOnReferences).flatExtracting(ProductDraft::getCategories)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(category.getKey());

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getTaxCategory)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(taxCategory.getKey(), taxCategory.getId());

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getState)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(state.getKey(), state.getKey());

        assertThat(productDraftsWithKeysOnReferences).extracting(ProductDraft::getMasterVariant)
                                                     .flatExtracting(ProductVariantDraft::getPrices)
                                                     .extracting(PriceDraft::getChannel)
                                                     .extracting(ResourceIdentifier::getId)
                                                     .containsExactly(channel.getKey(), channel.getKey());
    }

    @Test
    void replaceProductTypeReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String productTypeId = UUID.randomUUID().toString();
        final Reference<ProductType> productTypeReference = ProductType.referenceOfId(productTypeId);
        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(productTypeReference);

        final ResourceIdentifier<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNotNull();
        assertThat(productTypeReferenceWithKey.getId()).isEqualTo(productTypeId);
    }

    @Test
    void replaceProductTypeReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String productTypeKey = "productTypeKey";

        final ProductType productType = getProductTypeMock(productTypeKey);

        final Reference<ProductType> productTypeReference = Reference
            .ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productType.getId(), productType);

        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(productTypeReference);

        final ResourceIdentifier<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNotNull();
        assertThat(productTypeReferenceWithKey.getId()).isEqualTo(productTypeKey);
    }

    @Test
    void replaceProductTypeReferenceIdWithKey_WithNullReferences_ShouldReturnNull() {
        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(null);

        final ResourceIdentifier<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNull();
    }

    @Test
    void replaceTaxCategoryReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String taxCategoryId = UUID.randomUUID().toString();
        final Reference<TaxCategory> taxCategoryReference = TaxCategory.referenceOfId(taxCategoryId);
        final Product product = mock(Product.class);
        when(product.getTaxCategory()).thenReturn(taxCategoryReference);

        final ResourceIdentifier<TaxCategory> taxCategoryReferenceWithKey = ProductReferenceReplacementUtils
            .replaceTaxCategoryReferenceIdWithKey(product);

        assertThat(taxCategoryReferenceWithKey).isNotNull();
        assertThat(taxCategoryReferenceWithKey.getId()).isEqualTo(taxCategoryId);
    }

    @Test
    void replaceTaxCategoryReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String taxCategoryKey = "taxCategoryKey";
        final TaxCategory taxCategory = getTaxCategoryMock(taxCategoryKey);
        final Reference<TaxCategory> taxCategoryReference = Reference
            .ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategory.getId(), taxCategory);

        final Product product = mock(Product.class);
        when(product.getTaxCategory()).thenReturn(taxCategoryReference);

        final ResourceIdentifier<TaxCategory> taxCategoryReferenceWithKey = ProductReferenceReplacementUtils
            .replaceTaxCategoryReferenceIdWithKey(product);

        assertThat(taxCategoryReferenceWithKey).isNotNull();
        assertThat(taxCategoryReferenceWithKey.getId()).isEqualTo(taxCategoryKey);
    }

    @Test
    void replaceTaxCategoryReferenceIdWithKey_WithNullReference_ShouldReturnNull() {
        final Product product = mock(Product.class);
        when(product.getTaxCategory()).thenReturn(null);

        final ResourceIdentifier<TaxCategory> taxCategoryReferenceWithKey = ProductReferenceReplacementUtils
            .replaceTaxCategoryReferenceIdWithKey(product);

        assertThat(taxCategoryReferenceWithKey).isNull();
    }

    @Test
    void replaceStateReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String stateId = UUID.randomUUID().toString();
        final Reference<State> stateReference = State.referenceOfId(stateId);
        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(stateReference);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNotNull();
        assertThat(stateReferenceWithKey.getId()).isEqualTo(stateId);
    }

    @Test
    void replaceStateReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String stateKey = "stateKey";
        final State state = getStateMock(stateKey);
        final Reference<State> stateReference = Reference
            .ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);

        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(stateReference);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNotNull();
        assertThat(stateReferenceWithKey.getId()).isEqualTo(stateKey);
    }

    @Test
    void replaceStateReferenceIdWithKey_WithNullReferences_ShouldReturnNull() {
        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(null);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNull();
    }

    @Test
    void buildProductQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
        final ProductQuery productQuery = ProductReferenceReplacementUtils.buildProductQuery();
        assertThat(productQuery.expansionPaths())
            .containsExactly(ExpansionPath.of("productType"), ExpansionPath.of("taxCategory"),
                ExpansionPath.of("state"), ExpansionPath.of("masterData.staged.categories[*]"),
                ExpansionPath.of("masterData.staged.masterVariant.prices[*].channel"),
                ExpansionPath.of("masterData.staged.variants[*].prices[*].channel"),
                ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"),
                ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"),
                ExpansionPath.of("masterData.staged.masterVariant.attributes[*].value"),
                ExpansionPath.of("masterData.staged.variants[*].attributes[*].value"),
                ExpansionPath.of("masterData.staged.masterVariant.attributes[*].value[*]"),
                ExpansionPath.of("masterData.staged.variants[*].attributes[*].value[*]"),
                ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"),
                ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String categoryId = UUID.randomUUID().toString();
        final Set<Reference<Category>> categoryReferences = singleton(Category.referenceOfId(categoryId));
        final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(categoryReferences);

        final Product product = getProductMock(categoryReferences, categoryOrderHints);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

        assertThat(categoryReferencesWithKeys).extracting(ResourceIdentifier::getId)
                                              .containsExactlyInAnyOrder(categoryId);
        assertThat(categoryOrderHintsWithKeys).isEqualTo(product.getMasterData().getStaged().getCategoryOrderHints());
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithNonExpandedReferencesAndNoCategoryOrderHints_ShouldNotReplaceIds() {
        final String categoryId = UUID.randomUUID().toString();
        final Set<Reference<Category>> categoryReferences = singleton(Category.referenceOfId(categoryId));
        final Product product = getProductMock(categoryReferences, null);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

        assertThat(categoryReferencesWithKeys).extracting(ResourceIdentifier::getId)
                                              .containsExactlyInAnyOrder(categoryId);
        assertThat(categoryOrderHintsWithKeys).isEqualTo(product.getMasterData().getStaged().getCategoryOrderHints());
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String categoryKey = "categoryKey";
        final Category category = getCategoryMock(categoryKey);
        final Reference<Category> categoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(Category.referenceTypeId(), category.getId(), category);

        final Set<Reference<Category>> categoryReferences = singleton(categoryReference);
        final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(categoryReferences);

        final Product product = getProductMock(categoryReferences, categoryOrderHints);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

        assertThat(categoryReferencesWithKeys).containsExactlyInAnyOrderElementsOf(
            singleton(ResourceIdentifier.ofId(categoryKey)));

        assertThat(categoryOrderHintsWithKeys).isNotNull();
        assertThat(categoryOrderHintsWithKeys.getAsMap()).containsOnly(entry(categoryKey,
            product.getMasterData().getStaged().getCategoryOrderHints().getAsMap().get(category.getId())));
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithExpandedReferencesAndNoCategoryOrderHints_ShouldReplaceIds() {
        final String categoryKey = "categoryKey";
        final Category category = getCategoryMock(categoryKey);
        final Reference<Category> categoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(Category.referenceTypeId(), category.getId(), category);
        final Product product = getProductMock(singleton(categoryReference), null);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

        assertThat(categoryReferencesWithKeys).extracting(ResourceIdentifier::getId)
                                              .containsExactlyInAnyOrder(categoryKey);
        assertThat(categoryOrderHintsWithKeys).isNull();
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithExpandedReferencesAndSomeCategoryOrderHintsSet_ShouldReplaceIds() {
        final String categoryKey1 = "categoryKey1";
        final String categoryKey2 = "categoryKey2";

        final Category category1 = getCategoryMock(categoryKey1);
        final Category category2 = getCategoryMock(categoryKey2);

        final Reference<Category> categoryReference1 =
            Reference.ofResourceTypeIdAndIdAndObj(Category.referenceTypeId(), category1.getId(), category1);
        final Reference<Category> categoryReference2 =
            Reference.ofResourceTypeIdAndIdAndObj(Category.referenceTypeId(), category2.getId(), category2);

        final Set<Reference<Category>> categoryReferences = new HashSet<>();
        categoryReferences.add(categoryReference1);
        categoryReferences.add(categoryReference2);

        final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(singleton(categoryReference1));

        final Product product = getProductMock(categoryReferences, categoryOrderHints);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();


        assertThat(categoryReferencesWithKeys).extracting(ResourceIdentifier::getId)
                                              .containsExactlyInAnyOrder(categoryKey1, categoryKey2);

        assertThat(categoryOrderHintsWithKeys).isNotNull();
        assertThat(categoryOrderHintsWithKeys.getAsMap()).containsOnly(entry(categoryKey1,
            product.getMasterData().getStaged().getCategoryOrderHints().getAsMap().get(category1.getId())));
    }

    @Test
    void replaceCategoryReferencesIdsWithKeys_WithNoReferences_ShouldNotReplaceIds() {
        final Product product = getProductMock(Collections.emptySet(), null);

        final CategoryReferencePair categoryReferencePair =
            ProductReferenceReplacementUtils.replaceCategoryReferencesIdsWithKeys(product);

        assertThat(categoryReferencePair).isNotNull();

        final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
            categoryReferencePair.getCategoryResourceIdentifiers();
        final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();
        assertThat(categoryReferencesWithKeys).isEmpty();
        assertThat(categoryOrderHintsWithKeys).isNull();
    }

    @Nonnull
    private static Product getProductMock(@Nonnull final Set<Reference<Category>> references,
                                          @Nullable final CategoryOrderHints categoryOrderHints,
                                          @Nonnull final List<ProductVariant> productVariants) {
        final ProductData productData = mock(ProductData.class);
        mockProductDataCategories(references, categoryOrderHints, productData);
        mockProductDataVariants(productVariants, productData);
        return mockStagedProductData(productData);
    }

    @Nonnull
    private static Product getProductMock(@Nonnull final Set<Reference<Category>> references,
                                          @Nullable final CategoryOrderHints categoryOrderHints) {
        final ProductData productData = mock(ProductData.class);
        mockProductDataCategories(references, categoryOrderHints, productData);
        return mockStagedProductData(productData);
    }

    @Nonnull
    private static Product getProductMock(@Nonnull final List<ProductVariant> productVariants) {
        final ProductData productData = mock(ProductData.class);
        mockProductDataVariants(productVariants, productData);
        return mockStagedProductData(productData);
    }

    private static void mockProductDataCategories(@Nonnull final Set<Reference<Category>> references,
                                                  @Nullable final CategoryOrderHints categoryOrderHints,
                                                  @Nonnull final ProductData productData) {
        when(productData.getCategories()).thenReturn(references);
        when(productData.getCategoryOrderHints()).thenReturn(categoryOrderHints);
    }

    private static void mockProductDataVariants(@Nonnull final List<ProductVariant> productVariants,
                                                @Nonnull final ProductData productData) {
        if (!productVariants.isEmpty()) {
            final ProductVariant masterVariant = productVariants.get(0);
            final List<ProductVariant> variants = productVariants.subList(1, productVariants.size());

            when(productData.getMasterVariant()).thenReturn(masterVariant);
            when(productData.getVariants()).thenReturn(variants);
            when(productData.getAllVariants()).thenReturn(productVariants);
        }
    }

    @Nonnull
    private static CategoryOrderHints getCategoryOrderHintsMock(@Nonnull final Set<Reference<Category>> references) {
        final Map<String, String> categoryOrderHintMap = new HashMap<>();
        references.forEach(categoryReference -> categoryOrderHintMap.put(categoryReference.getId(), "0.1"));
        return CategoryOrderHints.of(categoryOrderHintMap);
    }

    @Nonnull
    private static Category getCategoryMock(@Nonnull final String key) {
        final Category category = mock(Category.class);
        when(category.getKey()).thenReturn(key);
        when(category.getId()).thenReturn(UUID.randomUUID().toString());
        return category;
    }

    @Nonnull
    private static ProductType getProductTypeMock(@Nonnull final String key) {
        final ProductType productType = mock(ProductType.class);
        when(productType.getKey()).thenReturn(key);
        when(productType.getId()).thenReturn(UUID.randomUUID().toString());
        return productType;
    }

    @Nonnull
    private static TaxCategory getTaxCategoryMock(@Nonnull final String key) {
        final TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn(key);
        when(taxCategory.getId()).thenReturn(UUID.randomUUID().toString());
        return taxCategory;
    }

    @Nonnull
    private static State getStateMock(@Nonnull final String key) {
        final State state = mock(State.class);
        when(state.getKey()).thenReturn(key);
        when(state.getId()).thenReturn(UUID.randomUUID().toString());
        return state;
    }

    @Nonnull
    private static Product mockStagedProductData(@Nonnull final ProductData productData) {
        final ProductCatalogData productCatalogData = mock(ProductCatalogData.class);
        when(productCatalogData.getStaged()).thenReturn(productData);

        final Product product = mock(Product.class);
        when(product.getMasterData()).thenReturn(productCatalogData);
        return product;
    }
}
