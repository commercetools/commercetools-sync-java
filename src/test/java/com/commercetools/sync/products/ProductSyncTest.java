package com.commercetools.sync.products;

import com.commercetools.sync.commons.actions.UpdateActionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.products.ProductTestUtils.addName;
import static com.commercetools.sync.products.ProductTestUtils.localizedString;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
public class ProductSyncTest {

    @Test
    public void sync_expectServiceTryFetchAndCreateNew() {
        ProductService serviceSpy = spy(ProductService.class);
        when(serviceSpy.fetch(any())).thenReturn(completedFuture(Optional.empty()));
        List<ProductDraft> drafts = new ArrayList<>();
        ProductDraft draftMock = mock(ProductDraft.class);
        drafts.add(draftMock);
        when(draftMock.getKey()).thenReturn("productKey");
        when(serviceSpy.create(any())).thenReturn(completedFuture(null));
        ProductSyncOptions syncOptionsMock = mock(ProductSyncOptions.class);
        UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilderSpy = spy(UpdateActionsBuilder.class);
        ProductSync sync = new ProductSync(syncOptionsMock, serviceSpy, updateActionsBuilderSpy);

        ProductSyncStatistics statistics = sync.sync(drafts).toCompletableFuture().join();

        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(drafts.size());
        assertThat(statistics.getCreated()).isEqualTo(1);
        verify(serviceSpy).fetch(eq(draftMock.getKey()));
        verify(serviceSpy).create(eq(draftMock));
        verifyNoMoreInteractions(serviceSpy);
    }

    @Test
    public void sync_expectServiceFetchAndUpdateExisting() {
        ProductService serviceSpy = spy(ProductService.class);
        Product productMock = mock(Product.class);
        when(serviceSpy.fetch(any())).thenReturn(completedFuture(Optional.of(productMock)));
        List<ProductDraft> drafts = new ArrayList<>();
        ProductDraft draftMock = mock(ProductDraft.class);
        drafts.add(draftMock);
        when(draftMock.getKey()).thenReturn("productKey");
        when(serviceSpy.update(any(), anyList())).thenReturn(completedFuture(null));
        UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilderSpy = spy(UpdateActionsBuilder.class);
        when(updateActionsBuilderSpy.buildActions(any(), any()))
                .thenReturn(singletonList(ChangeName.of(localizedString("name2"))));
        ProductSyncOptions syncOptionsMock = mock(ProductSyncOptions.class);
        ProductSync sync = new ProductSync(syncOptionsMock, serviceSpy, updateActionsBuilderSpy);

        ProductSyncStatistics statistics = sync.sync(drafts).toCompletableFuture().join();

        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(drafts.size());
        assertThat(statistics.getUpdated()).isEqualTo(1);
        verify(serviceSpy).fetch(eq(draftMock.getKey()));
        List<UpdateAction<Product>> updateActions = singletonList(ChangeName.of(localizedString("name2")));
        verify(serviceSpy).update(eq(productMock), eq(updateActions));
        verifyNoMoreInteractions(serviceSpy);
    }

    @Test
    public void sync_expectServiceFetchAndDoNotUpdateExistingAsIdentical() {
        ProductService serviceSpy = spy(ProductService.class);
        Product productMock = mock(Product.class);
        addName(productMock, null);
        when(serviceSpy.fetch(any())).thenReturn(completedFuture(Optional.of(productMock)));
        List<ProductDraft> drafts = new ArrayList<>();
        ProductDraft draftMock = mock(ProductDraft.class);
        drafts.add(draftMock);
        when(draftMock.getKey()).thenReturn("productKey");
        when(serviceSpy.update(any(), anyList())).thenReturn(completedFuture(null));
        UpdateActionsBuilder<Product, ProductDraft> updateActionsBuilderSpy = spy(UpdateActionsBuilder.class);
        when(updateActionsBuilderSpy.buildActions(any(), any())).thenReturn(emptyList());
        ProductSyncOptions syncOptionsMock = mock(ProductSyncOptions.class);
        ProductSync sync = new ProductSync(syncOptionsMock, serviceSpy, updateActionsBuilderSpy);
        ProductSyncStatistics statistics = sync.sync(drafts).toCompletableFuture().join();

        assertThat(statistics).isNotNull();
        assertThat(statistics.getProcessed()).isEqualTo(drafts.size());
        assertThat(statistics.getUpdated()).isEqualTo(0);
        verify(serviceSpy).fetch(eq(draftMock.getKey()));
        verifyNoMoreInteractions(serviceSpy);
    }

    @Test
    @Ignore
    public void getStatistics() {
    }

}
