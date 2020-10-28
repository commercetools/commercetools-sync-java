package com.commercetools.sync.shoppinglists;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void sync_WithEmptyKeyDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
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
        when(syncOptions.getCtpClient().execute(any()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        ShoppingListDraft shoppingListDraft =
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("shopping-list-name"))
                                    .key("shopping-list-key")
                                    .customer(ResourceIdentifier.ofKey("customer-key"))
                                    .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
                                    .build();
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


}
