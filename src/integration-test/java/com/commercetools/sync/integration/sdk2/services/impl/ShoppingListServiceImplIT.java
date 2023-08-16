package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.ShoppingListITUtils.deleteShoppingListTestData;
import static com.commercetools.sync.integration.sdk2.commons.utils.ShoppingListITUtils.deleteShoppingLists;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameAction;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.sync.integration.sdk2.commons.utils.ShoppingListITUtils;
import com.commercetools.sync.sdk2.services.ShoppingListService;
import com.commercetools.sync.sdk2.services.impl.ShoppingListServiceImpl;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.sdk2.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
    shoppingList = ShoppingListITUtils.ensureShoppingList(CTP_TARGET_CLIENT, "name", "key");
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
        ShoppingListITUtils.ensureShoppingList(CTP_TARGET_CLIENT, "other_name", "other_key");
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
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

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

    verify(spyClient, times(1)).graphql();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createShoppingList_WithValidShoppingList_ShouldCreateShoppingList() {
    // preparation
    final ProductType productType =
        createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .name(ofEnglish("newProduct"))
            .slug(ofEnglish("foo"))
            .variants(ProductVariantDraftBuilder.of().key("foo-new").sku("sku-new").build())
            .key("newProduct")
            .build();
    CTP_TARGET_CLIENT.products().create(productDraft).executeBlocking();
    final ShoppingListLineItemDraft lineItemDraft =
        ShoppingListLineItemDraftBuilder.of().sku("sku-new").quantity(1L).build();
    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of().name(ofEnglish("text")).quantity(1L).build();
    final ShoppingListDraft newShoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("new_name"))
            .key("new_key")
            .plusLineItems(lineItemDraft)
            .plusTextLineItems(textLineItemDraft)
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

    final ShoppingListSyncOptions options =
        ShoppingListSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .build();
    final ShoppingListService spyShoppingListService = new ShoppingListServiceImpl(options);

    // test
    final Optional<ShoppingList> createdShoppingList =
        spyShoppingListService
            .createShoppingList(newShoppingListDraft)
            .toCompletableFuture()
            .join();

    final ShoppingList shoppingList =
        CTP_TARGET_CLIENT
            .shoppingLists()
            .withKey("new_key")
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(createdShoppingList)
        .hasValueSatisfying(
            created -> {
              assertThat(created.getKey()).isEqualTo(shoppingList.getKey());
              assertThat(created.getName()).isEqualTo(shoppingList.getName());
              assertThat(created.getLineItems()).hasSize(1);
              assertThat(created.getTextLineItems()).hasSize(1);
            });
  }

  @Test
  void createShoppingList_WithNotExistingSkuInLineItem_ShouldNotCreateShoppingList() {
    // preparation
    final ShoppingListLineItemDraft lineItemDraft =
        ShoppingListLineItemDraftBuilder.of().sku("unknownSku").quantity(1L).build();
    final ShoppingListDraft newShoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("new_name"))
            .key("new_key")
            .plusLineItems(lineItemDraft)
            .build();

    final Optional<ShoppingList> createdShoppingListOptional =
        shoppingListService.createShoppingList(newShoppingListDraft).toCompletableFuture().join();

    assertThat(createdShoppingListOptional).isEmpty();
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains("Failed to create draft with key: 'new_key'. Reason: ");
    assertThat(errorCallBackMessages.get(0))
        .contains("No published product with an sku 'unknownSku' exists.");
  }

  @Test
  void updateCustomer_WithValidChanges_ShouldUpdateCustomerCorrectly() {
    final ShoppingListChangeNameAction updatedName =
        ShoppingListChangeNameActionBuilder.of().name(ofEnglish("updated_name")).build();

    final ShoppingList updatedShoppingList =
        shoppingListService
            .updateShoppingList(shoppingList, singletonList(updatedName))
            .toCompletableFuture()
            .join();
    assertThat(updatedShoppingList).isNotNull();

    final ShoppingList fetchedShoppingList =
        CTP_TARGET_CLIENT
            .shoppingLists()
            .withKey(shoppingList.getKey())
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedShoppingList.getKey()).isEqualTo(updatedShoppingList.getKey());
    assertThat(fetchedShoppingList.getName()).isEqualTo(updatedShoppingList.getName());
  }
}
