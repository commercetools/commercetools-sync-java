package com.commercetools.sync.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.RevertStagedChanges;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    private ProductService service;
    private ProductSyncOptions productSyncOptions;

    @Before
    public void setUp() {
        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        service = new ProductServiceImpl(productSyncOptions);
    }

    @Test
    public void create() {
        final Product mock = mock(Product.class);
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        ProductDraft draft = mock(ProductDraft.class);
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        assertThat(productOptional).isNotEmpty();
        assertThat(productOptional.get()).isSameAs(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductCreateCommand.of(draft)));
    }

    @Test
    public void update() {
        Product mock = mock(Product.class);
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        List<UpdateAction<Product>> updateActions =
            singletonList(ChangeName.of(LocalizedString.of(ENGLISH, "new name")));
        Product product = service.updateProduct(mock, updateActions).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductUpdateCommand.of(mock, updateActions)));
    }

    @Test
    public void publish() {
        Product mock = mock(Product.class);
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        Product product = service.publishProduct(mock).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductUpdateCommand.of(mock, Publish.of())));
    }

    @Test
    public void revert() {
        Product mock = mock(Product.class);
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        Product product = service.revertProduct(mock).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductUpdateCommand.of(mock, RevertStagedChanges.of())));
    }

/*    @Test
    public void fetch_missing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        when(result.head()).thenReturn(Optional.empty());
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(result));

        Optional<Product> productOptional = service.fetchProduct(null).toCompletableFuture().join();

        assertThat(productOptional).isNotPresent();
    }

    @Test
    public void fetch_existing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        Product product = mock(Product.class);
        when(result.head()).thenReturn(Optional.of(product));
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(result));

        final Optional<Product> productOptional = service.fetchProduct(null).toCompletableFuture().join();

        assertThat(productOptional).hasValue(product);
    }*/

}