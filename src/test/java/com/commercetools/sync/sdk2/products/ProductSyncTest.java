package com.commercetools.sync.sdk2.products;

import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.ChannelService;
import com.commercetools.sync.sdk2.services.CustomObjectService;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.CustomerService;
import com.commercetools.sync.sdk2.services.ProductService;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import com.commercetools.sync.sdk2.services.StateService;
import com.commercetools.sync.sdk2.services.TaxCategoryService;
import com.commercetools.sync.sdk2.services.TypeService;
import com.commercetools.sync.sdk2.services.UnresolvedReferencesService;
import com.commercetools.sync.sdk2.services.impl.ProductServiceImpl;
import com.commercetools.sync.sdk2.services.impl.ProductTypeServiceImpl;
import io.vrap.rmf.base.client.ApiHttpException;
import io.vrap.rmf.base.client.ApiHttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class ProductSyncTest {

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithNoValidDrafts_ShouldCompleteWithoutAnyProcessing() {
    // preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(ctpClient).build();

    final ProductService productService = mock(ProductService.class);
    final ProductSync productSync =
        new ProductSync(
            productSyncOptions,
            productService,
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    final ProductDraft productDraftWithoutKey =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .name(LocalizedString.ofEnglish("name"))
            .slug(LocalizedString.ofEnglish("slug"))
            .variants(emptyList())
            .build();

    // test
    final ProductSyncStatistics statistics =
        productSync.sync(singletonList(productDraftWithoutKey)).toCompletableFuture().join();

    // assertion
    verifyNoMoreInteractions(ctpClient);
    verifyNoMoreInteractions(productService);
    assertThat(statistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = spy(new ProductServiceImpl(syncOptions));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.cacheKeysToIds(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new ApiHttpException(500, "", new ApiHttpHeaders());
                }));

    final ProductSync productSync =
        new ProductSync(
            syncOptions,
            productService,
            productTypeService,
            mock(CategoryService.class),
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    final ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(ApiHttpException.class);

    assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final ProjectApiRoot mockClient = mock(ProjectApiRoot.class);

    when(mockClient.graphql()).thenReturn(mock());
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(mockClient.graphql().post(any(GraphQLRequest.class))).thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new ApiHttpException(500, "", new ApiHttpHeaders());
                }));

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = spy(new ProductServiceImpl(syncOptions));
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));

    final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSync productSync =
        new ProductSync(
            syncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    final ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(ApiHttpException.class);

    assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductService productService = mock(ProductService.class);
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(productService.createProduct(any())).thenReturn(completedFuture(Optional.empty()));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion
    verify(spyProductSyncOptions).applyBeforeCreateCallback(any());
    verify(spyProductSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final ProductProjection mockedExistingProduct =
        ProductMixin.toProjection(
            readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class),
            ProductProjectionType.STAGED);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final ProductService productService = mock(ProductService.class);
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingProduct)));
    when(productService.updateProduct(any(), any()))
        .thenReturn(completedFuture(mockedExistingProduct));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
    when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
        .thenReturn(completedFuture(Optional.of(new HashMap<>())));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion
    verify(spyProductSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyProductSyncOptions, never()).applyBeforeCreateCallback(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithEmptyAttributeMetaDataMap_ShouldCallErrorCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key("productTypeKey").build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final ProductProjection mockedExistingProduct =
        ProductMixin.toProjection(
            readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class),
            ProductProjectionType.STAGED);
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = mock(ProductService.class);
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingProduct)));
    when(productService.updateProduct(any(), any()))
        .thenReturn(completedFuture(mockedExistingProduct));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
    when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
        .thenReturn(completedFuture(Optional.empty()));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "Failed to update Product with key: 'productKey1'. Reason: Failed to"
                + " fetch a productType for the product to build the products' attributes metadata.");

    AssertionsForStatistics.assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }
}
