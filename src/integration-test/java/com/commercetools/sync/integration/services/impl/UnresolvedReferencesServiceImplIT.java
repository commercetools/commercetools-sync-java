package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_SPECIAL_CHARS_RESOURCE_PATH;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.singleton;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectByKeyGet;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.ProductDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY, WaitingToBeResolvedProducts.class);
  }

  @BeforeEach
  void setupTest() {
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY, WaitingToBeResolvedProducts.class);
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

    unresolvedReferencesService = new UnresolvedReferencesServiceImpl(productSyncOptions);
  }

  @Test
  void saveFetchAndDelete_WithoutExceptions_shouldWorkCorrectly() {
    // preparation
    final ProductDraft productDraft =
        SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, asSet("foo", "bar"));

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
        SphereJsonUtils.readObjectFromResource(
            PRODUCT_KEY_SPECIAL_CHARS_RESOURCE_PATH, ProductDraft.class);

    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, asSet("foo", "bar"));

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
    final CustomObjectByKeyGet<WaitingToBeResolvedProducts> customObjectByKeyGet =
        CustomObjectByKeyGet.of(
            CUSTOM_OBJECT_CONTAINER_KEY,
            sha1Hex(productDraft.getKey()),
            WaitingToBeResolvedProducts.class);
    final CustomObject<WaitingToBeResolvedProducts> createdCustomObject =
        CTP_TARGET_CLIENT.execute(customObjectByKeyGet).toCompletableFuture().join();
    // assertions
    assertThat(createdCustomObject.getKey()).isEqualTo(sha1Hex(productDraft.getKey()));

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
        SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);

    final WaitingToBeResolvedProducts productDraftWithUnresolvedRefs =
        new WaitingToBeResolvedProducts(productDraft, asSet("foo", "bar"));

    unresolvedReferencesService
        .save(
            productDraftWithUnresolvedRefs,
            CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
            WaitingToBeResolvedProducts.class)
        .toCompletableFuture()
        .join();

    final WaitingToBeResolvedProducts productDraftWithUnresolvedNewRefs =
        new WaitingToBeResolvedProducts(productDraft, asSet("foo123", "bar123"));

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

    final CustomObjectByKeyGet<WaitingToBeResolvedProducts> customObjectByKeyGet =
        CustomObjectByKeyGet.of(
            CUSTOM_OBJECT_CONTAINER_KEY,
            sha1Hex(productDraft.getKey()),
            WaitingToBeResolvedProducts.class);
    final CustomObject<WaitingToBeResolvedProducts> createdCustomObject =
        CTP_TARGET_CLIENT.execute(customObjectByKeyGet).toCompletableFuture().join();

    assertThat(createdCustomObject.getValue()).isEqualTo(productDraftWithUnresolvedNewRefs);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
