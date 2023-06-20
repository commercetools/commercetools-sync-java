package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.sync.integration.sdk2.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_SPECIAL_CHARS_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static java.util.Collections.singleton;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.UnresolvedReferencesService;
import com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnresolvedReferencesServiceImplIT {

  private static final String CUSTOM_OBJECT_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedReferencesService.productDrafts";
  private UnresolvedReferencesService<WaitingToBeResolvedProducts> unresolvedReferencesService;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  @AfterEach
  void tearDown() {
    deleteWaitingToBeResolvedCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY);
  }

  @BeforeEach
  void setupTest() {
    deleteWaitingToBeResolvedCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY);
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception);
                })
            .warningCallback(
                (syncException, productDraft, product) ->
                    warningCallBackMessages.add(syncException.getMessage()))
            .build();

    unresolvedReferencesService = new UnresolvedReferencesServiceImpl<>(productSyncOptions);
  }

  @Test
  void saveFetchAndDelete_WithoutExceptions_shouldWorkCorrectly() {
    // preparation
    final ProductDraft productDraft =
        readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

    final Set<String> missingKeysSet = new HashSet<>();
    missingKeysSet.add("foo");
    missingKeysSet.add("bar");
    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, missingKeysSet);

    // test
    final Optional<WaitingToBeResolvedProducts> result =
        unresolvedReferencesService
            .save(
                productDraftWithUnresolvedRefs,
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result)
        .hasValueSatisfying(
            waitingToBeResolved ->
                assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

    // test
    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                singleton(productDraft.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(waitingDrafts).containsExactly(productDraftWithUnresolvedRefs);

    // test
    final Optional<WaitingToBeResolvedProducts> deletionResult =
        unresolvedReferencesService
            .delete(
                productDraft.getKey(),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(deletionResult)
        .hasValueSatisfying(
            waitingToBeResolved ->
                assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void saveFetchAndDelete_WithKeyWithSpecialCharacter_shouldWorkCorrectly() {
    // preparation
    final ProductDraft productDraft =
        readObjectFromResource(PRODUCT_KEY_SPECIAL_CHARS_RESOURCE_PATH, ProductDraft.class);

    final Set<String> missingKeysSet = new HashSet<>();
    missingKeysSet.add("foo");
    missingKeysSet.add("bar");
    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, missingKeysSet);

    // test
    final Optional<WaitingToBeResolvedProducts> result =
        unresolvedReferencesService
            .save(
                productDraftWithUnresolvedRefs,
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result)
        .hasValueSatisfying(
            waitingToBeResolved ->
                assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

    // test
    CustomObject customObject =
        CTP_TARGET_CLIENT
            .customObjects()
            .withContainerAndKey(CUSTOM_OBJECT_CONTAINER_KEY, sha1Hex(productDraft.getKey()))
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
    // assertions
    assertThat(customObject.getKey()).isEqualTo(sha1Hex(productDraft.getKey()));

    // test
    final Set<WaitingToBeResolvedProducts> waitingDrafts =
        unresolvedReferencesService
            .fetch(
                singleton(productDraft.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(waitingDrafts).containsExactly(productDraftWithUnresolvedRefs);

    // test
    final Optional<WaitingToBeResolvedProducts> deletionResult =
        unresolvedReferencesService
            .delete(
                productDraft.getKey(),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(deletionResult)
        .hasValueSatisfying(
            waitingToBeResolved ->
                assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft));

    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void save_ExistingProductDraftWithoutException_overwritesOldCustomObjectValue() {
    // preparation
    final ProductDraft productDraft =
        readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

    final Set<String> missingKeysSet = new HashSet<>();
    missingKeysSet.add("foo");
    missingKeysSet.add("bar");
    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, missingKeysSet);

    unresolvedReferencesService
        .save(
            productDraftWithUnresolvedRefs,
            CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
            WaitingToBeResolvedProducts.class)
        .toCompletableFuture()
        .join();

    final Set<String> missingKeysSet123 = new HashSet<>();
    missingKeysSet.add("foo123");
    missingKeysSet.add("bar123");
    final WaitingToBeResolvedProducts productDraftWithUnresolvedNewRefs =
        new WaitingToBeResolvedProducts(productDraft, missingKeysSet123);

    // test
    final Optional<WaitingToBeResolvedProducts> latestResult =
        unresolvedReferencesService
            .save(
                productDraftWithUnresolvedNewRefs,
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(latestResult)
        .hasValueSatisfying(
            waitingToBeResolved -> {
              assertThat(waitingToBeResolved.getProductDraft()).isEqualTo(productDraft);
              assertThat(waitingToBeResolved.getMissingReferencedProductKeys())
                  .isEqualTo(productDraftWithUnresolvedNewRefs.getMissingReferencedProductKeys());
            });

    CustomObject customObject =
        CTP_TARGET_CLIENT
            .customObjects()
            .withContainerAndKey(CUSTOM_OBJECT_CONTAINER_KEY, sha1Hex(productDraft.getKey()))
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat((Map) customObject.getValue()).hasSize(3);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
