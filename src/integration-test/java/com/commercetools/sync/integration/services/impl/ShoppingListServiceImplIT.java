package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.createShoppingList;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.commons.utils.ShoppingListITUtils.deleteShoppingLists;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.services.impl.ShoppingListServiceImpl;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingListDraftDsl;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListServiceImplIT {
  private ShoppingListService shoppingListService;

  private ShoppingList shoppingList;
  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Deletes shopping list and products from the target CTP projects, then it populates the project
   * with test data.
   */
  @BeforeEach
  void setup() {
    deleteShoppingListTestData(CTP_TARGET_CLIENT);
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    shoppingList = createShoppingList(CTP_TARGET_CLIENT, "name", "key");
    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();
    shoppingListService = new ShoppingListServiceImpl(options);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteShoppingLists(CTP_TARGET_CLIENT);
  }

  @Test
  void fetchShoppingList_WithNonExistingShoppingList_ShouldReturnEmptyOptional() {
    final Optional<ShoppingList> shoppingList =
        shoppingListService.fetchShoppingList("not-existing-key").toCompletableFuture().join();

    assertThat(shoppingList).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchShoppingList_WithExistingShoppingList_ShouldFetchShoppingList() {
    final Optional<ShoppingList> shoppingList =
        shoppingListService
            .fetchShoppingList(this.shoppingList.getKey())
            .toCompletableFuture()
            .join();

    assertThat(shoppingList).isNotEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingShoppingListsByKeys_WithNotExistingKeys_ShouldReturnEmptySet() {
    final Set<String> shoppingListKeys = new HashSet<>();
    shoppingListKeys.add("not_existing_key_1");
    shoppingListKeys.add("not_existing_key_2");
    Set<ShoppingList> shoppingLists =
        shoppingListService
            .fetchMatchingShoppingListsByKeys(shoppingListKeys)
            .toCompletableFuture()
            .join();

    assertThat(shoppingLists).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void fetchMatchingShoppingListsByKeys_WithExistingShoppingListsKeys_ShouldReturnShoppingLists() {
    ShoppingList otherShoppingList =
        createShoppingList(CTP_TARGET_CLIENT, "other_name", "other_key");
    final Set<String> shoppingListKeys = new HashSet<>();
    shoppingListKeys.add(shoppingList.getKey());
    shoppingListKeys.add(otherShoppingList.getKey());
    Set<ShoppingList> shoppingLists =
        shoppingListService
            .fetchMatchingShoppingListsByKeys(shoppingListKeys)
            .toCompletableFuture()
            .join();

    assertThat(shoppingLists).hasSize(2);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithEmptyKeys_ShouldReturnCurrentCache() {
    Map<String, String> cache =
        shoppingListService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(0);

    cache =
        shoppingListService
            .cacheKeysToIds(singleton(shoppingList.getKey()))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    cache = shoppingListService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(1);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithCachedKeys_ShouldReturnCachedKeysWithoutRequest() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    final ShoppingListService shoppingListService =
        new ShoppingListServiceImpl(shoppingListSyncOptions);

    Map<String, String> cache =
        shoppingListService
            .cacheKeysToIds(singleton(shoppingList.getKey()))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    cache =
        shoppingListService
            .cacheKeysToIds(singleton(shoppingList.getKey()))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    verify(spyClient, times(1)).execute(any());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createShoppingList_WithValidShoppingList_ShouldCreateShoppingList() {
    // preparation
    ProductType productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                ResourceIdentifier.ofKey(productType.getKey()),
                LocalizedString.ofEnglish("newProduct"),
                LocalizedString.ofEnglish("foo"),
                ProductVariantDraftBuilder.of().key("foo-new").sku("sku-new").build())
            .key("newProduct")
            .build();
    executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    LineItemDraft lineItemDraft = LineItemDraftBuilder.ofSku("sku-new", Long.valueOf(1)).build();
    TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("text"), 1L).build();
    final ShoppingListDraftDsl newShoppingListDraft =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("new_name"))
            .key("new_key")
            .plusLineItems(lineItemDraft)
            .plusTextLineItems(textLineItemDraft)
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();
    ShoppingListService spyShoppingListService = new ShoppingListServiceImpl(options);

    // test
    final Optional<ShoppingList> createdShoppingList =
        spyShoppingListService
            .createShoppingList(newShoppingListDraft)
            .toCompletableFuture()
            .join();

    final Optional<ShoppingList> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                ShoppingListQuery.of()
                    .withPredicates(
                        shoppingListQueryModel -> shoppingListQueryModel.key().is("new_key")))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdShoppingList)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());
                          assertThat(created.getName()).isEqualTo(queried.getName());
                          assertThat(created.getLineItems()).hasSize(1);
                          assertThat(created.getTextLineItems()).hasSize(1);
                        }));
  }

  @Test
  void createShoppingList_WithNotExistingSkuInLineItem_ShouldNotCreateShoppingList() {
    // preparation
    LineItemDraft lineItemDraft = LineItemDraftBuilder.ofSku("unknownSku", Long.valueOf(1)).build();
    final ShoppingListDraft newShoppingListDraft =
        ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("new_name"))
            .key("new_key")
            .plusLineItems(lineItemDraft)
            .build();

    final Optional<ShoppingList> createdShoppingListOptional =
        shoppingListService.createShoppingList(newShoppingListDraft).toCompletableFuture().join();

    assertThat(createdShoppingListOptional).isEmpty();
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains(
            "Failed to create draft with key: 'new_key'. Reason: "
                + "detailMessage: No published product with an sku 'unknownSku' exists.");
  }

  @Test
  void updateCustomer_WithValidChanges_ShouldUpdateCustomerCorrectly() {
    final ChangeName updatedName = ChangeName.of(LocalizedString.ofEnglish("updated_name"));

    final ShoppingList updatedShoppingList =
        shoppingListService
            .updateShoppingList(shoppingList, singletonList(updatedName))
            .toCompletableFuture()
            .join();
    assertThat(updatedShoppingList).isNotNull();

    final Optional<ShoppingList> queried =
        CTP_TARGET_CLIENT
            .execute(
                ShoppingListQuery.of()
                    .withPredicates(
                        shoppingListQueryModel ->
                            shoppingListQueryModel.key().is(shoppingList.getKey())))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(queried).isNotEmpty();
    final ShoppingList fetchedShoppingList = queried.get();
    assertThat(fetchedShoppingList.getKey()).isEqualTo(updatedShoppingList.getKey());
    assertThat(fetchedShoppingList.getName()).isEqualTo(updatedShoppingList.getName());
  }
}
