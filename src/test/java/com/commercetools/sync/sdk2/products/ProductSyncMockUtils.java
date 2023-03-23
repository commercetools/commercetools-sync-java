package com.commercetools.sync.sdk2.products;

import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.sdk2.products.AssetUtils.createAssetDraft;
import static com.commercetools.sync.sdk2.products.utils.PriceUtils.createPriceDraft;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromInputStream;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifier;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.CustomerService;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: Migration needed - Please replace below services with package sdk2 services

public class ProductSyncMockUtils {
  public static final String PRODUCT_KEY_1_RESOURCE_PATH = "product-key-1.json";
  public static final String PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH =
      "product-key-1-no-attributes.json";
  public static final String PRODUCT_KEY_SPECIAL_CHARS_RESOURCE_PATH =
      "product-key-with-special-character.json";
  public static final String PRODUCT_KEY_1_CHANGED_RESOURCE_PATH = "product-key-1-changed.json";
  public static final String PRODUCT_KEY_1_CHANGED_ATTRIBUTES_RESOURCE_PATH =
      "product-key-1-changed-attributes.json";
  public static final String PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH =
      "product-key-1-with-prices.json";
  public static final String PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH =
      "product-key-1-changed-with-prices.json";
  public static final String PRODUCT_KEY_2_RESOURCE_PATH = "product-key-2.json";
  public static final String PRODUCT_WITH_VARS_RESOURCE_PATH = "product-with-variants.json";
  public static final String PRODUCT_NO_VARS_RESOURCE_PATH = "product-with-no-variants.json";
  public static final String PRODUCT_TYPE_RESOURCE_PATH = "product-type.json";
  public static final String PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH =
      "product-type-with-references.json";
  public static final String PRODUCT_TYPE_NO_KEY_RESOURCE_PATH = "product-type-no-key.json";
  public static final String PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH =
      "product-type-with-references-for-variant-attributes.json";
  public static final String CATEGORY_KEY_1_RESOURCE_PATH = "category-key-1.json";
  public static final String SIMPLE_PRODUCT_WITH_MASTER_VARIANT_RESOURCE_PATH =
      "simple-product-with-master-variant.json";
  public static final String SIMPLE_PRODUCT_WITH_MULTIPLE_VARIANTS_RESOURCE_PATH =
      "simple-product-with-multiple-variants.json";

  /**
   * Unfortunately, <a
   * href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
   * <i>Category Order Hints</i></a> in CTP platform is quite picky: it requires number values as a
   * string and only without trailing zeros and only in fixed point format.
   *
   * @see <a
   *     href="http://dev.commercetools.com/http-api-projects-products.html#category-order-hints">
   *     http://dev.commercetools.com/http-api-projects-products.html#category-order-hints</a>
   */
  private static final DecimalFormat ORDER_HINT_FORMAT;

  static {
    ORDER_HINT_FORMAT = new DecimalFormat();
    ORDER_HINT_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
    ORDER_HINT_FORMAT.setMaximumIntegerDigits(1);
    DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
    formatSymbols.setDecimalSeparator('.');
    formatSymbols.setGroupingSeparator('.');
    ORDER_HINT_FORMAT.setDecimalFormatSymbols(formatSymbols);
  }

