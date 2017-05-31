package com.commercetools.sync.products;

import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.products.ProductTestUtils.join;
import static com.commercetools.sync.products.ProductTestUtils.localizedString;
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
public class ProductSyncTest {

    private ProductService service;
    private UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilderSpy;
    private ProductSync sync;

    @Before
    public void setUp() {
        service = spy(ProductService.class);
        updateActionsBuilderSpy = spy(UpdateActionsBuilder.class);
        sync = new ProductSync(mock(ProductSyncOptions.class), service, updateActionsBuilderSpy);
    }

    @Test
    public void sync_expectServiceTryFetchAndCreateNew() {
        serviceWillFetch(null);
        when(service.create(any())).thenReturn(completedFuture(null));
        ProductDraft productDraft = productDraft();

        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 0, 1);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).create(same(productDraft));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilderSpy);
    }

    @Test
    public void sync_expectServiceFetchAndUpdateExisting() {
        Product product = serviceWillFetch(mock(Product.class));
        when(service.update(any(), anyList())).thenReturn(completedFuture(null));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(localizedString("name2")));
        when(updateActionsBuilderSpy.buildActions(any(), any())).thenReturn(updateActions);
        ProductDraft productDraft = productDraft();

        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 1, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(service).update(same(product), same(updateActions));
        verify(updateActionsBuilderSpy).buildActions(same(product), same(productDraft));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilderSpy);
    }

    @Test
    public void sync_expectServiceFetchAndDoNotUpdateExistingAsIdentical() {
        Product product = serviceWillFetch(mock(Product.class));
        when(updateActionsBuilderSpy.buildActions(any(), any())).thenReturn(emptyList());
        ProductDraft productDraft = productDraft();

        ProductSyncStatistics statistics = join(sync.sync(singletonList(productDraft)));

        verifyStatistics(statistics, 1, 0, 0);
        verify(service).fetch(eq(productDraft.getKey()));
        verify(updateActionsBuilderSpy).buildActions(same(product), same(productDraft));
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(updateActionsBuilderSpy);
    }

    private Product serviceWillFetch(Product product) {
        when(service.fetch(any())).thenReturn(completedFuture(Optional.ofNullable(product)));
        return product;
    }

    private void verifyStatistics(final ProductSyncStatistics statistics, final int processed, final int updated, final int created) {
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

    @Test
    @Ignore
    public void getStatistics() {
    }

}
