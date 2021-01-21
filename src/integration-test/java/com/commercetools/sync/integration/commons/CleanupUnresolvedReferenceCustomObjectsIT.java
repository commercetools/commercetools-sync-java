package com.commercetools.sync.integration.commons;

import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedTransitionsCustomObjects;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl.CUSTOM_OBJECT_CONTAINER_KEY;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.CleanupUnresolvedReferenceCustomObjects;
import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.commons.models.WaitingToBeResolvedCategories;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.UnresolvedTransitionsService;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CleanupUnresolvedReferenceCustomObjectsIT {
  private UnresolvedTransitionsService unresolvedTransitionsService;
  private UnresolvedReferencesService unresolvedReferencesService;

  @BeforeEach
  void setupTest() {
    deleteWaitingToBeResolvedTransitionsCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_CONTAINER_KEY);
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY, WaitingToBeResolvedProducts.class);
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY, WaitingToBeResolvedProducts.class);
    unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl(
            ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());

    unresolvedTransitionsService =
        new UnresolvedTransitionsServiceImpl(StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());
  }

  @Test
  void cleanup_withDeleteDaysAfterLastModification_ShouldDeleteAndReturnCleanupStatistics() {
    createSampleUnresolvedReferences();

    final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
        CleanupUnresolvedReferenceCustomObjects.of(CTP_TARGET_CLIENT)
            .pageSize(3) // for testing purpose, ensures the pagination works.
            .cleanup(-1) // to be able to test it.
            .join();

    assertThat(statistics.getTotalDeleted()).isEqualTo(15);
    assertThat(statistics.getTotalFailed()).isEqualTo(0);
    assertThat(statistics.getReportMessage())
        .isEqualTo("Summary: 15 custom objects were deleted in total (0 failed to delete).");
  }

  void createSampleUnresolvedReferences() {
    final ProductDraft sampleProductDraft =
        SphereJsonUtils.readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, ProductDraft.class);
    final CategoryDraft sampleCategoryDraft =
        SphereJsonUtils.readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, CategoryDraft.class);
    final Set<Reference<State>> sampleTransitions =
        new HashSet<>(Arrays.asList(State.referenceOfId("id1"), State.referenceOfId("id2")));

    final List<WaitingToBeResolved> productUnresolvedReferences = new ArrayList<>();
    final List<WaitingToBeResolved> categoryUnresolvedReferences = new ArrayList<>();
    final List<WaitingToBeResolvedTransitions> transitionUnresolvedReferences = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      productUnresolvedReferences.add(
          new WaitingToBeResolvedProducts(
              ProductDraftBuilder.of(sampleProductDraft).key(format("productKey%s", i)).build(),
              asSet("foo", "bar")));
      categoryUnresolvedReferences.add(
          new WaitingToBeResolvedCategories(
              CategoryDraftBuilder.of(sampleCategoryDraft).key(format("categoryKey%s", i)).build(),
              asSet("foo", "bar")));
      transitionUnresolvedReferences.add(
          new WaitingToBeResolvedTransitions(
              StateDraftBuilder.of(format("stateKeys%s", i), StateType.LINE_ITEM_STATE)
                  .transitions(sampleTransitions)
                  .build(),
              asSet("foo", "bar")));
    }

    CompletableFuture.allOf(
            CompletableFuture.allOf(
                productUnresolvedReferences.stream()
                    .map(
                        draft ->
                            unresolvedReferencesService.save(
                                draft,
                                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                                WaitingToBeResolvedProducts.class))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)),
            CompletableFuture.allOf(
                categoryUnresolvedReferences.stream()
                    .map(
                        draft ->
                            unresolvedReferencesService.save(
                                draft,
                                CUSTOM_OBJECT_CONTAINER_KEY,
                                WaitingToBeResolvedCategories.class))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)),
            CompletableFuture.allOf(
                transitionUnresolvedReferences.stream()
                    .map(unresolvedTransitionsService::save)
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)))
        .join();
  }
}
