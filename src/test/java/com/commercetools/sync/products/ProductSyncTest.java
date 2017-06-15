package com.commercetools.sync.products;

import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.products.ProductTestUtils.en;
import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.syncOptions;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
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
    private ProductSyncOptions syncOptions;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {true, true, true}, {true, true, false},
            {true, false, true}, {true, false, false},
            {false, true, true}, {false, true, false},
            {false, false, true}, {false, false, false}
        });
    }

    @Parameter
    public boolean publish;
    @Parameter(1)
    public boolean isPublished;
    @Parameter(2)
    public boolean hasStagedChanges;

    private ProductService service;
    private ProductUpdateActionsBuilder updateActionsBuilder;

    /**
     * Initialises dependencies of component under test.
     */
    @Before
    public void setUp() {
        product = mockProduct();
        service = spy(ProductService.class);
        when(service.publish(any())).thenReturn(completedFuture(null));
        when(service.update(any(), anyList())).thenReturn(completedFuture(product));
        updateActionsBuilder = mock(ProductUpdateActionsBuilder.class);
        productDraft = productDraft();
        syncOptions = syncOptions(publish);
    }

    @Test
    public void sync_expectServiceTryFetchAndCreateNew() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.empty()));
        when(service.create(any())).thenReturn(completedFuture(product));

        ProductSync sync = new ProductSync(syncOptions, service, updateActionsBuilder);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 0, 1);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).create(same(productDraft));
        verifyServicePublish();
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilder);
    }

    @Test
    public void sync_expectServiceFetchAndUpdateExisting() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.ofNullable(product)));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(en("name2")));
        when(updateActionsBuilder.buildActions(any(), any(), any())).thenReturn(updateActions);

        ProductSync sync = new ProductSync(syncOptions, service, updateActionsBuilder);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 1, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).update(same(product), same(updateActions));
        verifyServicePublish();
        verify(updateActionsBuilder).buildActions(same(product), same(productDraft), same(syncOptions));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilder);
    }

    @Test
    public void sync_expectServiceFetchAndUpdateExistingAndPublish() {
        Product fetched = mock(Product.class);
        when(service.fetch(any())).thenReturn(completedFuture(Optional.of(fetched)));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(en("name2")));
        when(updateActionsBuilder.buildActions(any(), any(), any())).thenReturn(updateActions);

        ProductSync sync = new ProductSync(syncOptions, service, updateActionsBuilder);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 1, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).update(same(fetched), same(updateActions));
        verifyServicePublish();
        verify(updateActionsBuilder).buildActions(same(fetched), same(productDraft), same(syncOptions));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilder);
    }

    @Test
    public void sync_expectServiceFetchAndNoUpdateExistingAsIdentical() {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.of(product)));
        when(updateActionsBuilder.buildActions(any(), any(), any())).thenReturn(emptyList());

        ProductSync sync = new ProductSync(syncOptions, service, updateActionsBuilder);
        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 0, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verifyServicePublish();
        verify(updateActionsBuilder).buildActions(same(product), same(productDraft), same(syncOptions));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilder);
    }

    private void verifyServicePublish() {
        if (publish && (!isPublished || hasStagedChanges)) {
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
