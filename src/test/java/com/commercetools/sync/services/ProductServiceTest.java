package com.commercetools.sync.services;

import com.commercetools.sync.products.ProductTestUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.RevertStagedChanges;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    private ProductService service;
    private SphereClient ctpClient;

    @Before
    public void setUp() {
        ctpClient = spy(SphereClient.class);
        service = ProductService.of(ctpClient);
    }

    @Test
    public void create() {
        Product mock = mock(Product.class);
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock));

        ProductDraft draft = mock(ProductDraft.class);
        Product product = service.create(draft).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(ctpClient).execute(eq(ProductCreateCommand.of(draft)));
    }

    @Test
    public void update() {
        Product mock = mock(Product.class);
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock));

        List<UpdateAction<Product>> updateActions =
            singletonList(ChangeName.of(ProductTestUtils.en("new name")));
        Product product = service.update(mock, updateActions).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(ctpClient).execute(eq(ProductUpdateCommand.of(mock, updateActions)));
    }

    @Test
    public void publish() {
        Product mock = mock(Product.class);
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock));

        Product product = service.publish(mock).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(ctpClient).execute(eq(ProductUpdateCommand.of(mock, Publish.of())));
    }

    @Test
    public void revert() {
        Product mock = mock(Product.class);
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock));

        Product product = service.revert(mock).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(ctpClient).execute(eq(ProductUpdateCommand.of(mock, RevertStagedChanges.of())));
    }

    @Test
    public void fetch_missing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        when(result.head()).thenReturn(Optional.empty());
        when(ctpClient.execute(any())).thenReturn(completedFuture(result));

        Optional<Product> productOptional = service.fetch(null).toCompletableFuture().join();

        assertThat(productOptional).isNotPresent();
    }

    @Test
    public void fetch_existing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        Product product = mock(Product.class);
        when(result.head()).thenReturn(Optional.of(product));
        when(ctpClient.execute(any())).thenReturn(completedFuture(result));

        Optional<Product> productOptional = service.fetch(null).toCompletableFuture().join();

        assertThat(productOptional).hasValue(product);
    }

}