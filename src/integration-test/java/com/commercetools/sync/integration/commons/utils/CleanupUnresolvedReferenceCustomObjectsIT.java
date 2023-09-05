package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.api.models.state.StateTypeEnum.LINE_ITEM_STATE;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TestUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.*;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.commons.CleanupUnresolvedReferenceCustomObjects;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolvedCategories;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.sdk2.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.UnresolvedReferencesService;
import com.commercetools.sync.sdk2.services.impl.UnresolvedReferencesServiceImpl;
import com.commercetools.sync.sdk2.states.StateSyncOptionsBuilder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CleanupUnresolvedReferenceCustomObjectsIT {
  private UnresolvedReferencesService<WaitingToBeResolvedProducts>
      unresolvedReferencesServiceForProducts;
  private UnresolvedReferencesService<WaitingToBeResolvedCategories>
      unresolvedReferencesServiceForCategories;
  private UnresolvedReferencesService<WaitingToBeResolvedTransitions>
      unresolvedReferencesServiceForTransitions;

  @BeforeEach
  void setupTest() {
    deleteWaitingToBeResolvedCustomObjects(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY);
    deleteWaitingToBeResolvedCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY);
    deleteWaitingToBeResolvedCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY);
    unresolvedReferencesServiceForProducts =
        new UnresolvedReferencesServiceImpl<>(
            ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());
    unresolvedReferencesServiceForCategories =
        new UnresolvedReferencesServiceImpl<>(
            CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());
    unresolvedReferencesServiceForTransitions =
        new UnresolvedReferencesServiceImpl<>(
            StateSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build());
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
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id("test").build(),
            null,
            null,
            null,
            null);
    final CategoryDraft sampleCategoryDraft =
        readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, CategoryDraft.class);

    final StateResourceIdentifier stateRef1 = StateResourceIdentifierBuilder.of().id("id1").build();
    final StateResourceIdentifier stateRef2 = StateResourceIdentifierBuilder.of().id("id2").build();

    final List<WaitingToBeResolvedProducts> productUnresolvedReferences = new ArrayList<>();
    final List<WaitingToBeResolvedCategories> categoryUnresolvedReferences = new ArrayList<>();
    final List<WaitingToBeResolvedTransitions> transitionUnresolvedReferences = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      productUnresolvedReferences.add(
          new WaitingToBeResolvedProducts(
              ProductDraftBuilder.of(sampleProductDraft).key(format("productKey%s", i)).build(),
              Set.of("foo", "bar")));
      categoryUnresolvedReferences.add(
          new WaitingToBeResolvedCategories(
              CategoryDraftBuilder.of(sampleCategoryDraft).key(format("categoryKey%s", i)).build(),
              Set.of("foo", "bar")));
      transitionUnresolvedReferences.add(
          new WaitingToBeResolvedTransitions(
              StateDraftBuilder.of()
                  .key(format("stateKeys%s", i))
                  .type(LINE_ITEM_STATE)
                  .transitions(stateRef1, stateRef2)
                  .build(),
              Set.of("foo", "bar")));
    }

    CompletableFuture.allOf(
            CompletableFuture.allOf(
                productUnresolvedReferences.stream()
                    .map(
                        draft ->
                            unresolvedReferencesServiceForProducts.save(
                                draft,
                                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                                WaitingToBeResolvedProducts.class))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)),
            CompletableFuture.allOf(
                categoryUnresolvedReferences.stream()
                    .map(
                        draft ->
                            unresolvedReferencesServiceForCategories.save(
                                draft,
                                CUSTOM_OBJECT_CATEGORY_CONTAINER_KEY,
                                WaitingToBeResolvedCategories.class))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)),
            CompletableFuture.allOf(
                transitionUnresolvedReferences.stream()
                    .map(
                        draft ->
                            unresolvedReferencesServiceForTransitions.save(
                                draft,
                                CUSTOM_OBJECT_TRANSITION_CONTAINER_KEY,
                                WaitingToBeResolvedTransitions.class))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new)))
        .join();
  }
}