  /**
   * Builds a {@link ProductDraftBuilder} based on the staged projection of the product JSON
   * resource located at the {@code jsonResourcePath} and based on the supplied {@code productType}.
   *
   * @param jsonResourcePath the path of the JSON resource to build the product draft from.
   * @param productTypeResourceIdentifier the reference of the product type that the product draft
   *     belongs to.
   * @return a {@link ProductDraftBuilder} instance containing the data from the current projection
   *     of the specified JSON resource and the product type.
   */
  public static ProductDraftBuilder createProductDraftBuilder(
      @Nonnull final String jsonResourcePath,
      @Nonnull final ProductTypeResourceIdentifier productTypeResourceIdentifier) {
    final ProductProjection stagedProductData = createProductFromJson(jsonResourcePath);

    @SuppressWarnings("ConstantConditions")
    final List<ProductVariantDraft> allVariants =
        stagedProductData.getAllVariants().stream()
            .map(productVariant -> createProductVariantDraftBuilder(productVariant).build())
            .collect(toList());

    final ProductVariantDraft masterVariant = allVariants.stream().findFirst().orElse(null);
    final List<ProductVariantDraft> variants =
        allVariants.stream().skip(1).collect(Collectors.toList());

    return ProductDraftBuilder.of()
        .productType(productTypeResourceIdentifier)
        .name(stagedProductData.getName())
        .slug(stagedProductData.getSlug())
        .masterVariant(masterVariant)
        .variants(variants)
        .metaDescription(stagedProductData.getMetaDescription())
        .metaKeywords(stagedProductData.getMetaKeywords())
        .metaTitle(stagedProductData.getMetaTitle())
        .description(stagedProductData.getDescription())
        .searchKeywords(stagedProductData.getSearchKeywords())
        .taxCategory(
            TaxCategoryResourceIdentifierBuilder.of()
                .id(stagedProductData.getTaxCategory().getId())
                .build())
        .key(stagedProductData.getKey())
        .categories(
            stagedProductData.getCategories().stream()
                .map(
                    categoryReference ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryReference.getId())
                            .build())
                .collect(toList()))
        .categoryOrderHints(stagedProductData.getCategoryOrderHints())
        .publish(stagedProductData.getPublished());
  }

  /**
   * Given a {@link Set} of {@link CategoryResourceIdentifier}, this method returns an instance of
   * {@link CategoryOrderHints} containing a {@link Map}, in which each entry has category id from
   * the supplied {@link Set} as a key and a random categoryOrderHint which is a {@link String}
   * containing a random double value between 0 and 1 (exclusive).
   *
   * <p>Note: The random double value is generated by the {@link ThreadLocalRandom#current()}
   * nextDouble method.
   *
   * @param categoryResourceIdentifiers set of resource identifiers of categories to build
   *     categoryOrderHints for.
   * @return an instance of {@link CategoryOrderHints} containing a categoryOrderHint for each
   *     category in the supplied set of category resource identifiers.
   */
  public static CategoryOrderHints createRandomCategoryOrderHints(
      @Nonnull final Set<CategoryResourceIdentifier> categoryResourceIdentifiers) {

    final List<CategoryReference> references =
        categoryResourceIdentifiers.stream()
            .map(
                categoryResourceIdentifier ->
                    CategoryReferenceBuilder.of().id(categoryResourceIdentifier.getId()).build())
            .collect(toList());
    return createRandomCategoryOrderHints(references);
  }

  /**
   * Given a {@link List} of {@link CategoryReference}, this method returns an instance of {@link
   * CategoryOrderHints} containing a {@link Map}, in which each entry has category id from the
   * supplied {@link List} as a key and a random categoryOrderHint which is a {@link String}
   * containing a random double value between 0 and 1 (exclusive).
   *
   * <p>Note: The random double value is generated by the {@link ThreadLocalRandom#current()}
   * nextDouble method.
   *
   * @param categoryResources list of references of categories to build categoryOrderHints for.
   * @return an instance of {@link CategoryOrderHints} containing a categoryOrderHint for each
   *     category in the supplied list of categories.
   */
  public static CategoryOrderHints createRandomCategoryOrderHints(
      @Nonnull final List<CategoryReference> categoryResources) {

    final Map<String, String> categoryOrderHints = new HashMap<>();
    categoryResources.forEach(
        resourceIdentifier -> {
          final double randomDouble = ThreadLocalRandom.current().nextDouble(1e-8, 1);
          categoryOrderHints.put(
              resourceIdentifier.getId(), ORDER_HINT_FORMAT.format(randomDouble));
        });
    return CategoryOrderHintsBuilder.of().values(categoryOrderHints).build();
  }

  public static ProductVariantDraftBuilder createProductVariantDraftBuilder(
      final ProductVariant productVariant) {
    List<AssetDraft> assetDrafts = createAssetDraft(productVariant.getAssets());
    List<PriceDraft> priceDrafts = createPriceDraft(productVariant.getPrices());
    List<Attribute> attributes = createAttributes(productVariant.getAttributes());
    return ProductVariantDraftBuilder.of()
        .assets(assetDrafts)
        .attributes(attributes)
        .images(productVariant.getImages())
        .prices(priceDrafts)
        .sku(productVariant.getSku())
        .key(productVariant.getKey());
  }

  public static List<Attribute> createAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .map(attribute -> AttributeBuilder.of(attribute).build())
        .collect(Collectors.toList());
  }

  public static ProductDraft createProductDraft(
      @Nonnull final String jsonResourcePath,
      @Nonnull final ProductTypeResourceIdentifier productTypeReference,
      @Nullable final TaxCategoryResourceIdentifier taxCategoryReference,
      @Nullable final StateResourceIdentifier stateReference,
      @Nonnull final List<CategoryResourceIdentifier> categoryResourceIdentifiers,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    return createProductDraftBuilder(jsonResourcePath, productTypeReference)
        .taxCategory(taxCategoryReference)
        .state(stateReference)
        .categories(categoryResourceIdentifiers)
        .categoryOrderHints(categoryOrderHints)
        .build();
  }

  /**
   * Builds a {@link ProductDraft} based on the current projection of the product JSON resource
   * located at the {@code jsonResourcePath} and based on the supplied {@code productType}, {@code
   * taxCategoryReference} and {@code stateReference}. The method also attaches the created {@link
   * ProductDraft} to all the {@code categories} specified and assigns {@code categoryOrderHints}
   * for it for each category assigned.
   *
   * @param jsonResourcePath the path of the JSON resource to build the product draft from.
   * @param productTypeReference the reference of the product type that the product draft belongs
   *     to.
   * @param categoryReferences the references to the categories to attach this product draft to.
   * @param categoryOrderHints the categoryOrderHint for each category this product belongs to.
   * @return a {@link ProductDraft} instance containing the data from the current projection of the
   *     specified JSON resource and the product type. The draft would be assigned also to the
   *     specified {@code categories} with the supplied {@code categoryOrderHints}.
   */
  public static ProductDraft createProductDraft(
      @Nonnull final String jsonResourcePath,
      @Nonnull final ProductTypeReference productTypeReference,
      @Nonnull final TaxCategoryReference taxCategoryReference,
      @Nonnull final StateReference stateReference,
      @Nonnull final List<CategoryReference> categoryReferences,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    ProductTypeResourceIdentifier productTypeRI =
        ProductTypeResourceIdentifierBuilder.of().id(productTypeReference.getId()).build();
    return createProductDraftBuilder(jsonResourcePath, productTypeRI)
        .taxCategory(
            TaxCategoryResourceIdentifierBuilder.of().id(taxCategoryReference.getId()).build())
        .state(StateResourceIdentifierBuilder.of().id(stateReference.getId()).build())
        .categories(
            categoryReferences.stream()
                .map(
                    categoryReference ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryReference.getId())
                            .build())
                .collect(toList()))
        .categoryOrderHints(categoryOrderHints)
        .build();
  }

  public static ProductProjection createProductFromJson(@Nonnull final String jsonResourcePath) {
    final Product productFromJson = createObjectFromResource(jsonResourcePath, Product.class);
    return new ProductToProductProjectionWrapper(productFromJson, true);
  }

  public static ProductDraft createProductDraftFromJson(@Nonnull final String jsonResourcePath) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(jsonResourcePath);
    return fromInputStream(resourceAsStream, ProductDraft.class);
  }

  public static <T> T createObjectFromResource(
      final String resourcePath, final Class<T> objectType) {
    final InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    final T resourceFromJson = fromInputStream(resourceAsStream, objectType);
    return resourceFromJson;
  }

  /**
   * Creates a mock {@link ProductTypeService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link ProductTypeService#fetchCachedProductTypeId(String)}
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
   * Creates a mock {@link TaxCategoryService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link TaxCategoryService#fetchCachedTaxCategoryId(String)}
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
   * Creates a mock {@link StateService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link StateService#fetchCachedStateId(String)}
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
   * Creates a mock {@link CustomerGroup} with the supplied {@code id} and {@code key}.
   *
   * @param id the id of the created mock {@link CustomerGroup}.
   * @param key the key of the created mock {@link CustomerGroup}.
   * @return a mock customerGroup with the supplied id and key.
   */
  public static CustomerGroup getMockCustomerGroup(final String id, final String key) {
    final CustomerGroup customerGroup = mock(CustomerGroup.class);
    when(customerGroup.getId()).thenReturn(id);
    when(customerGroup.getKey()).thenReturn(key);
    return customerGroup;
  }

  /**
   * Creates a mock {@link CustomerGroupService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link CustomerGroupService#fetchCachedCustomerGroupId(String)}
   * </ul>
   *
   * @return the created mock of the {@link CustomerGroupService}.
   */
  public static CustomerGroupService getMockCustomerGroupService(
      @Nonnull final CustomerGroup customerGroup) {
    final String customerGroupId = customerGroup.getId();

    final CustomerGroupService customerGroupService = mock(CustomerGroupService.class);
    when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
        .thenReturn(completedFuture(Optional.of(customerGroupId)));
    return customerGroupService;
  }

  /**
   * Creates a mock {@link ProductService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link ProductService#getIdFromCacheOrFetch(String)}
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
   * Creates a mock {@link CategoryService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link CategoryService#fetchCachedCategoryId(String)}
   * </ul>
   *
   * @return the created mock of the {@link CategoryService}.
   */
  public static CategoryService getMockCategoryService(@Nonnull final String id) {
    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchCachedCategoryId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
    return categoryService;
  }

  /**
   * Creates a mock {@link Price} with the supplied {@link Channel} {@link Reference}, {@link
   * CustomerGroup} {@link Reference}, and custom {@link Type} {@link Reference}.
   *
   * <p>If the supplied {@code customTypeReference} is {@code null}, no custom fields are stubbed on
   * the resulting price mock.
   *
   * @param channelReference the channel reference to attach on the mock {@link Price}.
   * @param customTypeReference the custom type reference to attach on the mock {@link Price}.
   * @param customerGroupReference the custom type reference to attach on the mock {@link Price}.
   * @return a mock price with the supplied references.
   */
  @Nonnull
  public static Price getPriceMockWithReferences(
      @Nullable final ChannelReference channelReference,
      @Nullable final TypeReference customTypeReference,
      @Nullable final CustomerGroupReference customerGroupReference) {
    final Price price = mock(Price.class);
    when(price.getChannel()).thenReturn(channelReference);
    when(price.getCustomerGroup()).thenReturn(customerGroupReference);

    return ofNullable(customTypeReference)
        .map(
            typeReference -> {
              // If type reference is supplied, mock Custom with expanded type reference.
              final CustomFields mockCustomFields = mock(CustomFields.class);
              when(mockCustomFields.getType()).thenReturn(customTypeReference);
              when(price.getCustom()).thenReturn(mockCustomFields);
              return price;
            })
        .orElse(price);
  }

  /**
   * Creates a mock {@link ProductVariant} with the supplied {@link Price} {@link List}.
   *
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
   * Creates a mock {@link ProductVariant} with the supplied {@link Price} and {@link Asset} {@link
   * List}.
   *
   * @param prices the prices to attach on the mock {@link ProductVariant}.
   * @param assets the assets to attach on the mock {@link ProductVariant}.
   * @return a mock product variant with the supplied prices and assets.
   */
  @Nonnull
  public static ProductVariant getProductVariantMock(
      @Nonnull final List<Price> prices, @Nonnull final List<Asset> assets) {
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getPrices()).thenReturn(prices);
    when(productVariant.getAssets()).thenReturn(assets);
    return productVariant;
  }

  /**
   * Creates a mock {@link Channel} with the supplied {@code key}..
   *
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
   * Creates an {@link Attribute} with the supplied {@code attributeName} and {@code references}.
   *
   * @param attributeName the name to set on the {@link Attribute}.
   * @param references the references to set on the {@link Attribute}.
   * @return an {@link Attribute} with the supplied {@code attributeName} and {@code references}.
   */
  @Nonnull
  public static Attribute getReferenceSetAttributeDraft(
      @Nonnull final String attributeName, @Nonnull final ObjectNode... references) {
    final ArrayNode referenceSet = JsonNodeFactory.instance.arrayNode();
    referenceSet.addAll(Arrays.asList(references));
    return AttributeBuilder.of().name(attributeName).value(referenceSet).build();
  }

  /**
   * Creates an {@link ObjectNode} that represents a product reference with a random uuid in the id
   * field.
   *
   * @return an {@link ObjectNode} that represents a product reference with a random uuid in the id
   *     field.
   */
  @Nonnull
  public static ObjectNode getProductReferenceWithRandomId() {
    final ObjectNode productReference = JsonNodeFactory.instance.objectNode();
    productReference.put("typeId", "product");
    productReference.put("id", UUID.randomUUID().toString());
    return productReference;
  }

  /**
   * Creates an {@link ObjectNode} that represents a product reference with the supplied {@code id}
   * in the id field.
   *
   * @return an {@link ObjectNode} that represents a product reference with the supplied {@code id}
   *     in the id field.
   */
  @Nonnull
  public static ObjectNode getProductReferenceWithId(@Nonnull final String id) {
    return createReferenceObject(id, Product.typeReference().getType().getTypeName());
  }

  /**
   * Creates an {@link ObjectNode} that represents a reference with the supplied {@code id} in the
   * id field and {@code typeId} field in the typeId field.
   *
   * @return an {@link ObjectNode} that represents a product reference with the supplied {@code id}
   *     in the id field and {@code typeId} field in the typeId field.
   */
  @Nonnull
  public static ObjectNode createReferenceObject(
      @Nonnull final String id, @Nonnull final String typeId) {
    final ObjectNode reference = JsonNodeFactory.instance.objectNode();
    reference.put(REFERENCE_TYPE_ID_FIELD, typeId);
    reference.put(REFERENCE_ID_FIELD, id);
    return reference;
  }

  @Nonnull
  public static ProductDraftBuilder getBuilderWithProductTypeRefKey(@Nullable final String refKey) {
    return ProductDraftBuilder.of()
        .productType(ProductTypeResourceIdentifierBuilder.of().key(refKey).build())
        .name(LocalizedString.ofEnglish("testName"))
        .slug(LocalizedString.ofEnglish("testSlug"));
  }

  @Nonnull
  public static ProductDraftBuilder getBuilderWithRandomProductType() {
    return getBuilderWithProductTypeRefKey("anyKey");
  }

  /**
   * Creates a mock {@link CustomObjectService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link CustomObjectService#fetchCachedCustomObjectId}
   * </ul>
   *
   * @return the created mock of the {@link CustomObjectService}.
   */
  public static CustomObjectService getMockCustomObjectService(@Nonnull final String id) {
    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchCachedCustomObjectId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
    return customObjectService;
  }

  /**
   * Creates a mock {@link CustomerService} that returns a completed {@link CompletableFuture}
   * containing an {@link Optional} containing the id of the supplied value whenever the following
   * method is called on the service:
   *
   * <ul>
   *   <li>{@link CustomerService#fetchCachedCustomerId(String)}
   * </ul>
   *
   * @return the created mock of the {@link CustomerService}.
   */
  public static CustomerService getMockCustomerService(@Nonnull final String id) {
    final CustomerService customerService = mock(CustomerService.class);
    when(customerService.fetchCachedCustomerId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(id)));
    return customerService;
  }
}
