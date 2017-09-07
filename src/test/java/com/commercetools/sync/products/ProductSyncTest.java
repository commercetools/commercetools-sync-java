package com.commercetools.sync.products;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
@RunWith(Parameterized.class)
public class ProductSyncTest {

    private Product product;
    private ProductDraft productDraft;
    private ProductSyncOptions productSyncOptions;
    private ProductService service;

    /**
     * Create collection of all permutations of sync options.
     */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {true, true, true, true}, {true, true, true, false},
            {true, true, false, true}, {true, true, false, false},
            {true, false, true, true}, {true, false, true, false},
            {true, false, false, true}, {true, false, false, false},
            {false, true, true, true}, {false, true, true, false},
            {false, true, false, true}, {false, true, false, false},
            {false, false, true, true}, {false, false, true, false},
            {false, false, false, true}, {false, false, false, false}
        });
    }

    @Parameter
    public boolean publish;
    @Parameter(1)
    public boolean revertStagedChanges;
    @Parameter(2)
    public boolean isPublished;
    @Parameter(3)
    public boolean hasStagedChanges;


    /**
     * Initialises dependencies of component under test.
     */
    @Before
    public void setUp() {
        product = mockProduct();
        service = spy(ProductService.class);
        when(service.update(any(), anyList())).thenReturn(CompletableFuture.completedFuture(product));
        when(service.revert(any())).thenReturn(CompletableFuture.completedFuture(product));
        when(service.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        productDraft = productDraft();
        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void sync_expectServiceTryFetchAndCreateNew() {
        when(service.fetch(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(service.create(any())).thenReturn(CompletableFuture.completedFuture(product));

        final ProductSync sync = new ProductSync(productSyncOptions, service);
        final ProductSyncStatistics statistics = sync.sync(singletonList(productDraft)).toCompletableFuture().join();

        verifyStatistics(statistics, 1, 0, 1);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).create(same(productDraft));
        verifyServicePublish();
        verifyNoMoreInteractions(service);
    }

    /*
    @Test
    public void sync_expectServiceFetchAndUpdateExisting() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.ofNullable(product)));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(en("name2")));
        when(ProductSyncUtils.buildActions(any(), any(), any())).thenReturn(updateActions);

        ProductSync sync = new ProductSync(productSyncOptions, service);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 1, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).update(same(product), same(updateActions));
        verifyServiceRevert();
        verifyServicePublish();
        verify(ProductSyncUtils.buildActions(same(product), same(productDraft), same(productSyncOptions)));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void sync_expectServiceFetchAndUpdateExistingAndPublish() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.of(product)));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(en("name2")));
        when(ProductSyncUtils.buildActions(any(), any(), any())).thenReturn(updateActions);

        ProductSync sync = new ProductSync(productSyncOptions, service);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 1, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).update(same(product), same(updateActions));
        verifyServiceRevert();
        verifyServicePublish();
        verify(ProductSyncUtils.buildActions(same(product), same(productDraft), same(productSyncOptions)));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void sync_expectServiceFetchAndNoUpdateExistingAsIdentical() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.of(product)));
        when(ProductSyncUtils.buildActions(any(), any(), any())).thenReturn(emptyList());

        ProductSync sync = new ProductSync(productSyncOptions, service);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, shouldBePublished() ? 1 : 0, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verifyServiceRevert();
        verifyServicePublish();
        verify(ProductSyncUtils.buildActions(same(product), same(productDraft), same(productSyncOptions)));
        verifyNoMoreInteractions(service);
    }


    private ProductSyncOptions productSyncOptions() {
        return ProductTestUtils.productSyncOptions(mock(SphereClient.class), publish, true, revertStagedChanges);
    }*/

    private void verifyServiceRevert() {
        if (revertStagedChanges && hasStagedChanges) {
            verify(service).revert(same(product));
        }
    }

    private boolean shouldBePublished() {
        return publish && (!isPublished || hasStagedChanges);
    }

    private void verifyServicePublish() {
        if (shouldBePublished()) {
            verify(service).publish(same(product));
        }
    }

    private Product mockProduct() {
        Product product = mock(Product.class);
        ProductCatalogData data = mock(ProductCatalogData.class);
        when(product.getMasterData()).thenReturn(data);
        when(data.isPublished()).thenReturn(isPublished);
        when(data.hasStagedChanges()).thenReturn(hasStagedChanges);
        return product;
    }

    private void verifyStatistics(final ProductSyncStatistics statistics,
                                  final int processed, final int updated, final int created) {
        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(processed);
        assertThat(statistics.getCreated()).isEqualTo(created);
        assertThat(statistics.getUpdated()).isEqualTo(updated);
    }

    private ProductDraft productDraft() {
        ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("productKey");
        return productDraft;
    }

}
