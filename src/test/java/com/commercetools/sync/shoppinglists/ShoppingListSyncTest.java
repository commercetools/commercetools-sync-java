package com.commercetools.sync.shoppinglists;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShoppingListSyncTest {

    private ShoppingListSyncOptions syncOptions;
    private List<String> errorMessages;
    private List<Throwable> exceptions;

    @BeforeEach
    void setup() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = ShoppingListSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();
    }

    @Test
    void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

        //test
        ShoppingListSyncStatistics statistics = shoppingListSync
            .sync(singletonList(null))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(SHOPPING_LIST_DRAFT_IS_NULL);
    }

    @Test
    void sync_WithNullKeyDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        ShoppingListDraft shoppingListDraft =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shopping-list-name")).build();
        ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

        //test
        ShoppingListSyncStatistics statistics = shoppingListSync
            .sync(singletonList(shoppingListDraft))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
    }

    @Test
    void sync_WithEmptyKeyDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shopping-list-name")).key("").build();
        ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

        //test
        ShoppingListSyncStatistics statistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
                .hasSize(1)
                .singleElement(as(STRING))
                .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
    }

    @Test
    void sync_WithoutName_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        ShoppingListDraft shoppingListDraft =
            ShoppingListDraftBuilder.of(LocalizedString.of()).key("shopping-list-key").build();
        ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions);

        //test
        ShoppingListSyncStatistics statistics = shoppingListSync
            .sync(singletonList(shoppingListDraft))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_NAME_NOT_SET, shoppingListDraft.getKey()));
    }

    @Test
    void sync_WithExceptionOnCachingKeysToIds_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //preparation
        final TypeService typeService = mock(TypeService.class);
        final CustomerService customerService = mock(CustomerService.class);

        when(typeService.cacheKeysToIds(any()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        when(customerService.cacheKeysToIds(any()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));


        final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions, mock(ShoppingListService.class),
                customerService , typeService);

        ShoppingListDraft shoppingListDraft =
            ShoppingListDraftBuilder
                    .of(LocalizedString.ofEnglish("shopping-list-name"))
                    .key("shopping-list-key")
                    .customer(ResourceIdentifier.ofKey("customer-key"))
                    .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
                    .build();

        //test
        ShoppingListSyncStatistics statistics = shoppingListSync
            .sync(singletonList(shoppingListDraft))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo("Failed to build a cache of keys to ids.");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasCauseExactlyInstanceOf(CompletionException.class)
            .hasRootCauseExactlyInstanceOf(SphereException.class);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final ShoppingListService shoppingListService = mock(ShoppingListService.class);

        when(shoppingListService.fetchMatchingShoppingListsByKeys(singleton("shopping-list-key")))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final ShoppingListSync shoppingListSync = new ShoppingListSync(syncOptions, shoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shopping-list-name"))
                        .key("shopping-list-key")
                        .customer(ResourceIdentifier.ofKey("customer-key"))
                        .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
                        .build();
        // test
        final ShoppingListSyncStatistics statistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();


        // assertions
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo("Failed to fetch existing shopping lists with keys: '[shopping-list-key]'.");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasCauseExactlyInstanceOf(CompletionException.class)
            .hasRootCauseExactlyInstanceOf(SphereException.class);
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
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class) , mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                        .of(LocalizedString.ofEnglish("NAME"))
                        .key("shoppingListKey")
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 1, 0, 0);

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
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                        .of(LocalizedString.ofEnglish("NAME"))
                        .key("shoppingListKey")
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

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
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class) , mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                        .of(LocalizedString.ofEnglish("NAME"))
                        .key("shoppingListKey")
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

        verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
    }

    @Test
    void sync_WithUnchangedShoppingListDraftAndUpdatedLineItemDraft_ShouldIncrementUpdated() {
        // preparation
        final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
        final ShoppingList mockShoppingList = mock(ShoppingList.class);

        final LineItem mockLineItem = mock(LineItem.class);
        when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
        when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("shoppingListName"));

        final ProductVariant mockProductVariant = mock(ProductVariant.class);
        when(mockProductVariant.getSku()).thenReturn("dummy-sku");
        when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
        when(mockLineItem.getQuantity()).thenReturn(10L);

        when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockShoppingList)));

        when(mockShoppingListService.updateShoppingList(any(), anyList()))
            .thenReturn(completedFuture(mockShoppingList));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final List<LineItemDraft> lineItemDrafts = singletonList(
                LineItemDraftBuilder.ofSku("dummy-sku", 5L).build());

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shoppingListName"))
                        .key("shoppingListKey")
                        .lineItems(lineItemDrafts)
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

        verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
    }

    @Test
    void sync_WithUnchangedShoppingListDraftAndUpdatedTextLineItemDraft_ShouldIncrementUpdated() {
        // preparation
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
        when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("shoppingListName"));

        final TextLineItem mockTextLineItem = mock(TextLineItem.class);
        when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("textLineItemName"));
        when(mockTextLineItem.getQuantity()).thenReturn(10L);

        final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
        when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockShoppingList)));
        when(mockShoppingListService.updateShoppingList(any(), anyList()))
            .thenReturn(completedFuture(mockShoppingList));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final List<TextLineItemDraft> textLineItemDrafts = singletonList(
                TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("textLineItemName"), 5L).build());

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shoppingListName"))
                        .key("shoppingListKey")
                        .textLineItems(textLineItemDrafts)
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);

        verify(spySyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
    }

    @Test
    void sync_WithoutUpdateActions_ShouldNotIncrementUpdated() {
        // preparation
        final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
        when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("shoppingListName"));
        when(mockShoppingList.getDescription()).thenReturn(LocalizedString.ofEnglish("shoppingListDesc"));
        when(mockShoppingList.getSlug()).thenReturn(LocalizedString.ofEnglish("shoppingListSlug"));
        when(mockShoppingList.getAnonymousId()).thenReturn("shoppingListAnonymousId");
        when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(360);

        when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockShoppingList)));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                        .of(LocalizedString.ofEnglish("shoppingListName"))
                        .key("shoppingListKey")
                        .description(mockShoppingList.getDescription())
                        .slug(mockShoppingList.getSlug())
                        .anonymousId(mockShoppingList.getAnonymousId())
                        .deleteDaysAfterLastModification(mockShoppingList.getDeleteDaysAfterLastModification())
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 0);

        verify(spySyncOptions).applyBeforeUpdateCallback(emptyList(), shoppingListDraft, mockShoppingList);
        verify(spySyncOptions, never()).applyBeforeCreateCallback(shoppingListDraft);
    }

    @Test
    void sync_WithBadRequestException_ShouldFailToUpdateAndIncreaseFailedCounter() {
        // preparation
        final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getKey()).thenReturn("shoppingListKey");

        when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockShoppingList)));

        when(mockShoppingListService.updateShoppingList(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new BadRequestException("Invalid request")));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shoppingListName"))
                                        .key("shoppingListKey")
                                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .contains("Invalid request");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasRootCauseExactlyInstanceOf(BadRequestException.class);
    }

    @Test
    void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCustomerWithSuccess() {
        // preparation
        final ShoppingListService mockShoppingListService = mock(ShoppingListService.class);
        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("shoppingListName"));
        when(mockShoppingList.getKey()).thenReturn("shoppingListKey");
        when(mockShoppingList.getDescription()).thenReturn(LocalizedString.ofEnglish("shoppingListDesc"));
        when(mockShoppingList.getSlug()).thenReturn(LocalizedString.ofEnglish("shoppingListSlug"));
        when(mockShoppingList.getAnonymousId()).thenReturn("shoppingListAnonymousId");
        when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(360);

        when(mockShoppingListService.fetchMatchingShoppingListsByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockShoppingList)));

        when(mockShoppingListService.updateShoppingList(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
            .thenReturn(completedFuture(mockShoppingList));

        when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
            .thenReturn(completedFuture(Optional.of(mockShoppingList)));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                    .of(LocalizedString.ofEnglish("shoppingListName"))
                    .key("shoppingListKey")
                    .description(LocalizedString.ofEnglish("newShoppingListDesc"))
                    .slug(mockShoppingList.getSlug())
                    .anonymousId(mockShoppingList.getAnonymousId())
                    .deleteDaysAfterLastModification(mockShoppingList.getDeleteDaysAfterLastModification())
                    .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 1, 0);
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
            .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
            .thenReturn(completedFuture(mockShoppingList));

        when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
            .thenReturn(exceptionallyCompletedFuture(new SphereException()));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                    .of(LocalizedString.ofEnglish("shoppingListName"))
                    .key("shoppingListKey")
                    .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
                .hasSize(1)
                .singleElement(as(STRING))
                .contains("Failed to fetch from CTP while retrying after concurrency modification.");

        assertThat(exceptions)
                .hasSize(1)
                .singleElement(as(THROWABLE))
                .isExactlyInstanceOf(SyncException.class)
                .hasRootCauseExactlyInstanceOf(SphereException.class);
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
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockShoppingList));

        when(mockShoppingListService.fetchShoppingList("shoppingListKey"))
                .thenReturn(completedFuture(Optional.empty()));

        final ShoppingListSyncOptions spySyncOptions = spy(syncOptions);
        final ShoppingListSync shoppingListSync = new ShoppingListSync(spySyncOptions, mockShoppingListService,
                mock(CustomerService.class), mock(TypeService.class));

        final ShoppingListDraft shoppingListDraft =
                ShoppingListDraftBuilder
                        .of(LocalizedString.ofEnglish("shoppingListName"))
                        .key("shoppingListKey")
                        .build();

        //test
        final ShoppingListSyncStatistics shoppingListSyncStatistics = shoppingListSync
                .sync(singletonList(shoppingListDraft))
                .toCompletableFuture()
                .join();

        // assertions
        AssertionsForStatistics.assertThat(shoppingListSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .contains("Not found when attempting to fetch while retrying after concurrency modification.");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasNoCause();
    }
}
