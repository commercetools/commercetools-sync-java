package com.commercetools.sync.internals.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.commercetools.sync.internals.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnorderedCollectionSyncUtilsTest {

    @Test
    public void buildRemoveUpdateActions_withNullNewDraftsAndEmptyOldCollection_ShouldNotBuildActions() {
        // preparation
        final HashMap<String, ProductVariant> oldResourcesMap = new HashMap<>();

        final List<ProductVariantDraft> newDrafts = null;

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldResourcesMap, newDrafts,
            ProductVariantDraft::getKey, p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

    @Test
    public void buildRemoveUpdateActions_withNullNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
        // preparation
        final HashMap<String, ProductVariant> oldResourcesMap = new HashMap<>();

        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        oldResourcesMap.put("1", productVariant);

        final List<ProductVariantDraft> newDrafts = null;

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldResourcesMap, newDrafts,
            ProductVariantDraft::getKey, p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions)
            .containsExactly(RemoveVariant.ofVariantId(productVariant.getId(), true));
    }

    @Test
    public void buildRemoveUpdateActions_withEmptyNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
        // preparation
        final HashMap<String, ProductVariant> oldResourcesMap = new HashMap<>();

        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        oldResourcesMap.put("1", productVariant);

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldResourcesMap,
            new ArrayList<>(), ProductVariantDraft::getKey, p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions)
            .containsExactly(RemoveVariant.ofVariantId(productVariant.getId(), true));
    }

    @Test
    public void buildRemoveUpdateActions_withIdenticalNewDraftsAndOldMap_ShouldNotBuildRemoveActions() {
        // preparation
        final HashMap<String, ProductVariant> oldResourcesMap = new HashMap<>();

        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        when(productVariant.getKey()).thenReturn("1");
        oldResourcesMap.put("1", productVariant);

        final List<ProductVariantDraft> newDrafts =
            singletonList(ProductVariantDraftBuilder.of(productVariant).build());

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldResourcesMap,
            newDrafts, ProductVariantDraft::getKey, p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

}
