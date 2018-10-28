package com.commercetools.sync.internals.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.commercetools.sync.internals.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnorderedCollectionSyncUtilsTest {

    @Test
    public void buildRemoveUpdateActions_withNullNewDraftsAndEmptyOldCollection_ShouldNotBuildActions() {
        // preparation
        final List<ProductVariant> oldVariants = new ArrayList<>();
        final List<ProductVariantDraft> newDrafts = null;

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants, newDrafts,
            ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

    @Test
    public void buildRemoveUpdateActions_withNullNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        when(productVariant.getKey()).thenReturn("1");
        final List<ProductVariant> oldVariants = singletonList(productVariant);
        final List<ProductVariantDraft> newDrafts = null;

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants, newDrafts,
            ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions)
            .containsExactly(RemoveVariant.ofVariantId(productVariant.getId(), true));
    }

    @Test
    public void buildRemoveUpdateActions_withEmptyNewDrafts_ShouldBuildRemoveActionsForEveryDraft() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        when(productVariant.getKey()).thenReturn("1");
        final List<ProductVariant> oldVariants = singletonList(productVariant);
        final List<ProductVariantDraft> newDrafts = new ArrayList<>();

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions)
            .containsExactly(RemoveVariant.ofVariantId(productVariant.getId(), true));
    }

    @Test
    public void buildRemoveUpdateActions_withSomeMatchingDrafts_ShouldBuildRemoveActionsForEveryMissingDraft() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        when(productVariant.getKey()).thenReturn("1");

        final ProductVariant productVariant2 = mock(ProductVariant.class);
        when(productVariant2.getId()).thenReturn(2);
        when(productVariant2.getKey()).thenReturn("2");

        final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
        final List<ProductVariantDraft> newDrafts =
            singletonList(ProductVariantDraftBuilder.of(productVariant).build());

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions)
            .containsExactly(RemoveVariant.ofVariantId(productVariant2.getId(), true));
    }

    @Test
    public void buildRemoveUpdateActions_withDraftsWithNullKeys_ShouldNotBuildRemoveActionsForDraftsWithNullKeys() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);

        final ProductVariant productVariant2 = mock(ProductVariant.class);
        when(productVariant2.getId()).thenReturn(2);

        final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
        final List<ProductVariantDraft> newDrafts =
            singletonList(ProductVariantDraftBuilder.of(productVariant).build());

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

    @Test
    public void buildRemoveUpdateActions_withNullDrafts_ShouldNotBuildRemoveActions() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);

        final ProductVariant productVariant2 = mock(ProductVariant.class);
        when(productVariant2.getId()).thenReturn(2);

        final List<ProductVariant> oldVariants = asList(productVariant, productVariant2);
        final List<ProductVariantDraft> newDrafts = singletonList(null);

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

    @Test
    public void buildRemoveUpdateActions_withNullReturningActionMapper_ShouldBuildActionsWithNoNullElement() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);

        final ProductVariant productVariant2 = mock(ProductVariant.class);
        when(productVariant2.getId()).thenReturn(2);
        when(productVariant2.getKey()).thenReturn("2");

        final ProductVariant productVariant3 = mock(ProductVariant.class);
        when(productVariant2.getId()).thenReturn(3);
        when(productVariant2.getKey()).thenReturn("3");

        final List<ProductVariant> oldVariants = asList(productVariant, productVariant2, productVariant3);
        final List<ProductVariantDraft> newDrafts =
            singletonList(ProductVariantDraftBuilder.of(productVariant).build());

        // test
        final Function<ProductVariant, UpdateAction<Product>> onlyRemoveV2Mapper = variant -> {
            if (Objects.equals(variant.getKey(), productVariant2.getKey())) {
                return RemoveVariant.ofVariantId(productVariant2.getId(), true);
            }
            return null;
        };
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey, onlyRemoveV2Mapper);

        // assertion
        assertThat(removeUpdateActions).doesNotContainNull();
    }

    @Test
    public void buildRemoveUpdateActions_withIdenticalNewDraftsAndOldMap_ShouldNotBuildRemoveActions() {
        // preparation
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1);
        when(productVariant.getKey()).thenReturn("1");
        final List<ProductVariant> oldVariants = singletonList(productVariant);
        final List<ProductVariantDraft> newDrafts =
            singletonList(ProductVariantDraftBuilder.of(productVariant).build());

        // test
        final List<UpdateAction<Product>> removeUpdateActions = buildRemoveUpdateActions(oldVariants,
            newDrafts, ProductVariant::getKey, ProductVariantDraft::getKey,
            p -> RemoveVariant.ofVariantId(p.getId(), true));

        // assertion
        assertThat(removeUpdateActions).isEmpty();
    }

}
