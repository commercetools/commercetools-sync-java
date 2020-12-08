package com.commercetools.sync.products;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.utils.CompletableFutureUtils;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ProductSyncWithReferencedProductsInAnyOrderTest {
    private ProductSyncOptions syncOptions;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> actions;

    private List<UpdateAction<Product>> collectActions(@Nonnull final List<UpdateAction<Product>> actions,
                                                       @Nonnull final ProductDraft productDraft,
                                                       @Nonnull final Product product) {
        this.actions.addAll(actions);
        return actions;
    }

    @BeforeEach
    void setUp() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        actions = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Test
    void sync_withFailToFetchCustomObject_shouldSyncCorrectly() {
        // preparation
        final String productReferenceAttributeName = "product-reference";
        final String parentProductKey = "parent-product-key";

        final AttributeDraft productReferenceAttribute = AttributeDraft
                .of(productReferenceAttributeName, Reference.of(Product.referenceTypeId(), parentProductKey));

        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = ProductSyncOptionsBuilder
                .of(ctpClient)
                .errorCallback((syncException, draft, product, updateActions) -> {
                    errorCallBackMessages.add(syncException.getMessage());
                    errorCallBackExceptions.add(syncException);
                })
                .beforeUpdateCallback(this::collectActions)
                .build();

        final Product mockProduct = buildMockProduct();

        final ProductType mockProductType = readObjectFromResource(PRODUCT_TYPE_RESOURCE_PATH, ProductType.class);
        when(mockProduct.getProductType()).thenReturn(mockProductType.toReference());

        final Product mockCreatedProduct = buildCreatedMockProduct(parentProductKey);
        when(mockCreatedProduct.getProductType()).thenReturn(mockProductType.toReference());

        final ProductService mockProductService = mock(ProductService.class);
        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockProduct)));
        when(mockProductService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
        when(mockProductService.createProduct(any())).thenReturn(completedFuture(Optional.of(mockCreatedProduct)));

        final CategoryService mockCategoryService = mock(CategoryService.class);
        when(mockCategoryService.fetchMatchingCategoriesByKeys(anySet())).thenReturn(completedFuture(emptySet()));

        final ProductDraft childDraft1 = ProductDraftBuilder
                .of(mockProductType, ofEnglish("foo"), ofEnglish("foo-slug"),
                        ProductVariantDraftBuilder
                                .of()
                                .key("foo")
                                .sku("foo")
                                .attributes(productReferenceAttribute)
                                .build())
                .key(mockProduct.getKey())
                .build();

        final ProductDraft parentDraft = ProductDraftBuilder
                .of(mockProductType, ofEnglish(parentProductKey), ofEnglish(parentProductKey),
                        ProductVariantDraftBuilder
                                .of()
                                .sku(parentProductKey)
                                .key(parentProductKey)
                                .build())
                .key(parentProductKey)
                .build();

        final WaitingToBeResolved mockWaitingToBeResolved = mock(WaitingToBeResolved.class);
        final Set<String> missingReferencedProductKeySet = new HashSet<>();
        missingReferencedProductKeySet.add("parent-product-key");
        missingReferencedProductKeySet.add("parent-product-key2");
        when(mockWaitingToBeResolved.getProductDraft()).thenReturn(childDraft1);
        when(mockWaitingToBeResolved.getMissingReferencedProductKeys()).thenReturn(missingReferencedProductKeySet);
        final UnresolvedReferencesService mockUnresolvedReferencesService = mock(UnresolvedReferencesService.class);
        when(mockUnresolvedReferencesService.fetch(anySet()))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(
                        new CompletionException(new BadGatewayException("failed to respond."))));
        when(mockUnresolvedReferencesService.save(any()))
                .thenReturn(completedFuture(Optional.of(mockWaitingToBeResolved)));

        // test
        // final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSync productSync = new ProductSync(spy(syncOptions), mockProductService,
                mock(ProductTypeService.class), mockCategoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class),
                mockUnresolvedReferencesService,
                mock(CustomObjectService.class));

        final ProductSyncStatistics syncStatistics = productSync
                .sync(singletonList(childDraft1))
                .thenCompose(ignoredResult -> productSync.sync(singletonList(parentDraft)))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(2, 1, 0, 1, 0);
        assertThat(errorCallBackMessages)
                .containsExactly("Failed to fetch ProductDrafts waiting to be resolved with keys '[foo]'.");
        assertThat(errorCallBackExceptions)
                .singleElement().satisfies(exception ->
                        assertThat(exception.getCause()).hasCauseExactlyInstanceOf(BadGatewayException.class));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Nonnull
    private ProductData buildMockProductData() {
        final ProductData mockProductData = mock(ProductData.class);
        final ProductVariant mockProductVariant = mock(ProductVariant.class);

        when(mockProductVariant.getKey()).thenReturn("foo");
        when(mockProductVariant.getSku()).thenReturn("foo");

        when(mockProductData.getName()).thenReturn(LocalizedString.ofEnglish("foo"));
        when(mockProductData.getSlug()).thenReturn(LocalizedString.ofEnglish("foo-slug"));
        when(mockProductData.getAllVariants()).thenReturn(emptyList());
        when(mockProductData.getMasterVariant()).thenReturn(mockProductVariant);

        return mockProductData;
    }

    @Nonnull
    private Product buildMockProduct() {
        final Product mockProduct = mock(Product.class);
        final ProductData mockProductData = buildMockProductData();

        final String productId = UUID.randomUUID().toString();

        when(mockProduct.getKey()).thenReturn("foo");
        when(mockProduct.getId()).thenReturn(productId);

        when(mockProduct.getMasterData()).thenReturn(mock(ProductCatalogData.class));
        when(mockProduct.getMasterData().getCurrent()).thenReturn(mockProductData);
        when(mockProduct.getMasterData().getStaged()).thenReturn(mockProductData);

        return mockProduct;
    }

    @Nonnull
    private Product buildCreatedMockProduct(@Nonnull final String parentProductKey) {
        final Product mockCreatedProduct = mock(Product.class);
        final ProductData mockCreatedProductData = mock(ProductData.class);
        final ProductVariant mockCreatedProductVariant = mock(ProductVariant.class);
        final String createdProductId = UUID.randomUUID().toString();
        final ProductData mockProductData = buildMockProductData();

        when(mockCreatedProductData.getName()).thenReturn(LocalizedString.ofEnglish(parentProductKey));
        when(mockCreatedProductData.getSlug()).thenReturn(LocalizedString.ofEnglish(parentProductKey));
        when(mockCreatedProductData.getAllVariants()).thenReturn(emptyList());
        when(mockCreatedProductData.getMasterVariant()).thenReturn(mockCreatedProductVariant);
        when(mockCreatedProductVariant.getKey()).thenReturn(parentProductKey);
        when(mockCreatedProductVariant.getSku()).thenReturn(parentProductKey);
        when(mockCreatedProduct.getKey()).thenReturn(parentProductKey);
        when(mockCreatedProduct.getId()).thenReturn(createdProductId);

        when(mockCreatedProduct.getMasterData()).thenReturn(mock(ProductCatalogData.class));
        when(mockCreatedProduct.getMasterData().getCurrent()).thenReturn(mockProductData);
        when(mockCreatedProduct.getMasterData().getStaged()).thenReturn(mockProductData);

        return mockCreatedProduct;
    }
}
