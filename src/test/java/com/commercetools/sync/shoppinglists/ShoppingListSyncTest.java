package com.commercetools.sync.shoppinglists;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import com.commercetools.sync.services.impl.ShoppingListServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.BaseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ShoppingListSyncTest {

  private ShoppingListSyncOptions syncOptions;
  private List<String> errorMessages;
  private List<Throwable> exceptions;
  private ShoppingList errorCallbackOldResource;
  private ShoppingListDraft errorCallbackNewResource;
  private List<ShoppingListUpdateAction> errorCallbackUpdateActions;

  @BeforeEach
  void setup() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);

    syncOptions =
        ShoppingListSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, newResource, oldResource, updateActions) -> {
                  this.errorCallbackOldResource = oldResource.orElse(null);
                  this.errorCallbackNewResource = newResource.orElse(null);
                  this.errorCallbackUpdateActions = updateActions;
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();
  }

  @Test
  void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

    // test
    final ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(null)).toCompletableFuture().join();

    assertThat(statistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL);
  }

  @Test
  void sync_WithNullKeyDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("shopping-list-name")).build();
    final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

    // test
    final ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    assertThat(statistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET,
                shoppingListDraft.getName()));
  }

  @Test
  void sync_WithEmptyKeyDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("shopping-list-name")).key("").build();
    final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

    // test
    final ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    assertThat(statistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET,
                shoppingListDraft.getName()));
  }

  @Test
  void sync_WithoutName_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(LocalizedString.of()).key("shopping-list-key").build();
    final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

    // test
    final ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    assertThat(statistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET,
                shoppingListDraft.getKey()));
  }

  @Test
  void sync_WithExceptionOnCachingKeysToIds_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final TypeService typeService = mock(TypeService.class);
    final CustomerService customerService = mock(CustomerService.class);

    when(typeService.cacheKeysToIds(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    when(customerService.cacheKeysToIds(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            syncOptions, Mockito.mock(ShoppingListService.class), customerService, typeService);

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shopping-list-name"))
            .key("shopping-list-key")
            .customer(CustomerResourceIdentifierBuilder.of().key("customer-key").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.key("typeKey"))
                    .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                    .build())
            .build();

    // test
    ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    assertThat(statistics).hasValues(1, 0, 0, 1);

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
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final ShoppingListService shoppingListService = mock(ShoppingListService.class);

    when(shoppingListService.fetchMatchingShoppingListsByKeys(singleton("shopping-list-key")))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            syncOptions, shoppingListService, mock(CustomerService.class), mock(TypeService.class));

    ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shopping-list-name"))
            .key("shopping-list-key")
            .customer(CustomerResourceIdentifierBuilder.of().key("customer-key").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        typeResourceIdentifierBuilder ->
                            typeResourceIdentifierBuilder.key("typeKey"))
                    .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                    .build())
            .build();
    // test
    final ShoppingListSyncStatistics statistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(statistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to fetch existing shopping lists with keys: '[shopping-list-key]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(BaseException.class);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallbackAndIncrementCreated() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(singleton("shoppingListKey")))
        .thenReturn(completedFuture(new HashSet<>(singletonList(mockShoppingList))));

    when(mockShoppingListService.createShoppingList(any()))
        .thenReturn(completedFuture(Optional.of(mockShoppingList)));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("NAME")).key("shoppingListKey").build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 1, 0, 0);

    verify(spySyncOptions).applyBeforeCreateCallback(shoppingListDraft);
    verify(spySyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_FailedOnCreation_ShouldCallBeforeCreateCallbackAndIncrementFailed() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(singleton("shoppingListKey")))
        .thenReturn(completedFuture(new HashSet<>(singletonList(mockShoppingList))));

    // simulate an error during create, service will return an empty optional.
    when(mockShoppingListService.createShoppingList(any()))
        .thenReturn(completedFuture(Optional.empty()));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("NAME")).key("shoppingListKey").build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

    verify(spySyncOptions).applyBeforeCreateCallback(shoppingListDraft);
    verify(spySyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(completedFuture(mockShoppingList));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of().name(ofEnglish("NAME")).key("shoppingListKey").build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
  }

  @Test
  void sync_WithUnchangedShoppingListDraftAndUpdatedLineItemDraft_ShouldIncrementUpdated() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);

    final ShoppingListLineItem mockLineItem = mock(ShoppingListLineItem.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
    when(mockShoppingList.getName()).thenReturn(ofEnglish("shoppingListName"));

    final ProductVariant mockProductVariant = mock(ProductVariant.class);
    when(mockProductVariant.getSku()).thenReturn("dummy-sku");
    when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
    when(mockLineItem.getQuantity()).thenReturn(10L);

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(completedFuture(mockShoppingList));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final List<ShoppingListLineItemDraft> lineItemDrafts =
        singletonList(ShoppingListLineItemDraftBuilder.of().sku("dummy-sku").quantity(5L).build());

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .lineItems(lineItemDrafts)
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
  }

  @Test
  void sync_WithUnchangedShoppingListDraftAndUpdatedTextLineItemDraft_ShouldIncrementUpdated() {
    // preparation
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
    when(mockShoppingList.getName()).thenReturn(ofEnglish("shoppingListName"));

    final TextLineItem mockTextLineItem = mock(TextLineItem.class);
    when(mockTextLineItem.getName()).thenReturn(ofEnglish("textLineItemName"));
    when(mockTextLineItem.getQuantity()).thenReturn(10L);

    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));
    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(completedFuture(mockShoppingList));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final List<TextLineItemDraft> textLineItemDrafts =
        singletonList(
            TextLineItemDraftBuilder.of().name(ofEnglish("textLineItemName")).quantity(5L).build());

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .textLineItems(textLineItemDrafts)
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

    verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
  }

  @Test
  void sync_WithoutUpdateActions_ShouldNotIncrementUpdated() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
    when(mockShoppingList.getName()).thenReturn(ofEnglish("shoppingListName"));
    when(mockShoppingList.getDescription()).thenReturn(ofEnglish("shoppingListDesc"));
    when(mockShoppingList.getSlug()).thenReturn(ofEnglish("shoppingListSlug"));
    when(mockShoppingList.getAnonymousId()).thenReturn("shoppingListAnonymousId");
    when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(360L);

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .description(mockShoppingList.getDescription())
            .slug(mockShoppingList.getSlug())
            .anonymousId(mockShoppingList.getAnonymousId())
            .deleteDaysAfterLastModification(mockShoppingList.getDeleteDaysAfterLastModification())
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 0);

    verify(spySyncOptions)
        .applyBeforeUpdateCallback(emptyList(), shoppingListDraft, mockShoppingList);
    verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
  }

  @Test
  void sync_WithBadGatewayException_ShouldFailToUpdateAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(CompletableFuture.failedFuture(ExceptionUtils.createBadGatewayException()));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1).singleElement(as(STRING)).contains("test");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasRootCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCustomerWithSuccess() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getName()).thenReturn(ofEnglish("shoppingListName"));
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
    when(mockShoppingList.getDescription()).thenReturn(ofEnglish("shoppingListDesc"));
    when(mockShoppingList.getSlug()).thenReturn(ofEnglish("shoppingListSlug"));
    when(mockShoppingList.getAnonymousId()).thenReturn("shoppingListAnonymousId");
    when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(360L);

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(
            CompletableFuture.failedFuture(
                new BaseException(ExceptionUtils.createConcurrentModificationException("test"))))
        .thenReturn(completedFuture(mockShoppingList));

    when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
        .thenReturn(completedFuture(Optional.of(mockShoppingList)));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .description(ofEnglish("newShoppingListDesc"))
            .slug(mockShoppingList.getSlug())
            .anonymousId(mockShoppingList.getAnonymousId())
            .deleteDaysAfterLastModification(mockShoppingList.getDeleteDaysAfterLastModification())
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(
            CompletableFuture.failedFuture(
                new BaseException(ExceptionUtils.createConcurrentModificationException("test"))))
        .thenReturn(completedFuture(mockShoppingList));

    when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
        .thenReturn(CompletableFuture.failedFuture(ExceptionUtils.createBadGatewayException()));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to fetch from CTP while retrying after concurrency modification.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasRootCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    when(mockShoppingListService.updateShoppingList(any(), anyList()))
        .thenReturn(
            CompletableFuture.failedFuture(
                new BaseException(ExceptionUtils.createConcurrentModificationException("test"))))
        .thenReturn(completedFuture(mockShoppingList));

    when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
        .thenReturn(completedFuture(Optional.empty()));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions,
            mockShoppingListService,
            mock(CustomerService.class),
            mock(TypeService.class));

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "Not found when attempting to fetch while retrying after concurrency modification.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasNoCause();
  }

  @Test
  void sync_WithExceptionOnReferenceResolution_ShouldFailToUpdateAndIncreaseFailedCounter() {
    // preparation
    final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockShoppingList)));

    final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.failedFuture(new BaseException("CTP error on fetch")));

    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            spySyncOptions, mockShoppingListService, mock(CustomerService.class), typeService);

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName"))
            .key("shoppingListKey")
            .build();

    // test
    final ShoppingListSyncStatistics shoppingListSyncStatistics =
        shoppingListSync.sync(singletonList(shoppingListDraft)).toCompletableFuture().join();

    // assertions
    assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to process the ShoppingListDraft with key:'shoppingListKey'");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class);
  }

  @Test
  void
      sync_WithErrorUpdatingShoppingListAndCustomErrorCallback_ShouldCallErrorCallbackAndContainResourceName() {
    // preparation
    final ShoppingListDraft newShoppingListDraft1 =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingListName1"))
            .key("shoppingListKey1")
            .build();

    final ShoppingListService mockShoppingListService = Mockito.mock(ShoppingListServiceImpl.class);
    final CustomerService mockCustomerService = Mockito.mock(CustomerServiceImpl.class);
    final TypeService mockTypeService = Mockito.mock(TypeServiceImpl.class);

    final ShoppingList existingShoppingList = mock(ShoppingList.class);
    when(existingShoppingList.getKey()).thenReturn(newShoppingListDraft1.getKey());
    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(any()))
        .thenReturn(CompletableFuture.completedFuture(singleton(existingShoppingList)));
    when(mockShoppingListService.fetchMatchingShoppingListsByKeys(emptySet()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptySet()));
    when(mockShoppingListService.updateShoppingList(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));
    when(mockShoppingListService.createShoppingList(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(existingShoppingList)));
    when(mockShoppingListService.fetchShoppingList(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(existingShoppingList)));

    // test
    final ShoppingListSync shoppingListSync =
        new ShoppingListSync(
            syncOptions, mockShoppingListService, mockCustomerService, mockTypeService);
    shoppingListSync.sync(singletonList(newShoppingListDraft1)).toCompletableFuture().join();

    // assertions
    assertThat(errorCallbackOldResource).isEqualTo(existingShoppingList);
    assertThat(errorCallbackNewResource).isEqualTo(newShoppingListDraft1);
    assertThat(errorCallbackUpdateActions.get(0).getAction()).isEqualTo("changeName");

    assertThat(errorMessages.get(0))
        .contains(
            "Failed to update shopping lists with key: 'shoppingListKey1'. Reason: io.vrap.rmf.base.client.error.BaseException");
  }
}
