package com.commercetools.sync.sdk2.producttypes;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.producttypes.MockBuilderUtils.createMockProductTypeBuilder;
import static com.commercetools.sync.sdk2.producttypes.MockBuilderUtils.createMockProductTypeDraftBuilder;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeMockUtils.getProductTypeBuilder;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ByProjectKeyGraphqlRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductTypesByIDRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.ByProjectKeyProductTypesPost;
import com.commercetools.api.client.ByProjectKeyProductTypesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeNestedTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeDescriptionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponseBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import com.commercetools.sync.sdk2.services.impl.ProductTypeServiceImpl;
import io.vrap.rmf.base.client.ApiHttpClient;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BaseException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class ProductTypeSyncTest {

  @Test
  void sync_WithEmptyAttributeDefinitions_ShouldSyncCorrectly() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        createMockProductTypeDraftBuilder()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(emptyList())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

    final ProductType existingProductType =
        createMockProductTypeBuilder().key(newProductTypeDraft.getKey()).build();

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockProductTypeService.updateProductType(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(existingProductType));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void sync_WithNullAttributeDefinitions_ShouldSyncCorrectly() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes((List<AttributeDefinitionDraft>) null)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final List<ProductTypeUpdateAction> actions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .beforeUpdateCallback(
                (generatedActions, draft, productType) -> {
                  actions.addAll(generatedActions);
                  return generatedActions;
                })
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

    final ProductType existingProductType = mock(ProductType.class);
    when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockProductTypeService.updateProductType(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(existingProductType));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(actions)
        .containsExactly(
            ProductTypeChangeNameActionBuilder.of().name(newProductTypeDraft.getName()).build(),
            ProductTypeChangeDescriptionActionBuilder.of()
                .description(newProductTypeDraft.getDescription())
                .build());
    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void sync_WithNullInAttributeDefinitions_ShouldSyncCorrectly() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(singletonList(null))
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

    final ProductType existingProductType = mock(ProductType.class);
    when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockProductTypeService.updateProductType(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(existingProductType));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(emptyList())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeService.class);

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to fetch existing product types with keys: '[foo]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BaseException.class);

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void sync_WithErrorsOnSyncing_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(emptyList())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

    final ProductType existingProductType = mock(ProductType.class);
    when(existingProductType.getKey()).thenReturn(null);

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));

    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "Failed to process the productTypeDraft with key:'foo'."
                + " Reason: java.lang.NullPointerException");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(NullPointerException.class);

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void
      sync_WithErrorCachingKeysButNoKeysToCache_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(emptyList())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProjectApiRoot projectApiRoot = buildClientWithBaseErrorOnExecute();
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = new ProductTypeServiceImpl(syncOptions);

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to fetch existing product types with keys: '[foo]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BaseException.class);

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  private ProjectApiRoot buildClientWithBaseErrorOnExecute() {
    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
    when(projectApiRoot.getApiHttpClient()).thenReturn(apiHttpClient);

    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  if (uri.contains("product-types")) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(new BaseException());
                  }
                  return apiHttpClient.execute(request);
                })
            .withApiBaseUrl("test")
            .build("test");
    return testClient;
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithInvalidAttributeDefinitions_ShouldThrowError() {
    // preparation
    final String nestedAttributeTypeId = "attributeId";
    final ProductTypeReference productTypeReferenceMock = mock();
    when(productTypeReferenceMock.getId()).thenReturn(nestedAttributeTypeId).thenReturn(null);

    final AttributeNestedType nestedAttributeType =
        AttributeNestedTypeBuilder.of().typeReference(productTypeReferenceMock).build();
    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedAttributeType)
            .name("validNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(nestedTypeAttrDefDraft)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);

    final ProductType existingProductType = mock(ProductType.class);
    when(existingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            singleton(newProductTypeDraft.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockProductTypeService.updateProductType(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(existingProductType));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages.get(0))
        .contains(
            "This exception is unexpectedly thrown since the draft batch has been"
                + "already validated for blank keys");
    assertThat(errorMessages.get(1))
        .contains("Failed to process the productTypeDraft with key:'foo'");
    assertThat(exceptions.size()).isEqualTo(2);

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 2, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithErrorUpdatingProductType_ShouldCallErrorCallback() {
    final String draftKey = "key2";

    // preparation
    final ProductTypeDraft newProductTypeDraft2 =
        ProductTypeDraftBuilder.of()
            .key(draftKey)
            .name("name")
            .description("desc")
            .attributes(emptyList())
            .build();
    final AttributeNestedType nestedTypeAttrDefDraft1 =
        AttributeNestedTypeBuilder.of()
            .typeReference(productTypeReferenceBuilder -> productTypeReferenceBuilder.id(draftKey))
            .build();
    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(nestedTypeAttrDefDraft1)
            .name("validNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final ProductTypeDraft newProductTypeDraft1 =
        ProductTypeDraftBuilder.of()
            .key("key1")
            .name("name")
            .description("desc")
            .attributes(singletonList(nestedTypeAttrDefDraft))
            .build();
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = mock(ProductTypeServiceImpl.class);
    final ProductType existingProductType = mock(ProductType.class);
    when(existingProductType.getKey()).thenReturn(newProductTypeDraft1.getKey());
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            newLinkedHashSet(newProductTypeDraft2.getKey(), newProductTypeDraft1.getKey())))
        .thenReturn(
            CompletableFuture.completedFuture(Collections.emptySet()),
            CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(
            newLinkedHashSet(newProductTypeDraft1.getKey())))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingProductType)));
    when(mockProductTypeService.fetchMatchingProductTypesByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockProductTypeService.updateProductType(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));
    when(mockProductTypeService.cacheKeysToIds(anySet()))
        .thenReturn(CompletableFuture.completedFuture(emptyMap()));
    when(mockProductTypeService.createProductType(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(existingProductType)));
    when(mockProductTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("key1")));

    // test
    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);
    productTypeSync
        .sync(List.of(newProductTypeDraft2, newProductTypeDraft1))
        .toCompletableFuture()
        .join();

    // assertions
    assertThat(errorMessages.get(0))
        .contains(
            "Failed to update product type with key: 'key1'. Reason: io.vrap.rmf.base.client.error.BaseException");
  }

  @Test
  void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final AttributeDefinitionDraft nestedTypeAttrDefDraft =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder -> productTypeReferenceBuilder.id("x")))
            .name("validNested")
            .label(ofEnglish("koko"))
            .isRequired(true)
            .build();

    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("foo")
            .name("name")
            .description("desc")
            .attributes(singletonList(nestedTypeAttrDefDraft))
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(projectApiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();

    final ProductTypeService mockProductTypeService = new ProductTypeServiceImpl(syncOptions);

    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(projectApiRoot.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    final ProductTypeSync productTypeSync =
        new ProductTypeSync(syncOptions, mockProductTypeService);

    // test
    final ProductTypeSyncStatistics productTypeSyncStatistics =
        productTypeSync.sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BaseException.class);

    assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("newProductType")
            .name("productType")
            .description("a cool type")
            .attributes(emptyList())
            .build();

    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(projectApiRoot).build();

    final ProductTypeService productTypeService =
        new ProductTypeServiceImpl(productTypeSyncOptions);

    final String jsonStringProductTypes = "{\"data\":{\"productTypes\":{\"results\":[]}}}";
    final GraphQLResponse productTypesResult =
        fromJsonString(jsonStringProductTypes, GraphQLResponse.class);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(projectApiRoot.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    final CompletableFuture<ApiHttpResponse<GraphQLResponse>> apiHttpResponseCompletableFuture =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productTypesResult));
    when(byProjectKeyGraphqlPost.execute()).thenReturn(apiHttpResponseCompletableFuture);

    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(projectApiRoot.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesGet byProjectKeyProductTypesGet = mock();
    when(byProjectKeyProductTypesRequestBuilder.get()).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withWhere(anyString()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withPredicateVar(anyString(), anySet()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withLimit(anyInt())).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.execute())
        .thenReturn(
            CompletableFuture.completedFuture(
                new ApiHttpResponse<>(
                    200,
                    null,
                    ProductTypePagedQueryResponseBuilder.of()
                        .limit(0L)
                        .count(0L)
                        .offset(0L)
                        .results(emptyList())
                        .build())));

    final ByProjectKeyProductTypesPost byProjectKeyProductTypesPost = mock();
    when(byProjectKeyProductTypesRequestBuilder.post(any(ProductTypeDraft.class)))
        .thenReturn(byProjectKeyProductTypesPost);
    when(byProjectKeyProductTypesPost.execute())
        .thenReturn(
            CompletableFuture.completedFuture(
                new ApiHttpResponse<>(
                    200,
                    null,
                    getProductTypeBuilder()
                        .key(newProductTypeDraft.getKey())
                        .id(UUID.randomUUID().toString())
                        .build())));

    final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

    // test
    new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
        .sync(singletonList(newProductTypeDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyProductTypeSyncOptions).applyBeforeCreateCallback(newProductTypeDraft);
    verify(spyProductTypeSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final ProductTypeDraft newProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key("newProductType")
            .name("productType")
            .description("a cool type")
            .attributes(emptyList())
            .build();

    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(projectApiRoot).build();

    final ProductType mockedExistingProductType =
        createMockProductTypeBuilder()
            .key(newProductTypeDraft.getKey())
            .id(UUID.randomUUID().toString())
            .build();

    final ProductTypeService productTypeService =
        new ProductTypeServiceImpl(productTypeSyncOptions);
    //      final PagedQueryResult<ProductType> productTypePagedQueryResult =
    //   spy(PagedQueryResult.empty());
    //      when(productTypePagedQueryResult.getResults())
    //          .thenReturn(singletonList(mockedExistingProductType));
    //      when(projectApiRoot.execute(any(ProductTypeQuery.class)))
    //          .thenReturn(completedFuture(productTypePagedQueryResult));
    //      when(projectApiRoot.execute(any(ProductTypeUpdateCommand.class)))
    //          .thenReturn(completedFuture(mockedExistingProductType));

    final String jsonStringProductTypes = "{\"data\":{\"productTypes\":{\"results\":[]}}}";
    final GraphQLResponse productTypesResult =
        fromJsonString(jsonStringProductTypes, GraphQLResponse.class);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(projectApiRoot.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    final CompletableFuture<ApiHttpResponse<GraphQLResponse>> apiHttpResponseCompletableFuture =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productTypesResult));
    when(byProjectKeyGraphqlPost.execute()).thenReturn(apiHttpResponseCompletableFuture);

    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(projectApiRoot.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesGet byProjectKeyProductTypesGet = mock();
    when(byProjectKeyProductTypesRequestBuilder.get()).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withWhere(anyString()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withPredicateVar(anyString(), anySet()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withLimit(anyInt())).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.execute())
        .thenReturn(
            CompletableFuture.completedFuture(
                new ApiHttpResponse<>(
                    200,
                    null,
                    ProductTypePagedQueryResponseBuilder.of()
                        .limit(1L)
                        .count(1L)
                        .offset(0L)
                        .results(mockedExistingProductType)
                        .build())));

    final ByProjectKeyProductTypesByIDRequestBuilder byProjectKeyProductTypesByIDRequestBuilder =
        mock();
    when(byProjectKeyProductTypesRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyProductTypesByIDRequestBuilder);
    final ByProjectKeyProductTypesPost byProjectKeyProductTypesPost = mock();
    when(byProjectKeyProductTypesRequestBuilder.post(any(ProductTypeDraft.class)))
        .thenReturn(byProjectKeyProductTypesPost);
    when(byProjectKeyProductTypesPost.execute())
        .thenReturn(
            CompletableFuture.completedFuture(
                new ApiHttpResponse<>(200, null, mockedExistingProductType)));

    final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

    // test
    new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
        .sync(singletonList(newProductTypeDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyProductTypeSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyProductTypeSyncOptions, never()).applyBeforeCreateCallback(newProductTypeDraft);
  }
}
