package com.commercetools.sync.products;

import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductSyncMockUtils {
    public static final String PRODUCT_KEY_1_RESOURCE_PATH = "product-key-1.json";
    public static final String PRODUCT_KEY_1_CHANGED_RESOURCE_PATH = "product-key-1-changed.json";
    public static final String PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH = "product-key-1-with-prices.json";
    public static final String PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH =
        "product-key-1-changed-with-prices.json";
    public static final String PRODUCT_KEY_2_RESOURCE_PATH = "product-key-2.json";
    public static final String PRODUCT_WITH_VARS_RESOURCE_PATH = "product-with-variants.json";
    public static final String PRODUCT_NO_VARS_RESOURCE_PATH = "product-with-no-variants.json";
    public static final String PRODUCT_TYPE_RESOURCE_PATH = "product-type.json";
    public static final String PRODUCT_TYPE_NO_KEY_RESOURCE_PATH = "product-type-no-key.json";
    public static final String CATEGORY_KEY_1_RESOURCE_PATH = "category-key-1.json";
    public static final String BOOLEAN_ATTRIBUTE = "boolean-attribute.json";
    public static final String TEXT_ATTRIBUTE = "text-attribute.json";
    public static final String LTEXT_ATTRIBUTE = "ltext-attribute.json";
    public static final String ENUM_ATTRIBUTE = "enum-attribute.json";
    public static final String LENUM_ATTRIBUTE = "lenum-attribute.json";
    public static final String NUMBER_ATTRIBUTE = "number-attribute.json";
    public static final String MONEY_ATTRIBUTE = "money-attribute.json";
    public static final String DATE_ATTRIBUTE = "date-attribute.json";
    public static final String TIME_ATTRIBUTE = "time-attribute.json";
    public static final String DATE_TIME_ATTRIBUTE = "datetime-attribute.json";
    public static final String PRODUCT_REFERENCE_ATTRIBUTE = "product-reference-attribute.json";
    public static final String CATEGORY_REFERENCE_ATTRIBUTE = "category-reference-attribute.json";
    public static final String PRODUCT_REFERENCE_SET_ATTRIBUTE = "product-reference-set-attribute.json";
    public static final String LTEXT_SET_ATTRIBUTE = "ltext-set-attribute.json";


    /**
     * Unfortunately, <a href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
     * <i>Category Order Hints</i></a> in CTP platform is quite picky: it requires number values as a string
     * and only without trailing zeros and only in fixed point format.
     *
     * @see <a href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
     * http://dev.commercetools.com/http-api-projects-products.html#category-order-hints</a>
     */
    private static final DecimalFormat ORDER_HINT_FORMAT;

    static {
        ORDER_HINT_FORMAT = new DecimalFormat();
        ORDER_HINT_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
        ORDER_HINT_FORMAT.setMaximumIntegerDigits(1);
    }

    /**
     * Builds a {@link ProductDraftBuilder} based on the staged projection of the product JSON resource located at the
     * {@code jsonResourcePath} and based on the supplied {@code productType}.
     * TODO: GITHUB ISSUE#152
     *
     * @param jsonResourcePath     the path of the JSON resource to build the product draft from.
     * @param productTypeReference the reference of the product type that the product draft belongs to.
     * @return a {@link ProductDraftBuilder} instance containing the data from the current projection of the specified
     *          JSON resource and the product type.
     */
    public static ProductDraftBuilder createProductDraftBuilder(@Nonnull final String jsonResourcePath,
                                                                @Nonnull final Reference<ProductType>
                                                                    productTypeReference) {
        final Product productFromJson = readObjectFromResource(jsonResourcePath, Product.class);
        final ProductData productData = productFromJson.getMasterData().getStaged();

        @SuppressWarnings("ConstantConditions") final List<ProductVariantDraft> allVariants = productData
            .getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());

        return ProductDraftBuilder
            .of(productTypeReference, productData.getName(), productData.getSlug(), allVariants)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .taxCategory(productFromJson.getTaxCategory())
            .state(productFromJson.getState())
            .key(productFromJson.getKey())
            .categories(
                productData.getCategories().stream().map(Reference::toResourceIdentifier).collect(Collectors.toSet()))
            .categoryOrderHints(productData.getCategoryOrderHints())
            .publish(productFromJson.getMasterData().isPublished());
    }


    /**
     * Given a {@link List} of {@link Category}, this method returns an instance of {@link CategoryOrderHints}
     * containing a {@link Map}, in which each entry has category id from the supplied {@link List} as a key and a
     * random categoryOrderHint which is a {@link String} containing a random double value between 0 and 1 (exclusive).
     *
     * <p>Note: The random double value is generated by the {@link ThreadLocalRandom#current()} nextDouble method.
     *
     * @param categoryResources list of references of categories to build categoryOrderHints for.
     * @return an instance of {@link CategoryOrderHints} containing a categoryOrderHint for each category in the
     *         supplied list of categories.
     */
    public static CategoryOrderHints createRandomCategoryOrderHints(@Nonnull final List<Reference<Category>>
                                                                        categoryResources) {
        final Map<String, String> categoryOrderHints = new HashMap<>();
        categoryResources.forEach(resourceIdentifier -> {
            final double randomDouble = ThreadLocalRandom.current().nextDouble(1e-8, 1);
            categoryOrderHints.put(resourceIdentifier.getId(), ORDER_HINT_FORMAT.format(randomDouble));
        });
        return CategoryOrderHints.of(categoryOrderHints);
    }

    /**
     * Builds a {@link ProductDraft} based on the current projection of the product JSON resource located at the
     * {@code jsonResourcePath} and based on the supplied {@code productType}, {@code taxCategoryReference} and
     * {@code stateReference}. The method also attaches the created {@link ProductDraft} to all the {@code categories}
     * specified and assigns {@code categoryOrderHints} for it for each category assigned.
     * TODO: GITHUB ISSUE#152
     *
     * @param jsonResourcePath     the path of the JSON resource to build the product draft from.
     * @param productTypeReference the reference of the  product type that the product draft belongs to.
     * @param categoryReferences   the references to the categories to attach this product draft to.
     * @param categoryOrderHints   the categoryOrderHint for each category this product belongs to.
     * @return a {@link ProductDraft} instance containing the data from the current projection of the specified
     *         JSON resource and the product type. The draft would be assigned also to the specified {@code categories}
     *         with the supplied {@code categoryOrderHints}.
     */
    public static ProductDraft createProductDraft(@Nonnull final String jsonResourcePath,
                                                  @Nonnull final Reference<ProductType> productTypeReference,
                                                  @Nullable final Reference<TaxCategory> taxCategoryReference,
                                                  @Nullable final Reference<State> stateReference,
                                                  @Nonnull final List<Reference<Category>> categoryReferences,
                                                  @Nullable final CategoryOrderHints categoryOrderHints) {
        return createProductDraftBuilder(jsonResourcePath, productTypeReference)
            .taxCategory(taxCategoryReference)
            .state(stateReference)
            .categories(categoryReferences)
            .categoryOrderHints(categoryOrderHints)
            .build();
    }

    public static Product createProductFromJson(@Nonnull final String jsonResourcePath) {
        return readObjectFromResource(jsonResourcePath, Product.class);
    }

    public static ProductDraft createProductDraftFromJson(@Nonnull final String jsonResourcePath) {
        return readObjectFromResource(jsonResourcePath, ProductDraft.class);
    }


    /**
     * Creates a mock {@link ProductTypeService} that returns a completed {@link CompletableFuture} containing an
     * {@link Optional} containing the id of the supplied value whenever the following method is called on the service:
     * <ul>
     * <li>{@link ProductTypeService#fetchCachedProductTypeId(String)}</li>
     * </ul>
     *
     * @return the created mock of the {@link ProductTypeService}.
     */
    public static ProductTypeService getMockProductTypeService(@Nonnull final String id) {
        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
        return productTypeService;
    }

    /**
     * Creates a mock {@link TaxCategoryService} that returns a completed {@link CompletableFuture} containing an
     * {@link Optional} containing the id of the supplied value whenever the following method is called on the service:
     * <ul>
     * <li>{@link TaxCategoryService#fetchCachedTaxCategoryId(String)}</li>
     * </ul>
     *
     * @return the created mock of the {@link TaxCategoryService}.
     */
    public static TaxCategoryService getMockTaxCategoryService(@Nonnull final String id) {
        final TaxCategoryService taxCategoryService = mock(TaxCategoryService.class);
        when(taxCategoryService.fetchCachedTaxCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
        return taxCategoryService;
    }

    /**
     * Creates a mock {@link StateService} that returns a completed {@link CompletableFuture} containing an
     * {@link Optional} containing the id of the supplied value whenever the following method is called on the service:
     * <ul>
     * <li>{@link StateService#fetchCachedStateId(String)}</li>
     * </ul>
     *
     * @return the created mock of the {@link StateService}.
     */
    public static StateService getMockStateService(@Nonnull final String id) {
        final StateService stateService = mock(StateService.class);
        when(stateService.fetchCachedStateId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
        return stateService;
    }

    /**
     * Creates a mock {@link ProductService} that returns a completed {@link CompletableFuture} containing an
     * {@link Optional} containing the id of the supplied value whenever the following method is called on the service:
     * <ul>
     * <li>{@link ProductService#getIdFromCacheOrFetch(String)}</li>
     * </ul>
     *
     * @return the created mock of the {@link ProductService}.
     */
    public static ProductService getMockProductService(@Nonnull final String id) {
        final ProductService productService = mock(ProductService.class);
        when(productService.getIdFromCacheOrFetch(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
        return productService;
    }

    /**
     * Creates a mock {@link Price} with the supplied {@link Channel} {@link Reference}.
     * @param channelReference the channel reference to attach on the mock {@link Price}.
     * @return a mock price with the supplied channel reference.
     */
    @Nonnull
    public static Price getPriceMockWithChannelReference(@Nullable final Reference<Channel> channelReference) {
        final Price price = mock(Price.class);
        when(price.getChannel()).thenReturn(channelReference);
        return price;
    }

    @Nonnull
    public static Price getPriceMockWithReferences(@Nullable final Reference<Channel> channelReference, @Nullable final Reference<Type> typeReference) {
        // Mock Custom with expanded type reference
        final CustomFields mockCustomFields = mock(CustomFields.class);
        when(mockCustomFields.getType()).thenReturn(typeReference);

        final Price price = mock(Price.class);
        when(price.getChannel()).thenReturn(channelReference);
        when(price.getCustom()).thenReturn(mockCustomFields);
        return price;
    }

    /**
     * Creates a mock {@link ProductVariant} with the supplied {@link Price} {@link List}.
     * @param prices the prices to attach on the mock {@link ProductVariant}.
     * @return a mock product variant with the supplied prices.
     */
    @Nonnull
    public static ProductVariant getProductVariantMock(@Nonnull final List<Price> prices) {
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getPrices()).thenReturn(prices);
        return productVariant;
    }

    /**
     * Creates a mock {@link ProductVariant} with the supplied {@link Price} and {@link Asset} {@link List}.
     * @param prices the prices to attach on the mock {@link ProductVariant}.
     * @param assets the assets to attach on the mock {@link ProductVariant}.
     * @return a mock product variant with the supplied prices and assets.
     */
    @Nonnull
    public static ProductVariant getProductVariantMock(@Nonnull final List<Price> prices,
                                                       @Nonnull final List<Asset> assets) {
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getPrices()).thenReturn(prices);
        when(productVariant.getAssets()).thenReturn(assets);
        return productVariant;
    }

    /**
     * Creates a mock {@link Channel} with the supplied {@code key}..
     * @param key the key to to set on the mock {@link Channel}.
     * @return a mock channel with the supplied key.
     */
    @Nonnull
    public static Channel getChannelMock(@Nonnull final String key) {
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(key);
        when(channel.getId()).thenReturn(UUID.randomUUID().toString());
        return channel;
    }

    /**
     * Creates an {@link AttributeDraft} with the supplied {@code attributeName} and {@code references}.
     * @param attributeName the name to set on the {@link AttributeDraft}.
     * @param references the references to set on the {@link AttributeDraft}.
     * @return an {@link AttributeDraft} with the supplied {@code attributeName} and {@code references}.
     */
    @Nonnull
    public static AttributeDraft getProductReferenceSetAttributeDraft(@Nonnull final String attributeName,
                                                                      @Nonnull final ObjectNode... references) {
        final ArrayNode referenceSet = JsonNodeFactory.instance.arrayNode();
        referenceSet.addAll(Arrays.asList(references));
        return AttributeDraft.of(attributeName, referenceSet);
    }

    /**
     * Creates an {@link ObjectNode} that represents a product reference with a random uuid in the id field.
     * @return an {@link ObjectNode} that represents a product reference with a random uuid in the id field.
     */
    @Nonnull
    public static ObjectNode getProductReferenceWithRandomId() {
        final ObjectNode productReference = JsonNodeFactory.instance.objectNode();
        productReference.put("typeId", "product");
        productReference.put("id", UUID.randomUUID().toString());
        return productReference;
    }

    /**
     * Creates an {@link ObjectNode} that represents a product reference with the  supplied {@code id} in the id field.
     * @return an {@link ObjectNode} that represents a product reference with the  supplied {@code id} in the id field.
     */
    @Nonnull
    public static ObjectNode getProductReferenceWithId(@Nonnull final String id) {
        final ObjectNode productReference = JsonNodeFactory.instance.objectNode();
        productReference.put("typeId", "product");
        productReference.put("id", id);
        return productReference;
    }

    @Nonnull
    public static ProductDraftBuilder getBuilderWithProductTypeRefId(@Nonnull final String refId) {
        return ProductDraftBuilder.of(ProductType.referenceOfId(refId),
            LocalizedString.ofEnglish("testName"),
            LocalizedString.ofEnglish("testSlug"),
            (ProductVariantDraft)null);
    }

    @Nonnull
    public static ProductDraftBuilder getBuilderWithRandomProductTypeUuid() {
        return getBuilderWithProductTypeRefId(UUID.randomUUID().toString());
    }

    @Nonnull
    public static ProductDraftBuilder getBuilderWithProductTypeRef(
        @Nonnull final Reference<ProductType> reference) {
        return ProductDraftBuilder.of(reference,
            LocalizedString.ofEnglish("testName"),
            LocalizedString.ofEnglish("testSlug"),
            (ProductVariantDraft)null);
    }
}
